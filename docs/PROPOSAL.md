# Cohort (Android) — Project Proposal

> An Android app that turns scientific papers into plain-language study cards with transparent citations, glossary tooltips, and an explainable reliability heuristic.  
> **Educational only. Not medical advice.**

---

## 1. Overview

Many people learn about health, fitness, and science topics from short-form content (TikTok/Shorts) or blogs that may oversimplify, misinterpret, or selectively cite research. Even when users find real scientific papers, they often face paywalls, academic English, jargon, and statistics—making it hard to understand what a study actually shows and how reliable it is.

**Cohort** is an **Android mobile application** that helps users search for real research papers on a topic and read them in **plain language** with:
- **Study Cards** (TL;DR, design, results, limitations)
- **Citations** that link back to the original source (metadata/abstract/DOI)
- **Glossary tooltips** for statistics and research terms
- A **reliability heuristic** with a clear rubric-based explanation (not a “truth score”)

---

## 2. Problem

1. Search results mix **peer-reviewed research** with low-quality or misleading content.
2. Scientific papers are difficult for most users to interpret (methods, jargon, statistics).
3. Users lack tools to judge **study quality** and strength of evidence.
4. Language barriers (papers often in English) reduce accessibility.

---

## 3. Solution

An Android app that:

1. **Collects research papers** from trusted scholarly sources (e.g., PubMed / Semantic Scholar).
2. Converts each paper into a **readable study card**:
   - what was studied (population, intervention, duration)
   - what was measured (outcomes)
   - key findings (explained in everyday language)
   - limitations / confounders
   - what the study *can* and *cannot* conclude
3. Provides a **glossary + tooltips** for statistical concepts (p-value, CI, effect size, bias).
4. Displays a **reliability heuristic** using a transparent rubric and explanation.
5. Offers **translation of summaries** into the user’s preferred language (MVP-lite).

---

## 4. Target Users

- Students and curious learners
- People researching supplements/health topics and wanting evidence-based understanding
- Non-native English speakers
- Anyone trying to avoid “internet noise” and learn from credible sources

---

## 5. Core Features (MVP)

### 5.1 Topic Search & Paper Retrieval
- Search by query (e.g., “creatine brain”).
- Fetch papers from one or two scholarly APIs.
- Show a results list with:
  - title, year, authors, venue/source
  - tags (RCT / meta-analysis / observational / preprint)
  - link to DOI/source

### 5.2 Plain-Language Study Card
For a selected paper:
- **TL;DR** (5–8 lines)
- **Study design** (type + what it means)
- **Results explained**
- **Key numbers** (sample size, effect size if available, simplified stats)
- **Limitations**
- **Interpretation boundaries:** “What this does/doesn’t prove”
- **Citations:** link back to paper metadata and abstract source

### 5.3 Reliability Heuristic (Transparent Rubric)
A rubric-based score with rationale, based on signals like:
- study type (meta-analysis > RCT > cohort > cross-sectional > case report)
- sample size thresholds
- control group / randomization / blinding (if available)
- peer-reviewed vs preprint
- conflicts/funding disclosures (if available)
- limitations flags (short duration, self-report outcomes, etc.)

**Output example:**  
“**Moderate confidence** — RCT with n=120 and control group; short duration and limited outcome measures.”

### 5.4 Glossary + Tooltips
Tap terms like:
- p-value, confidence interval, effect size, placebo, confounding, selection bias  
to get a simple explanation + example.

### 5.5 Saved Reading List + Offline Access (Mobile-First)
- Save study cards to a reading list.
- Cache summaries locally for offline reading.

---

## 6. Non-Goals (Scope Control)

- No diagnosing, treatment recommendations, or personalized medical guidance
- No claiming a “final truth” (only summarize evidence with uncertainty)
- No re-hosting paywalled PDFs (metadata/abstract + external links only)
- Not a fully comprehensive “all studies ever” literature review engine

---

## 7. Suggested Tech Stack (Android-focused)

### 7.1 Android Client
- **Language:** Kotlin  
- **UI:** Jetpack Compose  
- **Architecture:** MVVM (ViewModel + Repository pattern)  
- **Networking:** Retrofit + OkHttp  
- **Local storage:** Room (SQLite)  
- **Async:** Kotlin Coroutines + Flow  
- **Dependency injection:** Hilt (optional)

### 7.2 Backend (Recommended)
A backend keeps API keys off the client and enables caching + consistent scoring/summarization.

- **Backend:** FastAPI (Python) *or* Node.js (Express/Nest)  
- **Database:** PostgreSQL  
- **Deployment:** Docker + Render/Fly.io  

### 7.3 Paper Sources/APIs (choose 1–2)
- PubMed / NCBI APIs
- Semantic Scholar API
- Crossref (metadata/DOIs)

---

## 8. High-Level Architecture

1. **Android App**
   - Search UI, study cards, glossary, saved list, offline cache
2. **Backend API**
   - Queries scholarly APIs, normalizes metadata
   - Generates/serves summaries + reliability rationale
   - Stores cached results in DB
3. **Database**
   - papers, summaries, rubric outputs, user saved items

---

## 9. User Stories

- As a user, I can search a topic and see a list of real papers.
- As a user, I can open a paper card and understand the results in plain language.
- As a user, I can see why the app considers a study higher/lower confidence.
- As a user, I can save papers and read them offline.
- As a user, I can translate summaries into my preferred language.

---

## 10. Evaluation / Success Criteria

- Users can answer after reading a card:
  - What was studied?
  - What did the results show?
  - What are the key limitations?
- Reliability score is **explainable** (not a black box).
- Every card links back to **original sources** (abstract/metadata/DOI).

---

## 11. Risks & Ethics

- **Misinformation risk:** summaries must avoid overclaiming causality.
- **Medical safety:** explicit disclaimer; no recommendations.
- **Rubric oversimplification:** show reasoning and uncertainty; allow “unknown” fields.
- **Copyright:** do not redistribute paywalled PDFs—link out only.

---

## 12. Roadmap (12-week example)

- Weeks 1–2: Requirements, UI mockups, choose APIs, define rubric
- Weeks 3–4: Backend ingestion + DB schema + Android search UI
- Weeks 5–6: Study card UI + glossary tooltips + caching
- Weeks 7–8: Reliability scoring + explanation outputs
- Weeks 9–10: Summarization pipeline + saved lists + offline mode
- Weeks 11–12: Translation + testing + polish + demo + documentation

---

## 13. Suggested Repo Structure

- `android-app/` — Kotlin + Jetpack Compose client
- `backend/` — ingestion/scoring/summarization API
- `docs/` — rubric definition, API contracts, architecture diagrams, meeting notes
- `scripts/` — data ingestion utilities, seed scripts
- `docker-compose.yml` — local dev environment

---

## 14. Team

- Amir Ileshev
- Aydin Mammadzada
- Danabay Kanat