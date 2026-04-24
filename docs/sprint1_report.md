#report #sprint_1

**Cohort** — Sprint 1 Report

🔹 **Context**
Cohort is a student software engineering project that retrieves scientific papers and turns them into simple study cards. Sprint 1 focused on setting up the backend infrastructure, integrating the OpenAlex API for paper search, and establishing AI agent guidelines for the project.

🔹 **Planned / Done**
- Initialize Ktor backend and run first server [done]
- Add initial /search endpoint with mock response [done]
- Integrate OpenAlex API and return raw JSON from /search [done]
- Parse OpenAlex response into PaperPreview list (title, year, doi, abstract) [done]
- Add OpenAlex open access fields to paper previews [done]
- Filter search results to open access full-text papers [done]
- Filter search results to open access papers with PDF links [done]
- Clean up repo: remove IDE files, update .gitignore [done]
- Create AGENTS.md file with project context, AI rules, and coding preferences [done]
- Create project proposal document [done]
- Create requirements document [done]
- Create and upload survey artifact [done]

🔹 **Demo**
No demo prepared for this sprint.

🔹 **Key Decisions**
- Chose **Kotlin + Ktor + Gradle** as the backend stack — lightweight, async-first, and suitable for a student project.
- Integrated **OpenAlex API** as the paper source — it is open access, free, and does not require API keys.
- Created an **AGENTS.md** file to define strict rules for AI-assisted development: no unnecessary restructuring, no new frameworks, keep code simple and readable.

🔹 **Problems & Next Steps**
- The /search endpoint currently lives on a feature branch (`feature/openalex-integration`) and needs to be merged into main.
- No frontend exists yet → Next sprint focus: begin frontend development and connect it to the search API.
- No flashcard/study card generation yet → Plan the card generation pipeline.
- Next sprint focus: **build a basic frontend UI and start converting papers into study cards.**
