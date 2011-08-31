/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
    Outlier BOOLEAN,
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
);

CREATE INDEX IX_LuminexDataRow_DataId ON luminex.DataRow (DataId);
CREATE INDEX IX_LuminexDataRow_AnalyteId ON luminex.DataRow (AnalyteId);

ALTER TABLE luminex.DataRow ADD COLUMN Dilution REAL;

ALTER TABLE luminex.DataRow ADD COLUMN DataRowGroup VARCHAR(25);

ALTER TABLE luminex.DataRow ADD COLUMN Ratio VARCHAR(25);

ALTER TABLE luminex.DataRow ADD COLUMN SamplingErrors VARCHAR(25);

/* luminex-2.20-2.30.sql */

ALTER TABLE luminex.DataRow ADD COLUMN PTID VARCHAR(32);

ALTER TABLE luminex.DataRow ADD COLUMN VisitID FLOAT;

ALTER TABLE luminex.DataRow ADD COLUMN Date TIMESTAMP;

/* luminex-2.30-8.10.sql */

ALTER TABLE luminex.DataRow DROP CONSTRAINT FK_LuminexDataRow_AnalyteId;

ALTER TABLE luminex.DataRow ADD CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.analyte(RowId);

ALTER TABLE luminex.DataRow ADD COLUMN ExtraSpecimenInfo VARCHAR(50);

/* luminex-8.10-8.20.sql */

UPDATE luminex.Analyte SET fitprob = NULL, resvar = NULL WHERE fitprob = 0 AND resvar = 0;

/* luminex-9.10-9.20.sql */

ALTER TABLE luminex.DataRow ADD COLUMN SpecimenID VARCHAR(50);

-- Copy anything in the Description column that might be a specimen id to the SpecimenID column
UPDATE luminex.DataRow SET SpecimenID = Description WHERE strpos(Description, ' ') = 0 AND strpos(Description, ',') = 0;

-- Add denormalized data to improve query performance
ALTER TABLE luminex.datarow ADD COLUMN Container UniqueIdentifier;
ALTER TABLE luminex.datarow ADD COLUMN ProtocolID INT;

-- Set the values for the new columns in existing rows
UPDATE luminex.datarow SET Container = (SELECT container FROM exp.data d WHERE d.rowid = dataid);
UPDATE luminex.datarow SET ProtocolID =
  (SELECT p.RowId FROM exp.experimentrun r, exp.data d, exp.protocol p 
    WHERE d.rowid = dataid AND r.rowid = d.runid AND r.protocollsid = p.lsid);

-- Lock down the values in the columns
ALTER TABLE luminex.datarow ALTER COLUMN Container SET NOT NULL;
ALTER TABLE luminex.datarow ALTER COLUMN ProtocolID SET NOT NULL;

ALTER TABLE luminex.datarow
    ADD CONSTRAINT FK_DataRow_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityID);

ALTER TABLE luminex.datarow
    ADD CONSTRAINT FK_DataRow_ProtocolID FOREIGN KEY (ProtocolID) REFERENCES exp.Protocol (RowID);

CREATE INDEX IDX_DataRow_Container_ProtocolID ON luminex.datarow(Container, ProtocolID);

ANALYZE luminex.datarow;

ALTER TABLE luminex.datarow ADD COLUMN NewOutlier INT;

UPDATE luminex.datarow set NewOutlier = 3 WHERE Outlier = TRUE;

UPDATE luminex.datarow set NewOutlier = 0 WHERE NewOutlier IS NULL;

ALTER TABLE luminex.datarow DROP COLUMN Outlier;

ALTER TABLE luminex.datarow RENAME COLUMN NewOutlier TO Outlier;

-- Correctly migrate existing Luminex records to the right outlier code
UPDATE luminex.datarow set Outlier = 1 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND dr.fioorindicator = '---' AND d.datafileurl like '%Summary%');

UPDATE luminex.datarow set Outlier = 2 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND dr.fioorindicator IS NULL AND d.datafileurl like '%Summary%');

UPDATE luminex.datarow set Outlier = 1 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND d.datafileurl like '%Raw%');

-- Trim leading spaces from the extraspecimeninfo column
UPDATE luminex.datarow set extraspecimeninfo = substring(extraspecimeninfo from 2) WHERE strpos(extraspecimeninfo, ' ') = 1;