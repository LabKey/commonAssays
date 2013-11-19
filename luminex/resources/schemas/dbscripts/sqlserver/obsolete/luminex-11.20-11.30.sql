/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* luminex-11.20-11.21.sql */

CREATE TABLE luminex.CurveFit
(
    RowId INT IDENTITY(1,1) NOT NULL,
	TitrationId INT NOT NULL,
	AnalyteId INT NOT NULL,
	CurveType VARCHAR(20) NOT NULL,
	MaxFI REAL NOT NULL,
	EC50 REAL NOT NULL,
	AUC REAL NOT NULL,

	CONSTRAINT PK_luminex_CurveFit PRIMARY KEY (rowid),
	CONSTRAINT FK_CurveFit_AnalyteIdTitrationId FOREIGN KEY (AnalyteId, TitrationId) REFERENCES luminex.AnalyteTitration (AnalyteId, TitrationId),
	CONSTRAINT UQ_CurveFit UNIQUE (AnalyteId, TitrationId, CurveType)
);

/* luminex-11.21-11.22.sql */

ALTER TABLE luminex.CurveFit ALTER COLUMN CurveType VARCHAR(30);

/* luminex-11.22-11.23.sql */

ALTER TABLE luminex.CurveFit ALTER COLUMN MaxFI REAL NULL;
ALTER TABLE luminex.CurveFit ALTER COLUMN AUC REAL NULL;
ALTER TABLE luminex.CurveFit ALTER COLUMN EC50 REAL NULL;

/* luminex-11.23-11.24.sql */

-- Add the 4 and 5 PL curve fit values
ALTER TABLE luminex.CurveFit ADD MinAsymptote REAL;
ALTER TABLE luminex.CurveFit ADD MaxAsymptote REAL;
ALTER TABLE luminex.CurveFit ADD Asymmetry REAL;
ALTER TABLE luminex.CurveFit ADD Inflection REAL;
ALTER TABLE luminex.CurveFit ADD Slope REAL;

-- Move MaxFI from CurveFit to Titration as it doesn't depend on the fit parameters
-- Don't bother to migrate values, no real data MaxFI has been stored in the CurveFit table yet
ALTER TABLE luminex.CurveFit DROP COLUMN MaxFI;
ALTER TABLE luminex.AnalyteTitration ADD MaxFI REAL;

CREATE TABLE luminex.GuideSet
(
    RowId INT IDENTITY(1,1) NOT NULL,
    ProtocolId INT NOT NULL,
    AnalyteName VARCHAR(50) NOT NULL,
    CurrentGuideSet BIT NOT NULL,
    Conjugate VARCHAR(50),
    Isotype VARCHAR(50),
    MaxFIAverage REAL,
    MaxFIStdDev REAL,

	CONSTRAINT PK_luminex_GuideSet PRIMARY KEY (RowId),
	CONSTRAINT FK_luminex_GuideSet_ProtocolId FOREIGN KEY (ProtocolId) REFERENCES exp.Protocol(RowId)
);

CREATE INDEX IDX_GuideSet_ProtocolId ON luminex.GuideSet(ProtocolId);

CREATE TABLE luminex.GuideSetCurveFit
(
	GuideSetId INT NOT NULL,
	CurveType VARCHAR(30) NOT NULL,
    AUCAverage REAL,
    AUCStdDev REAL,
    EC50Average REAL,
    EC50StdDev REAL,

	CONSTRAINT PK_luminex_GuideSetCurveFit PRIMARY KEY (GuideSetId, CurveType)
);

ALTER TABLE luminex.Analyte ADD GuideSetId INT;
ALTER TABLE luminex.Analyte ADD CONSTRAINT FK_Analyte_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet(RowId);

CREATE INDEX IDX_Analyte_GuideSetId ON luminex.Analyte(GuideSetId);

/* luminex-11.24-11.25.sql */

ALTER TABLE luminex.Analyte ADD IncludeInGuideSetCalculation BIT;

GO

UPDATE luminex.analyte SET IncludeInGuideSetCalculation = 0;
ALTER TABLE luminex.analyte ALTER COLUMN IncludeInGuideSetCalculation BIT NOT NULL;

DROP TABLE luminex.GuideSetCurveFit;

/* luminex-11.250-11.251.sql */

ALTER TABLE luminex.GuideSet ADD TitrationName VARCHAR(255);
ALTER TABLE luminex.GuideSet ADD Comment TEXT;
ALTER TABLE luminex.GuideSet ADD CreatedBy USERID;
ALTER TABLE luminex.GuideSet ADD Created DATETIME;
ALTER TABLE luminex.GuideSet ADD ModifiedBy USERID;
ALTER TABLE luminex.GuideSet ADD Modified DATETIME;

ALTER TABLE luminex.AnalyteTitration ADD GuideSetId INT;
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalytTitration_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet(RowId);

ALTER TABLE luminex.AnalyteTitration ADD IncludeInGuideSetCalculation BIT;

GO

UPDATE luminex.AnalyteTitration SET IncludeInGuideSetCalculation = 0;
ALTER TABLE luminex.AnalyteTitration ALTER COLUMN IncludeInGuideSetCalculation BIT NOT NULL;

CREATE INDEX IDX_AnalyteTitration_GuideSetId ON luminex.AnalyteTitration(GuideSetId);

DROP INDEX IDX_Analyte_GuideSetId ON luminex.Analyte;
ALTER TABLE luminex.Analyte DROP CONSTRAINT FK_Analyte_GuideSetId;
ALTER TABLE luminex.Analyte DROP COLUMN GuideSetId;
ALTER TABLE luminex.Analyte DROP COLUMN IncludeInGuideSetCalculation;

/* luminex-11.251-11.252.sql */

ALTER TABLE luminex.GuideSet DROP COLUMN MaxFIAverage;
ALTER TABLE luminex.GuideSet DROP COLUMN MaxFIStdDev;

/* luminex-11.252-11.253.sql */

ALTER TABLE luminex.DataRow ADD Summary BIT;
ALTER TABLE luminex.DataRow ADD CV REAL;

GO

UPDATE luminex.DataRow SET Summary = 1 WHERE patindex('%,%', Well) > 0;
UPDATE luminex.DataRow SET Summary = 0 WHERE Summary IS NULL;

ALTER TABLE luminex.DataRow ALTER COLUMN Summary BIT NOT NULL;

-- Calculate the StdDev for any summary rows that have already been uploaded
UPDATE luminex.DataRow SET StdDev =
	(SELECT STDEV(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = luminex.DataRow.description AND
		((dr2.dilution IS NULL AND luminex.DataRow.dilution IS NULL) OR luminex.DataRow.dilution = dr2.dilution) AND
		luminex.DataRow.dataid = dr2.dataid AND
		luminex.DataRow.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND luminex.DataRow.expConc IS NULL) OR luminex.DataRow.expConc = dr2.expConc))
	WHERE StdDev IS NULL;

-- Calculate the %CV for any summary rows that have already been uploaded
UPDATE luminex.DataRow SET CV =
	StdDev / (SELECT AVG(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = luminex.DataRow.description AND
		((dr2.dilution IS NULL AND luminex.DataRow.dilution IS NULL) OR luminex.DataRow.dilution = dr2.dilution) AND
		luminex.DataRow.dataid = dr2.dataid AND
		luminex.DataRow.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND luminex.DataRow.expConc IS NULL) OR luminex.DataRow.expConc = dr2.expConc))
	WHERE StdDev IS NOT NULL AND
	(SELECT AVG(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = luminex.DataRow.description AND
		((dr2.dilution IS NULL AND luminex.DataRow.dilution IS NULL) OR luminex.DataRow.dilution = dr2.dilution) AND
		luminex.DataRow.dataid = dr2.dataid AND
		luminex.DataRow.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND luminex.DataRow.expConc IS NULL) OR luminex.DataRow.expConc = dr2.expConc)) != 0;

/* luminex-11.253-11.254.sql */

ALTER TABLE luminex.WellExclusion ADD Type VARCHAR(10);

GO

-- Populate the WellExclusion Type column based on the value in the DataRow table for the given DataId/Description/Dilution
UPDATE luminex.WellExclusion SET Type =
	(SELECT types.Type FROM (SELECT dr.DataId, dr.Dilution, dr.Description, min(dr.Type) AS Type
			FROM luminex.DataRow dr
			GROUP BY dr.DataId, dr.Dilution, dr.Description) AS types
		WHERE luminex.WellExclusion.DataId = types.DataId
		AND ((luminex.WellExclusion.Dilution IS NULL AND types.Dilution IS NULL) OR luminex.WellExclusion.Dilution = types.Dilution)
		AND ((luminex.WellExclusion.Description IS NULL AND types.Description IS NULL) OR luminex.WellExclusion.Description = types.Description))
	WHERE Type IS NULL;

DROP INDEX UQ_WellExclusion ON luminex.WellExclusion;
CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Type, DataId);
ALTER TABLE luminex.WellExclusion DROP COLUMN Dilution;

/* luminex-11.254-11.255.sql */

DROP INDEX luminex.analyte.ix_luminexdatarow_lsid;
CREATE UNIQUE INDEX UQ_Analyte_LSID ON luminex.Analyte(LSID);