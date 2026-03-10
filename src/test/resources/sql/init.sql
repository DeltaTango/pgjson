CREATE SEQUENCE public.table1_id_seq
    START WITH 1
    INCREMENT BY 1 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.table1
(
    id           bigint DEFAULT nextval('public.table1_id_seq'::regclass) NOT NULL,
    tabledef_id  bigint                                                   NOT NULL,
    json_data    jsonb                                                    NOT NULL,
    id_uuid      character varying(50)                                    NOT NULL,
    data_created timestamp with time zone                                 NOT NULL,
    data_changed timestamp with time zone
);

CREATE SEQUENCE public.table2_id_seq
    START WITH 1
    INCREMENT BY 1 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.table2
(
    id           bigint DEFAULT nextval('public.table2_id_seq'::regclass) NOT NULL,
    tabledef_id  bigint                                                   NOT NULL,
    json_data    jsonb                                                    NOT NULL,
    id_uuid      character varying(50)                                    NOT NULL,
    data_created timestamp with time zone                                 NOT NULL,
    data_changed timestamp with time zone
);

CREATE SEQUENCE public.table3_id_seq
    START WITH 1
    INCREMENT BY 1 NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.table3
(
    id           bigint DEFAULT nextval('public.table3_id_seq'::regclass) NOT NULL,
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

CREATE TABLE public.test
(
    test text
);

---- sample data ----

INSERT INTO public.table1
VALUES (1, 1, '{
  "attribute1": "in",
  "attribute2": "nostrud do adipisicingXXXXXXX ipsum nulla",
  "attribute3": {
    "attribute7": "in",
    "attribute8": "A"
  },
  "attribute4": [
    {
      "attribute9": "X",
      "attribute10": "1234567890"
    },
    {
      "attribute9": "Y",
      "attribute10": "0987654321"
    },
    {
      "attribute9": "Z",
      "attribute10": "5678901234"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "1234567890"
      },
      {
        "attribute11": "0987654321"
      },
      {
        "attribute11": "5678901234"
      }
    ]
  }
}', 'f54be158-0ff9-49d1-a070-954e6b64caac', now(), null);

INSERT INTO public.table1
VALUES (2, 1, '{
  "attribute1": "sit amet dolore",
  "attribute2": "dolor ea veniam",
  "attribute3": {
    "attribute7": "non magna Duis reprehenderit",
    "attribute8": "C"
  },
  "attribute4": [
    {
      "attribute9": "Z",
      "attribute10": "sed aliqua id minim magna"
    },
    {
      "attribute9": "X",
      "attribute10": "proident voluptate"
    },
    {
      "attribute9": "Y",
      "attribute10": "Excepteur laboris aliquip"
    },
    {
      "attribute9": "Y",
      "attribute10": "Duis"
    },
    {
      "attribute9": "X",
      "attribute10": "laboris"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "Ut ea"
      },
      {
        "attribute11": "elit amet mollit dolor sunt"
      },
      {
        "attribute11": "exercitation tempor"
      },
      {
        "attribute11": "sit exercitation sunt"
      }
    ]
  }
}', 'c11c49b7-6687-47f3-ac4f-b0b6b17626e4', now(), null);

INSERT INTO public.table1
VALUES (3, 1, '{"attribute1": "do consectetur proident culpa",
  "attribute2": "non ad mollit aliqua",
  "attribute3": {
    "attribute7": "ullamco pariatur mollit",
    "attribute8": "B"
  },
  "attribute4": [
    {
      "attribute9": "Z",
      "attribute10": "occaecat ut"
    },
    {
      "attribute9": "Z",
      "attribute10": "do voluptate ut in deserunt"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "exercitation adipisidsfscing"
      },
      {
        "attribute11": "est minim"
      },
      {
        "attribute11": "in"
      },
      {
        "attribute11": "aute nulla do"
      }
    ]
  }
}', 'ec49407c-656f-4058-93ba-cfb7676c43ca', now(), null);

INSERT INTO public.table1
VALUES (4, 1, '{"attribute1": "Excepteur reprehenderit proident dolor",
  "attribute2": "sunt pariatur laborum",
  "attribute3": {
    "attribute7": "nisi ex ipsum aute sint",
    "attribute8": "C"
  },
  "attribute4": [
    {
      "attribute9": "X",
      "attribute10": "ex cupidatat ut labore"
    },
    {
      "attribute9": "Y",
      "attribute10": "enim esse anim dolor"
    },
    {
      "attribute9": "Z",
      "attribute10": "fugiat"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "cupidatat labore"
      },
      {
        "attribute11": "laborum sint"
      },
      {
        "attribute11": "non quis est tempor nulla"
      },
      {
        "attribute11": "ut ad officia in"
      }
    ]
  }
}', '1911df95-d580-4268-89f6-9d18bcbb3c6f', now(), null);

INSERT INTO public.table1
VALUES (5, 1, '{"attribute1": "do",
  "attribute2": "culpa exercitation pariatur incididunt magna",
  "attribute3": {
    "attribute7": "ipsum pariatur veniam do",
    "attribute8": "C"
  },
  "attribute4": [
    {
      "attribute9": "X",
      "attribute10": "proident esse ut sit"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "fugiat ut"
      },
      {
        "attribute11": "deserunt sint dolore Lorem"
      },
      {
        "attribute11": "cillum commodo in"
      },
      {
        "attribute11": "elit"
      }
    ]
  }
}', 'f1112cd1-463e-41ca-9041-873a862e40a9', now(), null);

INSERT INTO public.table1
VALUES (6, 1, '{"attribute1": "incididunt nulla",
  "attribute2": "nisi laborum tempor ullamco",
  "attribute3": {
    "attribute7": "qui",
    "attribute8": "A"
  },
  "attribute4": [
    {
      "attribute9": "Z",
      "attribute10": "eiusmod reprehenderit sed"
    },
    {
      "attribute9": "Z",
      "attribute10": "ad in Lorem"
    },
    {
      "attribute9": "X",
      "attribute10": "Lorem irure elit labore"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "veniam ut elit voluptate pariatur"
      }
    ]
  }
}', '33bdd316-6093-478f-9fef-963cf0e22d03', now(), null);

INSERT INTO public.table1
VALUES (7, 1, '{"attribute1": "occaecat",
  "attribute2": "veniam irure commodo minim",
  "attribute3": {
    "attribute7": "Ut veniam ipsum",
    "attribute8": "B"
  },
  "attribute4": [
    {
      "attribute9": "X",
      "attribute10": "tempor velit anim veniam ut"
    },
    {
      "attribute9": "Z",
      "attribute10": "elit eiusmod occaecat irure"
    },
    {
      "attribute9": "Z",
      "attribute10": "in"
    },
    {
      "attribute9": "Y",
      "attribute10": "consectetur sunt mollit"
    },
    {
      "attribute9": "Z",
      "attribute10": "eu in aute"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "adipissdfssficing sed ad ullamco"
      }
    ],
    "attribute12": {
      "attribute13": "sfsdlkfjsdlfjds"
    }
  }
}', '5a458cfb-cd4d-4f15-91f1-c1f0e7cd32cb', now(), null);

INSERT INTO public.table1
VALUES (8, 1, '{"attribute1": "magna fugiat cupidatat commodo reprehenderit",
  "attribute2": "veniam ipsum labore magna",
  "attribute3": {
    "attribute7": "aliqua",
    "attribute8": "A"
  },
  "attribute4": [
    {
      "attribute9": "Z",
      "attribute10": "ut minim"
    },
    {
      "attribute9": "Z",
      "attribute10": "cupidatat ullamco nostrud fugiat"
    },
    {
      "attribute9": "Y",
      "attribute10": "aute sint aliquip"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "sunt"
      },
      {
        "attribute11": "labore aute"
      },
      {
        "attribute11": "ut veniam fugiat nulla eiusmod"
      }
    ]
  }
}', 'caffe464-b867-456c-b40a-e7803e3ac113', now(), null);

INSERT INTO public.table1
VALUES (9, 1, '{"attribute1": "officia pariatur eu",
  "attribute2": "qui quis consequat id veniam",
  "attribute3": {
    "attribute7": "in elit nostrud",
    "attribute8": "C"
  },
  "attribute4": [
    {
      "attribute9": "X",
      "attribute10": "ea commodo nisi consequat"
    },
    {
      "attribute9": "X",
      "attribute10": "in officia"
    },
    {
      "attribute9": "Z",
      "attribute10": "consectetur deserunt ex et Ut"
    },
    {
      "attribute9": "Z",
      "attribute10": "anim dolor"
    },
    {
      "attribute9": "X",
      "attribute10": "ut"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "est amet enim"
      },
      {
        "attribute11": "dolor qui in"
      },
      {
        "attribute11": "sfdssdf qui consequat in"
      },
      {
        "attribute11": "cillum"
      },
      {
        "attribute11": "in esse fugiat labore"
      }
    ]
  }
}', '9bfcb02a-942a-4f73-861e-80ad99f1b6d3', now(), null);

INSERT INTO public.table1
VALUES (10, 1, '{"attribute1": "et consequat",
  "attribute2": "gadsghöfdjfsdljodfjöofdjgd",
  "attribute3": {
    "attribute7": "Duis voluptate amet commodo",
    "attribute8": "B"
  },
  "attribute4": [
    {
      "attribute9": "X",
      "attribute10": "laborum"
    },
    {
      "attribute9": "Y",
      "attribute10": "commodo do"
    },
    {
      "attribute9": "Z",
      "attribute10": "ad non"
    },
    {
      "attribute9": "Z",
      "attribute10": "in"
    },
    {
      "attribute9": "X",
      "attribute10": "dolore quis"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "cupidatat dolor",
        "attribute12": "sdfsdfdsdsfdsfsdfds"
      }
    ],
    "attribute12": {
      "attribute13": "sgdsfdhgfhghgf"
    }
  }
}', '0d08578e-477e-40b2-b7db-3a61bb0acc4f', now(), null);

INSERT INTO public.table1
VALUES (11, 1, '{"attribute1": "et consequat",
  "attribute2": "fvbcbcvxbvcbcvb",
  "attribute3": {
    "attribute7": "Duis voluptate amet commodo",
    "attribute8": "K"
  },
  "attribute4": [
    {
      "attribute9": "U",
      "attribute10": "laborum"
    },
    {
      "attribute9": "M",
      "attribute10": "commodo do"
    },
    {
      "attribute9": "B",
      "attribute10": "ad non"
    },
    {
      "attribute9": "Z",
      "attribute10": "in"
    },
    {
      "attribute9": "X",
      "attribute10": "dolore quis"
    }
  ],
  "attribute5": {
    "attribute6": [
      {
        "attribute11": "Blah%",
        "attribute12": "sdfsdfdsdsfdsfsdfds"
      }
    ],
    "attribute12": {
      "attribute13": "sgdsfdhgfhghgf"
    }
  }
}', '0d08578e-477e-40b2-b7db-3a61bb0acdfg', now(), null);

----- insert data into table2 ------

INSERT INTO public.table2
VALUES (1, 2, '{
  "attribute1": "et consequat",
  "attribute2": "fvbcbcvxbvcbcvb"
}', 'f2461bc7-8c3c-48d5-a928-59855db60414', now(), null);

INSERT INTO public.table2
VALUES (2, 2, '{
  "attribute1": "nt sfsreefgsf",
  "attribute2": "fsdfhjdsofue sdgsdfds rtewvhtg dftfeycerg"
}', '152168a3-543a-405c-970a-03f2b2f9d744', now(), null);

INSERT INTO public.table2
VALUES (3, 3, '{
  "attribute3": "nt sfsreefgsf",
  "attribute4": "fsdefsffhjdsofue"
}', '0704941d-fb36-4a4c-8b75-79ed558a2a78', now(), null);

INSERT INTO public.table2
VALUES (4, 3, '{
  "attribute3": "dfs",
  "attribute4": "kjlnfdgiofsufdsfjfg"
}', '87b84525-799d-4f86-bee0-f0d106122d1e', now(), null);

INSERT INTO public.table2
VALUES (5, 4, '{
  "attribute3": "dfs sdfsdnlekmfösog",
  "attribute4": "lisdouaeoutamiocrtjambhu",
  "attribute5": "ösnaögoucrsdfskctrd"
}', '87b84525-799d-4f86-bee0-f0d106122d2f', now(), null);


INSERT INTO public.table3
VALUES (1, 5, '{
  "attribute1":
  {
    "attribute11":
    {
      "attribute111": "value111"
    }
  }
}', 'f67f1c00-7937-440d-8be8-1c9781432404', now(), null);


----- table definitions ----

INSERT INTO public.tabledef
VALUES (1, '{"$schema":"http://json-schema.org/draft-07/schema#",
   "$id":"191c4a62-1cb2-47fe-91ec-d39c4060b825",
   "type":"object",
   "title":"PgJson test JSON sample schema",
   "required":[
      "attribute1",
      "attribute2",
      "attribute3",
      "attribute4",
      "attribute5"
   ],
   "properties":{
      "attribute1":{
         "$id":"8845de04-9e50-46a3-b991-4fe528dbfcf5",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"exact"
      },
      "attribute2":{
         "$id":"db891238-a11a-416e-8b1d-5ff90f379d7f",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"fts"
      },
      "attribute3":{
         "$id":"4b1a895d-2a22-44c5-a4e6-b4470be1c45e",
         "type":"object",
         "properties":{
            "attribute7":{
               "$id":"0406f476-4bbd-4f44-b056-74fee63bd58d",
               "type":"string",
               "uiLabel":{
                  "lang1":"Lang1 UI item",
                  "lang2":"Lang1 UI item"
               },
               "index":"fts"
            },
            "attribute8":{
               "$id":"988cb46e-b998-44df-940f-3e6d8527213a",
               "type":"string",
               "enum":[
                  "A",
                  "B",
                  "C"
               ],
               "uiLabel":{
                  "lang1":"Lang1 UI item",
                  "lang2":"Lang2 UI item"
               },
               "index":"exact"
            }
         },
         "required":[
            "attribute7",
            "attribute8"
         ],
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"object"
      },
      "attribute4":{
         "$id":"5954c02d-f6d1-4734-a68a-92a6ac8b3cf2",
         "type":"array",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang1 UI label"
         },
         "minItems":1,
         "items":{
            "type":["object","null"],
            "required":[
               "attribute9",
               "attribute10"
            ],
            "properties":{
               "attribute9":{
                  "$id":"c17fea3b-3a87-492f-97e6-e3ac9a2d2aff",
                  "type":"string",
                  "uiLabel":{
                     "lang1":"Lang1 UI label",
                     "lang2":"Lang1 UI label"
                  },
                  "enum":[
                     "X",
                     "Y",
                     "Z"
                  ],
                  "index":null
               },
               "attribute10":{
                  "$id":"569af819-a655-4424-ba30-b64b63e50e97",
                  "type":"string",
                  "uiLabel":{
                     "lang1":"Lang1 UI label",
                     "lang2":"Lang1 UI label"
                  },
                  "index":null
               }
            }
         },
         "index":"object"
      },
      "attribute5":{
         "$id":"d317561f-f65e-4189-9e83-821f87e1dce9",
         "type":"object",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang1 UI label"
         },
         "properties": {
            "attribute6": {
               "$id": "fd7a133a-6e55-4107-94de-a680f5bf093c",
               "type": "array",
               "minItems": 1,
               "uiLabel": {
                  "lang1": "Lang1 UI label",
                  "lang2": "Lang1 UI label"
               },
               "items": {
                  "type": "object",
                  "required": [
                     "attribute11"
                  ],
                  "properties": {
                     "attribute11": {
                        "$id": "a1d53f70-1ee0-4dfa-bdba-c31e85ca02d0",
                        "type": "string"
                     }
                  }
               },
               "index": "nestedobject"
            },
            "attribute12": {
               "$id": "e03f282d-1a72-41f3-a887-f323f8b6bdde",
               "type": "object",
               "uiLabel": {
                  "lang1": "Lang1 UI label",
                  "lang2": "Lang1 UI label"
               },
               "required": [
                  "attribute13"
               ],
               "properties": {
                  "attribute13": {
                     "$id": "6bc953c2-2350-4840-87f2-306ef0bc9f43",
                     "type": "string",
                     "uiLabel": {
                        "lang1": "Lang1 UI label",
                        "lang2": "Lang1 UI label"
                     }
                  }
               },
               "index": null
            }
         },
         "required":[
            "attribute6",
            "attribute12"
         ],
         "index":null
      }
   }
}', 'table1_schema', 'table1', '6dc4bf18-49e7-4c10-8a23-a4752e48f6d0', 'now()',
        '287e355e930e5839e632010513e61875e6ff53dadc420a01403490313e94ffd3');

INSERT INTO public.tabledef
VALUES (2, '{"$schema":"http://json-schema.org/draft-07/schema#",
   "$id":"6a4ab7da-eb74-4280-a0a2-990feff6f836",
   "type":"object",
   "title":"PgJson test JSON sample schema for table2",
   "required":[
      "attribute1",
      "attribute2"
   ],
   "properties":{
      "attribute1":{
         "$id":"75d71bd5-f6e2-4f3c-9350-5e64a9319699",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"exact"
      },
      "attribute2":{
         "$id":"2dc532f5-e521-4322-b1f4-bccd7346da38",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"fts"
      }
   }
}', 'table2_schema1', 'table2', '6a4ab7da-eb74-4280-a0a2-990feff6f836', 'now()',
        'a783f8c2a15dcd1035e0f600583be4f3a05536e034caf77d645f4c32b1dde0e7');

INSERT INTO public.tabledef
VALUES (3, '{"$schema":"http://json-schema.org/draft-07/schema#",
   "$id":"e2d92a99-e171-41bf-88e2-e932f3fc8bbb",
   "type":"object",
   "title":"PgJson test JSON sample schema 2 v1 for table2",
   "required":[
      "attribute3",
      "attribute4"
   ],
   "properties":{
      "attribute3":{
         "$id":"8b9e40be-bcc1-4def-ae3b-a0ad9720f22d",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"exact"
      },
      "attribute4":{
         "$id":"5ab44931-5f28-4815-a269-0158800b6af4",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"fts"
      }
   }
}', 'table2_schema2', 'table2', 'e2d92a99-e171-41bf-88e2-e932f3fc8bbb', 'now()',
        'c26a1d98ba96d249be1a8e2ae8bffaf48d97b71da0b80146300053d61969e31e');

INSERT INTO public.tabledef
VALUES (4, '{"$schema":"http://json-schema.org/draft-07/schema#",
   "$id":"e51e38d9-4a1e-427b-88fc-4026dfd576ec",
   "type":"object",
   "title":"PgJson test JSON sample schema 2 v2 for table2",
   "required":[
      "attribute3",
      "attribute4",
      "attribute5"
   ],
   "properties":{
      "attribute3":{
         "$id":"8b9e40be-bcc1-4def-ae3b-a0ad9720f22d",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"exact"
      },
      "attribute4":{
         "$id":"5ab44931-5f28-4815-a269-0158800b6af4",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"fts"
      },
      "attribute5":{
         "$id":"5ab44931-5f28-4815-a269-0158800b6af4",
         "type":"string",
         "uiLabel":{
            "lang1":"Lang1 UI label",
            "lang2":"Lang2 UI label"
         },
         "index":"fts"
      }
   }
}', 'table2_schema2', 'table2', 'e51e38d9-4a1e-427b-88fc-4026dfd576ec', 'now()',
        '26923e68387193bec9d0036274800c9afc307e7d763ad2f05e88ddaac42c1be6');

INSERT INTO public.tabledef
VALUES (5, '{"$schema":"http://json-schema.org/draft-07/schema#",
   "$id":"e03b2c2d-6882-4ae0-a805-587f6c68494f",
   "type":"object",
   "title":"PgJson test JSON sample schema 1 v1 for table3",
   "required":[
      "attribute1"
   ],
   "properties":{
      "attribute1":{
         "$id":"36312133-51bf-4434-8241-cec7fda548e3",
         "type": "object",
               "uiLabel": {
                  "lang1": "Lang1 UI label",
                  "lang2": "Lang1 UI label"
               },
               "required": [
                  "attribute11"
               ],
               "properties": {
                  "attribute11": {
                     "$id": "ae0dfdf9-df56-4c9e-b60e-fd148e511e3a",
                     "type": "object",
                     "uiLabel": {
                        "lang1": "Lang1 UI label",
                        "lang2": "Lang1 UI label"
                     },
                     "required": [
                        "attribute111"
                    ],
                    "properties": {
                        "attribute111": {
                           "$id": "7a08e114-c88b-407a-a991-48fd066dc9be",
                           "type": "string",
                           "uiLabel": {
                              "lang1": "Lang1 UI label",
                              "lang2": "Lang1 UI label"
                           },
                           "index": "exact"
                        }
                     },
                     "index": null
                  }
               },
         "index": null
      }
   }
}', 'table3_schema1', 'table3', 'e03b2c2d-6882-4ae0-a805-587f6c68494f', 'now()',
        '7932b7ad698a1c3d77eb7a10090f6bf39a0a3b5a3c40a9447b90d5d7eac62a39');


INSERT INTO public.test
VALUES ('Hello');

SELECT pg_catalog.setval('public.table1_id_seq', 12, true);

SELECT pg_catalog.setval('public.table2_id_seq', 6, true);

SELECT pg_catalog.setval('public.tabledef_id_seq', 5, true);

------------------------ CREATE CONSTRAINTS ------------------------------

ALTER TABLE ONLY public.table1 ADD CONSTRAINT table1_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.table2 ADD CONSTRAINT table2_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tabledef ADD CONSTRAINT tabledef_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.table1 ADD CONSTRAINT unique_table1_id_uuid UNIQUE (id_uuid);

ALTER TABLE ONLY public.table2 ADD CONSTRAINT unique_table2_id_uuid UNIQUE (id_uuid);

ALTER TABLE ONLY public.tabledef ADD CONSTRAINT unique_id_uuid_tabledef UNIQUE (id_uuid);

ALTER TABLE ONLY public.tabledef ADD CONSTRAINT unique_schema_hash UNIQUE (schema_hash);

-- remove constraint due to that same table can consist multiple schemas, but only one schema with same name can be latest considered as latest with same schema name
--ALTER TABLE ONLY public.tabledef ADD CONSTRAINT unique_schema_name UNIQUE (schema_name);

CREATE UNIQUE INDEX idx_id_uuid_tabledef ON public.tabledef USING btree (id_uuid);

ALTER TABLE ONLY public.table1 ADD CONSTRAINT fk_tabledef_table1_id FOREIGN KEY (tabledef_id) REFERENCES public.tabledef(id);

ALTER TABLE ONLY public.table2 ADD CONSTRAINT fk_tabledef_table2_id FOREIGN KEY (tabledef_id) REFERENCES public.tabledef(id);

----------------------------- Create indexes for table1----------------------------------

CREATE INDEX pgjson_exact_table1_attribute1 ON public.table1 ((json_data ->>'attribute1'));

CREATE INDEX pgjson_object_table1_attribute3 ON public.table1 USING gin((json_data->'attribute3') jsonb_path_ops);

CREATE INDEX pgjson_object_table1_attribute4 ON public.table1 USING gin((json_data->'attribute4') jsonb_path_ops);
CREATE INDEX pgjson_fts_table1_attribute2 ON public.table1 USING gin(to_tsvector('simple', json_data->'attribute2'));

--CREATE INDEX pgjson_nestedobject_attribute5_attribute6 ON public.table1 USING gin((json_data->'attribute5'->'attribute6') jsonb_path_ops);
CREATE INDEX pgjson_nestedobject_table1_attribute3_attribute8 ON public.table1 USING gin((json_data->'attribute3'->'attribute8') jsonb_path_ops);

CREATE INDEX pgjson_fts_table1_attribute5 ON public.table1 USING gin(to_tsvector('simple', json_data->'attribute5'));

----- indexes for table2 -----

CREATE INDEX pgjson_exact_table2_attribute1 ON public.table2 ((json_data ->>'attribute1'));

CREATE INDEX pgjson_fts_table2_attribute2 ON public.table2 USING gin(to_tsvector('simple', json_data->'attribute2'));

CREATE INDEX pgjson_exact_table2_attribute3 ON public.table2 ((json_data ->>'attribute3'));

CREATE INDEX pgjson_fts_table2_attribute4 ON public.table2 USING gin(to_tsvector('simple', json_data->'attribute4'));

CREATE INDEX pgjson_fts_table2_attribute5 ON public.table2 USING gin(to_tsvector('simple', json_data->'attribute5'));

----- indexes for table3 -----

CREATE INDEX pgjson_nestedobject_table3_attribute1_attribute11_attribute111 ON public.table3 USING gin((json_data->'attribute1'->'attribute11'->'attribute111') jsonb_path_ops);
