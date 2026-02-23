# Cohort (Android)

> Read real research without drowning in jargon: study cards, citations, glossary tooltips, and an explainable reliability heuristic.

![Platform](https://img.shields.io/badge/platform-Android-green)
![Language](https://img.shields.io/badge/language-Kotlin-blue)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-6f42c1)
![Architecture](https://img.shields.io/badge/architecture-MVVM-orange)
![Backend](https://img.shields.io/badge/backend-API%20(FastAPI%20%7C%20Node)-lightgrey)
![License](https://img.shields.io/badge/license-MIT-black)

---

## What it does
Cohort helps users search for **real scientific papers** (e.g., PubMed / Semantic Scholar) and read them in **plain language** with:
- **Study Cards**: TL;DR, design, results, limitations, and “what this does/doesn’t prove”
- **Citations**: every claim links back to a source (metadata/abstract)
- **Glossary Tooltips**: p-value, CI, effect size, bias, etc.
- **Reliability Heuristic**: a transparent rubric-based score with an explanation

> Educational only. Not medical advice.

---

## Screenshots (WIP)
| Search | Study Card | Reliability Breakdown |
|---|---|---|
| ![Search](docs/images/screenshot_search.png) | ![Study Card](docs/images/screenshot_card.png) | ![Reliability](docs/images/screenshot_reliability.png) |

> Add your screenshots to `docs/images/` and update the filenames above.

---

## Architecture (high level)
- **Android app** (Kotlin + Jetpack Compose): UI, search, saved list, offline cache
- **Backend API** (recommended): ingestion, normalization, summarization, scoring, caching
- **Database**: papers, summaries, scoring breakdowns, saved items

---

## Repository structure
- `android-app/` — Android client (Compose, MVVM)
- `backend/` — API (ingestion + scoring + caching)
- `docs/` — rubric, API contract, diagrams, meeting notes
- `scripts/` — ingestion helpers / seed scripts

---

## Getting started (local dev)
### Backend
1. `cd backend`
2. `docker compose up --build`

### Android
1. Open `android-app/` in Android Studio
2. Set API base URL in `local.properties` or `BuildConfig`
3. Run on emulator/device

---

## Team
- Amir Ileshev
- Aydin Mammadzada
- Kanat Danabay
