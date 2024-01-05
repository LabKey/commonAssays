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

CREATE UNIQUE INDEX UQ_Analyte_LSID ON luminex.Analyte(LSID);

ALTER TABLE luminex.Analyte ADD PositivityThreshold INT;

ALTER TABLE luminex.Analyte ADD NegativeBead VARCHAR(50);
GO

ALTER TABLE luminex.Analyte ADD BeadNumber NVARCHAR(50);
GO

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

ALTER TABLE luminex.DataRow ALTER COLUMN fistring NVARCHAR(64);
ALTER TABLE luminex.DataRow ALTER COLUMN fibackgroundstring NVARCHAR(64);
ALTER TABLE luminex.DataRow ALTER COLUMN stddevstring NVARCHAR(64);
ALTER TABLE luminex.DataRow ALTER COLUMN obsconcstring NVARCHAR(64);
ALTER TABLE luminex.DataRow ALTER COLUMN concinrangestring NVARCHAR(64);

ALTER TABLE luminex.datarow ADD LSID LSIDtype;
GO

ALTER TABLE luminex.datarow ALTER COLUMN LSID LSIDType NOT NULL;

ALTER TABLE luminex.datarow ADD TitrationId INT;

GO

CREATE INDEX IX_LuminexDataRow_TitrationId ON luminex.DataRow (TitrationId);

-- Add the Unknown type for a titration
ALTER TABLE luminex.DataRow ADD WellRole NVARCHAR(50);

GO

ALTER TABLE luminex.DataRow ADD Summary BIT;
ALTER TABLE luminex.DataRow ADD CV REAL;

GO

ALTER TABLE luminex.DataRow ALTER COLUMN Summary BIT NOT NULL;

CREATE INDEX IX_LuminexDataRow_LSID ON luminex.DataRow (LSID);

ALTER TABLE luminex.datarow ADD SinglePointControlId INT;
GO
CREATE INDEX IX_LuminexDataRow_SinglePointControlId ON luminex.DataRow (SinglePointControlId);

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

ALTER TABLE luminex.datarow ADD CONSTRAINT FK_Luminex_DataRow_TitrationId FOREIGN KEY (TitrationId) REFERENCES luminex.Titration(RowId);

CREATE INDEX IX_LuminexTitration_RunId ON luminex.Titration (RunId);

ALTER TABLE luminex.Titration ADD "unknown" BIT;

GO

ALTER TABLE luminex.Titration ALTER COLUMN "unknown" BIT NOT NULL;

ALTER TABLE luminex.Titration ADD OtherControl BIT;
GO
ALTER TABLE luminex.Titration ALTER COLUMN OtherControl BIT NOT NULL;

CREATE TABLE luminex.AnalyteTitration
(
    AnalyteId INT NOT NULL,
    TitrationId INT NOT NULL,

    CONSTRAINT PK_Luminex_AnalyteTitration PRIMARY KEY (AnalyteId, TitrationId)
);

ALTER TABLE luminex.AnalyteTitration ADD MaxFI REAL;
ALTER TABLE luminex.AnalyteTitration ADD GuideSetId INT;
ALTER TABLE luminex.AnalyteTitration ADD IncludeInGuideSetCalculation BIT NOT NULL;

GO

CREATE INDEX IDX_AnalyteTitration_GuideSetId ON luminex.AnalyteTitration(GuideSetId);

-- add missing FK constraint and indices for luminex.AnalyteTitration
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalyteTitration_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte(RowId);
CREATE INDEX IDX_LuminexAnalyteTitration_AnalyteId ON luminex.AnalyteTitration(AnalyteId);
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalyteTitration_TitrationId FOREIGN KEY (TitrationId) REFERENCES luminex.Titration(RowId);
CREATE INDEX IDX_LuminexAnalyteTitration_TitrationId ON luminex.AnalyteTitration(TitrationId);

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

ALTER TABLE luminex.WellExclusion ADD Type VARCHAR(10);
GO

CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Dilution, Type, DataId);

ALTER TABLE luminex.WellExclusion ADD Well NVARCHAR(50);

CREATE INDEX IDX_LuminexWellExclusion_DataId ON luminex.WellExclusion(DataId);

CREATE TABLE luminex.WellExclusionAnalyte
(
    AnalyteId INT,
    WellExclusionId INT NOT NULL,

    CONSTRAINT FK_LuminexWellExclusionAnalyte_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte(RowId),
    CONSTRAINT FK_LuminexWellExclusionAnalyte_WellExclusionId FOREIGN KEY (WellExclusionId) REFERENCES luminex.WellExclusion(RowId)
);

ALTER TABLE luminex.WellExclusionAnalyte ALTER COLUMN AnalyteId INT NOT NULL;
ALTER TABLE luminex.WellExclusionAnalyte ADD CONSTRAINT PK_LuminexWellExclusionAnalyte PRIMARY KEY (AnalyteId, WellExclusionId);
CREATE INDEX IDX_LuminexWellExclusionAnalyte_WellExclusionID ON luminex.WellExclusionAnalyte(WellExclusionId);

CREATE INDEX IDX_LuminexWellExclusionAnalyte_AnalyteId ON luminex.WellExclusionAnalyte(AnalyteId);

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
    RowId INT IDENTITY(1,1) NOT NULL,
    TitrationId INT NOT NULL,
    AnalyteId INT NOT NULL,
    CurveType VARCHAR(20) NOT NULL,
    EC50 REAL NULL,
    AUC REAL NULL,

    CONSTRAINT PK_luminex_CurveFit PRIMARY KEY (rowid),
    CONSTRAINT FK_CurveFit_AnalyteIdTitrationId FOREIGN KEY (AnalyteId, TitrationId) REFERENCES luminex.AnalyteTitration (AnalyteId, TitrationId),
    CONSTRAINT UQ_CurveFit UNIQUE (AnalyteId, TitrationId, CurveType)
);

ALTER TABLE luminex.CurveFit ALTER COLUMN CurveType VARCHAR(30);

-- Add the 4 and 5 PL curve fit values
ALTER TABLE luminex.CurveFit ADD MinAsymptote REAL;
ALTER TABLE luminex.CurveFit ADD MaxAsymptote REAL;
ALTER TABLE luminex.CurveFit ADD Asymmetry REAL;
ALTER TABLE luminex.CurveFit ADD Inflection REAL;
ALTER TABLE luminex.CurveFit ADD Slope REAL;

ALTER TABLE luminex.CurveFit ADD FailureFlag BIT;

-- add missing indices for FK constraints on existing table columns
CREATE INDEX IDX_LuminexCurveFit_AnalyteIdTitrationId ON luminex.CurveFit(AnalyteId, TitrationId);

CREATE TABLE luminex.GuideSet
(
    RowId INT IDENTITY(1,1) NOT NULL,
    ProtocolId INT NOT NULL,
    AnalyteName VARCHAR(50) NOT NULL,
    CurrentGuideSet BIT NOT NULL,
    Conjugate VARCHAR(50),
    Isotype VARCHAR(50),

    CONSTRAINT PK_luminex_GuideSet PRIMARY KEY (RowId),
    CONSTRAINT FK_luminex_GuideSet_ProtocolId FOREIGN KEY (ProtocolId) REFERENCES exp.Protocol(RowId)
);

ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalytTitration_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet(RowId);

CREATE INDEX IDX_GuideSet_ProtocolId ON luminex.GuideSet(ProtocolId);

ALTER TABLE luminex.GuideSet ADD ControlName VARCHAR(255);
ALTER TABLE luminex.GuideSet ADD Comment TEXT;
ALTER TABLE luminex.GuideSet ADD CreatedBy USERID;
ALTER TABLE luminex.GuideSet ADD Created DATETIME;
ALTER TABLE luminex.GuideSet ADD ModifiedBy USERID;
ALTER TABLE luminex.GuideSet ADD Modified DATETIME;
ALTER TABLE luminex.GuideSet ADD ValueBased BIT;
GO

ALTER TABLE luminex.GuideSet ALTER COLUMN ValueBased BIT NOT NULL;

ALTER TABLE luminex.GuideSet ADD EC504PLAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD EC504PLStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD EC505PLAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD EC505PLStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD AUCAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD AUCStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD MaxFIAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ADD MaxFIStdDev DOUBLE PRECISION;

ALTER TABLE luminex.GuideSet ADD EC504PLEnabled BIT NOT NULL DEFAULT 1;
ALTER TABLE luminex.GuideSet ADD EC505PLEnabled BIT NOT NULL DEFAULT 1;
ALTER TABLE luminex.GuideSet ADD AUCEnabled BIT NOT NULL DEFAULT 1;
ALTER TABLE luminex.GuideSet ADD MaxFIEnabled BIT NOT NULL DEFAULT 1;

ALTER TABLE luminex.GuideSet ADD IsTitration BIT;
GO

ALTER TABLE luminex.GuideSet ALTER COLUMN IsTitration BIT NOT NULL;

CREATE TABLE luminex.SinglePointControl
(
    RowId INT IDENTITY(1,1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_Luminex_SinglePointControl PRIMARY KEY (RowId),
    CONSTRAINT FK_Luminex_SinglePointControl_RunId FOREIGN KEY (RunId) REFERENCES exp.ExperimentRun(RowId),
    CONSTRAINT UQ_SinglePointControl UNIQUE (Name, RunId)
);

CREATE INDEX IX_LuminexSinglePointControl_RunId ON luminex.SinglePointControl(RunId);

ALTER TABLE luminex.datarow ADD CONSTRAINT FK_Luminex_DataRow_SinglePointControlId FOREIGN KEY (SinglePointControlId) REFERENCES luminex.SinglePointControl(RowId);

CREATE TABLE luminex.AnalyteSinglePointControl
(
    SinglePointControlId INT NOT NULL,
    AnalyteId INT NOT NULL,
    GuideSetId INT,
    IncludeInGuideSetCalculation BIT NOT NULL,

    CONSTRAINT PK_AnalyteSinglePointControl PRIMARY KEY (AnalyteId, SinglePointControlId),
    CONSTRAINT FK_AnalyteSinglePointControl_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte (RowId),
    CONSTRAINT FK_AnalyteSinglePointControl_SinglePointControlId FOREIGN KEY (SinglePointControlId) REFERENCES luminex.SinglePointControl (RowId),
    CONSTRAINT FK_AnalyteSinglePointControl_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet (RowId)
);

CREATE INDEX IDX_AnalyteSinglePointControl_GuideSetId ON luminex.AnalyteSinglePointControl(GuideSetId);
CREATE INDEX IDX_AnalyteSinglePointControl_SinglePointControlId ON luminex.AnalyteSinglePointControl(SinglePointControlId);
CREATE INDEX IDX_AnalyteSinglePointControl_AnalyteId ON luminex.AnalyteSinglePointControl(AnalyteId);
