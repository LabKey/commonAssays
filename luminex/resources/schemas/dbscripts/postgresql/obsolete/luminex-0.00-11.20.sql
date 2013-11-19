/*
 * Copyright (c) 2010-2013 LabKey Corporation
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

/* luminex-0.00-11.10.sql */

/* luminex-0.00-10.10.sql */

CREATE SCHEMA luminex;

CREATE TABLE luminex.Analyte
(
    RowId SERIAL NOT NULL,
    LSID LSIDType NOT NULL,
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
    RowId SERIAL NOT NULL,
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
    PTID VARCHAR(32),
    VisitID FLOAT,
    Date TIMESTAMP,
    ExtraSpecimenInfo VARCHAR(50),
    SpecimenID VARCHAR(50),
    Container UniqueIdentifier NOT NULL,
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
  ALTER COLUMN fistring TYPE VARCHAR(64),
  ALTER COLUMN fibackgroundstring TYPE VARCHAR(64),
  ALTER COLUMN stddevstring TYPE VARCHAR(64),
  ALTER COLUMN obsconcstring TYPE VARCHAR(64),
  ALTER COLUMN concinrangestring TYPE VARCHAR(64);

/* luminex-11.10-11.20.sql */

/* luminex-11.10-11.11.sql */

ALTER TABLE luminex.datarow ADD COLUMN LSID LSIDtype;

UPDATE luminex.datarow SET LSID = 'urn:lsid:' || COALESCE ((SELECT p.value
FROM prop.properties p, prop.propertysets ps, core.containers c
WHERE
	p.name = 'defaultLsidAuthority' AND
	ps.set = p.set AND
	ps.category = 'SiteConfig' AND
	ps.objectid = c.entityid AND
	c.name IS NULL AND c.parent IS NULL), 'localhost') || ':LuminexDataRow:' || RowId;

ALTER TABLE luminex.datarow ALTER COLUMN LSID SET NOT NULL;

CREATE INDEX IX_LuminexDataRow_LSID ON luminex.Analyte (LSID);

/* luminex-11.11-11.12.sql */

CREATE TABLE luminex.Titration
(
    RowId SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Standard BOOLEAN NOT NULL,
    QCControl BOOLEAN NOT NULL,

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
    SELECT RowId, 'Standard', true, false FROM exp.experimentrun WHERE protocollsid LIKE '%:LuminexAssayProtocol.%';

-- Assign all existing analytes to the single titration created above
INSERT INTO luminex.AnalyteTitration
    (AnalyteId, TitrationId)
    SELECT a.RowId, t.RowId
        FROM luminex.analyte a, exp.data d, luminex.titration t, exp.protocolapplication pa
        WHERE a.dataid = d.rowid AND t.runid = pa.runid AND d.sourceapplicationid = pa.rowid;

ALTER TABLE luminex.datarow ADD COLUMN TitrationId INT;

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

/* luminex-11.12-11.13.sql */

-- Add the Unknown type for a titration
ALTER TABLE luminex.Titration ADD COLUMN "unknown" BOOLEAN;

UPDATE luminex.Titration SET "unknown" = FALSE;

ALTER TABLE luminex.Titration ALTER COLUMN "unknown" SET NOT NULL;

-- Make sure we have a row in exp.Object for all Luminex data files so that we can migrate the Excel run properties to it
INSERT INTO exp.Object (ObjectURI, Container)
	SELECT d.LSID, d.Container
	FROM exp.Data d, exp.ExperimentRun r, exp.ProtocolApplication pa
	WHERE d.SourceApplicationId = pa.RowId and r.RowId = pa.RunId AND r.ProtocolLSID LIKE '%:LuminexAssayProtocol.Folder-%' AND d.LSID NOT IN (SELECT ObjectURI FROM exp.Object);

-- Clean up run field values that were orphaned when we failed to delete them as part of deleting a Luminex run
DELETE FROM exp.ObjectProperty WHERE ObjectId IN (SELECT o.ObjectId FROM exp.Object o LEFT OUTER JOIN exp.ExperimentRun r ON o.ObjectURI = r.LSID WHERE o.ObjectURI like '%:LuminexAssayRun.Folder-%' AND r.LSID IS NULL);

-- Migrate Excel-based values from run to data 
UPDATE exp.ObjectProperty SET ObjectId =
	(SELECT MIN(dataO.ObjectId) FROM exp.Object dataO, exp.Object runO, exp.Data d, exp.ExperimentRun r, exp.ProtocolApplication pa
	WHERE dataO.ObjectURI = d.LSID AND runO.ObjectURI = r.LSID AND runO.ObjectId = exp.ObjectProperty.ObjectId AND d.SourceApplicationId = pa.RowId AND pa.RunId = r.RowId AND r.ProtocolLSID LIKE '%:LuminexAssayProtocol.Folder-%')
WHERE PropertyId IN (SELECT pd.PropertyId FROM exp.PropertyDescriptor p, exp.DomainDescriptor d, exp.PropertyDomain pd WHERE p.PropertyId = pd.PropertyId AND d.domainuri LIKE '%:AssayDomain-ExcelRun.Folder-%' AND pd.DomainId = d.DomainId);

/* luminex-11.13-11.14.sql */

-- Add the Unknown type for a titration
ALTER TABLE luminex.DataRow ADD COLUMN WellRole VARCHAR(50);

UPDATE luminex.DataRow SET WellRole = 'Standard' WHERE Type ILIKE 'S%' OR Type ILIKE 'ES%';
UPDATE luminex.DataRow SET WellRole = 'Control' WHERE Type ILIKE 'C%';
UPDATE luminex.DataRow SET WellRole = 'Background' WHERE Type ILIKE 'B%';
UPDATE luminex.DataRow SET WellRole = 'Unknown' WHERE Type ILIKE 'U%';

/* luminex-11.14-11.15.sql */

CREATE TABLE luminex.WellExclusion
(
    RowId SERIAL NOT NULL,
    Description VARCHAR(50),
    Dilution REAL,
    DataId INT NOT NULL,
    Comment VARCHAR(2000),
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

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
    Comment VARCHAR(2000),
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

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

/* luminex-11.15-11.16.sql */

-- Don't allow AnalyteId to be NULL in the exclusion tables

ALTER TABLE luminex.WellExclusionAnalyte ALTER COLUMN AnalyteId SET NOT NULL;

DROP INDEX luminex.UQ_WellExclusionAnalyte;

ALTER TABLE luminex.WellExclusionAnalyte ADD CONSTRAINT PK_LuminexWellExclusionAnalyte PRIMARY KEY (AnalyteId, WellExclusionId);

CREATE INDEX IDX_LuminexWellExclusionAnalyte_WellExclusionID ON luminex.WellExclusionAnalyte(WellExclusionId);

ALTER TABLE luminex.RunExclusionAnalyte ALTER COLUMN AnalyteId SET NOT NULL;

DROP INDEX luminex.UQ_RunExclusionAnalyte;

ALTER TABLE luminex.RunExclusionAnalyte ADD CONSTRAINT PK_LuminexRunExclusionAnalyte PRIMARY KEY (AnalyteId, RunId);

CREATE INDEX IDX_LuminexRunExclusionAnalyte_RunID ON luminex.RunExclusionAnalyte(RunId);