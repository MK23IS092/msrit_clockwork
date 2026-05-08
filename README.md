# VectorMind

VectorMind is a research-intelligence platform with:

- a Python FastAPI backend (`backend/`) for ingestion, ranking, blueprints, and pipeline generation
- an Android client (`android/`) for dashboard, trends, blueprints, and pipeline views

## Hackathon Submission — Quick Links

| Deliverable | Link |
| --- | --- |
| **Android APK** (60 MB, hosted backend URL baked in) | [Direct download](https://github.com/MK23IS092/msrit_clockwork/raw/main/dist/vectormind-judges.apk) |
| **Full source archive** (zip — includes code + APK) | [v1.0.0.zip](https://github.com/MK23IS092/msrit_clockwork/archive/refs/tags/v1.0.0.zip) |
| **Demo video** | [`product_demo.mp4`](./product_demo.mp4) |
| **Presentation** | [`Clockwork_MSRIT.pdf`](./Clockwork_MSRIT.pdf) |
| **AI Disclosure** | [`OpenClaw_AI_Disclosure (1) (1).docx`](./OpenClaw_AI_Disclosure%20%281%29%20%281%29.docx) |
| **Hosted backend (live)** | https://teletubbies-zamzung.hf.space — see [`/health`](https://teletubbies-zamzung.hf.space/health) and [`/docs`](https://teletubbies-zamzung.hf.space/docs) |

### Installing the APK on Android

1. On your Android phone, open the [Direct download](https://github.com/MK23IS092/msrit_clockwork/raw/main/dist/vectormind-judges.apk) link.
2. Allow the browser/file manager to install from unknown sources when prompted.
3. Open **VectorMind**. The app talks to the hosted backend automatically — no manual configuration needed.

## Repository Layout

- `backend/` FastAPI app, agents, ingestion, vector search, intelligence engines
- `android/` multi-module Android app (`app`, `core:network`, `core:data`, `core:skills`, `core:service`)
- `dist/vectormind-judges.apk` ready-to-install Android build with `BACKEND_URL` baked in
- `Clockwork_MSRIT.pdf` submission presentation
- `OpenClaw_AI_Disclosure (1) (1).docx` AI disclosure document
- `product_demo.mp4` walkthrough video
- `docker-compose.yml` Postgres + Redis + Kafka stack for local development
- `docs/DEPLOY.md` Hugging Face + GitHub Actions deployment guide
- `docs/FEATURE_MATRIX.md` feature checklist

## Quick Start

### Backend

```bash
cd backend
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload
```

Backend runs on `http://127.0.0.1:8000`.

### Android

```bash
cd android
./gradlew :app:assembleDebug
```

Set `BACKEND_URL` in `android/local.properties` if not using emulator default:

```properties
BACKEND_URL=http://10.0.2.2:8000
```

## Environment Variables (Backend)

Common keys used by backend modules:

- `GEMINI_API_KEY` — required for real LLM blueprint and brief generation
- `GITHUB_TOKEN` — required for publishing notebooks as Colab-ready Gists
- `HUGGINGFACE_TOKEN` — improves dataset metadata fetch
- `KAGGLE_USERNAME` / `KAGGLE_KEY` — Kaggle dataset search
- `TELEGRAM_BOT_TOKEN` — enables real two-way Telegram bot
- `ENABLE_PATENTS_REAL`, `ENABLE_STARTUPS_REAL`, `ENABLE_SOCIAL_REAL`, `ENABLE_BLOG_REAL`, `ALLOW_SIMULATED_SOURCES` — ingestion source switches
- `DB_BACKEND=postgres`, `POSTGRES_DSN`, `STATE_STORE_BACKEND=redis`, `REDIS_URL`, `MESSAGE_BUS_BACKEND=kafka_mirror`, `KAFKA_BOOTSTRAP_SERVERS` — infra wiring (use `docker-compose.yml`)

`TELEGRAM_CHAT_ID` is no longer required: the bot persists subscribers (chat ids) via `/start` in the `telegram_subscribers` table and broadcasts alerts to all of them.

## Telegram Bot (production)

The backend embeds a `python-telegram-bot` long-polling client started in the FastAPI lifespan. The bot supports:

- `/start` — subscribe and show command help
- `/help` — show command help
- `/status` — live platform stats (signals, trends, pipelines, agent status)
- `/trends` — top 5 emerging techniques
- `/pipelines` — recent pipelines (with Colab links)
- `/unsubscribe` — stop receiving alerts

Outbound alerts (high-impact trend, ingestion summary, training pipeline ready) are broadcast to every subscriber. There is no mock fallback: when `TELEGRAM_BOT_TOKEN` is unset the bot is a no-op; when set, every send is a real Telegram API call.

To verify: open the bot in Telegram, send `/start`, then `python backend/scripts/check_subscribers.py` to confirm persistence.

## Colab pipeline publishing

`POST /api/pipelines/generate` builds a real `.ipynb` notebook (split into NLP / vision / audio / tabular cells) and pushes it to a public GitHub Gist via `GITHUB_TOKEN`. The response includes:

- `colab_url` — `https://colab.research.google.com/gist/<owner>/<gist_id>/<file>.ipynb`
- `metrics.colab_gist` — `{owner, gist_id, gist_url, filename}`

If `GITHUB_TOKEN` is missing or GitHub is unreachable, the pipeline still returns a generic Colab create URL so callers always have a usable handle.

## Testing

Backend tests:

```bash
cd backend
pytest -q
```

CI workflows are defined in `.github/workflows/` for backend and Android builds.

Android unit tests are under:

- `android/app/src/test/`
- `android/core/skills/src/test/`

Android instrumentation scaffolding is under:

- `android/app/src/androidTest/`

## Pipeline Dataset Preview API

Before generating a pipeline, you can preview ranked dataset candidates:

- `POST /api/pipelines/dataset-candidates`

Request body:

```json
{
  "technique_name": "Sparse Attention",
  "description": "Long-context NLP models",
  "task_type": "text-classification",
  "top_k": 8
}
```

## Local Distributed Infra (Docker)

Bring up PostgreSQL + Redis + Kafka locally:

```bash
cd ..
docker compose up -d
```

Then configure backend using `backend/.env.example` values (copy to `backend/.env`).

Useful local endpoints:

- Kafka UI: `http://localhost:8085`
- Postgres: `localhost:5432`
- Redis: `localhost:6379`

## Troubleshooting

- Android emulator cannot reach `localhost` on host directly; use `http://10.0.2.2:8000`.
- If backend URL differs, set `BACKEND_URL` in `android/local.properties`.
- Start backend before launching app flows that fetch stats/trends.
- If Android local build fails with `IllegalArgumentException: 25`, your runtime JDK is too new for current Gradle Kotlin DSL tooling; use JDK 17/21 for Gradle execution.
