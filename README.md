# AI LoL Drafting Coach

A local-only school project that teaches League of Legends drafting.
Pick your role, the AI generates a realistic draft around you, you pick a
champion, and the AI coach explains what's good, what's bad, and suggests
stronger alternatives.

Pattern taken from the `chatgpt-jokes` example: Spring Boot + WebClient to
call the OpenAI API + static HTML / CSS / JavaScript frontend served from the
same Spring Boot app.

## Run it

1. Get an OpenAI API key from <https://platform.openai.com/>.
2. Set the env var and start the app:

   ```powershell
   # Windows
   $env:API_KEY = "sk-..."
   .\mvnw spring-boot:run
   ```

   ```bash
   # macOS / Linux
   export API_KEY=sk-...
   ./mvnw spring-boot:run
   ```

3. Open <http://localhost:8080>.

4. **Drop your Summoner's Rift map image** at
   `src/main/resources/static/img/rift.jpg` so the home screen shows it behind
   the role buttons.

## Endpoints

| Method | Path                  | Body                   | Returns         |
| ------ | --------------------- | ---------------------- | --------------- |
| GET    | `/api/v1/champions`   | -                      | `Champion[]`    |
| POST   | `/api/v1/draft`       | `{"role":"JGL"}`       | `DraftResponse` |
| POST   | `/api/v1/coach`       | `CoachRequest`         | `CoachResponse` |

## Project layout

```
src/main/java/org/example/ailoldraftingcheck/
├── AiLolDraftingCheckApplication.java
├── api/
│   ├── ChampionController.java   GET /api/v1/champions
│   ├── DraftController.java      POST /api/v1/draft
│   └── CoachController.java      POST /api/v1/coach
├── dtos/
│   ├── ChatCompletionRequest.java   (OpenAI request shape)
│   ├── ChatCompletionResponse.java  (OpenAI response shape)
│   ├── Champion.java
│   ├── DraftPick.java
│   ├── DraftResponse.java
│   ├── CoachRequest.java
│   └── CoachResponse.java
└── service/
    ├── OpenAiService.java         (WebClient wrapper around ChatGPT)
    └── DataDragonService.java     (loads champion list from Riot)

src/main/resources/
├── application.properties
└── static/
    ├── index.html                 (Bootstrap 5 + 3 views)
    ├── css/main.css
    ├── img/rift.jpg               (YOU add this)
    └── js/
        ├── state.js               (global state + view switching)
        ├── api.js                 (fetch wrappers)
        ├── teamRender.js          (HTML for team columns)
        ├── home.js                (role buttons)
        ├── draft.js               (draft view + autocomplete)
        ├── coach.js               (coach feedback view)
        └── main.js                (boot: wires everything up)
```
