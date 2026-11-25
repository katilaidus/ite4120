--liquibase formatted sql

--changeset ite4120:guidelines-90-demo-data context:demo
--comment Demo guidelines, taken from the national register
/* Real records: codes, titles and publication dates come from the published
   Estonian guideline register, not from imagination. Two things are visible
   here on purpose.
   First, the codes: RJ- is a clinical guideline, PJ- a patient guideline.
   Second, the gap. Of the four domains under study only cardiology and
   prostate disease have published national guidelines at all; radiology and
   cervical cancer have none, so the review report answers zero for them.
   That emptiness is the finding, not a gap in the demo data.
   Convergent: ON CONFLICT DO NOTHING lets this land twice safely. */
insert into guidelines.guideline (code, title, domain_code, published_on) values
    ('PJ-I/31.1-2019', 'Kodade virvendusarütmia (KVA) kui haiguse olemus, vormid, tüsistused, sümptomid, diagnoosimine', 'CARDIOLOGY', date '2021-10-26'),
    ('PJ-I/54.1-2021', 'Kroonilise südamepuudulikkusega täiskasvanud patsiendi käsitlus esmatasandil', 'CARDIOLOGY', date '2023-04-03'),
    ('RJ-I/21.2-2023', 'Kroonilise venoosse haavandi käsitlus (ajakohastatud)', 'CARDIOLOGY', date '2023-10-10'),
    ('RJ-I/1.3-2026', 'Täiskasvanute kõrgvererõhktõve käsitlus esmatasandil (ajakohastatud)', 'CARDIOLOGY', date '2026-08-06'),
    ('PJ-Z/67.1-2023', 'Eesnäärmevähi varane avastamine PSA sisalduse määramisega veres', 'PROSTATE', date '2023-09-29')
on conflict do nothing;
--

--changeset ite4120:guidelines-90-demo-recommendations context:demo
--comment Illustrative recommendations for the hypertension guideline
/* Unlike the guidelines above, these recommendation statements are
   ILLUSTRATIVE — the register's own recommendation records do not exist in
   machine-readable form anywhere yet, which is precisely the problem this
   component is a first step towards. The GRADE strengths and evidence levels
   follow the handbook's vocabulary. */
insert into guidelines.recommendation (guideline_id, statement, strength, evidence_level)
select g.id, v.statement, v.strength, v.evidence_level
from guidelines.guideline g
cross join (values
    ('Mootke vererohku vahemalt kord aastas igal taiskasvanul.', 'STRONG_FOR', 'B'),
    ('Alustage eluviisinoustamisega enne ravimravi kaalumist.',   'STRONG_FOR', 'A'),
    ('Kaaluge kodust vererohu monitoorimist diagnoosi kinnitamiseks.', 'WEAK_FOR', 'C'),
    ('Dokumenteerige ravieesmark patsiendi kaardil.', 'PRACTICE', null)
) as v(statement, strength, evidence_level)
where g.code = 'RJ-I/1.3-2026'
on conflict do nothing;
--
