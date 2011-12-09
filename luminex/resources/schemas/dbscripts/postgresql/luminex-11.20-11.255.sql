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
	RowId serial NOT NULL,
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

ALTER TABLE luminex.CurveFit ALTER COLUMN CurveType TYPE VARCHAR(30);

/* luminex-11.22-11.23.sql */

ALTER TABLE luminex.CurveFit ALTER COLUMN MaxFI DROP NOT NULL;
ALTER TABLE luminex.CurveFit ALTER COLUMN AUC DROP NOT NULL;
ALTER TABLE luminex.CurveFit ALTER COLUMN EC50 DROP NOT NULL;

/* luminex-11.23-11.24.sql */

-- Add the 4 and 5 PL curve fit values
ALTER TABLE luminex.CurveFit ADD COLUMN MinAsymptote REAL;
ALTER TABLE luminex.CurveFit ADD COLUMN MaxAsymptote REAL;
ALTER TABLE luminex.CurveFit ADD COLUMN Asymmetry REAL;
ALTER TABLE luminex.CurveFit ADD COLUMN Inflection REAL;
ALTER TABLE luminex.CurveFit ADD COLUMN Slope REAL;

-- Move MaxFI from CurveFit to Titration as it doesn't depend on the fit parameters
-- Don't bother to migrate values, no real data MaxFI has been stored in the CurveFit table yet
ALTER TABLE luminex.CurveFit DROP COLUMN MaxFI;
ALTER TABLE luminex.AnalyteTitration ADD COLUMN MaxFI REAL;

CREATE TABLE luminex.GuideSet
(
	RowId SERIAL NOT NULL,
    ProtocolId INT NOT NULL,
    AnalyteName VARCHAR(50) NOT NULL,
    CurrentGuideSet BOOLEAN NOT NULL,
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

ALTER TABLE luminex.Analyte ADD COLUMN GuideSetId INT;

ALTER TABLE luminex.Analyte ADD CONSTRAINT FK_Analyte_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet(RowId);

CREATE INDEX IDX_Analyte_GuideSetId ON luminex.Analyte(GuideSetId);

/* luminex-11.24-11.25.sql */

ALTER TABLE luminex.Analyte ADD COLUMN IncludeInGuideSetCalculation BOOLEAN;

UPDATE luminex.analyte SET IncludeInGuideSetCalculation = FALSE;

ALTER TABLE luminex.analyte ALTER COLUMN IncludeInGuideSetCalculation SET NOT NULL;

DROP TABLE luminex.GuideSetCurveFit;

/* luminex-11.250-11.251.sql */

ALTER TABLE luminex.GuideSet ADD COLUMN TitrationName VARCHAR(255);
ALTER TABLE luminex.GuideSet ADD COLUMN Comment TEXT;
ALTER TABLE luminex.GuideSet ADD COLUMN CreatedBy USERID;
ALTER TABLE luminex.GuideSet ADD COLUMN Created TIMESTAMP;
ALTER TABLE luminex.GuideSet ADD COLUMN ModifiedBy USERID;
ALTER TABLE luminex.GuideSet ADD COLUMN Modified TIMESTAMP;

ALTER TABLE luminex.AnalyteTitration ADD COLUMN GuideSetId INT;
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalytTitration_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet(RowId);
CREATE INDEX IDX_AnalyteTitration_GuideSetId ON luminex.AnalyteTitration(GuideSetId);

ALTER TABLE luminex.AnalyteTitration ADD COLUMN IncludeInGuideSetCalculation BOOLEAN;
UPDATE luminex.AnalyteTitration SET IncludeInGuideSetCalculation = FALSE;
ALTER TABLE luminex.AnalyteTitration ALTER COLUMN IncludeInGuideSetCalculation SET NOT NULL;

ALTER TABLE luminex.Analyte DROP COLUMN GuideSetId;
ALTER TABLE luminex.Analyte DROP COLUMN IncludeInGuideSetCalculation;

/* luminex-11.251-11.252.sql */

ALTER TABLE luminex.GuideSet DROP COLUMN MaxFIAverage;
ALTER TABLE luminex.GuideSet DROP COLUMN MaxFIStdDev;

/* luminex-11.252-11.253.sql */

ALTER TABLE luminex.DataRow ADD COLUMN Summary Boolean;
ALTER TABLE luminex.DataRow ADD COLUMN CV REAL;

UPDATE luminex.DataRow SET Summary = TRUE WHERE POSITION(',' IN Well) > 0;
UPDATE luminex.DataRow SET Summary = FALSE WHERE Summary IS NULL;

ALTER TABLE luminex.DataRow ALTER COLUMN Summary SET NOT NULL;

-- Calculate the StdDev for any summary rows that have already been uploaded
UPDATE luminex.DataRow dr1 SET StdDev =
	(SELECT STDDEV(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = dr1.description AND
		((dr2.dilution IS NULL AND dr1.dilution IS NULL) OR dr1.dilution = dr2.dilution) AND
		dr1.dataid = dr2.dataid AND
		dr1.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND dr1.expConc IS NULL) OR dr1.expConc = dr2.expConc))
	WHERE StdDev IS NULL;

-- Calculate the %CV for any summary rows that have already been uploaded
UPDATE luminex.DataRow dr1 SET CV =
	StdDev / (SELECT AVG(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = dr1.description AND
		((dr2.dilution IS NULL AND dr1.dilution IS NULL) OR dr1.dilution = dr2.dilution) AND
		dr1.dataid = dr2.dataid AND
		dr1.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND dr1.expConc IS NULL) OR dr1.expConc = dr2.expConc))
	WHERE StdDev IS NOT NULL AND
	(SELECT AVG(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = dr1.description AND
		((dr2.dilution IS NULL AND dr1.dilution IS NULL) OR dr1.dilution = dr2.dilution) AND
		dr1.dataid = dr2.dataid AND
		dr1.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND dr1.expConc IS NULL) OR dr1.expConc = dr2.expConc)) != 0;

/* luminex-11.253-11.254.sql */

ALTER TABLE luminex.wellexclusion ADD COLUMN "type" character varying(10);

-- Populate the WellExclusion Type column based on the value in the DataRow table for the given DataId/Description/Dilution
UPDATE luminex.wellexclusion we SET "type" =
	(SELECT types."type" FROM (SELECT dr.dataid, dr.dilution, dr.description, min(dr."type") AS "type"
			FROM luminex.datarow AS dr
			GROUP BY dr.dataid, dr.dilution, dr.description) AS types
		WHERE we.dataid = types.dataid
		AND ((we.dilution IS NULL AND types.dilution IS NULL) OR we.dilution = types.dilution)
		AND ((we.description IS NULL AND types.description IS NULL) OR we.description = types.description))
	WHERE "type" IS NULL;

DROP INDEX luminex.UQ_WellExclusion;

CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Type, DataId);	

ALTER TABLE luminex.wellexclusion DROP COLUMN dilution;

/* luminex-11.254-11.255.sql */

DROP INDEX luminex.ix_luminexdatarow_lsid;

CREATE UNIQUE INDEX UQ_Analyte_LSID ON luminex.Analyte(LSID);