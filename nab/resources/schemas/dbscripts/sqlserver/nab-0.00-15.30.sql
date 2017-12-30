/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

/* nab-12.30-13.10.sql */

CREATE SCHEMA nab;
GO

CREATE TABLE nab.CutoffValue
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    NAbSpecimenId INT NOT NULL,
    Cutoff DOUBLE PRECISION,
    Point DOUBLE PRECISION,
    PointOORIndicator NVARCHAR(20),

    IC_Poly DOUBLE PRECISION,
    IC_PolyOORIndicator NVARCHAR(20),
    IC_4pl DOUBLE PRECISION,
    IC_4plOORIndicator NVARCHAR(20),
    IC_5pl DOUBLE PRECISION,
    IC_5plOORIndicator NVARCHAR(20),

    CONSTRAINT PK_NAb_CutoffValue PRIMARY KEY (RowId)
);

CREATE TABLE nab.NAbSpecimen
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    DataId INT,
    RunId INT NOT NULL,
    SpecimenLSID LSIDtype NOT NULL,
    FitError DOUBLE PRECISION,
    WellgroupName NVARCHAR(100),

    AUC_poly DOUBLE PRECISION,
    PositiveAUC_Poly DOUBLE PRECISION,
    AUC_4pl DOUBLE PRECISION,
    PositiveAUC_4pl DOUBLE PRECISION,
    AUC_5pl DOUBLE PRECISION,
    PositiveAUC_5pl DOUBLE PRECISION,

    -- For legacy migration purposes
    ObjectUri NVARCHAR(300),
    ObjectId INT NOT NULL,
    ProtocolId INT,
	viruslsid lsidtype,

    CONSTRAINT PK_NAb_Specimen PRIMARY KEY (RowId),
    CONSTRAINT FK_NAbSpecimen_ExperimentRun FOREIGN KEY (RunId)
      REFERENCES Exp.ExperimentRun (RowId)
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT FK_NAbSpecimen_SpecimenLSID FOREIGN KEY (SpecimenLSID)
      REFERENCES Exp.Material (LSID)
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX IDX_NAbSpecimen_RunId ON nab.NAbSpecimen(RunId);
CREATE INDEX IDX_NAbSpecimen_ObjectId ON nab.NAbSpecimen(ObjectId);
CREATE INDEX IDX_NAbSpecimen_DataId ON nab.NAbSpecimen(DataId);

ALTER TABLE nab.CutoffValue ADD CONSTRAINT FK_CutoffValue_NAbSpecimen FOREIGN KEY (NAbSpecimenId)
        REFERENCES nab.NAbSpecimen (rowid);
ALTER TABLE nab.NAbSpecimen ADD CONSTRAINT FK_NAbSpecimen_ProtocolId FOREIGN KEY (ProtocolId)
        REFERENCES Exp.Protocol (rowid);


CREATE INDEX IDX_NAbSpecimen_ProtocolId ON nab.NAbSpecimen(ProtocolId);
CREATE INDEX IDX_CutoffValue_NabSpecimenId ON nab.cutoffvalue(NabSpecimenId);
GO

/* nab-14.20-14.30.sql */

CREATE SCHEMA nabvirus;
GO

/* nab-15.10-15.11.sql */

CREATE INDEX IDX_NAbSpecimen_SpecimenLSID ON nab.NAbSpecimen(SpecimenLSID);

/* nab-15.20-15.21.sql */

CREATE TABLE nab.DilutionData
(
  RowId INT IDENTITY (1, 1) NOT NULL,
  Dilution DOUBLE PRECISION,
  DilutionOrder INT,
  PercentNeutralization DOUBLE PRECISION,
  NeutralizationPlusMinus DOUBLE PRECISION,
  Min DOUBLE PRECISION,
  Max DOUBLE PRECISION,
  Mean DOUBLE PRECISION,
  StdDev DOUBLE PRECISION,
  WellgroupName NVARCHAR(100),
  ReplicateName NVARCHAR(100),
  RunDataId INT,
  MaxDilution DOUBLE PRECISION,
  MinDilution DOUBLE PRECISION,
  PlateNumber INT,
  RunId INT,
  ProtocolId INT,
  Container ENTITYID NOT NULL,

  CONSTRAINT PK_NAb_DilutionData PRIMARY KEY (RowId)
);

ALTER TABLE nab.DilutionData ADD CONSTRAINT FK_DilutionData_ExperimentRun FOREIGN KEY (RunId)
	REFERENCES Exp.ExperimentRun (RowId)
	ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.DilutionData ADD CONSTRAINT FK_DilutionData_RunDataId FOREIGN KEY (RunDataId)
	REFERENCES nab.NAbSpecimen (RowId)
	ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE INDEX IDX_DilutionData_RunId ON nab.DilutionData(RunId);

CREATE TABLE nab.WellData
(
  RowId INT IDENTITY (1, 1) NOT NULL,
  RunId INT NOT NULL,
  SpecimenLsid LSIDtype NULL,
  RunDataId INT NULL,
  DilutionDataId INT,
  ProtocolId INT,
  "Row" INT,
  "Column" INT,
  Value REAL,
  ControlWellgroup NVARCHAR(100),
  VirusWellgroup NVARCHAR(100),
  SpecimenWellgroup NVARCHAR(100),
  ReplicateWellgroup NVARCHAR(100),
  ReplicateNumber INT,
  Container EntityId NOT NULL,
  PlateNumber INT,
  PlateVirusName NVARCHAR(100),

  CONSTRAINT PK_NAb_WellData PRIMARY KEY (RowId),
  CONSTRAINT FK_WellData_ExperimentRun FOREIGN KEY (RunId)
    REFERENCES Exp.ExperimentRun (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT FK_WellData_SpecimenLSID FOREIGN KEY (SpecimenLSID)
    REFERENCES Exp.Material (LSID)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT FK_WellData_RunDataId FOREIGN KEY (RunDataId)
    REFERENCES NAb.NAbSpecimen (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT FK_WellData_DilutionDataId FOREIGN KEY (DilutionDataId)
    REFERENCES NAb.DilutionData (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION

);

CREATE INDEX IDX_WellData_RunId ON nab.WellData(RunId);
