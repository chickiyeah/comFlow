# Four Job Sources Design

## Goal

Extend CampusFlow's existing background job-import pipeline with InThisWork, Wanted, Zighang, and Remember, then expose the verified aggregated jobs through the remote assistant's MCP server.

## Collection architecture

Each source is a focused Spring service that returns the existing `JobSearchResult` record. `JobImportService` remains the orchestration boundary: it calls every source independently, catches failures per source, rejects expired or URL-less results, and upserts by canonical URL into `ImportedJob`.

- InThisWork: read the public WordPress sitemap, examine the newest bounded set of posting pages, and parse title/company/deadline/body metadata from public HTML.
- Zighang: read the public recruitment sitemap and parse `JobPosting` JSON-LD from public recruitment pages.
- Remember: read the public job-posting sitemap and parse `JobPosting` JSON-LD or Next.js page data from public posting pages.
- Wanted: call the public read-only endpoints used by the public jobs UI: `/api/v4/jobs` for a bounded, filtered list and `/api/v4/jobs/{id}` for detail enrichment.

No browser automation, authentication, private positions, CAPTCHA bypass, proxy rotation, or anti-bot evasion is used.

## Operational safeguards

- Truthful CampusFlow user agent and normal public Referer headers.
- Bounded pages/items per run and six-hour scheduling through the existing scheduler.
- Connect/read timeouts, request spacing for sitemap-detail crawlers, and graceful empty-result fallback.
- Per-source exception isolation so one provider never aborts the import.
- Canonical URL upsert prevents duplicate rows across keywords and repeated runs.
- Parser tests use local fixtures; network smoke tests are explicit and separate from the deterministic unit suite.

## Data mapping

All adapters populate `id`, `title`, `company`, `location`, `url`, `deadline`, `jobType`, `salary`, and `source`. Unknown fields remain null rather than being guessed. Source names are stable Korean labels shown in the existing UI.

## MCP boundary

Only after CampusFlow unit, integration, and live smoke tests pass, add read-only MCP tools to `D:\claude_workspace\fleet_mcp`. The MCP layer calls CampusFlow's imported-jobs API; it does not duplicate scraping logic. It provides list/search and refresh-status operations with bounded output, and retains the existing assistant's SQLite/Discord fallback patterns.

## Acceptance criteria

1. Deterministic parser tests cover one representative posting and malformed input for each source.
2. `JobImportService` invokes all four new sources, isolates a failed source, and upserts valid results.
3. The full Maven test suite passes.
4. A low-volume live smoke request returns at least one structurally valid active posting from every source.
5. MCP tool discovery exposes the new job tools and an MCP call returns CampusFlow job data without changing external state.
