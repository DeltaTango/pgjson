CREATE SEQUENCE public.data_object_id_seq
    START WITH 1
    INCREMENT BY 1 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.data_object
(
    id           bigint DEFAULT nextval('public.data_object_id_seq'::regclass) NOT NULL,
    tabledef_id  bigint                                                   NOT NULL,
    json_data    jsonb                                                    NOT NULL,
    id_uuid      character varying(50)                                    NOT NULL,
    data_created timestamp with time zone                                 NOT NULL,
    data_changed timestamp with time zone
);

CREATE SEQUENCE public.tabledef_id_seq
    START WITH 1
    INCREMENT BY 1 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.tabledef
(
    id               bigint DEFAULT nextval('public.tabledef_id_seq'::regclass) NOT NULL,
    schema           text                                                       NOT NULL,
    schema_name      character varying(50)                                      NOT NULL,
    table_name       character varying(25)                                      NOT NULL,
    id_uuid          character varying(50)                                      NOT NULL,
    schema_timestamp timestamp with time zone                                   NOT NULL,
    schema_hash      character varying(64)                                      NOT NULL
);




