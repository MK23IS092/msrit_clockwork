# VectorMind — Deployment Guide

This document covers everything needed to ship VectorMind:

1. A **stable hosted backend** on Hugging Face Spaces
2. A **shareable Android APK** that always points to that hosted backend
3. **GitHub Actions** that re-deploy the backend and re-build the APK on push,
   so you never need to ship a new APK to judges just because the backend
   changed.

---

## 1. One-time setup

### 1.1 Create the Hugging Face Space

1. Sign in at https://huggingface.co/.
2. Go to https://huggingface.co/new-space.
3. **Owner**: your HF username (write it down — call it `<HF_USERNAME>`).
4. **Space name**: `vectormind-backend` (you can pick another name; whatever
   you choose here becomes `<HF_SPACE_NAME>`).
5. **License**: MIT.
6. **Space SDK**: **Docker**.
7. **Hardware**: CPU basic (free tier is enough).
8. **Visibility**: Public.

Once created the Space lives at:

```
https://huggingface.co/spaces/<HF_USERNAME>/<HF_SPACE_NAME>
```

The runtime URL of your API will be:

```
https://<HF_USERNAME>-<HF_SPACE_NAME>.hf.space
```

That URL is what gets baked into the Android APK.

### 1.2 Create a Hugging Face access token

1. Settings → Access Tokens → **New token**.
2. Name: `vectormind-ci`.
3. Role: **Write**.
4. Save the token — call it `<HF_TOKEN>`.

### 1.3 Add Hugging Face Space secrets

The backend reads these as environment variables at runtime. Inside the
Space → **Settings** → **Variables and secrets** → **New secret**:

| Key                   | Required | Notes                                                   |
|-----------------------|----------|---------------------------------------------------------|
| `GEMINI_API_KEY`      | yes      | Gemini API key (free tier OK).                          |
| `USE_MOCK_LLM`        | yes      | `false`. Default is `true` in code — must override.     |
| `TELEGRAM_BOT_TOKEN`  | optional | Only if you want Telegram alerts.                       |
| `TELEGRAM_CHAT_ID`    | optional | Telegram chat to notify.                                |
| `GITHUB_TOKEN`        | optional | Used for higher GitHub Trending API rate limits + Gist. |
| `HUGGINGFACE_TOKEN`   | optional | Used to fetch dataset candidates from HF.               |
| `KAGGLE_USERNAME` / `KAGGLE_KEY` | optional | Used for Kaggle dataset suggestions.        |

**Important:** secrets you add via the Space UI are *not* lost on rebuild.

### 1.4 Add GitHub repo secrets

Go to GitHub → repo → **Settings → Secrets and variables → Actions →
Secrets**, and add:

| Secret name                   | Value                                                    |
|-------------------------------|----------------------------------------------------------|
| `HF_USERNAME`                 | `<HF_USERNAME>`                                          |
| `HF_SPACE_NAME`               | `<HF_SPACE_NAME>`                                        |
| `HF_TOKEN`                    | `<HF_TOKEN>`                                             |
| `ANDROID_KEYSTORE_BASE64`     | (optional) base64 of a release keystore                  |
| `RELEASE_KEYSTORE_PASSWORD`   | (optional) keystore password                             |
| `RELEASE_KEY_ALIAS`           | (optional) key alias                                     |
| `RELEASE_KEY_PASSWORD`        | (optional) key password                                  |

The keystore secrets are only needed for a Play-Store-grade signed APK.
For a hackathon download you can skip them — the workflow falls back to a
debug-signed APK that any phone with “Install from unknown sources” will
accept.

### 1.5 Add GitHub repo variables

Same screen, **Variables** tab → **New variable**:

| Variable name | Value                                                          |
|---------------|----------------------------------------------------------------|
| `BACKEND_URL` | `https://<HF_USERNAME>-<HF_SPACE_NAME>.hf.space`               |

This is the URL the APK build pipeline bakes into every APK it produces.
You set it once and never touch it again.

---

## 2. The two workflows

### 2.1 `backend-deploy-hf.yml`

Triggers on every push to `main` that touches `backend/**`. It clones your
HF Space repo, replaces its contents with the current `backend/` folder,
re-writes the `README.md` front-matter (so HF knows it's a Docker SDK
Space on port 7860), commits, and pushes back. HF Space auto-rebuilds the
Docker image and restarts the API.

If the Space doesn't exist yet, the workflow creates it via the HF API on
the first run.

### 2.2 `android-release.yml`

Triggers on push to `main`, on tag pushes (`v*`), and on manual
`workflow_dispatch`. It:

1. Reads `BACKEND_URL` from `vars.BACKEND_URL` (or the manual input).
2. Builds `:app:assembleDebug` with `BACKEND_URL` baked into `BuildConfig`.
3. If `ANDROID_KEYSTORE_BASE64` is set, also builds `:app:assembleRelease`.
4. Uploads APKs as workflow artefacts.
5. On a tag push, attaches the APKs to a GitHub Release for download.

To cut a release for judges:

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions builds and attaches `vectormind-v1.0.0-debug.apk`
(and `-release.apk` if signing is configured) to the Release.

---

## 3. Connecting the loop — what happens when the backend changes

```
push backend code → backend-deploy-hf.yml → HF Space rebuilds → judges' APKs work
```

The APK never has to change. It always pings the same
`https://<user>-<space>.hf.space` URL. Backend updates ship transparently.

---

## 4. Testing locally

To run the backend against the same Docker image HF will run:

```bash
cd backend
docker build -t vectormind-backend .
docker run --rm -p 7860:7860 \
  -e GEMINI_API_KEY=$GEMINI_API_KEY \
  -e USE_MOCK_LLM=false \
  vectormind-backend
```

Then `curl http://localhost:7860/health` should answer `{"status":"ok",...}`.

To build an APK locally that points at the hosted backend:

```bash
cd android
echo BACKEND_URL=https://<HF_USERNAME>-<HF_SPACE_NAME>.hf.space >> local.properties
./gradlew :app:assembleDebug
# APK at android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Free-tier caveats (worth telling judges)

| Concern                  | Reality on HF Spaces (CPU basic)                                                                |
|--------------------------|--------------------------------------------------------------------------------------------------|
| Cold start               | First request after sleep takes ~30-60s while the container warms up.                            |
| Persistence              | `/data` survives container restarts but **not** Space rebuilds. Use SQLite + accept resets.      |
| Inactivity sleep         | Free Spaces sleep after ~48h of inactivity; first hit wakes them.                                |
| RAM / CPU                | 16 GB / 2 vCPU. Plenty for ingestion + Gemini calls; not enough for heavy local model training.  |
| Outbound API             | Unrestricted — Gemini, Telegram, arXiv, GitHub all work.                                         |

If you outgrow free tier you can upgrade the Space to a CPU upgrade or
move the backend to Render/Railway/Fly without changing the APK — just
update the GitHub variable `BACKEND_URL` and re-run the Android workflow.
