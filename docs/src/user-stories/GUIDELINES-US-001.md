# GUIDELINES-US-001 · Register and review clinical guidelines

> **GUIDELINES-US-001** · state **Proposed** · traces to [GUIDELINES.01](../specifications/GUIDELINES.01-guideline-register.md)

**As** a clinical guideline coordinator at the Guidelines Council
**I want** a register of Estonian clinical guidelines, their recommendations and
their review dates
**so that** I can tell at any moment which guidelines are past the four-year
review deadline, and in which clinical domain the gap is worst — without opening
sixty PDFs.

## Context

Estonian clinical guidelines (*ravijuhendid*) are published as PDFs. The
handbook (*Ravijuhendi käsiraamat*) requires every guideline to be reviewed at
least **every four years**, but nothing computes that date: the coordinator
tracks it by hand, which means in practice that nobody tracks it at all.

This register holds the three things needed to answer the question
automatically — the guideline, the clinical domain it belongs to, and the
recommendations it carries. The review status is **derived**, never typed in:
an overdue guideline is overdue because of its publication date, not because
someone remembered to tick a box.

The coordinator who approved a guideline is identified by personal code only.
The name is **not** stored: it is fetched live from the population registry
through the approver-registry adapter (mock in this course, X-Road-shaped in
production). One source of truth per fact.

Scope is four clinical domains, matching the domains under study: cardiology,
prostate disease, radiology and cervical cancer. Excluded: the guideline text
itself (the register points at a document, it is not a document store), the
drafting workflow, and indicator definitions.

## Acceptance criteria

- A guideline can be registered with a code, title, clinical domain, publication
  date and an optional approver personal code.
- Every guideline reports whether it is **overdue for review** — derived from
  the publication date against the handbook's four-year rule, never stored.
- A guideline's recommendations are recorded with their GRADE strength and
  evidence level, and cannot exist without a guideline.
- Obvious nonsense is refused with a clear message: unknown domain, publication
  date in the future, malformed personal code.
- The register answers a domain-by-domain review report: how many guidelines,
  how many overdue, how many recommendations — in one call.
- For a guideline with an approver, the coordinator sees that person's current
  name, fetched live from the registry; when the registry is unreachable the
  guideline stays fully usable.
