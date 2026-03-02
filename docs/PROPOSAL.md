# Cohort (Android) — Project Proposal

> An Android app that turns scientific papers into plain-language **study cards** with transparent citations, glossary tooltips, and an explainable reliability heuristic.  
> **Educational only. Not medical advice.**

---

## 1. Overview

Many people learn about health, fitness, and science topics from short-form content (TikTok/Shorts) or blogs that may oversimplify, misinterpret, or selectively cite research. Even when users find real scientific papers, they often face paywalls, academic English, jargon, and statistics—making it hard to understand what a study actually shows and how reliable it is.

**Cohort** is an **Android mobile application** that helps users search for real research papers on a topic and read them in **plain language** with:
- **Study Cards** (TL;DR, design, key numbers, results, limitations, and “what this can/can’t conclude”)
- **Citations** that link back to the original source (metadata/abstract/DOI)
- **Glossary tooltips** for statistics and research terms
- A **rule-based reliability heuristic** with a clear rubric-based explanation (not a “truth score”)
- **Personalized discovery** (filters, interests, and recommendations based on reading history)
- **Translation** of summaries and explanations to support non-native English speakers

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
   - what was studied (population, intervention/exposure, duration)
   - what was measured (outcomes)
   - key findings (explained in everyday language)
   - limitations / confounders
   - what the study *can* and *cannot* conclude
3. Provides a **glossary + tooltips** for statistical concepts (p-value, CI, effect size, bias).
4. Displays a **reliability heuristic** using a transparent rubric and explanation.
5. Makes exploration engaging via **filters, categories, saved interests, and recommendations**.
6. Improves accessibility via **translation** of summaries and explanations.

---

## 4. Target Users

- Students and curious learners
- People researching supplements/health topics and wanting evidence-based understanding
- Non-native English speakers
- Anyone trying to avoid “internet noise” and learn from credible sources

---

## 5. Positioning Compared to Existing Tools (Elicit / Consensus)

We recognize that tools like **Elicit** and **Consensus** are web-based LLM products that help users interact with research literature. Our approach differs in several important ways:

**Mobile-first learning experience.** Existing tools are powerful web platforms built mainly for researchers. **Cohort is designed to work naturally on a phone**: lightweight, fast to scan, and optimized for short sessions. Instead of complex dashboards, Cohort uses short, structured **study cards** that help a user grasp the core of a paper in **1–2 minutes**.

**Transparency about evidence strength.** While many tools generate summaries, they often do not clearly communicate how strong the underlying evidence is. Cohort introduces a **rule-based reliability rubric** and makes its inputs explicit. We surface (when available):
- What type of study it is
- How large the sample size is
- Whether it was peer-reviewed vs. a preprint
- What limitations are reported

We are **not** trying to decide what is “true.” Our goal is to help users understand **how strong the evidence is** and what the study can reasonably support.

**Students first (not research workflow tooling).** Cohort’s card format is educational and beginner-friendly:
- a short TL;DR summary
- an explanation of study design
- key numbers
- limitations
- what the study can and cannot conclude

This learning-oriented structure differentiates us from research workflow tools.

---

## 6. Core Features (MVP)

### 6.1 Topic Search & Paper Retrieval
- Search by query (e.g., “creatine brain”).
- Fetch papers from one or two scholarly APIs.
- Show a results list with:
  - title, year, authors, venue/source
  - tags (RCT / meta-analysis / observational / preprint)
  - link to DOI/source

### 6.2 Plain-Language Study Card
For a selected paper:
- **TL;DR** (5–8 lines)
- **Study design** (type + what it means)
- **Results explained**
- **Key numbers** (sample size, effect size if available, simplified stats)
- **Limitations**
- **Interpretation boundaries:** “What this does/doesn’t prove”
- **Citations:** link back to paper metadata and abstract source

### 6.3 Reliability Heuristic (Transparent Rubric)
A rubric-based confidence level with rationale, based on signals like:
- study type (meta-analysis > RCT > cohort > cross-sectional > case report)
- sample size thresholds
- control group / randomization / blinding (if available)
- peer-reviewed vs preprint
- conflicts/funding disclosures (if available)
- limitations flags (short duration, self-report outcomes, etc.)

**Output example:**  
“**Moderate confidence** — RCT with n=120 and control group; short duration and limited outcome measures.”

#### 6.3.1 Reliability Heuristic — Responsibility and Validation
We understand that introducing a reliability heuristic is a serious responsibility, so our approach is intentionally conservative and transparent:

- **Rule-based, not AI-generated.** The rubric follows widely accepted evidence hierarchies (e.g., meta-analyses and randomized controlled trials generally provide stronger evidence than observational studies or case reports). We are not asking an LLM to judge scientific validity.
- **Every level is explained.** Each confidence label is accompanied by a clear explanation. Users can see *why* a study is classified as moderate or lower confidence (e.g., small sample size, lack of randomization, short duration).
- **No “true/false” claims.** Science is rarely binary. We present evidence strength in relative terms and allow “unknown” when metadata is missing.
- **Educational framing.** The rubric is a learning tool and does not replace expert evaluation or professional medical advice.

In short, the reliability system is meant to **increase transparency**, not to act as a “truth detector.”

### 6.4 Glossary + Tooltips
Tap terms like:
- p-value, confidence interval, effect size, placebo, confounding, selection bias  
to get a simple explanation + example.

### 6.5 Saved Reading List + Offline Access (Mobile-First)
- Save study cards to a reading list.
- Cache summaries locally for offline reading.

---

## 7. Scope and Personalization

We agree that simply browsing random scientific papers may not be engaging for most users. To keep the experience focused and relevant, Cohort will support:

### 7.1 Topic Filters, Interests, and Keywords
- **Topic filters** (for health-related studies: nutrition, mental health, exercise, sleep, supplements)
- **Saved interests** (user-selected topics)
- **Followed keywords** (e.g., “creatine”, “ADHD”, “sleep quality”)

### 7.2 Category-Based Exploration (Mobile-friendly Navigation)
Instead of forcing every user to craft search queries, the app will offer **category navigation**, for example:
- Nutrition
- Mental Health
- Exercise
- Sleep
- Supplements

A user can tap: **Mental Health → Depression → Latest Studies** to explore without feeling overwhelmed.

### 7.3 Personalized Suggestions (Simple, Explainable Logic)
Personalization does not have to mean complex AI. For early versions, we can use simple logic such as:
- track which categories the user reads most
- recommend adjacent topics and recent papers in those categories

Example: if a user reads several cards about **anxiety, depression, cognitive therapy**, the app may suggest:  
“You may also be interested in recent studies about **sleep and anxiety**.”

### 7.4 Prototype Scope: Health + Psychology First
For the prototype/MVP phase, we may **narrow the domain to health and psychology research**. These areas have well-established evidence hierarchies, making them suitable for a clear and explainable reliability framework.

We limit the first version because **each scientific field evaluates evidence differently**. Mixing domains too early would make the scoring system inconsistent, overly complex, or simply wrong. Starting with a narrower domain enables a consistent and defensible rubric for the MVP.

### 7.5 Translation as a Key Differentiator
Translation can be a meaningful differentiator. Many students struggle not only with scientific language but also with **English** itself. Making research understandable also means making it **linguistically accessible**.

We plan to prioritize translation of:
- TL;DR summaries
- study card explanations
- glossary definitions

This improves accessibility for non-native English speakers and broadens the app’s educational impact.

---

## 8. Non-Goals (Scope Control)

- No diagnosing, treatment recommendations, or personalized medical guidance
- No claiming a “final truth” (only summarize evidence with uncertainty)
- No re-hosting paywalled PDFs (metadata/abstract + external links only)
- Not a fully comprehensive “all studies ever” literature review engine

---

## 9. Suggested Tech Stack (Android-focused)

### 9.1 Android Client
- **Language:** Kotlin  
- **UI:** Jetpack Compose  
- **Architecture:** MVVM (ViewModel + Repository pattern)  
- **Networking:** Retrofit + OkHttp  
- **Local storage:** Room (SQLite)  
- **Async:** Kotlin Coroutines + Flow  
- **Dependency injection:** Hilt (optional)

### 9.2 Backend (Recommended)
A backend keeps API keys off the client and enables caching + consistent scoring/summarization.

- **Backend:** FastAPI (Python) *or* Node.js (Express/Nest)  
- **Database:** PostgreSQL  
- **Deployment:** Docker + Render/Fly.io  

### 9.3 Paper Sources/APIs (choose 1–2)
- PubMed / NCBI APIs
- Semantic Scholar API
- Crossref (metadata/DOIs)

---

## 10. High-Level Architecture

1. **Android App**
   - Search UI, study cards, glossary, saved list, offline cache
2. **Backend API**
   - Queries scholarly APIs, normalizes metadata
   - Generates/serves summaries + reliability rationale
   - Stores cached results in DB
3. **Database**
   - papers, summaries, rubric outputs, user saved items

---

## 11. User Stories

- As a user, I can search a topic and see a list of real papers.
- As a user, I can open a paper card and understand the results in plain language.
- As a user, I can see why the app considers a study higher/lower confidence.
- As a user, I can save papers and read them offline.
- As a user, I can translate summaries into my preferred language.
- As a user, I can follow interests/keywords and get relevant suggestions.

---

## 12. Evaluation / Success Criteria

- Users can answer after reading a card:
  - What was studied?
  - What did the results show?
  - What are the key limitations?
- Reliability output is **explainable** (not a black box).
- Every card links back to **original sources** (abstract/metadata/DOI).
- Translations preserve meaning and keep citations intact.

---

## 13. Risks & Ethics

- **Misinformation risk:** summaries must avoid overclaiming causality.
- **Medical safety:** explicit disclaimer; no recommendations.
- **Rubric oversimplification:** show reasoning and uncertainty; allow “unknown” fields.
- **Copyright:** do not redistribute paywalled PDFs—link out only.

### 13.1 LLM Hallucination Risks and Mitigation
We acknowledge that AI-generated summaries carry risks, especially hallucination or over-interpretation. To reduce these risks:

- AI will **only summarize information already present in the abstract** (or other explicitly provided metadata fields).
- It will **not generate new scientific claims** or interpretations.
- The **original abstract and DOI** will always be displayed alongside the summary.
- AI-generated content will be **clearly labeled**.
- The **reliability rubric remains fully rule-based** and independent from the LLM.

In our design, AI is a **simplification tool**, not a scientific authority. It makes text more readable, but does not replace the original source or expert judgment.

---

## 14. Roadmap (12-week example)

- Weeks 1–2: Requirements, UI mockups, choose APIs, define rubric
- Weeks 3–4: Backend ingestion + DB schema + Android search UI
- Weeks 5–6: Study card UI + glossary tooltips + caching
- Weeks 7–8: Reliability scoring + explanation outputs
- Weeks 9–10: Summarization pipeline + saved lists + offline mode
- Weeks 11–12: Translation + testing + polish + demo + documentation

---

## 15. Suggested Repo Structure

- `android-app/` — Kotlin + Jetpack Compose client
- `backend/` — ingestion/scoring/summarization API
- `docs/` — rubric definition, API contracts, architecture diagrams, meeting notes
- `scripts/` — data ingestion utilities, seed scripts
- `docker-compose.yml` — local dev environment

---

## 16. Team

- Amir Ileshev
- Aydin Mammadzada
- Kanat Danabay
