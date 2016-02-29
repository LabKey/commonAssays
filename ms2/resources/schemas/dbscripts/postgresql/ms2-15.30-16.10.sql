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
/* ms2-15.30-15.31.sql */

ALTER TABLE ms2.Runs ADD COLUMN MascotFile VARCHAR(300) NULL;
ALTER TABLE ms2.Runs ADD COLUMN DistillerRawFile VARCHAR(500) NULL;


ALTER TABLE ms2.PeptidesData ADD COLUMN QueryNumber int NULL;
ALTER TABLE ms2.PeptidesData ADD COLUMN HitRank int NOT NULL DEFAULT 1;
ALTER TABLE ms2.PeptidesData ADD COLUMN Decoy boolean NOT NULL DEFAULT FALSE;

-- Issue 25276: MS2 script rollup for 16.1 should not create UQ_MS2PeptidesData_FractionScanCharge index twice
-- SELECT core.fn_dropifexists('PeptidesData','ms2', 'INDEX','UQ_MS2PeptidesData_FractionScanCharge');
-- CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge, HitRank, Decoy);

/* ms2-15.31-15.32.sql */

ALTER TABLE ms2.PeptidesData ALTER COLUMN PeptideProphet DROP NOT NULL;

/* ms2-15.33-15.34.sql */

ALTER TABLE ms2.peptidesdata
  ADD CONSTRAINT FK_ms2PeptidesData_ProtSequences FOREIGN KEY (seqid) REFERENCES prot.sequences (seqid);

/* ms2-15.34-15.35.sql */

-- This index should replace the one created in ms2-15.30-15.31.sql.  There is no need to create the older one first.
SELECT core.fn_dropifexists('PeptidesData','ms2', 'INDEX','UQ_MS2PeptidesData_FractionScanCharge');
CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge, HitRank, Decoy, QueryNumber);

/* ms2-15.35-15.36.sql */

-- Create a new set of properties for Mascot settings
INSERT INTO prop.propertysets (category, objectid, userid) SELECT 'MascotConfig' AS Category, EntityId, -1 AS UserId FROM core.containers WHERE parent IS NULL;

-- Migrate existing Mascot settings
UPDATE prop.properties SET "set" = (SELECT MAX("set") FROM prop.propertysets)
  WHERE name LIKE 'Mascot%' AND "set" = (SELECT "set" FROM prop.propertysets WHERE category = 'SiteConfig' AND userid = -1 AND objectid = (SELECT entityid FROM core.containers WHERE parent IS NULL));

/* ms2-15.36-15.37.sql */

CREATE TABLE ms2.FastaRunMapping (
  Run INT NOT NULL,
  FastaId INT NOT NULL,

  CONSTRAINT PK_FastaRunMapping PRIMARY KEY (Run, FastaId),
  CONSTRAINT FK_FastaRunMapping_Run FOREIGN KEY (Run) REFERENCES ms2.Runs (Run),
  CONSTRAINT FK_FastaRunMapping_FastaId FOREIGN KEY (FastaId) REFERENCES prot.FastaFiles (FastaId)
);

INSERT INTO ms2.FastaRunMapping( Run, FastaId ) SELECT Run, FastaId FROM ms2.Runs WHERE FastaId IN (SELECT FastaId FROM prot.FastaFiles);

CREATE INDEX IX_FastaRunMapping_FastaId ON ms2.FastaRunMapping(FastaId);

ALTER TABLE ms2.Runs DROP COLUMN FastaId;