--liquibase formatted sql

--changeset ite4120:guidelines-91-demo-society-documents context:demo
--comment Demo rows from the specialist societies
/* Titles and years are real documents published by the Estonian specialist
   societies: the gynaecologists (ENS), the radiologists (ERY), the
   urologists (EUS) and the cardiologists (EKS). The CODES, however, are
   assigned here: society documents carry no registry code, which is itself
   part of the problem this register is a first step towards.
   Note the cardiology rows from 2001-2007. They are genuinely that old, and
   they are what makes the review report's overdue column say something. */
insert into guidelines.guideline (code, title, domain_code, document_type, published_on) values
    ('ENS-2025-01', 'Emakakaelavähi sõeluuringu tegevusjuhend',
     'CERVIX',     'TEGEVUSJUHEND', date '2025-01-01'),
    ('ENS-2021-04', 'Emakakaela, tupe ja vulva vähieelsete muutuste diagnoosimine, jälgimine ja ravi',
     'CERVIX',     'SELTSI_JUHEND', date '2021-01-01'),
    ('ERY-2020-01', 'Veenide ultraheliuuringu juhend',
     'RADIOLOGY',  'SELTSI_JUHEND', date '2020-03-03'),
    ('ERY-2019-02', 'Rinna MRT — EUSOBI soovitused naistele antava teabe kohta',
     'RADIOLOGY',  'SELTSI_JUHEND', date '2019-01-01'),
    ('EUS-2018-01', 'Kehavälise lööklaineliteripsia (ESWL) juhend',
     'PROSTATE',   'SELTSI_JUHEND', date '2018-01-01'),
    ('EKS-2001-01', 'Lipiidijuhised',
     'CARDIOLOGY', 'SELTSI_JUHEND', date '2001-01-01'),
    ('EKS-2004-01', 'Hüpertensiooni käsitlus',
     'CARDIOLOGY', 'SELTSI_JUHEND', date '2004-01-01'),
    ('EKS-2007-01', 'Krooniline südamepuudulikkus',
     'CARDIOLOGY', 'SELTSI_JUHEND', date '2007-01-01')
on conflict do nothing;
--
