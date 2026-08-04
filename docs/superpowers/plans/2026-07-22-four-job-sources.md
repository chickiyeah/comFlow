# Four Job Sources Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add InThisWork, Wanted, Zighang, and Remember to CampusFlow's bounded background job importer, verify every source, and expose the resulting read-only job feed through the existing remote MCP assistant.

**Architecture:** Three sitemap-backed adapters and one public JSON adapter implement a small `JobFeedCollector` interface. `JobImportService` collects these feeds once per run beside the existing keyword searches, isolates failures, and reuses URL upsert. The remote MCP server delegates to CampusFlow's imported-jobs HTTP API instead of scraping again.

**Tech Stack:** Java 17, Spring Boot 3.3.5, RestClient, Jackson, Jsoup 1.17.2, JUnit 5/AssertJ/Mockito, Python MCP server on `.29`.

## Global Constraints

- Preserve every pre-existing tracked and untracked user change in `C:\Users\0_8_2\comFlow` and `D:\claude_workspace\fleet_mcp`.
- Use only public, unauthenticated pages/endpoints; do not bypass access controls.
- Bound every collection run and retain per-source graceful failure.
- Do not register MCP tools until all CampusFlow deterministic and live smoke tests pass.
- Baseline on 2026-07-22: Maven tests pass, 12 tests and 0 failures; the local Maven cache emits a pre-existing empty Jakarta BOM warning.

---

### Task 1: Common feed contract and sitemap parsing

**Files:**
- Create: `src/main/java/com/campusflow/domain/career/service/jobfeed/JobFeedCollector.java`
- Create: `src/main/java/com/campusflow/domain/career/service/jobfeed/SitemapParser.java`
- Test: `src/test/java/com/campusflow/domain/career/service/jobfeed/SitemapParserTest.java`

**Interfaces:**
- Produces: `String source()` and `List<JobSearchResult> collectLatest()` on `JobFeedCollector`.
- Produces: `List<SitemapEntry> parse(String xml)` where `SitemapEntry` contains `url` and nullable `lastModified`.

- [ ] Write tests proving sitemap index and URL-set parsing, namespace tolerance, newest-first ordering, and malformed XML returning an empty list.
- [ ] Run `mvn.cmd -Dtest=SitemapParserTest test` and confirm failure because the types do not exist.
- [ ] Add the minimal interface, record, and Jsoup XML parser.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Zighang and Remember JSON-LD collectors

**Files:**
- Create: `src/main/java/com/campusflow/domain/career/service/jobfeed/JsonLdJobPostingParser.java`
- Create: `src/main/java/com/campusflow/domain/career/service/jobfeed/ZighangJobFeedCollector.java`
- Create: `src/main/java/com/campusflow/domain/career/service/jobfeed/RememberJobFeedCollector.java`
- Test: `src/test/java/com/campusflow/domain/career/service/jobfeed/JsonLdJobPostingParserTest.java`
- Test: `src/test/java/com/campusflow/domain/career/service/jobfeed/ZighangJobFeedCollectorTest.java`
- Test: `src/test/java/com/campusflow/domain/career/service/jobfeed/RememberJobFeedCollectorTest.java`

**Interfaces:**
- Consumes: `SitemapParser.parse` and `JobFeedCollector`.
- Produces: normalized `JobSearchResult` values with stable sources `직행` and `리멤버`.

- [ ] Add local HTML/XML fixtures inside each test for representative `JobPosting`, missing optional fields, expired dates, and invalid JSON-LD.
- [ ] Run the three focused tests and confirm expected missing-class failures.
- [ ] Implement a shared JSON-LD parser supporting object, array, and `@graph`, then bounded collectors using the latest public sitemap entries.
- [ ] Re-run focused tests; refactor duplicated HTTP/header/date code only while green.

### Task 3: InThisWork HTML collector

**Files:**
- Create: `src/main/java/com/campusflow/domain/career/service/jobfeed/InThisWorkJobFeedCollector.java`
- Test: `src/test/java/com/campusflow/domain/career/service/jobfeed/InThisWorkJobFeedCollectorTest.java`

**Interfaces:**
- Consumes: `SitemapParser` and public WordPress article HTML.
- Produces: normalized `JobSearchResult` values with source `인디스워크`.

- [ ] Write fixture tests for the public title format `회사｜포지션 – IN THIS WORK`, article metadata, deadline extraction, and malformed pages.
- [ ] Run the focused test and confirm failure because the collector is absent.
- [ ] Implement bounded newest-sitemap discovery and tolerant Jsoup parsing without guessing unavailable fields.
- [ ] Re-run the focused test and confirm green.

### Task 4: Wanted public JSON collector

**Files:**
- Create: `src/main/java/com/campusflow/domain/career/service/jobfeed/WantedJobFeedCollector.java`
- Test: `src/test/java/com/campusflow/domain/career/service/jobfeed/WantedJobFeedCollectorTest.java`

**Interfaces:**
- Consumes: public `/api/v4/jobs` list JSON and `/api/v4/jobs/{id}` detail JSON.
- Produces: normalized `JobSearchResult` values with source `원티드`.

- [ ] Write list/detail fixture tests for pagination fields, company/location/career/deadline mapping, hidden or inactive job rejection, and missing optional values.
- [ ] Run the focused test and confirm the collector is missing.
- [ ] Implement one bounded list request plus detail enrichment with public-page headers, timeouts, and graceful empty fallback.
- [ ] Re-run the focused test and confirm green.

### Task 5: Import orchestration and regression coverage

**Files:**
- Modify: `src/main/java/com/campusflow/domain/career/service/JobImportService.java`
- Test: `src/test/java/com/campusflow/domain/career/service/JobImportServiceTest.java`

**Interfaces:**
- Consumes: injected `List<JobFeedCollector>`.
- Preserves: existing keyword-based JobKorea, Work24, Saramin, and Worknet behavior.

- [ ] Write Mockito tests proving each feed is called once per `importAll`, one thrown exception does not block later feeds, expired/blank-URL jobs are skipped, and duplicate URLs refresh rather than insert.
- [ ] Run the focused test and confirm it fails because feeds are not integrated.
- [ ] Add a once-per-run `importFeeds()` path and reuse a single `upsertResults(results, keyword)` helper.
- [ ] Re-run the focused test, then run the complete Maven suite.

### Task 6: Live source smoke verification

**Files:**
- Create: `src/test/java/com/campusflow/domain/career/service/jobfeed/JobFeedLiveSmokeTest.java`

**Interfaces:**
- Produces: opt-in tests guarded by `JOB_FEED_LIVE_TEST=true` so normal CI remains deterministic.

- [ ] Add one bounded live assertion per source: nonempty output, HTTPS URL, nonblank title/company, correct source, and non-expired deadline when present.
- [ ] Run normal `mvn.cmd test` and confirm smoke tests are skipped.
- [ ] Run with `JOB_FEED_LIVE_TEST=true` and confirm all four sources return structurally valid data.
- [ ] If any live source fails, add a fixture-backed regression test before changing its parser.

### Task 7: Remote assistant MCP tools

**Files:**
- Modify after inspection: `D:\claude_workspace\fleet_mcp\assistant.py`
- Modify after inspection: `D:\claude_workspace\fleet_mcp\server.py`
- Test after inspection: matching existing `D:\claude_workspace\fleet_mcp\tests\test_*.py`

**Interfaces:**
- Produces: read-only MCP tool `assistant_job_search(keyword: str = "", hide_expired: bool = true, limit: int = 20)`.
- Produces: read-only MCP tool `assistant_job_sources()` summarizing counts and freshness by source.
- Consumes: `http://127.0.0.1:8080/api/career/search/imported-jobs`.

- [ ] Inspect the actual MCP registration and HTTP-helper conventions without overwriting dirty files.
- [ ] Write failing Python tests for bounded query parameters, CampusFlow outage fallback, normalization, and MCP registration.
- [ ] Run focused pytest and confirm expected failures.
- [ ] Add the smallest HTTP delegation functions and register the tools with existing naming/error conventions.
- [ ] Re-run focused pytest, full remote pytest, MCP discovery, and one real MCP call.

### Task 8: Final verification and handoff

**Files:**
- Modify: `docs/superpowers/specs/2026-07-22-four-job-sources-design.md` only if verified behavior differs.

- [ ] Run the full Maven suite from a clean test invocation and record counts.
- [ ] Run four-source live smoke verification and record source counts.
- [ ] Run remote full pytest and MCP tool discovery/call.
- [ ] Inspect `git diff`/`git status` in both repositories and confirm only intended files overlap user work.
- [ ] Report exact files, tests, operational limits, and any pre-existing warnings; do not deploy or commit without a separate request.
