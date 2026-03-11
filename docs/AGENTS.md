Project context

This project is called Cohort.

It is a student software engineering project.
The goal is to build a system that retrieves scientific papers and turns them into simple study cards.

Current backend stack:
- Kotlin
- Ktor
- Gradle

Current backend state:
- Ktor server is running
- /search?q=... endpoint exists
- /search calls OpenAlex successfully
- OpenAlex response is parsed into a simplified list of papers
- each paper currently includes:
  - title
  - year
  - doi
  - abstract

Rules for AI agents

1. Do NOT restructure the whole project.
2. Do NOT rename files unless absolutely necessary.
3. Do NOT add new frameworks or libraries.
4. Keep the existing Ktor structure intact.
5. Only change the minimum number of files required.
6. Keep the code simple and readable for students.
7. Prefer explicit code over complex abstractions.
8. Do NOT add database logic.
9. Do NOT add authentication.
10. Do NOT modify unrelated routes.
11. Do NOT modify build.gradle unless absolutely necessary.
12. Do NOT modify IDE files like .idea.

Coding preferences

- Use Kotlin data classes
- Use kotlinx.serialization
- Keep functions short
- Avoid overengineering

-------------------------------------
