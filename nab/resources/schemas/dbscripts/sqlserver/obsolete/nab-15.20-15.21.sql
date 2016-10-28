/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
