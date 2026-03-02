# Cohort — Requirements Specification

Version: 1.0  
Project: Cohort (Android)  
Type: Software Requirements Specification (SRS)

---

# 1. Purpose

This document defines the functional and non-functional requirements for the Cohort Android application.

Cohort is a mobile-first educational application that transforms scientific papers into structured, plain-language study cards with transparent citations and an explainable reliability heuristic.

This document serves as a formal specification of system behavior and scope for the MVP phase.

---

# 2. Scope

The MVP version focuses on:

- Retrieval of scholarly papers from at least one trusted API
- Generation of structured study cards
- Rule-based reliability evaluation (validated for health and psychology)
- Basic personalization
- Translation of summaries
- Offline saving

The system is educational and does not provide medical advice!

---

# 3. Stakeholders

- End users (students, curious learners)
- Project team
- Course instructors

---

# 4. User Stories (Backlog)
## Prioritization Method

We use the MoSCoW prioritization framework:

- **MUST** — Essential requirement. The system is incomplete without it.
- **SHOULD** — Important requirement, but the system can function without it.
- **COULD** — Optional improvement, implemented if time allows.
- **WON’T (for MVP)** — Explicitly excluded from the current scope.

## 4.1 Search & Discovery

US1 — As a user, I want to search for a topic so that I can find relevant research papers.  
US2 — As a user, I want to browse papers by category so that I can explore topics without writing queries.  
US3 — As a user, I want to follow keywords/interests so that I can see relevant studies.  
US4 — As a user, I want to receive recommendations based on my reading history.

Priority: MUST (US1), SHOULD (US2-US3–US4)

---

## 4.2 Study Card

US5 — As a user, I want a short TL;DR summary.  
US6 — As a user, I want the study design explained in simple terms.  
US7 — As a user, I want to see key numbers (e.g., sample size).  
US8 — As a user, I want to see study limitations.  
US9 — As a user, I want to understand what the study can and cannot conclude.  
US10 — As a user, I want to access the original abstract and DOI.

Priority: MUST

---

## 4.3 Reliability

US11 — As a user, I want to see how strong the evidence is.  
US12 — As a user, I want to understand why that reliability level was assigned.

Priority: MUST

---

## 4.4 Accessibility

US13 — As a user, I want summaries translated into my preferred language.  
US14 — As a user, I want glossary explanations for statistical terms.  
US15 — As a user, I want to save papers and read them offline.

Priority: SHOULD

---

# 5. Functional Requirements

## 5.1 Paper Retrieval

FR1 — The system shall retrieve papers from at least one scholarly API.  
FR2 — The system shall support keyword-based search.  
FR3 — The system shall support category-based browsing.  
FR4 — The system shall display title, authors, year, and DOI.

---

## 5.2 Study Card Generation

FR5 — The system shall generate a structured study card including:
- TL;DR summary
- Study design explanation
- Key numbers (if available)
- Limitations
- Interpretation boundaries

FR6 — The system shall display the original abstract and DOI alongside the summary.  
FR7 — AI-generated content shall be clearly labeled.  

Acceptance Criteria:
- Study card can be read within 2 minutes.
- Original source is always accessible.

---

## 5.3 Reliability Heuristic

FR8 — The system shall calculate a reliability level using a rule-based rubric.  
FR9 — The system shall display the reasoning behind the assigned reliability level.  
FR10 — The system shall allow "unknown" values when metadata is missing.  
FR11 — The reliability heuristic shall be independent from AI summarization.

Constraint:
- Reliability validation applies initially to health and psychology domains only.

---

## 5.4 Personalization

FR12 — The system shall track reading categories.  
FR13 — The system shall recommend related topics based on reading history.  
FR14 — The system shall allow users to follow keywords/interests.

---

## 5.5 Translation

FR15 — The system shall translate TL;DR summaries.  
FR16 — The system shall translate study card explanations.  
FR17 — The system shall preserve citations during translation.

---

## 5.6 Offline Storage

FR18 — The system shall allow users to save study cards.  
FR19 — The system shall cache saved summaries for offline access.

---

# 6. Non-Functional Requirements

## 6.1 Usability

NFR1 — The system shall be optimized for mobile-first usage.  
NFR2 — Study cards shall be readable within 1–2 minutes.  
NFR3 — Navigation shall require no more than three taps to open a study card from the home screen.

---

## 6.2 Performance

NFR4 — Search results shall load within 3 seconds under normal conditions.  
NFR5 — Cached study cards shall open within 2 seconds.

---

## 6.3 Transparency & Integrity

NFR6 — The reliability rubric shall be explainable and rule-based.  
NFR7 — The system shall not produce binary “true/false” judgments.  
NFR8 — AI shall not generate new scientific claims beyond provided metadata.

---

## 6.4 Security

NFR9 — API keys shall not be exposed in the client application.  
NFR10 — User saved items and preferences shall be stored securely.

---

## 6.5 Ethics & Safety

NFR11 — The system shall display a medical disclaimer.  
NFR12 — The system shall not provide diagnosis or treatment advice.  
NFR13 — The system shall not redistribute paywalled PDFs.

---

# 7. Constraints

- MVP reliability rubric validated only for health and psychology.
- AI summarization limited to abstract-level information.
- The app is educational and not a medical device.

---

# 8. Acceptance Criteria (System-Level)

The system is considered acceptable if:

1. A user can search and retrieve papers.
2. A study card clearly explains:
   - what was studied
   - main findings
   - limitations
   - evidence strength
3. Reliability reasoning is visible and understandable.
4. Every study card links to original sources.
5. AI-generated content is labeled and verifiable.
6. Translation preserves meaning and citations.

---

# 9. Future Extensions (Out of Scope for MVP)

- Domain-specific reliability rubrics for additional scientific fields(physics, biology, chemistry, etc.).
- Advanced AI-driven personalization.
- Multi-paper comparison view.