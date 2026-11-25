--liquibase formatted sql

--changeset ite4120:guidelines-01-domain
--comment Clinical domain reference table
/* The lookup list the guideline table points at. Natural-key reference data:
   no core.seq_id, no sys columns. Compare with 02-guideline.sql, which is a
   full business table. */
create table guidelines.domain (
    code                text not null,
    name                text not null,
    constraint domain_pk primary key (code)
);
--

--changeset ite4120:guidelines-01-domain-seed
--comment Seed the four clinical domains under study
/* The four domains of the research this register serves. Names are Estonian
   because that is how the Guidelines Council names them; codes are the stable
   machine identifiers. Convergent: ON CONFLICT DO NOTHING lets this land on a
   database that already holds some of the rows. */
insert into guidelines.domain (code, name) values
    ('CARDIOLOGY', 'Kardioloogia'),
    ('PROSTATE',   'Eesnäärmehaigused'),
    ('RADIOLOGY',  'Radioloogia'),
    ('CERVIX',     'Emakakaelavähk')
on conflict do nothing;
--
