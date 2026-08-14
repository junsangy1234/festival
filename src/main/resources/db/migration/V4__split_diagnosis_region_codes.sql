ALTER TABLE diagnoses ADD COLUMN area_code VARCHAR(2);
ALTER TABLE diagnoses ADD COLUMN signgu_code VARCHAR(5);

UPDATE diagnoses
SET signgu_code = region_code
WHERE signgu_code IS NULL;

UPDATE diagnoses
SET area_code = SUBSTRING(signgu_code FROM 1 FOR 2)
WHERE area_code IS NULL;

ALTER TABLE diagnoses ALTER COLUMN area_code SET NOT NULL;
ALTER TABLE diagnoses ALTER COLUMN signgu_code SET NOT NULL;
ALTER TABLE diagnoses DROP COLUMN region_code;
