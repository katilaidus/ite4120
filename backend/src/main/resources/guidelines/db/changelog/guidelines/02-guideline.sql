--liquibase formatted sql

--changeset ite4120:guidelines-02-guideline
--comment The main business table — one row per clinical guideline
/* Platform idioms, same as the worked example:
   - id from the SHARED sequence core.seq_id;
   - sys_* columns declared here, FILLED by the core.sys_columns() trigger,
     which core.create_table_metadata() installs — never by hand;
   - soft delete: rows are never removed, sys_status goes A -> C;
   - approver_isikukood is TEXT — an identifier, never a number.
   Note what is NOT here: a review-due date. Overdue status is DERIVED from
   published_on against the handbook's four-year rule, computed on every read.
   A stored flag would be wrong the day after it was written. */
create table guidelines.guideline (
    id                  bigint default nextval('core.seq_id') not null,
    code                text not null,
    title               text not null,
    domain_code         text not null,
    published_on        date not null,
    approver_isikukood  text,
    source_url          text,
    sys_status          char(1),
    sys_version         int,
    sys_created_at      timestamptz,
    sys_created_by      text,
    sys_modified_at     timestamptz,
    sys_modified_by     text,
    constraint guideline_pk primary key (id),
    constraint guideline_domain_fk foreign key (domain_code) references guidelines.domain (code),
    constraint guideline_isikukood_chk check (approver_isikukood is null or approver_isikukood ~ '^[0-9]{11}$')
);

select core.create_table_metadata('guidelines.guideline');

/* Unique among ACTIVE rows only: a retired guideline's code may be reused. */
create unique index guideline_code_ukey
    on guidelines.guideline (code) where (sys_status = 'A');

create index guideline_domain_idx on guidelines.guideline (domain_code) where (sys_status = 'A');

/* The review report filters and sorts on this date across the whole table. */
create index guideline_published_idx on guidelines.guideline (published_on) where (sys_status = 'A');
--
