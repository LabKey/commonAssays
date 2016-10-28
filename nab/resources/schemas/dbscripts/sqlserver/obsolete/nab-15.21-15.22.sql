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
