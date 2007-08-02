DELETE FROM luminex.DataRow
GO

DELETE FROM luminex.Analyte
GO

ALTER TABLE luminex.Analyte ADD
    MinStandardRecovery INT NOT NULL,
    MaxStandardRecovery INT NOT NULL
GO