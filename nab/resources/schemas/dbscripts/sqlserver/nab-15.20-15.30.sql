/*
 * Copyright (c) 2015 LabKey Corporation
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

/* nab-15.20-15.21.sql */

CREATE TABLE NAb.DilutionData
(
  RowId INT IDENTITY (1, 1) NOT NULL,
  Dilution REAL,
  DilutionOrder INT,
  PercentNeutralization REAL,
  NeutralizationPlusMinus REAL,
  Min REAL,
  Max REAL,
  Mean REAL,
  StdDev REAL,

  CONSTRAINT PK_NAb_DilutionData PRIMARY KEY (RowId)

);

CREATE TABLE NAb.WellData
(
  RowId INT IDENTITY (1, 1) NOT NULL,
  RunId INT NOT NULL,
  SpecimenLsid LSIDtype NOT NULL,
  RunDataId INT NOT NULL,
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

CREATE INDEX IDX_WellData_RunId ON NAb.WellData(RunId);

/* nab-15.21-15.22.sql */

ALTER TABLE nab.WellData ALTER COLUMN SpecimenLsid LSIDType NULL;
ALTER TABLE nab.WellData ALTER COLUMN RunDataId INTEGER NULL;
ALTER TABLE nab.WellData ADD PlateNumber INT;
ALTER TABLE nab.WellData ADD PlateVirusName NVARCHAR(100);

ALTER TABLE nab.DilutionData ALTER COLUMN Dilution DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN PercentNeutralization DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN NeutralizationPlusMinus DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN Min DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN Max DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN Mean DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN StdDev DOUBLE PRECISION;

ALTER TABLE nab.DilutionData ADD WellgroupName NVARCHAR(100);
ALTER TABLE nab.DilutionData ADD ReplicateName NVARCHAR(100);
ALTER TABLE nab.DilutionData ADD RunDataId INT;
ALTER TABLE nab.DilutionData ADD MaxDilution DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ADD MinDilution DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ADD PlateNumber INT;
ALTER TABLE nab.DilutionData ADD RunId INT;
ALTER TABLE nab.DilutionData ADD ProtocolId INT;
ALTER TABLE nab.DilutionData ADD Container ENTITYID NOT NULL;

ALTER TABLE nab.DilutionData ADD CONSTRAINT FK_DilutionData_ExperimentRun FOREIGN KEY (RunId)
  REFERENCES Exp.ExperimentRun (RowId)
  ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.DilutionData ADD CONSTRAINT FK_DilutionData_RunDataId FOREIGN KEY (RunDataId)
  REFERENCES nab.NAbSpecimen (RowId)
  ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE INDEX IDX_DilutionData_RunId ON nab.DilutionData(RunId);

EXEC core.executeJavaUpgradeCode 'upgradeDilutionAssayWithNewTables';

/* nab-15.22-15.23.sql */

ALTER TABLE nab.NabSpecimen ALTER COLUMN FitError DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_poly DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_Poly DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_4pl DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_4pl DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_5pl DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_5pl DOUBLE PRECISION;

ALTER TABLE nab.CutoffValue ALTER COLUMN Cutoff DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN Point DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_Poly DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_4pl DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_5pl DOUBLE PRECISION;