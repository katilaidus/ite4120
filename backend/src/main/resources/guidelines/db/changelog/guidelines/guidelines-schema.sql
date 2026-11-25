--liquibase formatted sql

--changeset ite4120:guidelines-schema
--comment Create the guidelines schema
/* One schema per component — boundaries in the database mirror boundaries in
   the code. */
create schema if not exists guidelines;
--
