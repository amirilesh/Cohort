# Cohort — Backend

Ktor (Kotlin) backend that searches scientific papers via OpenAlex, extracts PDF text, and generates study cards using an LLM. Results are persisted to PostgreSQL.

---

## Local setup

### 1. Start PostgreSQL with Docker

```bash
docker run -d \
  --name cohort-db \
  -e POSTGRES_DB=cohort \
  -e POSTGRES_USER=cohort \
  -e POSTGRES_PASSWORD=cohort \
  -p 5432:5432 \
  postgres:16
```

The schema is applied automatically at startup — no manual migrations needed.

### 2. Environment variables

| Variable | Default | Required |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cohort` | No |
| `DB_USER` | `cohort` | No |
| `DB_PASSWORD` | `cohort` | No |
| `OPENAI_API_KEY` | — | Yes (for `/studycard`) |

Export before running:

```bash
export OPENAI_API_KEY=sk-...
```

### 3. Run

```bash
./gradlew run
```

Open in browser:
http://localhost:8080/health

---

## API endpoints

### Health check

```bash
curl http://localhost:8080/health
```

```json
{
  "status": "ok",
  "ktor": "up",
  "openAlex": "up",
  "openAiKey": "present"
}
```

### Search papers

```bash
curl "http://localhost:8080/search?q=stress+sleep&page=1&perPage=5"
```

Searches scientific papers via OpenAlex, returns metadata (title, year, DOI, abstract), and persists results to PostgreSQL.

### Generate a study card
Generates a structured summary (TL;DR, study design, limitations, key findings) using an LLM.

From a DOI:

```bash
curl "http://localhost:8080/studycard?doi=10.1038/s41586-021-03819-2"
```

From a PDF URL:

```bash
curl "http://localhost:8080/studycard?url=https://example.com/paper.pdf"
```

### Extract PDF text

```bash
curl "http://localhost:8080/pdftext?url=https://example.com/paper.pdf"
```

### History

```bash
curl http://localhost:8080/search/history
curl http://localhost:8080/studycards/recent
```

### Analytics

```bash
curl http://localhost:8080/analytics/top-searches
curl http://localhost:8080/analytics/popular-papers
```

---

## Database tables

| Table | Purpose |
|---|---|
| `papers` | Deduplicated paper records (DOI, title, abstract, year) |
| `search_queries` | Every `/search` call with its query text and pagination |
| `search_results` | Join table linking a query to the papers it returned, with rank |
| `study_cards` | Generated cards (TL;DR, key findings, study design, limitations) |

The schema lives in `src/main/resources/schema.sql` and uses `IF NOT EXISTS` so it is safe to re-run.

---

## Rate limits

Expensive endpoints are protected per client IP:

| Endpoint | Limit |
|---|---|
| `/search` | 30 requests / minute |
| `/studycard` | 10 requests / minute |

Exceeding the limit returns `429 Too Many Requests`. Response headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset` are included on every response from these endpoints.

---

## Notes

**Connection pooling** — HikariCP manages a pool of up to 10 PostgreSQL connections. The pool is initialized once at startup and shared across all requests.

**Structured logging** — SLF4J with Logback. Logs are written to stdout in plain text. The Ktor `CallLogging` plugin logs every request at `INFO` level. Configure log levels in `src/main/resources/logback.xml`.
