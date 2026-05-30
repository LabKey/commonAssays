/*
 * Copyright (c) 2015-2026 LabKey Corporation
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

CREATE SCHEMA elispotlk;
CREATE SCHEMA elispotantigen;

CREATE TABLE elispotlk.rundata
(
    RowId SERIAL NOT NULL,
    RunId INT NOT NULL,
    SpecimenLsid lsidtype NOT NULL,
    AntigenLsid lsidtype,
    SpotCount REAL,
    WellgroupName VARCHAR(4000),
    WellgroupLocation VARCHAR(4000),
    NormalizedSpotCount REAL,
    AntigenWellgroupName VARCHAR(4000),
    Analyte VARCHAR(4000),
    Activity DOUBLE PRECISION,
    Intensity DOUBLE PRECISION,

    ObjectUri VARCHAR(300),
    ObjectId INT NOT NULL,

    CONSTRAINT pk_elispot_rundata PRIMARY KEY (RowId),
    CONSTRAINT fk_elispotrundata_experimentrun FOREIGN KEY (RunId)
      REFERENCES exp.experimentrun (RowId) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_elispotrundata_specimenlsid FOREIGN KEY (Specimenlsid)
      REFERENCES exp.material (Lsid)
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX idx_elispotrundata_runid ON elispotlk.rundata(RunId);

ALTER TABLE elispotlk.rundata ADD COLUMN Cytokine VARCHAR(4000);
ALTER TABLE elispotlk.rundata ADD COLUMN SpotSize REAL;
