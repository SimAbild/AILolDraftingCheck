# AI LoL Drafting Coach 🎮🤖

An AI-powered analysis tool for **League of Legends** drafting. You pick your role, the AI builds a realistic 5v5 draft around you, you choose a champion, and an AI coach explains the **strengths and weaknesses** of your pick and suggests **3 stronger alternatives**.

Self-chosen school project at the Computer Science AP Degree (Datamatiker), Erhvervsakademi København (EK).

**🔗 Live demo:** <https://ailoldraftingcheck.onrender.com/> (hosted on Render's free tier, so the first load can take up to a minute while the server wakes up)

## Purpose

The goal was to learn how to **integrate a large language model (LLM) into a Java/Spring Boot backend**, from building the request and writing the prompts to parsing structured output back into Java objects.

A key design choice was to give the AI **official champion data from Riot's Data Dragon API** together with general drafting principles, instead of relying on scraped third-party statistics. That made the project a practical exercise in **prompt engineering**: the quality of the feedback depends on what context and instructions the model is given.

## Features

- **Role picker**: choose your role on Summoner's Rift (Top, Jungle, Mid, ADC, Support)
- **AI-generated draft**: the AI creates a realistic 5v5 draft with your role left open
- **Champion search**: autocomplete over every champion, loaded from Riot's Data Dragon API
- **AI coach feedback**: strengths, weaknesses and 3 alternative picks for your choice
- **Structured output**: the model answers in JSON mode, and the backend maps the reply straight to Java DTOs

## Tech stack

| Area | Technology |
|---|---|
| Backend | Java 25, Spring Boot, Spring WebFlux `WebClient` |
| AI | OpenAI Chat Completions API (`gpt-4o-mini`, JSON mode) |
| External data | Riot Data Dragon API |
| Frontend | HTML, CSS, Bootstrap 5, vanilla JavaScript (ES modules) |
| Deployment | Docker (multi-stage build), Render |

## Architecture

```
Browser (HTML / JS)
   │  GET  /api/v1/champions
   │  POST /api/v1/draft
   │  POST /api/v1/coach
   ▼
Controllers (api/)
   ▼
Services
   ├─ DataDragonService ──► Riot Data Dragon (champion list, loaded at startup)
   ├─ DraftService ─┐
   └─ CoachService ─┴─► OpenAiService ──► OpenAI API (JSON response → DTO)
```

All business logic lives in the service layer. The controllers only handle HTTP, and DTOs describe both the public API and the OpenAI request/response format.

## Getting started

**Requirements:** Java 25 and an OpenAI API key (<https://platform.openai.com/>)

```powershell
# Windows (PowerShell)
$env:API_KEY = "sk-..."
.\mvnw spring-boot:run
```

```bash
# macOS / Linux
export API_KEY=sk-...
./mvnw spring-boot:run
```

Then open <http://localhost:8080>.

**With Docker:**

```bash
docker build -t lol-drafting-coach .
docker run -p 8080:8080 -e API_KEY=sk-... lol-drafting-coach
```

## API

| Method | Endpoint | Body | Returns |
|---|---|---|---|
| `GET` | `/api/v1/champions` | – | `Champion[]` |
| `POST` | `/api/v1/draft` | `{"role":"JGL"}` | `DraftResponse` |
| `POST` | `/api/v1/coach` | `CoachRequest` | `CoachResponse` |
