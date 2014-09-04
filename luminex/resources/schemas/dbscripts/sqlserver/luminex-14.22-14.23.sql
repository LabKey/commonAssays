
ALTER TABLE luminex.datarow ADD SinglePointControlId INT;
GO
ALTER TABLE luminex.datarow ADD CONSTRAINT FK_Luminex_DataRow_SinglePointControlId FOREIGN KEY (SinglePointControlId) REFERENCES luminex.SinglePointControl(RowId);
CREATE INDEX IX_LuminexDataRow_SinglePointControlId ON luminex.DataRow (SinglePointControlId);

UPDATE luminex.datarow SET SinglePointControlId =
  (SELECT s.rowid FROM luminex.singlepointcontrol s, exp.data d, exp.protocolapplication pa
    WHERE pa.runid = s.runid AND d.sourceapplicationid = pa.rowid AND dataid = d.rowid AND description = s.name)
WHERE SinglePointControlId IS NULL;