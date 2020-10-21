ALTER TABLE nab.NAbSpecimen ADD FitParameters NVARCHAR(500);
GO
EXEC core.executeJavaUpgradeCode 'populateFitParameters';