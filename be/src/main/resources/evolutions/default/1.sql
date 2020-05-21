# --- !Ups

CREATE DOMAIN column_data_type AS VARCHAR CHECK (
  VALUE IN ('TEXT', 'BOOLEAN', 'INTEGER')
);

CREATE TABLE spec1(
  -- If column_num is not unique we can map same column to different name and type potentially. Not good.
  -- For example we could have (1, 'valid', 'BOOLEAN') and (1, 'isok', 'TEXT') which is problematic.
  column_num INTEGER NOT NULL UNIQUE CHECK (column_num > 0),
  -- I make column_name unique as well as I don't think it makes sense to not have it unique.
  -- We can imagine a scenario (1, 'name', 'TEXT') and (2, 'name', 'TEXT'). More flexible maybe, but not practical.
  -- Also we don't want super long and rubbish column_nameS so just limited to 32 chars [a-z_].
  column_name VARCHAR(32) NOT NULL CHECK (column_name ~ '^[a-z_]+$'),
  data_type column_data_type NOT NULL
);

INSERT INTO spec1 VALUES (1, 'name', 'TEXT');
INSERT INTO spec1 VALUES (2, 'valid', 'BOOLEAN');
INSERT INTO spec1 VALUES (3, 'count', 'INTEGER');

CREATE TABLE data1(
  row INTEGER,
  column_1 TEXT,
  column_2 BOOLEAN,
  column_3 INTEGER
);

INSERT INTO data1 VALUES (1, 'Apple', true, 1);
INSERT INTO data1 VALUES (1, 'Banana', false, -12);

# --- !Downs

DROP TABLE data1;

DROP TABLE spec1;

DROP DOMAIN column_data_type;
