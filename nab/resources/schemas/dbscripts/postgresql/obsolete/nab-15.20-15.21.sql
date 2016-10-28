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

CREATE TABLE nab.dilutiondata
(
  RowId SERIAL NOT NULL,
  Dilution REAL,
  DilutionOrder INT,
  PercentNeutralization REAL,
  NeutralizationPlusMinus REAL,
  Min REAL,
  Max REAL,
  Mean REAL,
  StdDev REAL,

  CONSTRAINT pk_dilutiondata PRIMARY KEY (RowId)

);

CREATE TABLE nab.welldata
(
  RowId SERIAL NOT NULL,
  RunId INT NOT NULL,
  SpecimenLsid lsidtype NOT NULL,
  RunDataId INT NOT NULL,
  DilutionDataId INT,
  ProtocolId INT,
  "Row" INT,
  "Column" INT,
  Value REAL,
  ControlWellgroup VARCHAR(100),
  VirusWellgroup VARCHAR(100),
  SpecimenWellgroup VARCHAR(100),
  ReplicateWellgroup VARCHAR(100),
  ReplicateNumber INT,
  Container ENTITYID NOT NULL,

  CONSTRAINT pk_welldata PRIMARY KEY (RowId),
  CONSTRAINT fk_welldata_experimentrun FOREIGN KEY (RunId)
    REFERENCES exp.experimentrun (RowId) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_welldata_specimenlsid FOREIGN KEY (SpecimenLsid)
    REFERENCES exp.material (Lsid)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_welldata_rundataid FOREIGN KEY (RunDataId)
    REFERENCES nab.nabspecimen (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_welldata_dilutiondataid FOREIGN KEY (DilutionDataId)
    REFERENCES nab.dilutiondata (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION

);

CREATE INDEX idx_welldata_runid ON nab.welldata(RunId);
