# PvP Quiz — Specifikation for AILolDraftingCheck

Jeg har et eksisterende Java/Spring Boot projekt kaldet "AILolDraftingCheck" som er en AI-drevet drafting coach til League of Legends. Projektet er allerede deployed på Render via Docker og bruger OpenAI API (GPT-4o-mini) til at generere drafts og coaching-feedback.

Jeg vil udvide projektet med en **PvP Quiz-mode** hvor to spillere kan konkurrere mod hinanden om hvem der har den bedste drafting-viden. Derudover vil jeg tilføje et **pointsystem** baseret på rigtige statistikker fra OP.GG, samt vise **winrate-data** for alternative champions.

---

## NUVÆRENDE PROJEKTSTRUKTUR

```
AILolDraftingCheck/
├── pom.xml
├── Dockerfile
├── mvnw / mvnw.cmd
├── src/main/java/org/example/ailoldraftingcheck/
│   ├── AiLolDraftingCheckApplication.java
│   ├── api/
│   │   ├── ChampionController.java
│   │   ├── CoachController.java
│   │   └── DraftController.java
│   ├── dtos/
│   │   ├── Champion.java
│   │   ├── ChatCompletionRequest.java
│   │   ├── ChatCompletionResponse.java
│   │   ├── CoachRequest.java
│   │   ├── CoachResponse.java
│   │   ├── DataDragonResponse.java
│   │   ├── DraftPick.java
│   │   └── DraftResponse.java
│   └── service/
│       ├── CoachService.java
│       ├── DataDragonService.java
│       ├── DraftService.java
│       └── OpenAiService.java
├── src/main/resources/
│   ├── application.properties
│   └── static/
│       ├── index.html
│       ├── img/rift.jpg
│       ├── css/main.css
│       └── js/
│           ├── config.js
│           ├── draft.champion.placeholder.js
│           ├── utils.js
│           ├── api.js
│           ├── champ.pick.js
│           ├── team.lineup.js
│           ├── role.picker.js
│           ├── champ.search.bar.js
│           ├── draft.page.js
│           ├── coach.feedback.js
│           ├── coach.js
│           └── app.js
```

**Teknologi-stack:** Java 25, Spring Boot 4.0.5, Maven, Lombok, WebClient (til OpenAI API), Docker, Render.

**Vigtige detaljer om eksisterende kode:**
- `OpenAiService.java` håndterer alle kald til OpenAI API via WebClient. API-nøglen læses fra environment variablen `API_KEY`.
- `DraftService.java` genererer drafts ved at sende en system-prompt til OpenAI der returnerer JSON med enemy (5 picks) og ally (4 picks, ekskl. brugerens rolle).
- `CoachService.java` analyserer brugerens champion-valg og returnerer positives, negatives, og 3 alternative champions.
- `DataDragonService.java` henter champion-data fra Riot's Data Dragon API ved opstart og cacher det.
- Frontenden er vanilla JavaScript med en komponent-baseret arkitektur (rolePicker → draftPage → coachPage).

---

## DEL 1 — PVP QUIZ: NAVIGATION

Tilføj en PvP Quiz-knap i navigationsbjælken.

### Ændringer i index.html
Tilføj en knap/link i højre side af den eksisterende `<nav>`:
- Venstre side: eksisterende logo "AI LoL Drafting Coach" (uændret)
- Højre side: knap "PvP Quiz" der linker til `pvp.html`

### Ny fil: pvp.html
Opret `pvp.html` i `src/main/resources/static/` med:
- Samme `<nav>` som index.html, men med en "Solo Quiz" knap i højre side der linker tilbage til `index.html`
- Samme CSS-fil (`css/main.css`) og samme grundstruktur

---

## DEL 2 — PVP QUIZ: WEBSOCKET BACKEND

Brug **Spring WebSocket** (STOMP over SockJS) til real-time kommunikation. Tilføj til `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 2a — WebSocket-konfiguration

Opret `config/WebSocketConfig.java`:
- Aktivér STOMP message broker med `@EnableWebSocketMessageBroker`
- Registrer STOMP endpoint: `/ws` med SockJS fallback
- Konfigurer message broker med prefix `/topic` (broadcasts) og `/queue` (personlige beskeder)
- Application destination prefix: `/app`

### 2b — PvP Game Service

Opret `service/PvpGameService.java` med følgende logik:

**Data-strukturer:**
- `waitingPlayers`: ConcurrentLinkedQueue der holder spillere der søger match. Hver entry har: WebSocket session-ID og brugernavn.
- `activeGames`: ConcurrentHashMap<String, PvpGame> der holder aktive spil. Key er et rum-ID (f.eks. "DRAFT-" + 4 tilfældige tegn).
- `PvpGame` indeholder: roomId, player1 (session + username), player2 (session + username), tildelt rolle, draft (enemy + ally teams), player1Choice, player2Choice, countdown-status.

**Logik:**

EVENT: `find-match`
- Modtages fra klient via STOMP når spiller trykker "Find match"
- Payload: `{ username: string }`
- Tilføj spilleren til ventelisten
- Send bekræftelse til spilleren: "Søger efter modstander..."
- Hvis ventelisten har 2 eller flere spillere:
  - Tag de første 2 spillere ud af ventelisten
  - Generer et unikt rum-ID
  - Vælg en tilfældig rolle fra [TOP, JGL, MID, ADC, SUPP]
  - Kald OpenAI via den eksisterende `DraftService` for at generere et draft (5 enemy, 4 ally ekskl. den tilfældige rolle)
  - Send til begge spillere via deres personlige WebSocket-kanal: `{ roomId, role, enemyTeam, allyTeam, player1Username, player2Username }`
  - Start countdown (se DEL 3)

EVENT: `champion-selected`
- Modtages fra klient når en spiller vælger en champion
- Payload: `{ roomId: string, champion: string }`
- Gem valget i `PvpGame` under den rigtige spiller (match på session-ID)
- Send IKKE valget til modstanderen
- Hvis begge spillere har valgt: notér det, men vent stadig på countdown

EVENT: `disconnect`
- Når en spiller afbryder: fjern fra ventelisten hvis de venter
- Hvis de er i et aktivt spil: notificer modstanderen

### 2c — PvP Controller

Opret `api/PvpController.java` med `@MessageMapping` endpoints:
- `/app/pvp/find-match` → kalder PvpGameService
- `/app/pvp/champion-selected` → kalder PvpGameService

---

## DEL 3 — COUNTDOWN OG REVEAL

### Server-side countdown
Når et match er fundet og draft er sendt til begge spillere:
- Start en 60-sekunders countdown på serveren (brug `ScheduledExecutorService` eller Spring's `@Scheduled`)
- Send hvert sekund til begge spillere i rummet: `{ seconds: N }`
- Når countdown rammer 0:
  1. Hvis en spiller IKKE har valgt en champion: de får 0 point og feedback "Du valgte ikke en champion inden for tidsfristen"
  2. Send `reveal` til begge spillere med begge valg: `{ player1: { username, champion }, player2: { username, champion } }`
  3. For hver spiller der har valgt: kald OP.GG MCP og OpenAI for at beregne point og coaching (se DEL 5 og 6)
  4. Send `results` til begge spillere: `{ player1: { feedback, points }, player2: { feedback, points } }`

---

## DEL 4 — PVP FRONTEND (pvp.html)

### JavaScript-filer til PvP
Opret nye JavaScript-filer i `js/` mappen:
- `pvp.js` — hovedlogik for PvP-flowet
- `pvp.socket.js` — WebSocket-forbindelse og event-handling

### Klient-side dependencies
Tilføj SockJS og STOMP klient via CDN i pvp.html:
```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
```

### Tre tilstande i pvp.html:

**TILSTAND 1 — Matchmaking**
Vises når siden indlæses.
- Tekstfelt til brugernavn (påkrævet)
- Knap: "Find Match"
- Statusbesked der vises efter knappen trykkes: "Søger efter modstander..."
- Knappen disables mens der søges

**TILSTAND 2 — Draft og Champion-valg**
Vises når `match-found` event modtages fra serveren.
- Øverst: Countdown-timer (stort, tydeligt, starter ved 60 sekunder)
- To kolonner side om side:
  - Venstre kolonne: "[Spiller 1's navn]'s Draft"
  - Højre kolonne: "[Spiller 2's navn]'s Draft"
- Begge kolonner viser **identisk** draft:
  - Enemy team: 5 champion-ikoner vandret med navne nedenunder
  - "VS" separator i midten
  - Ally team: 4 champion-ikoner vandret med navne nedenunder
  - Tom plads til brugerens eget valg (den tildelte rolle)
- Under draftet: Champion-søgefelt (genbruger eksisterende `championSearch`-logik)
- Spillerens eget valg vises KUN i deres egen kolonne — modstanderens valg vises som "?" indtil countdown udløber

**TILSTAND 3 — Resultater**
Vises når `reveal` og `results` events modtages.
- Øverst: Vinderannoncering — "[Navn] vandt med [X] point!" (eller "Uafgjort!")
- To kolonner side om side med begge spilleres:
  - Valgte champion (ikon + navn). Hvis ikke valgt: "Ingen champion valgt"
  - Point vist som "###pts" (se DEL 5 for farver)
  - Coaching feedback: "What works" og "What doesn't work" lister
  - Alternative champions med winrate-data (se DEL 6)

---

## DEL 5 — POINTSYSTEM VIA OP.GG MCP

### OP.GG MCP konfiguration
- Server URL: `https://mcp-api.op.gg/mcp`
- Ingen API-nøgle kræves
- Tilgås via HTTP POST fra server.js (MCP protocol)

### Ny service: OpggService.java
Opret `service/OpggService.java` der kommunikerer med OP.GG's MCP endpoint.

MCP-kald sker via HTTP POST til `https://mcp-api.op.gg/mcp` med JSON-RPC format:
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "lol-champion-analysis",
    "arguments": { "champion_name": "Lulu", "role": "MID" }
  },
  "id": 1
}
```

Relevante MCP tools:
1. **`lol-champion-analysis`** — Henter detaljeret champion-analyse inklusiv winrates mod specifikke champions
2. **`lol-champion-meta-data`** — Henter meta-data inklusiv "works with" (synergier) og "strong against" (counters)

### Pointberegning
Point beregnes for hver spiller individuelt på en skala fra 0-100.

Beregningen baseres på 3 parametre:

**1. Counterpick (60% vægt)**
- Hent brugerens champions winrate direkte mod enemy laner fra OP.GG MCP
- Eksempel: MID Lulu mod MID Syndra — hvad er Lulus winrate mod Syndra?
- Brug `lol-champion-analysis` tool
- Konverter winrate til point: 50% winrate = 50 point, 55% = 75 point, 45% = 25 point (lineær skalering)

**2. Synergi med ally team (20% vægt)**
- Hent brugerens champions "works with" statistik fra OP.GG MCP
- Sammenlign med de 4 ally champions i draftet
- Jo flere af ens allies der optræder i "works with" listen, jo højere score
- Brug `lol-champion-meta-data` tool

**3. Counter mod enemy team (20% vægt)**
- Hent brugerens champions "strong against" statistik fra OP.GG MCP
- Sammenlign med de 5 enemy champions i draftet
- Jo flere af ens enemies der optræder i "strong against" listen, jo højere score
- Brug `lol-champion-meta-data` tool

### OpenAI beregner endelig score
Send de tre datapunkter (counterpick winrate, synergi-matches, counter-matches) til OpenAI. OpenAI modtager rå data og returnerer:
- En samlet score fra 0-100
- Coaching-feedback (positives, negatives, alternatives)

### UI for point-visning
Point vises som `###pts` med farvekodning:
- 0-40: Rød (`#dc3545`)
- 41-70: Gul/orange (`#ffc107`)
- 71-100: Grøn (`#198754`)

---

## DEL 6 — WINRATE PÅ ALTERNATIVER

### Ændring i CoachService.java
Når OpenAI foreslår alternative champions i coaching-feedbacken, skal serveren for hvert alternativ:
1. Kalde OP.GG MCP (`lol-champion-analysis`) for at hente alternativets winrate mod enemy laner
2. Inkludere winrate i response-objektet

### Ændring i CoachResponse.java
Tilføj et `winrate` felt til `Alternative` klassen:
```java
private double winrate; // f.eks. 54.3
```

### UI-visning af alternativer (gælder både solo og PvP)
Under hvert alternativ tilføjes en linje:
```
"Vi — 54.3% winrate mod [enemy laner champion navn]"
```

Maksimalt 3 alternativer vises (uændret fra nuværende logik).

---

## DEL 7 — CSS TILFØJELSER

Tilføj følgende CSS til `main.css`:

- `.nav-link` — styling til PvP Quiz / Solo Quiz knap i navbar
- `.pvp-container` — wrapper for PvP-flowet
- `.matchmaking-form` — brugernavn-input og Find Match knap
- `.countdown-timer` — stort, centreret countdown-display
- `.pvp-drafts` — flex-container med to kolonner for side-om-side drafts
- `.pvp-draft-col` — individuel kolonne med spillernavn og draft
- `.pvp-results` — resultat-sektion med vinderannoncering
- `.points-display` — stort tal med farvekodning (rød/gul/grøn)
- `.winner-banner` — fremhævet vinderbesked øverst

---

## TEKNISKE NOTER

**OpenAI API (uændret):**
- Model: gpt-4o-mini
- Max tokens: 900
- API endpoint: https://api.openai.com/v1/chat/completions
- Environment variable: `API_KEY`

**OP.GG MCP:**
- Server URL: https://mcp-api.op.gg/mcp
- Protokol: JSON-RPC 2.0 over HTTP POST
- Ingen API-nøgle kræves
- Tilgås fra backend (Java) via WebClient

**Riot Data Dragon (uændret):**
- Champions hentes dynamisk med seneste patch-version
- Ikoner: `https://ddragon.leagueoflegends.com/cdn/{patch}/img/champion/{ChampionId}.png`

**WebSocket:**
- Protokol: STOMP over SockJS
- Endpoint: `/ws`
- Klient-biblioteker via CDN: sockjs-client + stompjs

**Docker/Render:**
- Eksisterende Dockerfile er allerede konfigureret til Java 25 med multi-stage build
- Ingen ændringer nødvendige til Dockerfile
- Ny environment variable kræves IKKE (OP.GG MCP har ingen nøgle)
