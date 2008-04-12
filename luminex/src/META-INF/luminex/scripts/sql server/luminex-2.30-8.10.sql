/* luminex-2.30-2.31.sql */

ALTER TABLE luminex.DataRow DROP CONSTRAINT FK_LuminexDataRow_AnalyteId
GO

ALTER TABLE luminex.DataRow ADD CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.analyte(RowId)
GO

/* luminex-2.31-2.32.sql */

ALTER TABLE luminex.DataRow ADD ExtraSpecimenInfo NVARCHAR(50)
GO