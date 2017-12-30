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

CREATE TABLE nab.cutoffvalue
(
    rowid SERIAL NOT NULL,
    nabspecimenid INT NOT NULL,
    cutoff DOUBLE PRECISION,
    point DOUBLE PRECISION,
    pointoorindicator VARCHAR(20),
    
    ic_poly DOUBLE PRECISION,
    ic_polyoorindicator VARCHAR(20),
    ic_4pl DOUBLE PRECISION,
    ic_4ploorindicator VARCHAR(20),
    ic_5pl DOUBLE PRECISION,
    ic_5ploorindicator VARCHAR(20),

    CONSTRAINT pk_nab_cutoffvalue PRIMARY KEY (rowid)
);

CREATE TABLE nab.nabspecimen
(
    rowid SERIAL NOT NULL,
    dataid INT,
    runid INT NOT NULL,
    specimenlsid lsidtype NOT NULL,
    FitError DOUBLE PRECISION,
    WellgroupName VARCHAR(100),
    
    auc_poly DOUBLE PRECISION,
    positiveauc_poly DOUBLE PRECISION,
    auc_4pl DOUBLE PRECISION,
    positiveauc_4pl DOUBLE PRECISION,
    auc_5pl DOUBLE PRECISION,
    positiveauc_5pl DOUBLE PRECISION,

    -- For legacy migration purposes
    objecturi VARCHAR(300),
    objectid INT NOT NULL,
    protocolid INT,
    viruslsid lsidtype,
    
    CONSTRAINT pk_nab_specimen PRIMARY KEY (rowid),
    CONSTRAINT fk_nabspecimen_experimentrun FOREIGN KEY (runid)
      REFERENCES exp.experimentrun (rowid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_nabspecimen_specimenlsid FOREIGN KEY (specimenlsid)
      REFERENCES exp.material (lsid)
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX idx_nabspecimen_runid ON nab.nabspecimen(runid);
CREATE INDEX idx_nabspecimen_objectid ON nab.nabspecimen(objectid);
CREATE INDEX idx_nabspecimen_dataid ON nab.nabspecimen(dataid);

ALTER TABLE nab.cutoffvalue ADD CONSTRAINT fk_cutoffvalue_nabspecimen FOREIGN KEY (nabspecimenid)
        REFERENCES nab.nabspecimen (rowid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.nabspecimen ADD CONSTRAINT fk_nabspecimen_protocolid FOREIGN KEY (protocolid)
        REFERENCES exp.protocol (rowid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION;


CREATE INDEX IDX_NAbSpecimen_ProtocolId ON nab.NAbSpecimen(ProtocolId);
CREATE INDEX IDX_CutoffValue_NabSpecimenId ON nab.cutoffvalue USING btree (NabSpecimenId);

/* nab-14.20-14.30.sql */

CREATE SCHEMA nabvirus;

/* nab-15.10-15.11.sql */

CREATE INDEX IDX_NAbSpecimen_SpecimenLSID ON nab.NAbSpecimen(SpecimenLSID);

/* nab-15.20-15.21.sql */

CREATE TABLE nab.dilutiondata
(
    RowId SERIAL NOT NULL,
    Dilution DOUBLE PRECISION,
    DilutionOrder INT,
    PercentNeutralization DOUBLE PRECISION,
    NeutralizationPlusMinus DOUBLE PRECISION,
    Min DOUBLE PRECISION,
    Max DOUBLE PRECISION,
    Mean DOUBLE PRECISION,
    StdDev DOUBLE PRECISION,
    WellgroupName VARCHAR(100),
    ReplicateName VARCHAR(100),
    RunDataId INT,
    MinDilution DOUBLE PRECISION,
    MaxDilution DOUBLE PRECISION,
    PlateNumber INT,
    RunId INT,
    ProtocolId INT,
    Container ENTITYID NOT NULL,

    CONSTRAINT pk_dilutiondata PRIMARY KEY (RowId)

);

ALTER TABLE nab.dilutiondata ADD CONSTRAINT fk_dilutiondata_experimentrun FOREIGN KEY (RunId)
    REFERENCES exp.experimentrun (RowId) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.dilutiondata ADD CONSTRAINT fk_dilutiondata_rundataid FOREIGN KEY (RunDataId)
    REFERENCES nab.nabspecimen (RowId) ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE INDEX IDX_DilutionData_RunId ON nab.DilutionData(RunId);

CREATE TABLE nab.welldata
(
  RowId SERIAL NOT NULL,
  RunId INT NOT NULL,
  SpecimenLsid lsidtype,
  RunDataId INT,
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
  PlateNumber INT,
  PlateVirusName VARCHAR(100),

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
