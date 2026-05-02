CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS papers (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    doi              TEXT,
    title            TEXT        NOT NULL,
    publication_year INT,
    abstract         TEXT,
    oa_url           TEXT,
    oa_status        TEXT,
    first_seen_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS papers_doi_unique
    ON papers (doi) WHERE doi IS NOT NULL;

CREATE TABLE IF NOT EXISTS search_queries (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    query_text  TEXT        NOT NULL,
    page        INT         NOT NULL DEFAULT 1,
    per_page    INT         NOT NULL DEFAULT 10,
    total_count INT,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT search_queries_page_positive    CHECK (page >= 1),
    CONSTRAINT search_queries_per_page_positive CHECK (per_page >= 1)
);

CREATE INDEX IF NOT EXISTS search_queries_executed_at_idx
    ON search_queries (executed_at DESC);

CREATE TABLE IF NOT EXISTS search_results (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query_id    UUID NOT NULL REFERENCES search_queries(id) ON DELETE CASCADE,
    paper_id    UUID NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    result_rank INT  NOT NULL,
    CONSTRAINT search_results_rank_positive CHECK (result_rank > 0),
    CONSTRAINT search_results_unique_rank    UNIQUE (query_id, result_rank),
    CONSTRAINT search_results_unique_paper   UNIQUE (query_id, paper_id)
);

CREATE INDEX IF NOT EXISTS search_results_query_id_idx
    ON search_results (query_id);

CREATE INDEX IF NOT EXISTS search_results_paper_id_idx
    ON search_results (paper_id);

CREATE TABLE IF NOT EXISTS study_cards (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    paper_id          UUID        REFERENCES papers(id) ON DELETE SET NULL,
    source_url        TEXT        NOT NULL,
    tldr              TEXT        NOT NULL,
    study_design      TEXT        NOT NULL,
    limitations       TEXT        NOT NULL,
    key_findings      JSONB       NOT NULL DEFAULT '[]',
    generation_source TEXT        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT study_cards_generation_source_check CHECK (generation_source IN ('llm', 'llm_abstract', 'fallback')),
    CONSTRAINT study_cards_key_findings_is_array   CHECK (jsonb_typeof(key_findings) = 'array')
);

CREATE INDEX IF NOT EXISTS study_cards_paper_id_idx
    ON study_cards (paper_id);

ALTER TABLE study_cards
    ADD COLUMN IF NOT EXISTS is_saved BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS study_cards_saved_created_at_idx
    ON study_cards (created_at DESC)
    WHERE is_saved = TRUE;

-- Migration: update existing constraint for databases already provisioned
ALTER TABLE study_cards
DROP CONSTRAINT IF EXISTS study_cards_generation_source_check;

ALTER TABLE study_cards
    ADD CONSTRAINT study_cards_generation_source_check
        CHECK (generation_source IN ('llm', 'fallback', 'llm_abstract'));
