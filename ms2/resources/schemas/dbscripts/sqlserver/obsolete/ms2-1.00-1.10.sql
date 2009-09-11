/*
 * Copyright (c) 2005-2008 Fred Hutchinson Cancer Research Center
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

-- GO Terms

IF OBJECT_ID('prot.GoTerm','U') IS NOT NULL
   DROP TABLE prot.GoTerm
GO

CREATE TABLE prot.GoTerm (
  id INTEGER PRIMARY KEY,
  name VARCHAR(255) NOT NULL DEFAULT '',
  termtype VARCHAR(55) NOT NULL DEFAULT '',
  acc VARCHAR(255) NOT NULL DEFAULT '',
  isobsolete INTEGER NOT NULL DEFAULT 0,
  isroot INTEGER NOT NULL DEFAULT 0
)
GO 
CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name)
CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype)
CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc)
GO

-- GO Term2Term 

IF OBJECT_ID('prot.GoTerm2Term','U') IS NOT NULL
   DROP TABLE prot.GoTerm2Term
GO

CREATE TABLE prot.GoTerm2Term (
  id INTEGER PRIMARY KEY NOT NULL,
  relationshipTypeId INTEGER NOT NULL DEFAULT 0,
  term1Id INTEGER NOT NULL DEFAULT 0,
  term2Id INTEGER NOT NULL DEFAULT 0,
  complete INTEGER NOT NULL DEFAULT 0
)
GO 

CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id)
CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id)
CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id,term2Id)
CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId)
CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId)
GO

-- Graph path

IF OBJECT_ID('prot.GoGraphPath','U') IS NOT NULL
   DROP TABLE prot.GoGraphPath
GO

CREATE TABLE prot.GoGraphPath (
  id INTEGER PRIMARY KEY,
  term1Id INTEGER NOT NULL DEFAULT 0,
  term2Id INTEGER NOT NULL DEFAULT 0,
  distance INTEGER NOT NULL DEFAULT 0
)

GO 
CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id)
CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id)
CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id)
CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance)
GO

-- Go term definitions

IF OBJECT_ID('prot.GoTermDefinition','U') IS NOT NULL
   DROP TABLE prot.GoTermDefinition
GO

CREATE TABLE prot.GoTermDefinition (
  termId INTEGER NOT NULL DEFAULT 0,
  termDefinition text NOT NULL,
  dbXrefId INTEGER NULL DEFAULT NULL,
  termComment text NULL DEFAULT NULL,
  reference VARCHAR(255) NULL DEFAULT NULL
)
GO 
CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId)
CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId)
GO

-- GO term synonyms


IF OBJECT_ID('prot.GOTermSynonym','U') IS NOT NULL
   DROP TABLE prot.GOTermSynonym
GO

CREATE TABLE prot.GoTermSynonym (
  termId INTEGER NOT NULL DEFAULT 0,
  termSynonym VARCHAR(255) NULL DEFAULT NULL,
  accSynonym VARCHAR(255) NULL DEFAULT NULL,
  synonymTypeId INTEGER NOT NULL DEFAULT 0
)

GO 
CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId)
CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId)
CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym)
CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym)
GO

EXEC sp_rename
    @objname = 'ms2.MS2Runs.ApplicationLSID',
    @newname = 'ExperimentRunLSID',
    @objtype = 'COLUMN'
GO

CREATE INDEX IX_ProtOrganisms_Genus ON prot.ProtOrganisms(Genus)
GO

CREATE INDEX IX_ProtOrganisms_Species ON prot.ProtOrganisms(Species)
GO
