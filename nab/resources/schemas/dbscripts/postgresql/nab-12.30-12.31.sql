/*
 * Copyright (c) 2010-2012 LabKey Corporation
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

CREATE SCHEMA nab;

CREATE TABLE nab.cutoffvalue
(
    rowid SERIAL NOT NULL,
    nabspecimenid INT NOT NULL,
    cutoff REAL,
    point REAL,
    pointoorindicator VARCHAR(20),
    
    ic_poly REAL,
    ic_polyoorindicator VARCHAR(20),
    ic_4pl REAL,
    ic_4ploorindicator VARCHAR(20),
    ic_5pl REAL,
    ic_5ploorindicator VARCHAR(20),

    CONSTRAINT pk_nab_cutoffvalue PRIMARY KEY (rowid)
);

CREATE TABLE nab.nabspecimen
(
    rowid SERIAL NOT NULL,
    dataid INT,
    runid INT NOT NULL,
    specimenlsid lsidtype NOT NULL,
    FitError REAL,
    WellgroupName VARCHAR(100),
    
    auc_poly REAL,
    positiveauc_poly REAL,
    auc_4pl REAL,
    positiveauc_4pl REAL,
    auc_5pl REAL,
    positiveauc_5pl REAL,

    -- For legacy migration purposes
    objecturi VARCHAR(300),
    objectid INT NOT NULL,
    protocolid INT,

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
