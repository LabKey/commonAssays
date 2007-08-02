DELETE FROM luminex.DataRow;
DELETE FROM luminex.Analyte;

ALTER TABLE luminex.Analyte
    ADD COLUMN MinStandardRecovery INT,
    ADD COLUMN MaxStandardRecovery INT;