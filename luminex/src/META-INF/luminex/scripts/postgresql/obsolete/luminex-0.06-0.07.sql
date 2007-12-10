ALTER TABLE luminex.DataRow ADD COLUMN Dilution REAL;

ALTER TABLE luminex.DataRow ADD COLUMN DataRowGroup VARCHAR(25);

ALTER TABLE luminex.DataRow ADD COLUMN Ratio VARCHAR(25);

ALTER TABLE luminex.DataRow ADD COLUMN SamplingErrors VARCHAR(25);
