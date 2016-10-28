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
