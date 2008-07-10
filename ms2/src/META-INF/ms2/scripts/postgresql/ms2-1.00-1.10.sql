/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
-- All tables used for GO data
-- Data will change frequently, with updates from the GO consortium
-- See  
--      http://www.geneontology.org/GO.downloads.shtml
--
SET search_path TO prot,ms2,public;

-- GO Terms

CREATE TABLE GoTerm (
  id INTEGER PRIMARY KEY,
  name VARCHAR(255) NOT NULL DEFAULT '',
  termtype VARCHAR(55) NOT NULL DEFAULT '',
  acc VARCHAR(255) NOT NULL DEFAULT '',
  isobsolete INTEGER NOT NULL DEFAULT 0,
  isroot INTEGER NOT NULL DEFAULT 0
);
 
CREATE INDEX IX_GoTerm_Name ON GoTerm(name);
CREATE INDEX IX_GoTerm_TermType ON GoTerm(termtype);
CREATE UNIQUE INDEX UQ_GoTerm_Acc ON GoTerm(acc);

-- GO Term2Term 

CREATE TABLE GoTerm2Term (
  id INTEGER PRIMARY KEY NOT NULL,
  relationshipTypeId INTEGER NOT NULL DEFAULT 0,
  term1Id INTEGER NOT NULL DEFAULT 0,
  term2Id INTEGER NOT NULL DEFAULT 0,
  complete INTEGER NOT NULL DEFAULT 0
); 

CREATE INDEX IX_GoTerm2Term_term1Id ON GoTerm2Term(term1Id);
CREATE INDEX IX_GoTerm2Term_term2Id ON GoTerm2Term(term2Id);
CREATE INDEX IX_GoTerm2Term_term1_2_Id ON GoTerm2Term(term1Id,term2Id);
CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON GoTerm2Term(relationshipTypeId);
CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON GoTerm2Term(term1Id,term2Id,relationshipTypeId);

-- Graph path

CREATE TABLE GoGraphPath (
  id INTEGER PRIMARY KEY,
  term1Id INTEGER NOT NULL DEFAULT 0,
  term2Id INTEGER NOT NULL DEFAULT 0,
  distance INTEGER NOT NULL DEFAULT 0
);
 
CREATE INDEX IX_GoGraphPath_term1Id ON GoGraphPath(term1Id);
CREATE INDEX IX_GoGraphPath_term2Id ON GoGraphPath(term2Id);
CREATE INDEX IX_GoGraphPath_term1_2_Id ON GoGraphPath(term1Id,term2Id);
CREATE INDEX IX_GoGraphPath_t1_distance ON GoGraphPath(term1Id,distance);

-- Go term definitions

CREATE TABLE GoTermDefinition (
  termId INTEGER NOT NULL DEFAULT 0,
  termDefinition text NOT NULL,
  dbXrefId INTEGER NULL DEFAULT NULL,
  termComment text NULL DEFAULT NULL,
  reference VARCHAR(255) NULL DEFAULT NULL
);
 
CREATE INDEX IX_GoTermDefinition_dbXrefId ON GoTermDefinition(dbXrefId);
CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON GoTermDefinition(termId);

-- GO term synonyms

CREATE TABLE GoTermSynonym (
  termId INTEGER NOT NULL DEFAULT 0,
  termSynonym VARCHAR(255) NULL DEFAULT NULL,
  accSynonym VARCHAR(255) NULL DEFAULT NULL,
  synonymTypeId INTEGER NOT NULL DEFAULT 0
);


CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON GoTermSynonym(synonymTypeId);
CREATE INDEX IX_GoTermSynonym_TermId ON GoTermSynonym(termId);
CREATE INDEX IX_GoTermSynonym_termSynonym ON GoTermSynonym(termSynonym);
CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON GoTermSynonym(termId,termSynonym);

ALTER TABLE ms2.MS2Runs RENAME COLUMN ApplicationLSID TO ExperimentRunLSID;

SET search_path TO prot,ms2,public;

CREATE INDEX ix_protorganisms_genus ON prot.protorganisms(genus);
CREATE INDEX ix_protorganisms_species ON prot.protorganisms(species);
