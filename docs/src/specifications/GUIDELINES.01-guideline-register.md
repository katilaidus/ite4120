# GUIDELINES.01 · Clinical guideline register

> **GUIDELINES.01** · state **Proposed** · traces from [GUIDELINES-US-001](../user-stories/GUIDELINES-US-001.md)
> · source `backend/src/main/java/ee/taltech/ite4120/guidelines/`, `backend/src/main/resources/guidelines/db/changelog/`, `frontend/src/pages/guidelines/`

## Description

A register of Estonian clinical guidelines: create, search, view, update, retire
(soft delete), plus the recommendations each guideline carries. The register's
reason to exist is the **four-year review rule** from the *Ravijuhendi
käsiraamat*: review status is derived from the publication date on every read,
never stored and never typed in.

The approver is held as a personal code only; the name is resolved live from the
population registry through the approver-registry adapter (mock in this course,
X-Road-shaped in production).

Excluded: the guideline text itself (the register links to a document, it does
not store one), the drafting and approval workflow, indicator definitions, and
authentication management (mock auth only).

## Data model  *(→ changesets 01–03)*

| Table | Field | Type | Rules |
| --- | --- | --- | --- |
| `guidelines.domain` | `code` PK | text | reference list: CARDIOLOGY, PROSTATE, RADIOLOGY, CERVIX |
| | `name` | text | Estonian display name |
| `guidelines.guideline` | `id` PK | bigint | from `core.seq_id` |
| | `code` | text | required; registry code, e.g. `PJ-I/4.3-2026`; **unique among active rows** |
| | `title` | text | required, ≤500 |
| | `domain_code` | text | required; FK → `domain` |
| | `published_on` | date | required; **not in the future** |
| | `approver_isikukood` | text | optional; exactly 11 digits — a string, never a number |
| | `source_url` | text | optional, ≤1000 |
| | `sys_*` | — | platform columns, trigger-managed |
| `guidelines.recommendation` | `id` PK | bigint | from `core.seq_id` |
| | `guideline_id` | bigint | required; FK → `guideline` |
| | `text` | text | required, ≤2000 |
| | `strength` | text | required; GRADE: STRONG_FOR, WEAK_FOR, WEAK_AGAINST, STRONG_AGAINST, PRACTICE |
| | `evidence_level` | text | optional; A, B, C or D |
| | `sys_*` | — | platform columns, trigger-managed |

**Derived, not stored:** `overdueForReview` = `published_on` is more than four
years before today. An unknown date cannot occur (the field is required), so the
answer is always defined.

**A note from the real data.** In the national register, a guideline code is
shared by the main guideline and its patient-facing companions — `PJ-M/39.2-2026`
appears three times with three different titles. This register requires the code
to be unique among active rows, which is a deliberate simplification: it models
the guideline, not its derived publications.

## API  *(→ session 3)*

| Method + path | Does | Failure answers |
| --- | --- | --- |
| `GET /api/guidelines` | paged search: `textContains`, `domainCode`, `overdueOnly`, `limit`, `offset`, `sort` | — |
| `GET /api/guidelines/{id}` | one active guideline | 404 |
| `POST /api/guidelines` | register; answers **201 + Location** | 400 validation · 409 duplicate code |
| `PUT /api/guidelines/{id}` | update; code immutable | 400 · 404 |
| `DELETE /api/guidelines/{id}` | retire (soft) — **204** | 404 |
| `GET /api/guidelines/{id}/recommendations` | the guideline's recommendations | 404 |
| `POST /api/guidelines/{id}/recommendations` | add one; **201 + Location** | 400 · 404 |
| **`GET /api/guidelines/review-report`** | **reporting operation** — one row per domain: guidelines, overdue count, recommendations, oldest publication date | — |
| `GET /api/guidelines/{id}/approver` | approver name via the registry adapter | 404 · 502 registry down |
| `GET /api/guidelines/domains` | the reference list | — |

Every error is `application/problem+json`. All endpoints require
`Authorization: Bearer <user>` (mock auth); the contract is live at
`/swagger-ui.html`.

The review report is the one query that joins all three tables: domain left
join guideline left join recommendation, grouped by domain — so a domain with
no guidelines still reports a zero rather than vanishing.

**Failure policy** for the approver lookup: **fail open** — an unreachable
registry answers 502 on the `/approver` endpoint only; the guideline itself, the
search and the review report stay fully usable. Rationale: the approver's name
is auxiliary. The review deadline is computed from a date this register owns,
so no external system can prevent the register from answering its main question.

## UI  *(→ session 4)*

| Screen | Element | Component | Rules |
| --- | --- | --- | --- |
| Guideline list `/guidelines` | table | `ResourceList` | columns: code (locked, **a link to the guideline's page**), title, domain tag, published, overdue flag; search field; filter by domain; toggle "only overdue" |
| | register button | `AppButtonPrimary` | navigates to the form |
| | overdue cell | `Tag` | red when overdue, neutral otherwise — the derived field, never editable |
| Guideline page `/guidelines/{id}` | view / edit | `ResourceForm` + `useDataController` | sections Identity (code read-only, title, domain) and Details (published date, approver code, source URL); Edit → Save is `PUT`, Cancel restores; Retire is the soft `DELETE` with confirmation |
| | recommendations | `ResourceList` inside the page | text, strength tag, evidence level; add via the `POST` endpoint |
| | approver | `ResourceForm` field, view mode | the personal code plus the name via `/approver`; omitted when the registry fails — the page never depends on it |
| | metadata | `ResourceForm.sidebar` — `AppCard` of `FieldItem`s | created at / by, modified at / by, version — platform `sys*` columns, read-only |
| Register form | code | `Input` | required, ≤50 |
| | title | `Input` | required, ≤500 |
| | domain | `Select` | required, options from `/domains` |
| | published date | `DatePicker` | required, not in the future |
| | approver code | `Input` | pattern `\d{11}` |
| | submit | `AppButtonPrimary` | 400/409 problem text shown verbatim in a notification |

## Business tests  *(→ executable in `GuidelineBusinessRulesIT`)*

1. **A guideline is overdue for review four years after publication.**
   Given a guideline published on 2020-01-01, when it is read today, then it
   reports `overdueForReview = true`; and given one published two years ago,
   then it reports `false`. The boundary belongs to the guideline: exactly four
   years to the day is **not** yet overdue.

2. **A guideline code is unique among active guidelines.**
   Given an active guideline with code `PJ-I/4.3-2026`, when a second guideline
   claims the same code, then the registration conflicts (409). And when the
   first is retired, the code may be used again — retirement is a soft delete.
