/*
 * Copyright (c) 2019 LabKey Corporation
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

CREATE INDEX IX_LuminexDataRow_LSID ON luminex.Analyte (LSID);

CREATE UNIQUE INDEX UQ_Analyte_LSID ON luminex.Analyte(LSID);

ALTER TABLE luminex.Analyte ADD COLUMN PositivityThreshold INT;

ALTER TABLE luminex.Analyte ADD COLUMN NegativeBead VARCHAR(50);

ALTER TABLE luminex.Analyte ADD BeadNumber VARCHAR(50);

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

ALTER TABLE luminex.DataRow
  ALTER COLUMN fistring TYPE VARCHAR(64),
  ALTER COLUMN fibackgroundstring TYPE VARCHAR(64),
  ALTER COLUMN stddevstring TYPE VARCHAR(64),
  ALTER COLUMN obsconcstring TYPE VARCHAR(64),
  ALTER COLUMN concinrangestring TYPE VARCHAR(64);

ALTER TABLE luminex.datarow ADD COLUMN LSID LSIDtype;

ALTER TABLE luminex.datarow ALTER COLUMN LSID SET NOT NULL;

ALTER TABLE luminex.datarow ADD COLUMN TitrationId INT;

CREATE INDEX IX_LuminexDataRow_TitrationId ON luminex.DataRow (TitrationId);

-- Add the Unknown type for a titration
ALTER TABLE luminex.DataRow ADD COLUMN WellRole VARCHAR(50);

ALTER TABLE luminex.DataRow ADD COLUMN Summary Boolean NOT NULL;
ALTER TABLE luminex.DataRow ADD COLUMN CV REAL;

ALTER TABLE luminex.datarow ADD COLUMN SinglePointControlId INT;
CREATE INDEX IX_LuminexDataRow_SinglePointControlId ON luminex.DataRow (SinglePointControlId);

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

CREATE INDEX IX_LuminexTitration_RunId ON luminex.Titration (RunId);

-- Add the Unknown type for a titration
ALTER TABLE luminex.Titration ADD COLUMN "unknown" BOOLEAN NOT NULL;

ALTER TABLE luminex.Titration ADD COLUMN OtherControl BOOLEAN NOT NULL;

ALTER TABLE luminex.datarow ADD CONSTRAINT FK_Luminex_DataRow_TitrationId FOREIGN KEY (TitrationId) REFERENCES luminex.Titration(RowId);

CREATE TABLE luminex.AnalyteTitration
(
    AnalyteId INT NOT NULL,
    TitrationId INT NOT NULL,

    CONSTRAINT PK_Luminex_AnalyteTitration PRIMARY KEY (AnalyteId, TitrationId)
);

ALTER TABLE luminex.AnalyteTitration ADD COLUMN MaxFI REAL;

ALTER TABLE luminex.AnalyteTitration ADD COLUMN GuideSetId INT;
CREATE INDEX IDX_AnalyteTitration_GuideSetId ON luminex.AnalyteTitration(GuideSetId);

ALTER TABLE luminex.AnalyteTitration ADD COLUMN IncludeInGuideSetCalculation BOOLEAN NOT NULL;

-- add missing FK constraint and indices for luminex.AnalyteTitration
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalyteTitration_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte(RowId);
CREATE INDEX IDX_LuminexAnalyteTitration_AnalyteId ON luminex.AnalyteTitration(AnalyteId);
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalyteTitration_TitrationId FOREIGN KEY (TitrationId) REFERENCES luminex.Titration(RowId);
CREATE INDEX IDX_LuminexAnalyteTitration_TitrationId ON luminex.AnalyteTitration(TitrationId);

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

ALTER TABLE luminex.WellExclusion ADD COLUMN "type" VARCHAR(10);

CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Dilution, Type, DataId);

ALTER TABLE luminex.WellExclusion ADD COLUMN Well VARCHAR(50);

CREATE INDEX IDX_LuminexWellExclusion_DataId ON luminex.WellExclusion(DataId);

CREATE TABLE luminex.WellExclusionAnalyte
(
    AnalyteId INT NOT NULL,
    WellExclusionId INT NOT NULL,

    CONSTRAINT FK_LuminexWellExclusionAnalyte_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte(RowId),
    CONSTRAINT FK_LuminexWellExclusionAnalyte_WellExclusionId FOREIGN KEY (WellExclusionId) REFERENCES luminex.WellExclusion(RowId)
);

ALTER TABLE luminex.WellExclusionAnalyte ADD CONSTRAINT PK_LuminexWellExclusionAnalyte PRIMARY KEY (AnalyteId, WellExclusionId);

CREATE INDEX IDX_LuminexWellExclusionAnalyte_WellExclusionID ON luminex.WellExclusionAnalyte(WellExclusionId);
CREATE INDEX IDX_LuminexWellExclusionAnalyte_AnalyteId ON luminex.WellExclusionAnalyte(AnalyteId);

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

CREATE INDEX IDX_LuminexRunExclusion_RunId ON luminex.RunExclusion(RunId);

CREATE TABLE luminex.RunExclusionAnalyte
(
    AnalyteId INT NOT NULL,
    RunId INT NOT NULL,

    CONSTRAINT FK_LuminexRunExclusionAnalyte_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte(RowId),
    CONSTRAINT FK_LuminexRunExclusionAnalyte_RunId FOREIGN KEY (RunId) REFERENCES luminex.RunExclusion(RunId)
);

ALTER TABLE luminex.RunExclusionAnalyte ADD CONSTRAINT PK_LuminexRunExclusionAnalyte PRIMARY KEY (AnalyteId, RunId);

CREATE INDEX IDX_LuminexRunExclusionAnalyte_RunID ON luminex.RunExclusionAnalyte(RunId);
CREATE INDEX IDX_LuminexRunExclusionAnalyte_AnalyteId ON luminex.RunExclusionAnalyte(AnalyteId);

CREATE TABLE luminex.CurveFit
(
	RowId serial NOT NULL,
	TitrationId INT NOT NULL,
	AnalyteId INT NOT NULL,
	CurveType VARCHAR(20) NOT NULL,
	EC50 REAL NOT NULL,
	AUC REAL NOT NULL,

	CONSTRAINT PK_luminex_CurveFit PRIMARY KEY (rowid),
	CONSTRAINT FK_CurveFit_AnalyteIdTitrationId FOREIGN KEY (AnalyteId, TitrationId) REFERENCES luminex.AnalyteTitration (AnalyteId, TitrationId),
	CONSTRAINT UQ_CurveFit UNIQUE (AnalyteId, TitrationId, CurveType)
);

ALTER TABLE luminex.CurveFit ALTER COLUMN CurveType TYPE VARCHAR(30);
ALTER TABLE luminex.CurveFit ALTER COLUMN AUC DROP NOT NULL;
ALTER TABLE luminex.CurveFit ALTER COLUMN EC50 DROP NOT NULL;

-- Add the 4 and 5 PL curve fit values
ALTER TABLE luminex.CurveFit ADD COLUMN MinAsymptote REAL;
ALTER TABLE luminex.CurveFit ADD COLUMN MaxAsymptote REAL;
ALTER TABLE luminex.CurveFit ADD COLUMN Asymmetry REAL;
ALTER TABLE luminex.CurveFit ADD COLUMN Inflection REAL;
ALTER TABLE luminex.CurveFit ADD COLUMN Slope REAL;

ALTER TABLE luminex.CurveFit ADD COLUMN FailureFlag BOOLEAN;

-- add missing indices for FK constraints on existing table columns
CREATE INDEX IDX_LuminexCurveFit_AnalyteIdTitrationId ON luminex.CurveFit(AnalyteId, TitrationId);

CREATE TABLE luminex.GuideSet
(
	RowId SERIAL NOT NULL,
    ProtocolId INT NOT NULL,
    AnalyteName VARCHAR(50) NOT NULL,
    CurrentGuideSet BOOLEAN NOT NULL,
    Conjugate VARCHAR(50),
    Isotype VARCHAR(50),

	CONSTRAINT PK_luminex_GuideSet PRIMARY KEY (RowId),
	CONSTRAINT FK_luminex_GuideSet_ProtocolId FOREIGN KEY (ProtocolId) REFERENCES exp.Protocol(RowId)
);

CREATE INDEX IDX_GuideSet_ProtocolId ON luminex.GuideSet(ProtocolId);

ALTER TABLE luminex.GuideSet ADD COLUMN ControlName VARCHAR(255);
ALTER TABLE luminex.GuideSet ADD COLUMN Comment TEXT;
ALTER TABLE luminex.GuideSet ADD COLUMN CreatedBy USERID;
ALTER TABLE luminex.GuideSet ADD COLUMN Created TIMESTAMP;
ALTER TABLE luminex.GuideSet ADD COLUMN ModifiedBy USERID;
ALTER TABLE luminex.GuideSet ADD COLUMN Modified TIMESTAMP;
ALTER TABLE luminex.GuideSet ADD COLUMN ValueBased BOOLEAN NOT NULL;

ALTER TABLE luminex.GuideSet ADD COLUMN EC504PLAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD COLUMN EC504PLStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD COLUMN EC505PLAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD COLUMN EC505PLStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD COLUMN AUCAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD COLUMN AUCStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD COLUMN MaxFIAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD COLUMN MaxFIStdDev DOUBLE PRECISION;

ALTER TABLE luminex.GuideSet ADD EC504PLEnabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE luminex.GuideSet ADD EC505PLEnabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE luminex.GuideSet ADD AUCEnabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE luminex.GuideSet ADD MaxFIEnabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE luminex.GuideSet ADD IsTitration BOOLEAN NOT NULL;

ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalytTitration_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet(RowId);

CREATE TABLE luminex.SinglePointControl
(
    RowId SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,

    CONSTRAINT PK_Luminex_SinglePointControl PRIMARY KEY (RowId),
    CONSTRAINT FK_Luminex_SinglePointControl_RunId FOREIGN KEY (RunId) REFERENCES exp.ExperimentRun(RowId),
    CONSTRAINT UQ_SinglePointControl UNIQUE (Name, RunId)
);
CREATE INDEX IX_LuminexSinglePointControl_RunId ON luminex.SinglePointControl (RunId);

ALTER TABLE luminex.datarow ADD CONSTRAINT FK_Luminex_DataRow_SinglePointControlId FOREIGN KEY (SinglePointControlId) REFERENCES luminex.SinglePointControl(RowId);

CREATE TABLE luminex.AnalyteSinglePointControl
(
    SinglePointControlId INT NOT NULL,
    AnalyteId INT NOT NULL,
    GuideSetId INT,
    IncludeInGuideSetCalculation BOOLEAN NOT NULL,

    CONSTRAINT PK_AnalyteSinglePointControl PRIMARY KEY (AnalyteId, SinglePointControlId),
    CONSTRAINT FK_AnalyteSinglePointControl_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte (RowId),
    CONSTRAINT FK_AnalyteSinglePointControl_SinglePointControlId FOREIGN KEY (SinglePointControlId) REFERENCES luminex.SinglePointControl (RowId),
    CONSTRAINT FK_AnalyteSinglePointControl_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet (RowId)
);

CREATE INDEX IDX_AnalyteSinglePointControl_GuideSetId ON luminex.AnalyteSinglePointControl(GuideSetId);
CREATE INDEX IDX_AnalyteSinglePointControl_SinglePointControlId ON luminex.AnalyteSinglePointControl(SinglePointControlId);
CREATE INDEX IDX_AnalyteSinglePointControl_AnalyteId ON luminex.AnalyteSinglePointControl(AnalyteId);
