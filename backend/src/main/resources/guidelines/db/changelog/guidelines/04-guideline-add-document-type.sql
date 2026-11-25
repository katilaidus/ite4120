--liquibase formatted sql

--changeset ite4120:guidelines-04-document-type
--comment Add the document type — schema evolution by a NEW changeset
/* Changesets 01-03 have been released, so they are immutable: this column
   arrives as its own changeset rather than an edit to 02-guideline.sql.
   Why it is needed: a clinical guideline is not the only kind of document
   that tells a doctor what to do. The national register publishes RJ-
   (clinical guideline) and PJ- (patient guideline); the specialist
   societies publish their own, and in two of the four domains under study
   the society document is the ONLY one that exists. A register that
   admitted national guidelines alone would report those domains as empty
   while clinicians are in fact working from something. */
alter table guidelines.guideline add column document_type text;

/* Backfill from the code prefix the national register already uses, so the
   column is never null for rows that existed before it. */
update guidelines.guideline set document_type = 'RAVIJUHEND'      where code like 'RJ-%';
update guidelines.guideline set document_type = 'PATSIENDIJUHEND' where code like 'PJ-%';
update guidelines.guideline set document_type = 'RAVIJUHEND'      where document_type is null;

alter table guidelines.guideline alter column document_type set not null;

alter table guidelines.guideline add constraint guideline_document_type_chk
    check (document_type in ('RAVIJUHEND', 'PATSIENDIJUHEND', 'SELTSI_JUHEND', 'TEGEVUSJUHEND'));

create index guideline_document_type_idx
    on guidelines.guideline (document_type) where (sys_status = 'A');
--
