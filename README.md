# AI LoL Drafting Coach

A school project that teaches League of Legends drafting. You pick your role,
the AI generates a realistic draft around you, you pick a champion, and an
AI coach explains what's good, what's bad, and suggests stronger alternatives.

## Architecture at a glance

```
Browser (static HTML + JS)
        |
        v
Spring Boot 4 REST API
  ├── /api/v1/champions    -> Data Dragon (Riot, free)
  ├── /api/v1/draft        -> ChatGPT (OpenAI API)
  └── /api/v1/coach        -> ChatGPT + op.gg scrape (best effort)
        |
        v
H2 in-memory DB (token usage log)
```

## Run it

1. Get an OpenAI API key from <https://platform.openai.com/>.
2. Set the env var, then start the app:

   ```powershell
   # Windows PowerShell
   $env:API_KEY = "sk-..."
   .\mvnw spring-boot:run
   ```

   ```bash
   # macOS / Linux
   export API_KEY=sk-...
   ./mvnw spring-boot:run
   ```

3. Open <http://localhost:8080>.

## Endpoints

| Method | Path                          | Body / Params             | Returns         |
| ------ | ----------------------------- | ------------------------- | --------------- |
| GET    | `/api/v1/champions`           | -                         | `Champion[]`    |
| GET    | `/api/v1/champions/version`   | -                         | Data Dragon patch |
| POST   | `/api/v1/draft`               | `{"role":"JGL"}`          | `DraftResponse` |
| POST   | `/api/v1/coach`               | `CoachRequest`            | `CoachResponse` |

`POST` endpoints are IP-rate limited: 3 requests, refilled every 2 minutes.

## Configuration (`application.properties`)

| Key                            | What it does                                           |
| ------------------------------ | ------------------------------------------------------ |
| `app.api-key`                  | OpenAI key (read from `API_KEY` env var)               |
| `app.model`                    | OpenAI model, default `gpt-4o-mini`                    |
| `app.temperature`              | LLM randomness (0.7)                                   |
| `app.opgg.scrape-enabled`      | Toggle op.gg scraping on/off                           |
| `app.opgg.cache-ttl-hours`     | How long scrape results are cached (24h)               |
| `app.bucket_capacity`          | Rate-limit bucket size (3)                             |
| `app.refill_amount`            | Bucket refill amount (3)                               |
| `app.refill_time`              | Bucket refill period in minutes (2)                    |

## Things to know

**OP.GG scraping is best effort.** op.gg renders most content via JavaScript
that Jsoup cannot execute, and they change their HTML often. The scraper tries,
caches the result, and gracefully falls back to LLM-only reasoning when it
finds nothing. The coach response tells you which path was used via the
`dataSource` field.

**The LLM must return strict JSON.** Both the draft and coach endpoints ask
ChatGPT for JSON only and parse it. If the model hallucinates a champion that
doesn't exist, the draft endpoint backfills from the real Data Dragon list so
you always get a complete team.

**Token usage is logged.** Every ChatGPT call writes a row to H2. Browse
<http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:draftdb`, user `sa`,
no password) to see spending over a session.

## Project layout (mirrors `chatgpt-jokes` example)

```
src/main/java/org/example/ailoldraftingcheck/
├── AiLolDraftingCheckApplication.java
├── api/                     # REST controllers
│   ├── ChampionController.java
│   ├── DraftController.java
│   ├── CoachController.java
│   └── RateLimit.java       # shared bucket4j helper
├── dtos/                    # request/response shapes
├── entity/                  # JPA entity for token-usage logging
└── service/
    ├── OpenAiService.java   # ChatGPT wrapper
    ├── DataDragonService.java
    └── OpGgScraperService.java

src/main/resources/
├── application.properties
└── static/                  # served by Spring at /
    ├── index.html
    ├── css/main.css
    └── js/main.js
```
