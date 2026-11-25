# Defence demo — GUIDELINES.01

Ten minutes plus questions, in English, 27.11.

## Before you start

Three things must be running. Give yourself ten minutes for this — the
backend takes about five to start on this machine.

```bash
# 1. database
bash scripts/reset-db.sh          # only if it is not already up

# 2. backend  (from WSL — the Windows side cannot open a socket here)
wsl -d Ubuntu -- bash -lc "cd /mnt/c/Source/ite4120/backend && GRADLE_USER_HOME='/mnt/c/Users/Kati Laidus/.gradle' ./gradlew --no-daemon bootRun --args='--spring.profiles.active=local'"

# 3. frontend  (from Windows PowerShell)
cd C:\Source\ite4120\frontend; npm run dev
```

Open two browser tabs before you speak: `http://localhost:18640/guidelines`
and `http://localhost:18440/swagger-ui.html`.

---

## The ten minutes

### 1 · The problem  *(1 min — say this, do not show anything)*

Estonian clinical guidelines are published as PDFs. The handbook requires every
guideline to be reviewed at least every four years. Nothing computes that date,
so in practice nobody tracks it.

My doctoral work measures four clinical domains. This component is the smallest
useful thing that answers one question automatically: **which guidelines are
past their review deadline, and in which domain is the gap worst.**

### 2 · The specification came first  *(1 min)*

Two files, both in VS Code. Open them **before** you start speaking:

```
docs/src/user-stories/GUIDELINES-US-001.md        ← one page: As a / I want / so that
docs/src/specifications/GUIDELINES.01-guideline-register.md
```

In the Explorer that is `docs` → `src` → `user-stories` and `docs` → `src` →
`specifications`. Both folders already hold an `ANIMALS.*` file from the
template; yours is the `GUIDELINES.*` one directly beneath it.

Start with the user story (ten seconds — who needs this and why), then the
specification. Four places in it are worth pointing at, in this order:

| Where | What to say |
|---|---|
| The line under the title: *traces from GUIDELINES-US-001* | "Every spec names its story. Walk that link in either direction and you can see which requirement a piece of code exists for." |
| **Data model**, the note *"Why the document type exists"* | "A national guideline is not the only document that tells a clinician what to do. In two of my four domains the society document is the only one there is — so a register that admitted national guidelines alone would report those domains as empty while clinicians are working from something." |
| **Data model**, the note *"A note from the real data"* | "In the real register one code is shared by a guideline and its patient companions — `PJ-M/39.2-2026` appears three times. I require the code to be unique, which is a deliberate simplification, and I wrote down that it is one." |
| **API**, the *Failure policy* paragraph | "The fail-open decision is in the specification, not discovered in the code. It is a specification decision, as the lecture put it." |

Then scroll to the bottom, to **`## Business tests`**:

> These two sentences were written before any code. They are now two of the
> six executable tests. Spec first, tests second, implementation third.

### 3 · The application  *(3 min — the main event)*

**The list** — `/guidelines`

- Thirteen real documents from the Estonian register. The codes are genuine:
  `RJ-` is a national clinical guideline, `PJ-` a patient guideline, and
  `EKS-`/`ENS-`/`ERY-`/`EUS-` are specialist-society documents.
- The **Review** column is **derived, not stored** — the backend computes it
  from the publication date on every read. A stored flag would be wrong the day
  after it was written.
- Filter by domain, or switch on "only overdue".

**The report** — click *Review report*

This is the reporting operation, and the one query that joins all three tables.
Say the finding out loud:

> Radiology has two documents and **both are past the review deadline**.
> Cardiology has seven and four are overdue — including a lipid guideline from
> **2001**.

And then the design decision worth defending:

> A domain with no documents at all stays in this report showing zero. It would
> have been easy to drop empty rows, and it would have hidden exactly what a
> coordinator needs to see.

### 3b · The data layer  *(1.5 min)*

This is a graded criterion on its own, so show it rather than mention it.

```powershell
wsl -d Ubuntu -- bash /mnt/c/Source/ite4120/scripts/verify-guidelines-db.sh
```

Four blocks scroll past. Say one sentence about each:

1. **Three tables** in their own schema — `domain`, `guideline`,
   `recommendation`.
2. **Two foreign keys** — a recommendation cannot exist without its guideline,
   and the database enforces that, not hopeful application code.
3. **The Liquibase record.** Nine changesets, each with its author and the date
   it ran.

   > This is the point. The schema was not typed into a console — every table
   > came from a version-controlled migration, in order, and Liquibase keeps the
   > record. Drop the database and run it again and you get exactly this schema
   > back. That is also what the test run proves, against an empty container.

4. **The join** the report is built on, straight from SQL.

If you want to show the changesets themselves, they are in VS Code under
`backend/src/main/resources/guidelines/db/changelog/guidelines/`. Worth opening
`04-guideline-add-document-type.sql` for ten seconds:

> A released changeset is immutable. When I needed a new column, I added
> changeset 04 instead of editing 02 — because 02 had already run on a live
> database, and editing it would have made the history a lie.

### 4 · The API  *(2.5 min)*

**First the contract, for thirty seconds.** Open
`http://localhost:18440/swagger-ui.html` and scroll to the **Guidelines**
section — six endpoints.

> This page is generated from the controllers by springdoc, so it cannot drift
> from the code. It is not documentation I maintain; it is the contract, and
> this is how an integration partner would explore my API before writing a
> single line of client code.

The application is titled "Animals Register" because that is the template's
name — my component lives inside the same application, which is what the
assignment asks for. Worth saying out loud before anyone wonders.

**Then the script** — faster and more convincing than clicking:

```powershell
wsl -d Ubuntu -- bash /mnt/c/Source/ite4120/scripts/verify-guidelines-api.sh
```

Ten checks scroll past. **Five of them send deliberately broken requests** —
say so, because a screen full of 400s and 404s looks like failure unless you
name it:

| # | What the script sends | What the API must answer |
|---|---|---|
| 3 | a valid new guideline | **201** + a `Location` header — the one everybody forgets |
| 4 | id `999999`, which does not exist | **404**, as a problem document |
| 5 | publication date in 2099 | **400** — *"publishedOn must not be in the future"* |
| 6 | domain `KARDIOLOOGIA`, not in the list | **400**, naming the allowed values |
| 7 | a code already taken by an active guideline | **409** |
| 9 | personal code `00000000000` | **502**, and nothing else breaks |

> The second half of this script is deliberately broken requests. Every one
> comes back as a problem document with the right status, in
> `application/problem+json` — never HTML, never a stack trace, because a stack
> trace tells an attacker my framework, my versions and my internal structure.
>
> Showing only the happy path would prove nothing about the error model.

### 4b · The tests are green  *(1 min)*

Do **not** run `./gradlew test` live — it takes about seven minutes, because
every test class starts its own PostgreSQL through Testcontainers.

Show the report that the last run already produced. Open in the browser:

```
file:///C:/Source/ite4120/backend/build/reports/tests/test/index.html
```

It says **6 tests, 0 failures, 0 ignored**. Then say what that run also proves:

> Each of these ran against a real PostgreSQL in a throwaway container, so the
> same run proves the Liquibase changelog applies cleanly to an empty database.
> The schema is not something I built by hand once — it is rebuilt from the
> migrations every time.

If you want the source alongside it, the file is
`backend/src/test/java/ee/taltech/ite4120/guidelines/GuidelineBusinessRulesIT.java`
— every test there quotes the specification.

**Keep that report.** It lives under `build/`, which is gitignored, so running
`./gradlew clean` would delete it. Do not clean before the defence.

### 5 · X-Road  *(2 min — the strongest part)*

Two lines from the same script output:

```
8. X-ROAD: approver from the registry  -> 200  {"firstName":"Jaan","lastName":"Tamm"}
9. X-ROAD FAILURE                      -> 502  ...but the guideline 200, the report 200
```

Say:

> The register stores only the approver's personal code. The name belongs to
> the population register, so it is fetched live through an adapter — mock
> here, X-Road-shaped in production. The adapter interface does not change when
> the transport does.
>
> And the failure is deliberate: personal code `00000000000` always fails, so I
> can demonstrate it. The registry outage answers 502 on that one endpoint, and
> **nowhere else**. The guideline and the report keep working, because the
> register computes its main answer from a date it owns. No external system can
> stop it answering that.

### 6 · Close  *(1 min)*

Fourteen commits, all under my own name. Six tests green against a real PostgreSQL
via Testcontainers — which also proves the migrations replay clean from empty.
The specification matches what was built.

---

## The four questions you will be asked

**Why is the overdue flag not stored?**
Because it would be wrong the day after it was written. It is a function of the
publication date and today's date, so it is computed on read. The rule lives in
one place — `Guideline.isOverdue(publishedOn, today)` — and the list filter and
the report both use that same constant.

**Why do empty domains stay in the report?**
Both joins are LEFT and both are filtered in the ON clause. A filter in the
WHERE clause would silently turn it back into an inner join and drop them. In
my data two domains have no *national* guideline at all, and that absence is
the finding, not a missing row.

**Why fail open?**
The approver's name is auxiliary. The review deadline — the thing this register
exists to compute — comes from a date the register owns. Letting a registry
outage take down the whole page would make an external system's availability
into my register's availability, for no benefit. For an eligibility check
before dispensing medication the answer would be the opposite: fail closed.

**Why store only the personal code and not the name?**
One source of truth per fact. The population register owns names; a copy here
goes stale the day someone marries, and it is personal data I would then have
to protect, correct and erase. The easiest personal data to protect is the data
you did not collect.

---

## If something breaks during the demo

| Symptom | Cause | What to say |
|---|---|---|
| Backend will not start on Windows | `SocketException: Invalid argument` | "This machine cannot open a loopback socket from the Windows side — it kills Tomcat, Docker Desktop and Gradle alike. I build and run from WSL." |
| Frontend shows no rows | backend not up yet | reload after a few seconds; the list fetches on mount |
| `401` from the registry | mock not permitted in the security chain | already fixed in commit `3b44c55` — mention it as a bug the end-to-end check caught that reading the code did not |
| `pintf: command not found` | an old CRLF wrapper, no longer needed | run the script directly; `.gitattributes` keeps scripts LF now |
