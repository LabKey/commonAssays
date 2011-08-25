/*
 * Copyright (c) 2010 LabKey Corporation
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

/* luminex-0.00-2.20.sql */

EXEC sp_addapprole 'luminex', 'password'
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
)
GO

CREATE INDEX IX_LuminexAnalyte_DataId ON luminex.Analyte (DataId)
GO

CREATE TABLE luminex.DataRow
(
    RowId INT IDENTITY(1,1) NOT NULL,
    DataId INT NOT NULL,
    AnalyteId INT NOT NULL,
    Type VARCHAR(10),
    Well VARCHAR(50),
    Outlier BIT,
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

    CONSTRAINT PK_Luminex_DataRow PRIMARY KEY (RowId),
    CONSTRAINT FK_LuminexDataRow_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId),
    CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId)
)
GO

CREATE INDEX IX_LuminexDataRow_DataId ON luminex.DataRow (DataId)
GO

CREATE INDEX IX_LuminexDataRow_AnalyteId ON luminex.DataRow (AnalyteId)
GO


ALTER TABLE luminex.DataRow ADD Dilution REAL
GO

ALTER TABLE luminex.DataRow ADD DataRowGroup VARCHAR(25)
GO

ALTER TABLE luminex.DataRow ADD Ratio VARCHAR(25)
GO

ALTER TABLE luminex.DataRow ADD SamplingErrors VARCHAR(25)
GO

/* luminex-2.20-2.30.sql */

ALTER TABLE luminex.DataRow ADD PTID NVARCHAR(32)
GO

ALTER TABLE luminex.DataRow ADD VisitID FLOAT
GO

ALTER TABLE luminex.DataRow ADD Date DATETIME
GO

/* luminex-2.30-8.10.sql */

ALTER TABLE luminex.DataRow DROP CONSTRAINT FK_LuminexDataRow_AnalyteId
GO

ALTER TABLE luminex.DataRow ADD CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.analyte(RowId)
GO

ALTER TABLE luminex.DataRow ADD ExtraSpecimenInfo NVARCHAR(50)
GO

/* luminex-8.10-8.20.sql */

UPDATE luminex.Analyte SET fitprob = NULL, resvar = NULL WHERE fitprob = 0 AND resvar = 0
GO

/* luminex-9.10-9.20.sql */

ALTER TABLE luminex.DataRow ADD SpecimenID NVARCHAR(50)
GO

-- Copy anything in the Description column that might be a specimen id to the SpecimenID column
UPDATE luminex.DataRow SET SpecimenID = Description WHERE patindex('% %', Description) = 0 AND patindex('%,%', Description) = 0
GO

-- Add denormalized data to improve query performance
ALTER TABLE luminex.datarow ADD Container UniqueIdentifier
GO
ALTER TABLE luminex.datarow ADD ProtocolID INT
GO

-- Set the values for the new columns in existing rows
UPDATE luminex.datarow SET Container = (SELECT container FROM exp.data d WHERE d.rowid = dataid)
GO
UPDATE luminex.datarow SET ProtocolID =
  (SELECT p.RowId FROM exp.experimentrun r, exp.data d, exp.protocol p 
    WHERE d.rowid = dataid AND r.rowid = d.runid AND r.protocollsid = p.lsid)
GO

-- Lock down the values in the columns
ALTER TABLE luminex.datarow ALTER COLUMN Container EntityID NOT NULL
GO
ALTER TABLE luminex.datarow ALTER COLUMN ProtocolID INT NOT NULL
GO

ALTER TABLE luminex.datarow
    ADD CONSTRAINT FK_DataRow_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityID)
GO

ALTER TABLE luminex.datarow
    ADD CONSTRAINT FK_DataRow_ProtocolID FOREIGN KEY (ProtocolID) REFERENCES exp.Protocol (RowID)
GO

CREATE INDEX IDX_DataRow_Container_ProtocolID ON luminex.datarow(Container, ProtocolID)
GO

ALTER TABLE luminex.datarow ADD NewOutlier INT
GO

UPDATE luminex.datarow set NewOutlier = 3 WHERE Outlier = 1
GO

UPDATE luminex.datarow set NewOutlier = 0 WHERE NewOutlier IS NULL
GO

ALTER TABLE luminex.datarow DROP COLUMN Outlier
GO

sp_rename 'luminex.datarow.NewOutlier', 'Outlier'
GO

-- Correctly migrate existing Luminex records to the right outlier code
UPDATE luminex.datarow set Outlier = 1 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND dr.fioorindicator = '---' AND d.datafileurl like '%Summary%')
GO
UPDATE luminex.datarow set Outlier = 2 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND dr.fioorindicator IS NULL AND d.datafileurl like '%Summary%')
GO
UPDATE luminex.datarow set Outlier = 1 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND d.datafileurl like '%Raw%')
GO

-- Trim leading spaces from the extraspecimeninfo column
UPDATE luminex.datarow SET extraspecimeninfo = substring(extraspecimeninfo, 2, len(extraspecimeninfo)) WHERE charindex(extraspecimeninfo, ' ') = 0
GO