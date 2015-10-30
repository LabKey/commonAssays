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

/* nab-15.21-15.22.sql */

ALTER TABLE nab.welldata ALTER COLUMN SpecimenLsid DROP NOT NULL;
ALTER TABLE nab.welldata ALTER COLUMN RunDataId DROP NOT NULL;
ALTER TABLE nab.welldata ADD COLUMN PlateNumber INT;
ALTER TABLE nab.welldata ADD COLUMN PlateVirusName VARCHAR(100);

ALTER TABLE nab.dilutiondata ALTER COLUMN Dilution TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN PercentNeutralization TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN NeutralizationPlusMinus TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN Min TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN Max TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN Mean TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN StdDev TYPE DOUBLE PRECISION;

ALTER TABLE nab.dilutiondata ADD COLUMN WellgroupName VARCHAR(100);
ALTER TABLE nab.dilutiondata ADD COLUMN ReplicateName VARCHAR(100);
ALTER TABLE nab.dilutiondata ADD COLUMN RunDataId INT;
ALTER TABLE nab.dilutiondata ADD COLUMN MinDilution DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ADD COLUMN MaxDilution DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ADD COLUMN PlateNumber INT;
ALTER TABLE nab.dilutiondata ADD COLUMN RunId INT;
ALTER TABLE nab.dilutiondata ADD COLUMN ProtocolId INT;
ALTER TABLE nab.dilutiondata ADD COLUMN Container ENTITYID NOT NULL;

ALTER TABLE nab.dilutiondata ADD CONSTRAINT fk_dilutiondata_experimentrun FOREIGN KEY (RunId)
  REFERENCES exp.experimentrun (RowId) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.dilutiondata ADD CONSTRAINT fk_dilutiondata_rundataid FOREIGN KEY (RunDataId)
  REFERENCES nab.nabspecimen (RowId)
  ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE INDEX IDX_DilutionData_RunId ON nab.DilutionData(RunId);

SELECT core.executeJavaUpgradeCode('upgradeDilutionAssayWithNewTables');

/* nab-15.22-15.23.sql */

ALTER TABLE nab.NabSpecimen ALTER COLUMN FitError TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_poly TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_Poly TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_4pl TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_4pl TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_5pl TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_5pl TYPE DOUBLE PRECISION;

ALTER TABLE nab.CutoffValue ALTER COLUMN Cutoff TYPE DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN Point TYPE DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_Poly TYPE DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_4pl TYPE DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_5pl TYPE DOUBLE PRECISION;