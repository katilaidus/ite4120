--liquibase formatted sql

--changeset ite4120:guidelines-03-recommendation
--comment Recommendations carried by a guideline
/* The child side of the one-to-many: a recommendation cannot exist without its
   guideline, which the foreign key enforces in the database rather than in
   hopeful application code.
   strength follows GRADE, the grading system the Estonian handbook adopts:
   the direction (for / against) and the confidence (strong / weak) are one
   decision, so they are one column. PRACTICE is the handbook's "good practice
   statement" — advice with no graded evidence behind it. */
create table guidelines.recommendation (
    id                  bigint default nextval('core.seq_id') not null,
    guideline_id        bigint not null,
    statement           text not null,
    strength            text not null,
    evidence_level      text,
    sys_status          char(1),
    sys_version         int,
    sys_created_at      timestamptz,
    sys_created_by      text,
    sys_modified_at     timestamptz,
    sys_modified_by     text,
    constraint recommendation_pk primary key (id),
    constraint recommendation_guideline_fk foreign key (guideline_id) references guidelines.guideline (id),
    constraint recommendation_strength_chk check (strength in ('STRONG_FOR', 'WEAK_FOR', 'WEAK_AGAINST', 'STRONG_AGAINST', 'PRACTICE')),
    constraint recommendation_evidence_chk check (evidence_level is null or evidence_level in ('A', 'B', 'C', 'D'))
);

select core.create_table_metadata('guidelines.recommendation');

create index recommendation_guideline_idx on guidelines.recommendation (guideline_id) where (sys_status = 'A');
--
