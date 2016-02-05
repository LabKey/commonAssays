/*
 * Copyright (c) 2010-2016 LabKey Corporation
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

/* luminex-0.00-10.10.sql */

CREATE SCHEMA luminex;
GO

CREATE TABLE luminex.Analyte
(
    RowId INT IDENTITY(1,1) NOT NULL,
    LSID LSIDtype NOT NULL ,
    Name VARCHAR(50) NOT NULL,
    DataId INT NOT NULL,
    FitProb REAL,
    ResVar REAL,
    RegressionType VARCHAR(100),
    StdCurve VARCHAR(255),
    MinStandardRecovery INT NOT NULL,
    MaxStandardRecovery INT NOT NULL,

    CONSTRAINT PK_Luminex_Analyte PRIMARY KEY (RowId),
    CONSTRAINT FK_LuminexAnalyte_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId)
);

CREATE INDEX IX_LuminexAnalyte_DataId ON luminex.Analyte (DataId);

CREATE TABLE luminex.DataRow
(
    RowId INT IDENTITY(1,1) NOT NULL,
    DataId INT NOT NULL,
    AnalyteId INT NOT NULL,
    Type VARCHAR(10),
    Well VARCHAR(50),
    Outlier INT,
    Description VARCHAR(50),
    FIString VARCHAR(20),
    FI REAL,
    FIOORIndicator VARCHAR(10),
    FIBackgroundString VARCHAR(20),
    FIBackground REAL,
    FIBackgroundOORIndicator VARCHAR(10),
    StdDevString VARCHAR(20),
    StdDev REAL,
    StdDevOORIndicator VARCHAR(10),
    ObsConcString VARCHAR(20),
    ObsConc REAL,
    ObsConcOORIndicator VARCHAR(10),
    ExpConc REAL,
    ObsOverExp REAL,
    ConcInRangeString VARCHAR(20),
    ConcInRange REAL,
    ConcInRangeOORIndicator VARCHAR(10),

    Dilution REAL,
    DataRowGroup VARCHAR(25),
    Ratio VARCHAR(25),
    SamplingErrors VARCHAR(25),
    PTID NVARCHAR(32),
    VisitID FLOAT,
    Date DATETIME,
    ExtraSpecimenInfo NVARCHAR(50),
    SpecimenID NVARCHAR(50),
    Container EntityID NOT NULL,
    ProtocolID INT NOT NULL,
    BeadCount INT,

    CONSTRAINT PK_Luminex_DataRow PRIMARY KEY (RowId),
    CONSTRAINT FK_LuminexDataRow_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId),
    CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.analyte(RowId),
    CONSTRAINT FK_DataRow_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityID),
    CONSTRAINT FK_DataRow_ProtocolID FOREIGN KEY (ProtocolID) REFERENCES exp.Protocol (RowID)
);
CREATE INDEX IX_LuminexDataRow_DataId ON luminex.DataRow (DataId);
CREATE INDEX IX_LuminexDataRow_AnalyteId ON luminex.DataRow (AnalyteId);
CREATE INDEX IDX_DataRow_Container_ProtocolID ON luminex.datarow(Container, ProtocolID);

/* luminex-10.30-11.10.sql */

ALTER TABLE luminex.DataRow
    ALTER COLUMN fistring NVARCHAR(64);

ALTER TABLE luminex.DataRow
    ALTER COLUMN fibackgroundstring NVARCHAR(64);

ALTER TABLE luminex.DataRow
    ALTER COLUMN stddevstring NVARCHAR(64);

ALTER TABLE luminex.DataRow
    ALTER COLUMN obsconcstring NVARCHAR(64);

ALTER TABLE luminex.DataRow
    ALTER COLUMN concinrangestring NVARCHAR(64);

/* luminex-11.10-11.20.sql */

ALTER TABLE luminex.datarow ADD LSID LSIDtype;
GO

UPDATE luminex.datarow SET LSID = 'urn:lsid:' + COALESCE ((SELECT p.value
FROM prop.properties p, prop.propertysets ps, core.containers c
WHERE
	p.name = 'defaultLsidAuthority' AND
	ps."set" = p."set" AND
	ps.category = 'SiteConfig' AND
	ps.objectid = c.entityid AND
	c.name IS NULL AND c.parent IS NULL), 'localhost') + ':LuminexDataRow:' + CAST(RowId AS NVARCHAR);

ALTER TABLE luminex.datarow ALTER COLUMN LSID LSIDType NOT NULL;

CREATE INDEX IX_LuminexDataRow_LSID ON luminex.Analyte (LSID);

CREATE TABLE luminex.Titration
(
    RowId INT IDENTITY(1,1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Standard BIT NOT NULL,
    QCControl BIT NOT NULL,

    CONSTRAINT PK_Luminex_Titration PRIMARY KEY (RowId),
    CONSTRAINT FK_Luminex_Titration_RunId FOREIGN KEY (RunId) REFERENCES exp.ExperimentRun(RowId),
    CONSTRAINT UQ_Titration UNIQUE (Name, RunId)
);

CREATE TABLE luminex.AnalyteTitration
(
    AnalyteId INT NOT NULL,
    TitrationId INT NOT NULL,

    CONSTRAINT PK_Luminex_AnalyteTitration PRIMARY KEY (AnalyteId, TitrationId)
);

CREATE INDEX IX_LuminexTitration_RunId ON luminex.Titration (RunId);

-- Assume a single titration for all existing data
INSERT INTO luminex.Titration
    (RunId, Name, Standard, QCControl)
    SELECT RowId, 'Standard', 1, 0 FROM exp.experimentrun WHERE protocollsid LIKE '%:LuminexAssayProtocol.%';

-- Assign all existing analytes to the single titration created above
INSERT INTO luminex.AnalyteTitration
    (AnalyteId, TitrationId)
    SELECT a.RowId, t.RowId
        FROM luminex.analyte a, exp.data d, luminex.titration t, exp.protocolapplication pa
        WHERE a.dataid = d.rowid AND t.runid = pa.runid AND d.sourceapplicationid = pa.rowid;

ALTER TABLE luminex.datarow ADD TitrationId INT;

GO

ALTER TABLE luminex.datarow ADD CONSTRAINT FK_Luminex_DataRow_TitrationId FOREIGN KEY (TitrationId) REFERENCES luminex.Titration(RowId);
CREATE INDEX IX_LuminexDataRow_TitrationId ON luminex.DataRow (TitrationId);

-- Assume that any existing data that has an expected concentration is part of the single standard titration
-- for that run
UPDATE luminex.datarow SET titrationid =
    (SELECT t.rowid FROM luminex.titration t, exp.data d, exp.protocolapplication pa
        WHERE pa.runid = t.runid AND d.sourceapplicationid = pa.rowid AND dataid = d.rowid)
    WHERE expconc IS NOT NULL;

-- Use the description as the titration name, grabbing an arbitrary one if they're not all the same
UPDATE luminex.titration SET name = (SELECT COALESCE(MIN(description), 'Standard') FROM luminex.datarow WHERE luminex.titration.rowid = titrationid);

ALTER TABLE luminex.Titration ADD "unknown" BIT;

GO

UPDATE luminex.Titration SET "unknown" = 0;

ALTER TABLE luminex.Titration ALTER COLUMN "unknown" BIT NOT NULL;

-- Make sure we have a row in exp.Object for all Luminex data files so that we can migrate the Excel run properties to it
INSERT INTO exp.Object (ObjectURI, Container)
	SELECT d.LSID, d.Container
	FROM exp.Data d, exp.ExperimentRun r, exp.ProtocolApplication pa
	WHERE d.SourceApplicationId = pa.RowId and r.RowId = pa.RunId AND r.ProtocolLSID LIKE '%:LuminexAssayProtocol.Folder-%' AND d.LSID NOT IN (SELECT ObjectURI FROM exp.Object);

-- Clean up run field values that were orphaned when we failed to delete them as part of deleting a Luminex run
DELETE FROM exp.ObjectProperty
    WHERE ObjectId IN (SELECT o.ObjectId FROM exp.Object o LEFT OUTER JOIN exp.ExperimentRun r ON o.ObjectURI = r.LSID WHERE o.ObjectURI like '%:LuminexAssayRun.Folder-%' AND r.LSID IS NULL);

-- Migrate Excel-based values from run to data
UPDATE exp.ObjectProperty SET ObjectId =
	(SELECT MIN(dataO.ObjectId) FROM exp.Object dataO, exp.Object runO, exp.Data d, exp.ExperimentRun r, exp.ProtocolApplication pa
	WHERE dataO.ObjectURI = d.LSID AND runO.ObjectURI = r.LSID AND runO.ObjectId = exp.ObjectProperty.ObjectId AND d.SourceApplicationId = pa.RowId AND pa.RunId = r.RowId AND r.ProtocolLSID LIKE '%:LuminexAssayProtocol.Folder-%')
WHERE PropertyId IN (SELECT pd.PropertyId FROM exp.PropertyDescriptor p, exp.DomainDescriptor d, exp.PropertyDomain pd WHERE p.PropertyId = pd.PropertyId AND d.domainuri LIKE '%:AssayDomain-ExcelRun.Folder-%' AND pd.DomainId = d.DomainId);

-- Add the Unknown type for a titration
ALTER TABLE luminex.DataRow ADD WellRole NVARCHAR(50);

GO

UPDATE luminex.DataRow SET WellRole = 'Standard' WHERE UPPER(Type) LIKE 'S%' OR UPPER(Type) LIKE 'ES%';
UPDATE luminex.DataRow SET WellRole = 'Control' WHERE UPPER(Type) LIKE 'C%';
UPDATE luminex.DataRow SET WellRole = 'Background' WHERE UPPER(Type) LIKE 'B%';
UPDATE luminex.DataRow SET WellRole = 'Unknown' WHERE UPPER(Type) LIKE 'U%';

CREATE TABLE luminex.WellExclusion
(
    RowId INT IDENTITY(1,1) NOT NULL,
    Description NVARCHAR(50),
    Dilution REAL,
    DataId INT NOT NULL,
    Comment NVARCHAR(2000),
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,

    CONSTRAINT PK_Luminex_WellExclusion PRIMARY KEY (RowId),
    CONSTRAINT FK_LuminexWellExclusion_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId)
);

CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Dilution, DataId);

CREATE TABLE luminex.WellExclusionAnalyte
(
    AnalyteId INT,
    WellExclusionId INT NOT NULL,

    CONSTRAINT FK_LuminexWellExclusionAnalyte_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte(RowId),
    CONSTRAINT FK_LuminexWellExclusionAnalyte_WellExclusionId FOREIGN KEY (WellExclusionId) REFERENCES luminex.WellExclusion(RowId)
);

CREATE UNIQUE INDEX UQ_WellExclusionAnalyte ON luminex.WellExclusionAnalyte(AnalyteId, WellExclusionId);

CREATE TABLE luminex.RunExclusion
(
    RunId INT NOT NULL,
    Comment NVARCHAR(2000),
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,

    CONSTRAINT PK_Luminex_RunExclusion PRIMARY KEY (RunId),
    CONSTRAINT FK_LuminexRunExclusion_RunId FOREIGN KEY (RunId) REFERENCES exp.ExperimentRun(RowId)
);

CREATE TABLE luminex.RunExclusionAnalyte
(
    AnalyteId INT,
    RunId INT NOT NULL,

    CONSTRAINT FK_LuminexRunExclusionAnalyte_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte(RowId),
    CONSTRAINT FK_LuminexRunExclusionAnalyte_RunId FOREIGN KEY (RunId) REFERENCES luminex.RunExclusion(RunId)
);

CREATE UNIQUE INDEX UQ_RunExclusionAnalyte ON luminex.RunExclusionAnalyte(AnalyteId, RunId);

-- Don't allow AnalyteId to be NULL in the exclusion tables

DROP INDEX UQ_WellExclusionAnalyte ON luminex.WellExclusionAnalyte;
ALTER TABLE luminex.WellExclusionAnalyte ALTER COLUMN AnalyteId INT NOT NULL;
ALTER TABLE luminex.WellExclusionAnalyte ADD CONSTRAINT PK_LuminexWellExclusionAnalyte PRIMARY KEY (AnalyteId, WellExclusionId);
CREATE INDEX IDX_LuminexWellExclusionAnalyte_WellExclusionID ON luminex.WellExclusionAnalyte(WellExclusionId);

DROP INDEX UQ_RunExclusionAnalyte ON luminex.RunExclusionAnalyte;
ALTER TABLE luminex.RunExclusionAnalyte ALTER COLUMN AnalyteId INT NOT NULL;
ALTER TABLE luminex.RunExclusionAnalyte ADD CONSTRAINT PK_LuminexRunExclusionAnalyte PRIMARY KEY (AnalyteId, RunId);
CREATE INDEX IDX_LuminexRunExclusionAnalyte_RunID ON luminex.RunExclusionAnalyte(RunId);

/* luminex-11.20-11.30.sql */

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

ALTER TABLE luminex.CurveFit ALTER COLUMN CurveType VARCHAR(30);

ALTER TABLE luminex.CurveFit ALTER COLUMN MaxFI REAL NULL;
ALTER TABLE luminex.CurveFit ALTER COLUMN AUC REAL NULL;
ALTER TABLE luminex.CurveFit ALTER COLUMN EC50 REAL NULL;

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

ALTER TABLE luminex.Analyte ADD IncludeInGuideSetCalculation BIT;

GO

UPDATE luminex.analyte SET IncludeInGuideSetCalculation = 0;
ALTER TABLE luminex.analyte ALTER COLUMN IncludeInGuideSetCalculation BIT NOT NULL;

DROP TABLE luminex.GuideSetCurveFit;

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

ALTER TABLE luminex.GuideSet DROP COLUMN MaxFIAverage;
ALTER TABLE luminex.GuideSet DROP COLUMN MaxFIStdDev;

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

DROP INDEX luminex.analyte.ix_luminexdatarow_lsid;
CREATE UNIQUE INDEX UQ_Analyte_LSID ON luminex.Analyte(LSID);

/* luminex-11.30-12.10.sql */

/* NOTE: this change was added on the 11.3 branch after installers for 11.3 were posted;
   this script is for servers that haven't upgraded to 11.31 yet.  */
CREATE INDEX IX_LuminexDataRow_LSID ON luminex.DataRow (LSID);

ALTER TABLE luminex.Analyte ADD PositivityThreshold INT;

ALTER TABLE luminex.CurveFit ADD FailureFlag BIT;