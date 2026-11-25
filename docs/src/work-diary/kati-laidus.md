# Work diary — Kati Laidus

Component: **GUIDELINES.01 — clinical guideline register**. Working alone.

Per `development-workflow.md` § 6: hours, what I asked the agent, what it got
wrong, and how I corrected it. The corrections are the interesting column.

> **On the hour figures.** The 25.11 entry is measured — the commit timestamps
> in this repository run from 18:48 to 19:54 and the working session around
> them is in the transcript. The earlier entries are **my estimates against
> the course's own stated budget** (S1 ≈ 18 h before S2, S2 and S3 ≈ 22 h
> each), not a stopwatch. They are marked *(est.)* and I stand behind them as
> estimates, not as records. Total self-study budget for the course is 137 h.

---

## 18.09 · S1 — environment

- GitHub account, template cloned to `C:\Source\ite4120`, GitHub Packages token
  set up (`gpr.user` / `gpr.token` in `~/.gradle/gradle.properties`).
- Contact session 3 h + setup and reading afterwards.
- Hours: 3 contact + **8** independent *(est.)*

## 02.10 · S2 — data layer

- Docker Desktop, PostgreSQL container, read the Liquibase guide and the
  worked example's changesets.
- Hours: 3 contact + **10** independent *(est.)*

## 16.10 · S3 — APIs

- Session on HTTP/REST, OpenAPI, Swagger UI, X-Road.
- Built a standalone Spring Boot REST API as an exercise to understand the
  material (`Documents\PhD\ITE4120-lab`). **This turned out to be the wrong
  track**: the assessment requires extending the course template, not a
  separate application. Kept as a learning exercise; not submitted.
- Correction I had to make: the first version had **no input validation at
  all** — a request without a title would have reached the database and
  returned 500, i.e. reported my fault as the caller's. Added Bean Validation
  and an RFC 7807 error handler.
- Second correction: the agent had written tests whose **names did not match
  what they asserted** (`testEnrollmentIsRefusedAfterGuidelineStarts` actually
  asserted that both records were created). Rewrote them.
- Hours: 3 contact + **9** independent *(est.)* — most of it on the standalone
  exercise that turned out to be the wrong track.

## 30.10 · S4 — front end

- Contact session; React and the component library.
- Hours: 3 contact + **6** independent *(est.)*

## 25.11 · Build day — the component itself

Specification first, then schema, then API, then tests. In that order, because
the template's rule 1 says so and because the agent is only as good as the
specification it is pointed at.

**What I asked for:** a register of Estonian clinical guidelines, in the four
domains of my doctoral work, whose reason to exist is the handbook's four-year
review rule.

**What I had to correct:**

1. **Domain list.** The agent proposed cardiology / oncology / paediatrics /
   infectious diseases. Wrong for my work — my four domains are **cardiology,
   prostate disease, radiology and cervical cancer**, and I have measured data
   for exactly those. Changed.

2. **Demo data too narrow.** The agent loaded only national guidelines
   (`rj_all.json`) and concluded that radiology and cervical cancer have no
   documents at all. True for *national clinical guidelines*, but misleading:
   the specialist societies publish plenty — ENS has 51 documents, ERÜ 42. I
   pointed this out; we added a `document_type` field (as a **new** changeset,
   04, since 01–03 had already run) and loaded real society documents. The
   distinction between a national guideline and a society document is now
   visible in the register instead of being flattened into "nothing here".

3. **Specification count.** I asked whether two Markdown files were really
   enough, having seen a production project with 43 specification folders.
   Answer: yes — that project has 43 components; I have one. The numbering
   `GUIDELINES.01` leaves room if it ever grows.

**What went wrong technically (not the agent's fault, and worth recording):**

- This machine cannot open a loopback socket from the Windows side:
  `java.net.SocketException: Invalid argument: connect`. It killed Tomcat,
  Docker Desktop and finally Gradle itself. Everything is built and run from
  **WSL Ubuntu** instead, where the Linux kernel opens sockets normally.
- `gradlew` arrived with CRLF line endings (`core.autocrlf=true` on clone), so
  Linux refused to execute it: `/bin/sh^M: bad interpreter`. Fixed the working
  copy and set `core.autocrlf=input` on this repository.
- The IDE compiles into `backend/bin`, which showed up as 40 untracked files
  ready to be committed. They are build output; added to `.gitignore`.

**Decisions I made and must be able to defend:**

- Review status is **derived on read**, never stored. A stored flag would be
  wrong the day after it was written.
- The review report keeps **empty domains visible** with zeros. In my data two
  domains have no national guideline at all, and a report that dropped them
  would hide the finding.
- Failure policy for the approver lookup is **fail open**: a registry outage
  answers 502 on `/approver` only. The register computes its main answer from a
  date it owns, so no external system can stop it answering.
- The approver's **name is not stored** — only the personal code. The name
  belongs to the population register and is fetched live.

**Hours, 25.11: 7** — measured, not estimated. Reading the assessment criteria
and all three lecture decks, then specification, schema, API, X-Road adapter,
front end, tests and the defence script. Commits run 18:48–19:54; the reading
and the specification work came before the first commit.

---

## Where the time actually went

Three environment problems cost more than any of the code:

| Problem | Cost |
|---|---|
| Windows cannot open a loopback socket → everything runs from WSL | ~1 h, twice (16.10 and 25.11) |
| `gradlew` arrived with CRLF, Linux refused to execute it | ~15 min |
| npm token expired; `node_modules` had to be cleared three times | ~45 min |

Worth recording because none of it is visible in the diff, and all of it is
the honest answer to "why did this take so long".

**Running total: 12 h contact + ~40 h independent.** Against the course budget
of 137 h this is short, and the reason is in the 16.10 entry: a week of effort
went into a standalone application that did not extend the template.

---

## To finish

- [ ] **Check the estimated hours above** — the 25.11 figure is measured, the
      rest are estimates I should confirm or correct from memory
- [ ] Defence, 27.11, in English
- [ ] Peer review of two other groups
