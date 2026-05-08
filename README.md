# VectorMind

**VectorMind** is a research-intelligence platform that ingests multi-source AI research signals, ranks emerging techniques, generates product blueprints, and produces executable ML pipeline notebooks with Colab integration. This repository contains the production **FastAPI** backend, **Jetpack Compose** Android client, deployment automation, and hackathon submission artefacts.

| Aspect | Description |
| --- | --- |
| **Backend** | FastAPI, multi-agent orchestration, embeddings, vector search, LLM-assisted blueprints, Telegram delivery |
| **Mobile** | Kotlin, Compose, Material 3, MVVM, Hilt, Retrofit, Room, DataStore, WorkManager |
| **Hosted API** | Public Space deployment (see [Live backend](#live-backend) below) |
| **Judges** | Pre-built APK with production `BACKEND_URL`; optional local run via Docker Compose |

---

## Demo video

The walkthrough below plays directly in the GitHub README in most desktop browsers. If playback is unavailable in your viewer, use the download link in the [Submission bundle](#submission-bundle) table.

<video controls width="100%" playsinline preload="metadata" src="https://raw.githubusercontent.com/MK23IS092/msrit_clockwork/main/product_demo.mp4">
  <a href="https://raw.githubusercontent.com/MK23IS092/msrit_clockwork/main/product_demo.mp4">Download product_demo.mp4</a>
</video>

---

## Submission bundle

| Deliverable | Access |
| --- | --- |
| **Android APK** (signed `v1.0.1` release; hosted backend URL baked in at compile time) | [Download `vectormind-judges.apk` (v1.0.1)](https://github.com/MK23IS092/msrit_clockwork/releases/download/v1.0.1/vectormind-judges.apk) |
| **APK (in-tree mirror)** | [`dist/vectormind-judges.apk`](https://github.com/MK23IS092/msrit_clockwork/raw/main/dist/vectormind-judges.apk) |
| **Full source snapshot** (ZIP, includes repository tree and deliverables) | [v1.0.1 source archive](https://github.com/MK23IS092/msrit_clockwork/archive/refs/tags/v1.0.1.zip) |
| **Demo video** (MP4) | [Stream / download (raw)](https://raw.githubusercontent.com/MK23IS092/msrit_clockwork/main/product_demo.mp4) |
| **Presentation** | [Clockwork_MSRIT.pdf](./Clockwork_MSRIT.pdf) |
| **AI disclosure** | [OpenClaw_AI_Disclosure (1) (1).docx](./OpenClaw_AI_Disclosure%20%281%29%20%281%29.docx) |
| **Telegram bot** | [t.me/VectormindsBot](https://t.me/VectormindsBot) |
| **Live backend** | [teletubbies-zamzung.hf.space](https://teletubbies-zamzung.hf.space) |
| **This README** | [README.md](./README.md) |

### Installing the APK (Android)

1. On the target device, open the [v1.0.1 APK download](https://github.com/MK23IS092/msrit_clockwork/releases/download/v1.0.1/vectormind-judges.apk) link.
2. Approve installation from the browser or file manager when prompted (unknown sources).
3. Launch the app. No manual API URL entry is required for the judge build; it targets the hosted backend below.

### Trying the Telegram bot

| Step | Action |
| --- | --- |
| 1 | Open [t.me/VectormindsBot](https://t.me/VectormindsBot) on a device that has Telegram installed. |
| 2 | Tap **Start**, or send `/start` to subscribe and see the command menu. |
| 3 | Use `/status`, `/trends`, `/pipelines`, `/help`, or `/unsubscribe`. Outbound alerts (high-impact trends, ingestion summaries, pipeline completions) broadcast automatically to subscribers. |

---

## Live backend

| Endpoint | URL |
| --- | --- |
| **Base** | `https://teletubbies-zamzung.hf.space` |
| **Health** | [`GET /health`](https://teletubbies-zamzung.hf.space/health) |
| **OpenAPI** | [`/docs`](https://teletubbies-zamzung.hf.space/docs) |

---

## Repository layout

| Path | Purpose |
| --- | --- |
| `backend/` | FastAPI application, agents, ingestion, intelligence engines, tests |
| `android/` | Gradle multi-module Android app (`app`, `core:network`, `core:data`, `core:skills`, `core:service`) |
| `dist/vectormind-judges.apk` | Shareable debug APK aligned with hosted backend |
| `product_demo.mp4` | Screen recording for reviewers |
| `docker-compose.yml` | Local PostgreSQL, Redis, Kafka (optional full stack) |
| `docs/DEPLOY.md` | Hugging Face Spaces and GitHub Actions deployment |
| `docs/FEATURE_MATRIX.md` | Capability checklist |
| `.github/workflows/` | CI: backend tests, Android build, HF deploy |

---

## Quick start (developers)

### Backend (local)

```bash
cd backend
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload
```

Default local URL: `http://127.0.0.1:8000`.

### Android (local build)

```bash
cd android
chmod +x gradlew   # Linux / macOS only
./gradlew :app:assembleDebug
```

Override the API base URL in `android/local.properties` if needed:

```properties
BACKEND_URL=http://10.0.2.2:8000
```

The emulator maps `10.0.2.2` to the host loopback interface.

### Full local stack (optional)

```bash
docker compose up -d
```

Then align `backend/.env` with `backend/.env.example` (Postgres, Redis, Kafka). Kafka UI is typically exposed at `http://localhost:8085` when enabled in your compose profile.

---

## Configuration (backend)

| Variable | Role |
| --- | --- |
| `GEMINI_API_KEY` | Real LLM output for blueprints and briefs |
| `USE_MOCK_LLM` | Set `false` in production or hosted demos |
| `GITHUB_TOKEN` | Gist publishing for Colab-linked notebooks |
| `HUGGINGFACE_TOKEN` | Dataset metadata enrichment |
| `KAGGLE_USERNAME` / `KAGGLE_KEY` | Kaggle dataset search |
| `TELEGRAM_BOT_TOKEN` | Telegram bot (long polling; single active poller per token) |
| `TELEGRAM_ENABLE_POLLING` | Default `true`; set `false` on secondary instances to avoid `getUpdates` conflicts |
| `DB_BACKEND`, `POSTGRES_*`, `REDIS_URL`, `MESSAGE_BUS_BACKEND`, `KAFKA_*` | Optional distributed backends via Docker |

Subscribers are persisted when users send `/start` to the bot; `TELEGRAM_CHAT_ID` is optional for broadcasts.

---

## Telegram bot

The production bot is live at [**t.me/VectormindsBot**](https://t.me/VectormindsBot). Tap the link on any Telegram client and press **Start** to subscribe.

| Command | Behaviour |
| --- | --- |
| `/start` | Subscribe and show help |
| `/help` | Command reference |
| `/status` | Live platform statistics (signals, trends, pipelines, agent state) |
| `/trends` | Top ranked emerging techniques |
| `/pipelines` | Recent pipelines with Colab links where available |
| `/unsubscribe` | Remove subscription |

Outbound alerts (high-impact trends, ingestion summaries, pipeline completions) broadcast to every subscriber. With no token configured, delivery is a no-op; the hosted Space ships with `TELEGRAM_BOT_TOKEN` already wired through Space secrets.

---

## Colab pipeline publishing

`POST /api/pipelines/generate` materialises a structured notebook and, when `GITHUB_TOKEN` is set, publishes a public Gist. The JSON response includes `colab_url` and structured `metrics.colab_gist`. If GitHub is unavailable, the API still returns a usable Colab entry path.

---

## Testing

| Layer | Command / location |
| --- | --- |
| Backend unit and integration | `cd backend && pytest -q` |
| Android unit | `android/app/src/test/`, `android/core/skills/src/test/` |
| Android instrumented | `android/app/src/androidTest/` |
| CI | Workflows under `.github/workflows/` |

---

## Dataset preview API

`POST /api/pipelines/dataset-candidates` accepts a technique name, description, `task_type`, and `top_k` to return ranked dataset suggestions before full pipeline generation.

---

## Troubleshooting

| Symptom | Mitigation |
| --- | --- |
| Emulator cannot reach host `localhost` | Use `http://10.0.2.2:8000` in `BACKEND_URL` |
| Custom backend URL | Set `BACKEND_URL` in `android/local.properties` and rebuild |
| Gradle JDK errors | Use Temurin JDK 17 or 21 for the Gradle daemon |
| Telegram `409 Conflict` | Only one process may call `getUpdates` per bot token; stop duplicate backends or set `TELEGRAM_ENABLE_POLLING=false` on non-primary hosts |
| Telegram timeout on cold start | Hosted deployments retry with extended HTTP timeouts; ensure `TELEGRAM_BOT_TOKEN` is set as a Space secret |

---

## Licence and attribution

Submit your licence choice in the repository settings or a root `LICENSE` file if required by the hackathon. VectorMind integrates third-party APIs and models (Gemini, Hugging Face, Telegram, GitHub, Kaggle) subject to their respective terms of use.
