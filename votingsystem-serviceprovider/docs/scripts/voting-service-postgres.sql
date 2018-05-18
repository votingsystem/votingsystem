drop table if exists vote cascade;
drop table if exists signature cascade;
drop table if exists election_option cascade;
drop table if exists election cascade;
drop table if exists certificate cascade;
drop table if exists signer cascade;
drop table if exists address cascade;
drop table if exists signed_document cascade;
drop table if exists anon_vote_cert_request cascade;
drop table if exists device cascade;
drop table if exists session_certification cascade;
drop table if exists user_csr_request cascade;

create table address
(
  id bigserial not null
    constraint address_pkey
    primary key,
  city varchar(48) not null,
  country varchar(255),
  date_created timestamp,
  last_update timestamp,
  meta_inf varchar(255),
  name varchar(255),
  postal_code varchar(10),
  province varchar(48) not null
)
;

create table signed_document
(
  dss_document_type varchar(31) not null,
  id bigserial not null
    constraint signed_document_pkey
    primary key,
  body text,
  date_created timestamp,
  indication varchar(255) not null,
  last_updated timestamp,
  message_digest varchar(255)
    constraint uk_kym3w8recwem51x5elw5prpfh
    unique,
  meta_inf text,
  operation_type varchar(255) not null,
  receipt_id bigint
    constraint fke7evx50odtu3h6peavwsri60u
    references signed_document
)
;

create table signer
(
  id bigserial not null
    constraint signer_pkey
    primary key,
  iban varchar(255),
  uuid varchar(255),
  cn varchar(255),
  country varchar(255),
  date_activated timestamp,
  date_canceled timestamp,
  date_created timestamp,
  description text,
  type_id varchar(255),
  email varchar(255),
  entity_id varchar(255),
  last_update timestamp,
  meta_inf text,
  name varchar(255),
  num_id varchar(255),
  phone varchar(255),
  state varchar(255),
  surname varchar(255),
  type varchar(255) not null,
  url varchar(255),
  address_id bigint
    constraint fkocei8upe8jwhesp36363rslap
    references address
)
;

create table certificate
(
  id bigserial not null
    constraint certificate_pkey
    primary key,
  uuid varchar(255),
  cancel_date timestamp,
  content bytea not null,
  is_root boolean,
  meta_inf text,
  revocation_hash varchar(255)
    constraint uk_opo2wx0v65dxqxynwld6s3dyu
    unique,
  serial_number bigint not null,
  state varchar(255) not null,
  state_date timestamp,
  subject_dn varchar(255),
  type varchar(255) not null,
  valid_from timestamp,
  valid_to timestamp,
  authority_certificate_id bigint
    constraint fk2hooicnj2a9de94lakk8qsf6q
    references certificate,
  signed_document_id bigint
    constraint fkigd5guas4wh1ya6sbqmmu3bhj
    references signed_document,
  signer_id bigint
    constraint fkmnq5s9rjqjlyb67wv1ecwo4an
    references signer,
  constraint ukijktj9utnk30yr3yklndd7hfk
  unique (serial_number, subject_dn)
)
;

create table election
(
  id bigserial not null
    constraint election_pkey
    primary key,
  backup_available boolean,
  content text,
  date_begin timestamp not null,
  date_canceled timestamp,
  date_created timestamp,
  date_finish timestamp not null,
  entity_id varchar(255),
  last_update timestamp,
  meta_inf text,
  state integer,
  subject varchar(255),
  uuid varchar(255)
    constraint uk_gdplyri7xhaj0utyg0wnp8u1d
    unique,
  certificate_id bigint
    constraint fkg8wfpugpemww9a2gcdul73e73
    references certificate,
  publisher_id bigint
    constraint fk8v5sbhckqgm0snnrdqm5k37cd
    references signer,
  signed_document_id bigint
    constraint fkkrfflgcyxk2jjkico7b2vnr5w
    references signed_document
)
;

create table election_option
(
  id bigserial not null
    constraint election_option_pkey
    primary key,
  content varchar(10000) not null,
  date_created timestamp,
  last_update timestamp,
  election bigint not null
    constraint fkrd2rq8ccdgxyiauu6j81w8vgp
    references election
)
;

create table signature
(
  id bigserial not null
    constraint signature_pkey
    primary key,
  signature_date timestamp,
  signature_id varchar(255) not null,
  ca_certificate_id bigint
    constraint fk4qt85akkgmnfte44t2xnveay7
    references certificate,
  document_id bigint not null
    constraint fk97qu4vxw80b41x8kvyae0bdv0
    references signed_document,
  signer_id bigint
    constraint fk2touhs5g8y358tqasfd96pecn
    references signer,
  signer_certificate_id bigint
    constraint fks2mgcdiwnkvvu3pj63csqt0p
    references certificate,
  constraint uk52wv2brkqojl95cxchxih2g3n
  unique (signature_id, document_id)
)
;

create table vote
(
  id bigserial not null
    constraint vote_pkey
    primary key,
  date_created timestamp,
  identity_service_entity_id varchar(255) not null,
  last_update timestamp,
  state varchar(255) not null,
  certificate_id bigint
    constraint fkb57r6k1j2l1heycx1ukfc0vx3
    references certificate,
  election_id bigint
    constraint fkhode5yg62tlloicyys0w7oamt
    references election,
  option_selected_id bigint
    constraint fkmm0bani8f8l0dgpt1xx7jaeti
    references election_option,
  signed_document_id bigint
    constraint fk82bdyahbyyg9c96q4ypvwx72q
    references signed_document
)
;

