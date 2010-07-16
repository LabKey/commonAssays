/*
 * Copyright (c) 2009-2010 LabKey Corporation
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

/* ms2-0.00-2.30.sql */

/* ms2-0.00-2.00.sql */

/* ms2-0.00-1.00.sql */

-- All tables used for MS2 data

CREATE SCHEMA prot;

/****** ProtAnnotInsertions                                 */

CREATE TABLE prot.ProtAnnotInsertions
(
    InsertId SERIAL NOT NULL,
    FileName VARCHAR(200) NULL,
    FileType VARCHAR(50) NULL,
    Comment VARCHAR(200) NULL,
    InsertDate TIMESTAMP NOT NULL,
    ChangeDate TIMESTAMP NULL,
    Mouthsful INT NULL DEFAULT 0,
    RecordsProcessed INT NULL DEFAULT 0,
    CompletionDate TIMESTAMP NULL,
    SequencesAdded INT NULL DEFAULT 0,
    AnnotationsAdded INT NULL DEFAULT 0,
    IdentifiersAdded INT NULL DEFAULT 0,
    OrganismsAdded INT NULL DEFAULT 0,
    MRMSize INT NULL DEFAULT 0,
    MRMSequencesAdded INT NULL,
    MRMAnnotationsAdded INT NULL,
    MRMIdentifiersAdded INT NULL,
    MRMOrganismsAdded INT NULL,
    DefaultOrganism VARCHAR(100) NULL DEFAULT 'Unknown unknown',
    OrgShouldBeGuessed INT NULL DEFAULT 1,

    CONSTRAINT PK_ProtAnnotInsertions PRIMARY KEY (InsertId)
);

/****** ProtInfoSources                                     */
CREATE TABLE prot.ProtInfoSources
(
    SourceId SERIAL NOT NULL,
    Name VARCHAR(50) NOT NULL,
    CurrentVersion VARCHAR(50) NULL,
    CurrentVersionDate TIMESTAMP NULL,
    Url VARCHAR(1000) NULL ,
    ProcessToObtain BYTEA NULL,
    LastUpdate TIMESTAMP NULL,
    InsertDate TIMESTAMP NOT NULL,
    ModDate TIMESTAMP NULL ,
    Deleted INT NOT NULL DEFAULT 0,
 
    CONSTRAINT PK_ProtSeqSources PRIMARY KEY (SourceId)
);

/****** ProtAnnotationTypes                                 */  
CREATE TABLE prot.ProtAnnotationTypes
(
    AnnotTypeId SERIAL NOT NULL,
    Name VARCHAR(50) NOT NULL,
    SourceId INT NULL,
    Description VARCHAR(200) NULL,
    EntryDate TIMESTAMP NOT NULL,
    ModDate TIMESTAMP NULL,
    Deleted INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtAnnotationTypes PRIMARY KEY (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotationTypes_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.ProtInfoSources (SourceId)
);
CREATE UNIQUE INDEX UQ_ProtAnnotationTypes ON prot.ProtAnnotationTypes(Name);

/****** ProtIdentTypes                                      */
CREATE TABLE prot.ProtIdentTypes
(
    IdentTypeId SERIAL NOT NULL,
    Name VARCHAR(50) NOT NULL,
    CannonicalSourceId INT NULL,
    EntryDate TIMESTAMP NOT NULL,
    Description VARCHAR(200) NULL,
    Deleted INT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtIdentTypes PRIMARY KEY (IdentTypeId),
    CONSTRAINT FK_ProtIdentTypes_ProtInfoSources FOREIGN KEY (CannonicalSourceId) REFERENCES prot.ProtInfoSources (SourceId)
);
CREATE UNIQUE INDEX UQ_ProtIdentTypes ON prot.ProtIdentTypes(Name);


/****** ProtOrganisms                                       */
CREATE TABLE prot.ProtOrganisms
(
    OrgId SERIAL NOT NULL,
    CommonName VARCHAR(50) NULL,
    Genus VARCHAR(100) NOT NULL,
    Species VARCHAR(100) NOT NULL,
    Comments VARCHAR(200) NULL,
    IdentId INT NULL,
    Deleted INT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtOrganisms PRIMARY KEY (OrgId)
);
CREATE UNIQUE INDEX UQ_ProtOrganisms_Genus_Species ON prot.ProtOrganisms(Genus, Species);


/****** ProtSequences                                       */  
CREATE TABLE prot.ProtSequences
(
    SeqId SERIAL NOT NULL,
    ProtSequence TEXT NULL,
    Hash VARCHAR(100) NULL ,
    Description VARCHAR(200) NULL,
    SourceId INT NULL,
    SourceVersion VARCHAR(50) NULL,
    InsertDate TIMESTAMP NOT NULL,
    ChangeDate TIMESTAMP NULL,
    SourceChangeDate TIMESTAMP NULL,
    SourceInsertDate TIMESTAMP NULL,
    OrgId INT NULL,
    Mass FLOAT NULL,
    BestName VARCHAR(50) NULL,
    BestGeneName VARCHAR(50) NULL,
    Length INT NULL,
    Deleted INT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtSequences PRIMARY KEY (SeqId),
    CONSTRAINT FK_ProtSequences_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.ProtInfoSources (SourceId),
    CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES prot.ProtOrganisms (OrgId)
);

CREATE INDEX IX_ProtSequences_OrgId ON prot.ProtSequences(OrgId);
CREATE INDEX IX_ProtSequences_Hash ON prot.ProtSequences(Hash);
CREATE INDEX IX_ProtSequences_BestGeneName ON prot.ProtSequences(BestGeneName);
CREATE UNIQUE INDEX UQ_ProtSequences_Hash_OrgId ON prot.ProtSequences(Hash, OrgId);


/****** ProtIdentifiers                                     */ 
CREATE TABLE prot.ProtIdentifiers
(
    IdentId SERIAL NOT NULL,
    IdentTypeId INT NOT NULL,
    Identifier VARCHAR(50) NOT NULL,
    SeqId INT NULL,
    SourceId INT NULL,
    EntryDate TIMESTAMP NOT NULL,
    SourceVersion VARCHAR(50) NULL,
    Deleted INT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtIdentifiers PRIMARY KEY (IdentId),
    CONSTRAINT FK_ProtIdentifiers_ProtIdentTypes FOREIGN KEY (IdentTypeId) REFERENCES prot.ProtIdentTypes (IdentTypeId),
    CONSTRAINT FK_ProtIdentifiers_ProtSeqSources FOREIGN KEY (SourceId)    REFERENCES prot.ProtInfoSources (SourceId),
    CONSTRAINT FK_ProtIdentifiers_ProtSequences  FOREIGN KEY (SeqId)       REFERENCES prot.ProtSequences (SeqId)
);
CREATE INDEX IX_ProtIdentifiers_Identifier ON prot.ProtIdentifiers(Identifier);
CREATE UNIQUE INDEX UQ_ProtIdentifiers_IdentTypeId_Identifier_SeqId ON prot.ProtIdentifiers(IdentTypeId, Identifier, SeqId);
CREATE INDEX IX_ProtIdentifiers_IdentTypeId_Identifier_IdentId_SeqId ON prot.ProtIdentifiers(IdentTypeId, Identifier, IdentId, SeqId); 

ALTER TABLE prot.ProtOrganisms ADD CONSTRAINT FK_ProtOrganisms_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES prot.ProtIdentifiers (IdentId);


/****** ProtAnnotations                                     */
CREATE TABLE prot.ProtAnnotations
(
    AnnotId SERIAL NOT NULL,
    AnnotTypeId INT NOT NULL,
    AnnotVal VARCHAR(200) NULL,
    AnnotIdent INT NULL,
    SeqId INT NULL,
    AnnotSourceId INT NULL,
    AnnotSourceVersion VARCHAR(50) NULL,
    InsertDate TIMESTAMP NOT NULL,
    ModDate TIMESTAMP NULL,
    StartPos INT NULL,
    EndPos INT NULL,
    Deleted INT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtAnnotations PRIMARY KEY (AnnotId),
    CONSTRAINT FK_ProtAnnotations_ProtAnnotationTypes FOREIGN KEY (AnnotTypeId) REFERENCES prot.ProtAnnotationTypes (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotations_ProtIdentifiers FOREIGN KEY (AnnotIdent) REFERENCES prot.ProtIdentifiers (IdentId),
    CONSTRAINT FK_ProtAnnotations_ProtSeqSources FOREIGN KEY (AnnotSourceId) REFERENCES prot.ProtInfoSources (SourceId),
    CONSTRAINT FK_ProtAnnotations_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.ProtSequences (SeqId)
);
CREATE INDEX IX_ProtAnnotations_SeqId_AnnotTypeId ON prot.ProtAnnotations(SeqId, AnnotTypeId);
CREATE UNIQUE INDEX UQ_ProtAnnotations_AnnotTypeId_AnnotVal_SeqId_StartPos_EndPos ON prot.ProtAnnotations(AnnotTypeId, AnnotVal, SeqId, StartPos, EndPos);


/****** ProtFastas                                          */
CREATE TABLE prot.ProtFastas
(
    FastaId SERIAL NOT NULL,
    FileName VARCHAR(200) NOT NULL,
    FileChecksum VARCHAR(50) NULL,
    Comment VARCHAR(200) NULL,
    InsertDate TIMESTAMP NOT NULL,
    DbName VARCHAR(100) NULL,
    DbVersion VARCHAR(100) NULL,
    DbSource INT NULL,
    DbDate TIMESTAMP NULL,
    Reference VARCHAR(200) NULL,
    NSequences INT NULL,
    Sequences TEXT NULL,
 
    CONSTRAINT FK_ProtFastas_ProtSeqSources FOREIGN KEY (DbSource) REFERENCES prot.ProtInfoSources (SourceId),
    CONSTRAINT PK_ProtFastas PRIMARY KEY (FastaId)
);

/****** ProtSprotOrgMap                                  */
CREATE TABLE prot.ProtSprotOrgMap
(
    SprotSuffix VARCHAR(5) NOT NULL,
    SuperKingdomCode CHAR(1) NULL,
    TaxonId INT NULL,
    FullName VARCHAR(200) NOT NULL,
    Genus VARCHAR(100) NOT NULL,
    Species VARCHAR(100) NOT NULL,
    CommonName VARCHAR(200) NULL,
    Synonym VARCHAR(200) NULL,

    CONSTRAINT PK_ProtSprotOrgMap PRIMARY KEY (SprotSuffix)
);

/*** Initializations */

INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('Genbank','http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=protein&cmd=search&term={}','2005-03-04 12:08:10');
INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('NiceProt','http://au.expasy.org/cgi-bin/niceprot.pl?{}','2005-03-04 12:08:10');
INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('GeneCards','http://bioinfo.weizmann.ac.il/cards-bin/carddisp?_symbol={}','2005-03-04 12:08:10');
INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('NCBI Taxonomy','http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id={}','2005-03-04 12:08:10');
INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('GO','http://www.godatabase.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}','2005-03-04 12:08:52');

INSERT INTO prot.ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_F',5,'2005-03-04 11:37:15');
INSERT INTO prot.ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_P',5,'2005-03-04 11:37:15');
INSERT INTO prot.ProtAnnotationTypes (Name,EntryDate) VALUES ('keyword','2005-03-04 11:37:15');
INSERT INTO prot.ProtAnnotationTypes (Name,EntryDate) VALUES ('feature','2005-03-04 11:37:15');
INSERT INTO prot.ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_C',5,'2005-03-04 11:38:13');
INSERT INTO prot.ProtAnnotationTypes (Name,EntryDate) VALUES ('FullOrganismName',now());
INSERT INTO prot.ProtAnnotationTypes (Name,EntryDate) VALUES ('LookupString',now());

INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('Genbank',1,'2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('SwissProt',2,'2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GeneName',3,'2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('NCBI Taxonomy',4,'2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('EMBL','2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('IntAct','2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Ensembl','2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('FlyBase','2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GO',5,'2005-03-04 11:37:14');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('InterPro','2005-03-04 11:37:15');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Pfam','2005-03-04 11:37:15');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PIR','2005-03-04 11:37:15');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Uniprot_keyword','2005-03-04 11:37:15');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SMART','2005-03-04 11:37:16');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('HSSP','2005-03-04 11:37:17');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ProDom','2005-03-04 11:37:17');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PROSITE','2005-03-04 11:37:17');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PRINTS','2005-03-04 11:37:19');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('TIGRFAMs','2005-03-04 11:37:22');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('EC','2005-03-04 11:37:22');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MaizeDB','2005-03-04 11:37:33');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('TRANSFAC','2005-03-04 11:37:34');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('WormBase','2005-03-04 11:37:38');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('WormPep','2005-03-04 11:37:39');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('COMPLUYEAST-2DPAGE','2005-03-04 11:37:39');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('DictyBase','2005-03-04 11:37:40');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Gramene','2005-03-04 11:37:45');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('OGP','2005-03-04 11:38:02');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Genew','2005-03-04 11:38:02');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('H-InvDB','2005-03-04 11:38:02');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MIM','2005-03-04 11:38:02');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MGD','2005-03-04 11:38:04');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('RGD','2005-03-04 11:38:06');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PDB','2005-03-04 11:38:10');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SWISS-2DPAGE','2005-03-04 11:38:33');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Aarhus/Ghent-2DPAGE','2005-03-04 11:38:33');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PMMA-2DPAGE','2005-03-04 11:38:45');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('TIGR','2005-03-04 11:38:49');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('TubercuList','2005-03-04 11:38:50');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Leproma','2005-03-04 11:39:05');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('GeneFarm','2005-03-04 11:39:35');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('GermOnline','2005-03-04 11:43:54');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SGD','2005-03-04 11:43:54');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('GeneDB_SPombe','2005-03-04 11:44:15');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PIRSF','2005-03-04 11:45:42');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('HAMAP','2005-03-04 11:46:49');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Reactome','2005-03-04 11:46:52');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ECO2DBASE','2005-03-04 11:46:55');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('EchoBASE','2005-03-04 11:46:55');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('EcoGene','2005-03-04 11:46:55');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SubtiList','2005-03-04 11:46:58');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ListiList','2005-03-04 11:47:14');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('GlycoSuiteDB','2005-03-04 11:47:44');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('StyGene','2005-03-04 11:51:59');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PHCI-2DPAGE','2005-03-04 11:52:19');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Siena-2DPAGE','2005-03-04 11:55:22');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('HSC-2DPAGE','2005-03-04 11:55:41');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MEROPS','2005-03-04 11:59:32');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('AGD','2005-03-04 12:14:40');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PhotoList','2005-03-04 12:15:22');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ZFIN','2005-03-04 12:15:39');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MypuList','2005-03-04 12:24:15');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SagaList','2005-03-04 12:25:40');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ANU-2DPAGE','2005-03-04 12:29:22');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Rat-heart-2DPAGE','2005-03-04 12:30:51');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PhosSite','2005-03-04 12:49:00');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('REBASE','2005-03-04 13:25:29');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Maize-2DPAGE','2005-03-04 15:10:53');
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('HIV','2005-03-04 22:13:40');

INSERT INTO prot.ProtOrganisms (CommonName,Genus,Species,Comments) VALUES ('Unknown organism','Unknown','unknown','Organism is unknown');

CREATE TABLE prot.ProteinDataBases
(
    DataBaseId SERIAL,
    ProteinDataBase VARCHAR(400),
    Loaded TIMESTAMP,

    CONSTRAINT PK_ProteinDataBases PRIMARY KEY (DataBaseId)
);


-- Special entry 0 for runs that contain no protein database
INSERT INTO prot.ProteinDataBases (DataBaseId, ProteinDataBase, Loaded) VALUES (0, NULL, NULL);


CREATE SCHEMA ms2;

/**** MS2Runs                                           */
CREATE TABLE ms2.MS2Runs
(
    _ts TIMESTAMP DEFAULT now(),
    Run SERIAL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Owner USERID NULL,

    Container ENTITYID NOT NULL,
    EntityId ENTITYID NOT NULL,
    Description VARCHAR(300),
    Path VARCHAR(500),
    FileName VARCHAR(300),
    Status VARCHAR(200),
    StatusId INT NOT NULL DEFAULT 0,
    Type VARCHAR(30),
    SearchEngine VARCHAR(20),
    MassSpecType VARCHAR(200),
    DataBaseId INT NOT NULL DEFAULT 0,
    SampleEnzyme VARCHAR(50),
    Deleted BOOLEAN NOT NULL DEFAULT '0',
    ApplicationLSID LSIDType NULL,
    
    CONSTRAINT PK_MS2Runs PRIMARY KEY (Run)
);


CREATE TABLE ms2.MS2Fractions
(
    Fraction SERIAL,
    Run INT NOT NULL,
    Description VARCHAR(300),
    FileName VARCHAR(300),
    HydroB0 REAL,
    HydroB1 REAL,
    HydroR2 REAL,
    HydroSigma REAL,

    CONSTRAINT PK_MS2Fractions PRIMARY KEY (Fraction)
);


CREATE TABLE ms2.MS2Modifications
(
    Run INT NOT NULL,
    AminoAcid VARCHAR (1) NOT NULL,
    MassDiff REAL NOT NULL,
    Variable BOOLEAN NOT NULL,
    Symbol VARCHAR (1) NOT NULL,

    CONSTRAINT PK_MS2Modifications PRIMARY KEY (Run, AminoAcid, Symbol)
);


-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE ms2.MS2SpectraData
(
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Spectrum BYTEA NOT NULL,

    CONSTRAINT PK_MS2SpectraData PRIMARY KEY (Fraction, Scan)
);


CREATE TABLE ms2.MS2History
(
    Date TIMESTAMP,
    Runs BIGINT,
    Peptides BIGINT,

    CONSTRAINT PK_MS2History PRIMARY KEY (Date)
);


-- ProteinDataBases with number of runs

/* ms2-1.00-1.10.sql */

-- All tables used for GO data
-- Data will change frequently, with updates from the GO consortium
-- See
--      http://www.geneontology.org/GO.downloads.shtml
--

-- GO Terms

CREATE TABLE prot.GoTerm
(
    id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL DEFAULT '',
    termtype VARCHAR(55) NOT NULL DEFAULT '',
    acc VARCHAR(255) NOT NULL DEFAULT '',
    isobsolete INTEGER NOT NULL DEFAULT 0,
    isroot INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT goterm_pkey PRIMARY KEY(id)
);
 
CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name);
CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype);
CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc);

-- GO Term2Term 

CREATE TABLE prot.GoTerm2Term
(
    id INTEGER NOT NULL,
    relationshipTypeId INTEGER NOT NULL DEFAULT 0,
    term1Id INTEGER NOT NULL DEFAULT 0,
    term2Id INTEGER NOT NULL DEFAULT 0,
    complete INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT goterm2term_pkey PRIMARY KEY(id)
); 

CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id);
CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id);
CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id,term2Id);
CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId);
CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId);

-- Graph path

CREATE TABLE prot.GoGraphPath
(
    id INTEGER NOT NULL,
    term1Id INTEGER NOT NULL DEFAULT 0,
    term2Id INTEGER NOT NULL DEFAULT 0,
    distance INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT gographpath_pkey PRIMARY KEY(id)
);
 
CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id);
CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id);
CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id);
CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance);

-- Go term definitions

CREATE TABLE prot.GoTermDefinition
(
    termId INTEGER NOT NULL DEFAULT 0,
    termDefinition text NOT NULL,
    dbXrefId INTEGER NULL DEFAULT NULL,
    termComment text NULL DEFAULT NULL,
    reference VARCHAR(255) NULL DEFAULT NULL
);
 
CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId);
CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId);

-- GO term synonyms

CREATE TABLE prot.GoTermSynonym
(
    termId INTEGER NOT NULL DEFAULT 0,
    termSynonym VARCHAR(255) NULL DEFAULT NULL,
    accSynonym VARCHAR(255) NULL DEFAULT NULL,
    synonymTypeId INTEGER NOT NULL DEFAULT 0
);


CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId);
CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId);
CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym);
CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym);

ALTER TABLE ms2.MS2Runs RENAME COLUMN ApplicationLSID TO ExperimentRunLSID;

CREATE INDEX ix_protorganisms_genus ON prot.protorganisms(genus);
CREATE INDEX ix_protorganisms_species ON prot.protorganisms(species);

/* ms2-1.10-1.20.sql */

-- Eliminate unique requirement on Date column, which is unnecessary and causes constraint violations
ALTER TABLE ms2.MS2History DROP CONSTRAINT PK_MS2History;
CREATE INDEX IX_MS2History ON ms2.MS2History (Date);

-- Move SeqId into MS2PeptidesData to eliminate inefficient joins and add RunDescription column
ALTER TABLE ms2.MS2Fractions
    ADD COLUMN PepXmlDataLSID LSIDType NULL,
    ADD COLUMN MzXmlURL VARCHAR(400) NULL;

-- Add a column for the hash of the file

ALTER TABLE prot.proteindatabases
    ADD COLUMN FileChecksum varchar(50) NULL;

/* ms2-1.20-1.30.sql */

-- add most common ncbi Taxonomy id's

CREATE TEMPORARY TABLE idents
(
    Identifier varchar(50) NOT NULL,
    CommonName varchar(20) NULL,
    Genus varchar(100) NOT NULL,
    Species varchar(100) NOT NULL,
    OrgId int NULL,
    IdentId int NULL,
    IdentTypeId int NULL
);

INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('chicken', 'Gallus', 'gallus', '9031');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('chimp', 'Pan', 'troglodytes', '9598');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('cow', 'Bos', 'taurus', '9913');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('dog', 'Canis', 'familiaris', '9615');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('ecoli', 'Escherichia', 'coli', '562');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('fruit fly', 'Drosophila', 'melanogaster', '7227');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('horse', 'Equus', 'caballus', '9796');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('human', 'Homo', 'sapiens', '9606');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('mouse', 'Mus', 'musculus', '10090');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('nematode', 'Caenorhabditis', 'elegans', '6239');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('pig', 'Sus', 'scrofa', '9823');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('rat', 'Rattus', 'norvegicus', '10116');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('yeast', 'Saccharomyces', 'cerevisiae', '4932');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
    VALUES ('zebrafish', 'Danio', 'rerio', '7955');

UPDATE idents
    SET IdentTypeId = (SELECT identtypeid FROM prot.ProtIdentTypes WHERE name='NCBI Taxonomy');

INSERT INTO prot.ProtOrganisms (CommonName, Genus, Species)
    SELECT CommonName, Genus, Species FROM idents
        WHERE NOT EXISTS
            (SELECT * FROM prot.ProtOrganisms PO INNER JOIN idents i ON (PO.Genus = i.Genus AND PO.Species = i.Species));

INSERT INTO prot.ProtIdentifiers (Identifier, IdentTypeId, entrydate)
    SELECT Identifier, IdentTypeId , now() FROM idents
    WHERE NOT EXISTS
        (SELECT * FROM prot.ProtIdentifiers PI INNER JOIN idents i ON (PI.Identifier = i.Identifier AND PI.IdentTypeId = i.IdentTypeId));

UPDATE idents
    SET OrgId = PO.OrgId
    FROM prot.ProtOrganisms PO
    WHERE idents.Genus = PO.Genus AND idents.Species = PO.Species;

UPDATE idents
    SET IdentId = PI.IdentId
    FROM prot.ProtIdentifiers PI
    WHERE idents.Identifier = PI.Identifier AND idents.IdentTypeId = PI.IdentTypeId;

UPDATE prot.ProtOrganisms
    SET IdentId = i.IdentID
    FROM idents i
    WHERE i.OrgId = ProtOrganisms.OrgId;

DROP TABLE idents;


CREATE TABLE ms2.MS2PeptidesData
(
    RowId BIGSERIAL NOT NULL,
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Charge SMALLINT NOT NULL,
    Score1 REAL NOT NULL DEFAULT 0,
    Score2 REAL NOT NULL DEFAULT 0,
    Score3 REAL NOT NULL DEFAULT 0,
    Score4 REAL NULL,
    Score5 REAL NULL,
    IonPercent REAL NOT NULL,
    Mass FLOAT8 NOT NULL,
    DeltaMass REAL NOT NULL,
    PeptideProphet REAL NOT NULL,
    Peptide VARCHAR (200) NOT NULL,
    PrevAA CHAR(1) NOT NULL DEFAULT '',
    TrimmedPeptide VARCHAR(200) NOT NULL DEFAULT '',
    NextAA CHAR(1) NOT NULL DEFAULT '',
    ProteinHits SMALLINT NOT NULL,
    SequencePosition INT NOT NULL DEFAULT 0,
    Protein VARCHAR(100) NOT NULL,
    SeqId INT NULL
);


CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.MS2PeptidesData (Fraction, Scan, Charge);

ALTER TABLE ms2.MS2PeptidesData
    ADD CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (RowId);

CREATE INDEX IX_MS2PeptidesData_Protein ON ms2.MS2PeptidesData (Protein);


CREATE TABLE ms2.MS2ProteinProphetFiles
(
    RowId SERIAL NOT NULL,
    FilePath VARCHAR(255) NOT NULL,
    Run INT NOT NULL,
    Container EntityId NOT NULL,
    UploadCompleted BOOLEAN NOT NULL DEFAULT '0',
    MinProbSeries BYTEA NULL,
    SensitivitySeries BYTEA NULL,
    ErrorSeries BYTEA NULL,
    PredictedNumberCorrectSeries BYTEA NULL,
    PredictedNumberIncorrectSeries BYTEA NULL,


    CONSTRAINT PK_MS2ProteinProphetFiles PRIMARY KEY (RowId),
    CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs(Run),
    CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run)
);

CREATE TABLE ms2.MS2ProteinGroups
(
    RowId SERIAL NOT NULL,
    GroupProbability REAL NOT NULL,
    ProteinProphetFileId INT NOT NULL,
    GroupNumber INT NOT NULL,
    IndistinguishableCollectionId INT NOT NULL,
    UniquePeptidesCount INT NOT NULL,
    TotalNumberPeptides INT NOT NULL,
    PctSpectrumIds REAL NOT NULL,
    PercentCoverage REAL NOT NULL,
    ProteinProbability REAL NOT NULL DEFAULT 0,

    CONSTRAINT PK_MS2ProteinGroups PRIMARY KEY (RowId),
    CONSTRAINT UQ_MS2ProteinGroups UNIQUE (GroupNumber, ProteinProphetFileId, IndistinguishableCollectionId),

    CONSTRAINT FK_MS2ProteinGroup_MS2ProteinProphetFileId FOREIGN KEY (ProteinProphetFileId) REFERENCES ms2.MS2ProteinProphetFiles(RowId)
);

CREATE TABLE ms2.MS2ProteinGroupMemberships
(
    ProteinGroupId INT NOT NULL,
    SeqId INT NOT NULL,
    Probability REAL NOT NULL,

    CONSTRAINT PK_MS2ProteinGroupMemberships PRIMARY KEY (ProteinGroupId, SeqId),
    CONSTRAINT FK_MS2ProteinGroupMemberships_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.ProtSequences (SeqId),
    CONSTRAINT FK_MS2ProteinGroupMembership_MS2ProteinGroups FOREIGN KEY (ProteinGroupId) REFERENCES ms2.MS2ProteinGroups (RowId)
);

CREATE TABLE ms2.ms2peptidememberships
(
    PeptideId int8 NOT NULL,
    ProteinGroupId int4 NOT NULL,
    NSPAdjustedProbability float4 NOT NULL,
    Weight float4 NOT NULL,
    NondegenerateEvidence bool NOT NULL,
    EnzymaticTermini int4 NOT NULL,
    SiblingPeptides float4 NOT NULL,
    SiblingPeptidesBin int4 NOT NULL,
    Instances int4 NOT NULL,
    ContributingEvidence bool NOT NULL,
    CalcNeutralPepMass float4 NOT NULL,

    CONSTRAINT pk_ms2peptidememberships PRIMARY KEY (proteingroupid, peptideid),
    CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid),
    CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid)
);

CREATE TABLE ms2.Quantitation
(
    PeptideId BIGINT NOT NULL,
    LightFirstscan INT NOT NULL,
    LightLastscan INT NOT NULL,
    LightMass REAL NOT NULL,
    HeavyFirstscan INT NOT NULL,
    HeavyLastscan INT NOT NULL,
    HeavyMass REAL NOT NULL,
    Ratio VARCHAR(20) NOT NULL,
    Heavy2lightRatio VARCHAR(20) NOT NULL,
    LightArea REAL NOT NULL,
    HeavyArea REAL NOT NULL,
    DecimalRatio REAL NOT NULL,

    CONSTRAINT PK_Quantitation PRIMARY KEY (PeptideId),
    CONSTRAINT FK_Quantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.MS2PeptidesData(RowId)
);

CREATE TABLE ms2.ProteinQuantitation
(
    ProteinGroupId INT NOT NULL,
    RatioMean REAL NOT NULL,
    RatioStandardDev REAL NOT NULL,
    RatioNumberPeptides INT NOT NULL,
    Heavy2LightRatioMean REAL NOT NULL,
    Heavy2LightRatioStandardDev REAL NOT NULL,
    CONSTRAINT PK_ProteinQuantitation PRIMARY KEY (ProteinGroupId),
    CONSTRAINT FK_ProteinQuantitation_ProteinGroup FOREIGN KEY (ProteinGroupId) REFERENCES ms2.MS2ProteinGroups (RowId)
);

ALTER TABLE ms2.MS2Runs
    ADD COLUMN HasPeptideProphet BOOLEAN NOT NULL DEFAULT '0';

CREATE TABLE ms2.PeptideProphetSummaries
(
    Run INT NOT NULL,
    FValSeries BYTEA NULL,
    ObsSeries1 BYTEA NULL,
    ObsSeries2 BYTEA NULL,
    ObsSeries3 BYTEA NULL,
    ModelPosSeries1 BYTEA NULL,
    ModelPosSeries2 BYTEA NULL,
    ModelPosSeries3 BYTEA NULL,
    ModelNegSeries1 BYTEA NULL,
    ModelNegSeries2 BYTEA NULL,
    ModelNegSeries3 BYTEA NULL,
    MinProbSeries BYTEA NULL,
    SensitivitySeries BYTEA NULL,
    ErrorSeries BYTEA NULL,

    CONSTRAINT PK_PeptideProphetSummmaries PRIMARY KEY (Run)
);

-- Update GeneCards URL
UPDATE prot.ProtInfoSources SET Url = 'http://www.genecards.org/cgi-bin/carddisp?{}&alias=yes'
    WHERE Name = 'GeneCards';


/* ms2-1.30-1.40.sql */

-- Store peptide and spectrum counts with each run to make computing stats much faster
ALTER TABLE ms2.MS2Runs
    ADD COLUMN PeptideCount INT NOT NULL DEFAULT 0,
    ADD COLUMN SpectrumCount INT NOT NULL DEFAULT 0;

-- Update counts for existing runs
UPDATE ms2.MS2Runs SET PeptideCount = PepCount FROM
    (SELECT Run, COUNT(*) AS PepCount FROM ms2.MS2PeptidesData pd INNER JOIN ms2.MS2Fractions f ON pd.Fraction = f.Fraction GROUP BY Run) x
WHERE MS2Runs.Run = x.Run;

UPDATE ms2.MS2Runs SET SpectrumCount = SpecCount FROM
    (SELECT Run, COUNT(*) AS SpecCount FROM ms2.MS2SpectraData sd INNER JOIN ms2.MS2Fractions f ON sd.Fraction = f.Fraction GROUP BY Run) x
WHERE MS2Runs.Run = x.Run;

-- Relax contraints on quantitation result columns; q3 does not generate string representations of ratios.
ALTER TABLE ms2.Quantitation ALTER COLUMN Ratio DROP NOT NULL;
ALTER TABLE ms2.Quantitation ALTER COLUMN Heavy2lightRatio DROP NOT NULL;

-- Add a quantitation summary table
CREATE TABLE ms2.QuantSummaries
(
    QuantId SERIAL NOT NULL,
    Run INTEGER NOT NULL,
    AnalysisType VARCHAR(20) NOT NULL,
    AnalysisTime TIMESTAMP NULL,
    Version VARCHAR(80) NULL,
    LabeledResidues VARCHAR(20) NULL,
    MassDiff VARCHAR(80) NULL,
    MassTol REAL NOT NULL,
    SameScanRange CHAR(1) NULL,
    XpressLight INT NULL,

    CONSTRAINT PK_QuantSummaries PRIMARY KEY (QuantId),
    CONSTRAINT FK_QuantSummaries_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs (Run),
    -- Current restrictions allow only one quantitation analysis per run
    CONSTRAINT UQ_QuantSummaries UNIQUE (Run)
);

-- Add a QuantId column to ms2.Quantitation to allow multiple results for each peptide
ALTER TABLE ms2.Quantitation ADD QuantId INT;

-- QuantId must be non-null; eventually (PeptideId, QuantId) should become a compound PK
ALTER TABLE ms2.Quantitation ALTER COLUMN QuantId SET NOT NULL;

/* ms2-1.40-1.50.sql */

-- Store peptide and spectrum counts with each run to make computing stats much faster
ALTER TABLE ms2.MS2Runs
    ADD COLUMN NegativeHitCount INT NOT NULL DEFAULT 0;

CREATE TABLE ms2.PeptideProphetData
(
    PeptideId BIGINT NOT NULL,
    ProphetFVal REAL NOT NULL,
    ProphetDeltaMass REAL NULL,
    ProphetNumTrypticTerm INT NULL,
    ProphetNumMissedCleav INT NULL,

    CONSTRAINT PK_PeptideProphetData PRIMARY KEY (PeptideId),
    CONSTRAINT FK_PeptideProphetData_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.MS2PeptidesData(RowId)
);

ALTER TABLE prot.ProteinDataBases
    ADD ScoringAnalysis boolean NOT NULL DEFAULT FALSE;

/* ms2-1.50-1.60.sql */

-- Simplify protein table names
ALTER TABLE prot.ProtAnnotations RENAME TO Annotations;
ALTER TABLE prot.ProtAnnotationTypes RENAME TO AnnotationTypes;
ALTER TABLE prot.ProtAnnotInsertions RENAME TO AnnotInsertions;
ALTER TABLE prot.ProteinDatabases RENAME TO FastaFiles;
ALTER TABLE prot.ProtFastas RENAME TO FastaLoads;
ALTER TABLE prot.ProtIdentifiers RENAME TO Identifiers;
ALTER TABLE prot.ProtIdentTypes RENAME TO IdentTypes;
ALTER TABLE prot.ProtInfoSources RENAME TO InfoSources;
ALTER TABLE prot.ProtOrganisms RENAME TO Organisms;
ALTER TABLE prot.ProtSequences RENAME TO Sequences;
ALTER TABLE prot.ProtSProtOrgMap RENAME TO SProtOrgMap;

-- Rename some columns
ALTER TABLE prot.FastaFiles RENAME DataBaseId TO FastaId;
ALTER TABLE prot.FastaFiles RENAME ProteinDataBase TO FileName;
ALTER TABLE ms2.MS2Runs RENAME DataBaseId TO FastaId;

-- Add column for retention time
ALTER TABLE ms2.MS2PeptidesData
    ADD COLUMN RetentionTime REAL NULL;

/* ms2-1.60-1.70.sql */

UPDATE ms2.ms2proteingroups SET pctspectrumids = pctspectrumids / 100, percentcoverage = percentcoverage / 100;

-- Index to speed up deletes from MS2PeptidesData
CREATE INDEX IX_MS2PeptideMemberships_PeptideId ON ms2.MS2PeptideMemberships(PeptideId);

-- Simplify MS2 table names
ALTER TABLE ms2.MS2Fractions RENAME TO Fractions;
ALTER TABLE ms2.MS2History RENAME TO History;
ALTER TABLE ms2.MS2Modifications RENAME TO Modifications;
ALTER TABLE ms2.MS2PeptideMemberships RENAME TO PeptideMemberships;
ALTER TABLE ms2.MS2PeptidesData RENAME TO PeptidesData;
ALTER TABLE ms2.MS2ProteinGroupMemberships RENAME TO ProteinGroupMemberships;
ALTER TABLE ms2.MS2ProteinGroups RENAME TO ProteinGroups;
ALTER TABLE ms2.MS2ProteinProphetFiles RENAME TO ProteinProphetFiles;
ALTER TABLE ms2.MS2Runs RENAME TO Runs;
ALTER TABLE ms2.MS2SpectraData RENAME TO SpectraData;

-- More accurate column name
ALTER TABLE ms2.Runs RENAME COLUMN SampleEnzyme TO SearchEnzyme;

CREATE TABLE prot.FastaSequences
(
    FastaId int NOT NULL,
    LookupString varchar (200) NOT NULL,
    SeqId int NULL
);

ALTER TABLE prot.FastaSequences ADD CONSTRAINT PK_FastaSequences PRIMARY KEY (FastaId,LookupString);
ALTER TABLE prot.FastaSequences ADD CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId);
CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId);
CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId);

--Bug 2193
CREATE INDEX IX_SequencesSource ON prot.Sequences(SourceId);
-- different name on pgsql
DROP INDEX prot.ix_protsequences_hash;

/* ms2-1.70-2.00.sql */

SELECT core.fn_dropifexists('ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId');
CREATE INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId);

SELECT core.fn_dropifexists('ProteinGroups', 'ms2', 'INDEX', 'IX_ProteinGroups_ProteinProphetFileId');
CREATE INDEX IX_ProteinGroups_ProteinProphetFileId ON ms2.ProteinGroups(ProteinProphetFileId);

SELECT core.fn_dropifexists('Fractions', 'ms2', 'INDEX', 'IX_Fractions_Run_Fraction');
CREATE INDEX IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction);

ALTER TABLE ms2.ProteinGroups
    ADD COLUMN ErrorRate REAL NULL;

ALTER TABLE ms2.PeptidesData
    ADD COLUMN PeptideProphetErrorRate REAL NULL;

SELECT core.fn_dropifexists ('ProteinQuantitation', 'ms2', 'INDEX', 'IX_ProteinQuantitation_ProteinGroupId');
SELECT core.fn_dropifexists ('PeptideMemberships', 'ms2', 'INDEX', 'IX_Peptidemembership_ProteingroupId');
SELECT core.fn_dropifexists ('PeptidesData', 'ms2', 'INDEX', 'IX_PeptidesData_SeqId');
SELECT core.fn_dropifexists ('ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId');

-- redundant after the restructure of the UQ constraint
SELECT core.fn_dropifexists ('ProteinGroups', 'ms2', 'INDEX', 'ProteinGroups.IX_ProteinGroups_ProteinProphetFileId');

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId);
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId);

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId);
CREATE INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability);

-- make PPfileid the left-most column in the index so that results by run can be found
ALTER TABLE ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups;

ALTER TABLE ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE
(
    ProteinProphetFileId,
    GroupNumber,
    IndistinguishableCollectionId
);

ALTER TABLE prot.FastaSequences DROP CONSTRAINT pk_fastasequences;

ALTER TABLE prot.FastaSequences ADD CONSTRAINT pk_fastasequences PRIMARY KEY(lookupstring, fastaid);

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval);

CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource);

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId);

CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident);

CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid);

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid);

CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId);

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid);

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId);

ALTER TABLE ms2.ProteinProphetFiles DROP COLUMN Container;

UPDATE prot.InfoSources SET Url = 'http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}'
    WHERE Name = 'GO';

/* ms2-2.00-2.10.sql */

CREATE TABLE prot.CustomAnnotationSet
(
    CustomAnnotationSetId SERIAL NOT NULL,
    Container EntityId NOT NULL,
    Name VARCHAR(200) NOT NULL,
    CreatedBy UserId,
    Created timestamp without time zone,
    ModifiedBy userid,
    Modified timestamp without time zone,
    CustomAnnotationType VARCHAR(20) NOT NULL,
    Lsid lsidtype,

    CONSTRAINT customannotationset_pkey PRIMARY KEY (CustomAnnotationSetId),
    CONSTRAINT fk_CustomAnnotationSet_Container FOREIGN KEY (container) REFERENCES core.containers(EntityId),
    CONSTRAINT fk_CustomAnnotationSet_CreatedBy FOREIGN KEY (createdby) REFERENCES core.usersdata(userid),
    CONSTRAINT fk_CustomAnnotationSet_ModifiedBy FOREIGN KEY (modifiedby) REFERENCES core.usersdata(userid),
    CONSTRAINT uq_CustomAnnotationSet UNIQUE (Container, Name)
);

CREATE INDEX IX_CustomAnnotationSet_Container ON prot.CustomAnnotationSet(Container);

CREATE TABLE prot.CustomAnnotation
(
    CustomAnnotationId SERIAL NOT NULL,
    CustomAnnotationSetId INT NOT NULL,
    ObjectURI LsidType NOT NULL,
    LookupString VARCHAR(200) NOT NULL,

    CONSTRAINT customannotation_pkey PRIMARY KEY (CustomAnnotationId),
    CONSTRAINT fk_CustomAnnotation_CustomAnnotationSetId FOREIGN KEY (CustomAnnotationSetId) REFERENCES prot.CustomAnnotationSet(CustomAnnotationSetId),
    CONSTRAINT UQ_CustomAnnotation_LookupString_SetId UNIQUE (LookupString, CustomAnnotationSetId),
    CONSTRAINT UQ_CustomAnnotation_ObjectURI UNIQUE (ObjectURI)
);

CREATE INDEX IX_CustomAnnotation_CustomAnnotationSetId ON prot.CustomAnnotation(CustomAnnotationSetId);

ALTER TABLE ms2.ProteinGroups ALTER COLUMN pctspectrumids DROP NOT NULL;
ALTER TABLE ms2.ProteinGroups ALTER COLUMN percentcoverage DROP NOT NULL;

/* ms2-2.10-2.20.sql */

-- Replace auto-generated GO primary key names with standard names
ALTER TABLE prot.goterm DROP CONSTRAINT goterm_pkey;
ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id);
ALTER TABLE prot.goterm2term DROP CONSTRAINT goterm2term_pkey;
ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id);
ALTER TABLE prot.gographpath DROP CONSTRAINT gographpath_pkey;
ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id);

-- -- Replace auto-generated custom annotation primary key names with standard names.  Dependent FK must be dropped and added.
ALTER TABLE prot.customannotation DROP CONSTRAINT FK_CustomAnnotation_CustomAnnotationSetId;
ALTER TABLE prot.customannotation DROP CONSTRAINT customannotation_pkey;
ALTER TABLE prot.customannotation ADD CONSTRAINT pk_customannotation PRIMARY KEY (CustomAnnotationId);
ALTER TABLE prot.customannotationset DROP CONSTRAINT customannotationset_pkey;
ALTER TABLE prot.customannotationset ADD CONSTRAINT pk_customannotationset PRIMARY KEY (CustomAnnotationSetId);
ALTER TABLE prot.customannotation ADD CONSTRAINT fk_CustomAnnotation_CustomAnnotationSetId FOREIGN KEY (CustomAnnotationSetId) REFERENCES prot.CustomAnnotationSet(CustomAnnotationSetId);

-- Create functions to drop & create all GO indexes.  This helps with load performance.
CREATE FUNCTION prot.drop_go_indexes() RETURNS void AS '
    BEGIN
        ALTER TABLE prot.goterm DROP CONSTRAINT pk_goterm;
        DROP INDEX prot.IX_GoTerm_Name;
        DROP INDEX prot.IX_GoTerm_TermType;
        DROP INDEX prot.UQ_GoTerm_Acc;

        ALTER TABLE prot.goterm2term DROP CONSTRAINT pk_goterm2term;
        DROP INDEX prot.IX_GoTerm2Term_term1Id;
        DROP INDEX prot.IX_GoTerm2Term_term2Id;
        DROP INDEX prot.IX_GoTerm2Term_term1_2_Id;
        DROP INDEX prot.IX_GoTerm2Term_relationshipTypeId;
        DROP INDEX prot.UQ_GoTerm2Term_1_2_R;

        ALTER TABLE prot.gographpath DROP CONSTRAINT pk_gographpath;
        DROP INDEX prot.IX_GoGraphPath_term1Id;
        DROP INDEX prot.IX_GoGraphPath_term2Id;
        DROP INDEX prot.IX_GoGraphPath_term1_2_Id;
        DROP INDEX prot.IX_GoGraphPath_t1_distance;

        DROP INDEX prot.IX_GoTermDefinition_dbXrefId;
        DROP INDEX prot.UQ_GoTermDefinition_termId;

        DROP INDEX prot.IX_GoTermSynonym_SynonymTypeId;
        DROP INDEX prot.IX_GoTermSynonym_TermId;
        DROP INDEX prot.IX_GoTermSynonym_termSynonym;
        DROP INDEX prot.UQ_GoTermSynonym_termId_termSynonym;
    END;
    ' LANGUAGE plpgsql;


CREATE FUNCTION prot.create_go_indexes() RETURNS void AS '
    BEGIN
        ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id);
        CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name);
        CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype);
        CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc);

        ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id);
        CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id);
        CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id);
        CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id,term2Id);
        CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId);
        CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId);

        ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id);
        CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id);
        CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id);
        CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id);
        CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance);

        CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId);
        CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId);

        CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId);
        CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId);
        CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym);
        CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym);
    END;
    ' LANGUAGE plpgsql;

/* ms2-2.20-2.30.sql */

ALTER TABLE prot.organisms ALTER COLUMN CommonName TYPE varchar(100);

-- Create indexes on ms2 Runs table to support common operations in MS2Manager

CREATE INDEX MS2Runs_Stats ON ms2.Runs(PeptideCount, SpectrumCount, Deleted, StatusId);
CREATE INDEX MS2Runs_ExperimentRunLSID ON ms2.Runs(ExperimentRunLSID);

-- Use fn_dropifexists to make drop GO indexes function more reliable
CREATE OR REPLACE FUNCTION prot.drop_go_indexes() RETURNS void AS $$
    BEGIN
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Constraint', 'pk_goterm');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'IX_GoTerm_Name');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'IX_GoTerm_TermType');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'UQ_GoTerm_Acc');

        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Constraint', 'pk_goterm2term');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term2Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1_2_Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_relationshipTypeId');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'UQ_GoTerm2Term_1_2_R');

        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Constraint', 'pk_gographpath');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term2Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1_2_Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_t1_distance');

        PERFORM core.fn_dropifexists('gotermdefinition', 'prot', 'Index', 'IX_GoTermDefinition_dbXrefId');
        PERFORM core.fn_dropifexists('gotermdefinition', 'prot', 'Index', 'UQ_GoTermDefinition_termId');

        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_SynonymTypeId');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_TermId');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_termSynonym');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym');
    END;
    $$ LANGUAGE plpgsql;

SELECT core.fn_dropifexists('Runs', 'ms2', 'INDEX','MS2Runs_Container');

CREATE INDEX MS2Runs_Container ON ms2.Runs(Container);

-- Add EndScan to PeptidesData table
DROP INDEX ms2.UQ_MS2PeptidesData_FractionScanCharge;

ALTER TABLE ms2.PeptidesData
    ADD EndScan INT NULL;

CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData (Fraction, Scan, EndScan, Charge);

/* ms2-2.30-8.10.sql */

/* ms2-2.30-2.31.sql */

SELECT core.fn_dropifexists('PeptidesData', 'ms2', 'INDEX','IX_MS2PeptidesData_TrimmedPeptide');

CREATE INDEX IX_MS2PeptidesData_TrimmedPeptide ON ms2.PeptidesData(TrimmedPeptide);

SELECT core.fn_dropifexists('PeptidesData', 'ms2', 'INDEX','IX_MS2PeptidesData_Peptide');

CREATE INDEX IX_MS2PeptidesData_Peptide ON ms2.PeptidesData(Peptide);

/* ms2-2.31-2.32.sql */

CREATE INDEX IX_Annotations_IdentId ON prot.annotations(AnnotIdent);

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'SPROT_NAME' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt');

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'UPSP' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt');

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'UPTR' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt');

DELETE FROM prot.identifiers WHERE (identifier = '' OR identifier IS NULL) AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'GeneName');

/* ms2-2.32-2.33.sql */

DELETE FROM prot.FastaFiles WHERE FastaId = 0;

/* ms2-2.33-2.34.sql */

-- Clean up blank BestName entries from protein annotation loads in old versions

UPDATE prot.sequences SET bestname = (SELECT MIN(fs.lookupstring) FROM prot.fastasequences fs WHERE fs.seqid = prot.sequences.seqid) WHERE bestname IS NULL OR bestname = '';

UPDATE prot.sequences SET bestname = (SELECT MIN(identifier) FROM prot.identifiers i WHERE i.seqid = prot.sequences.seqid) WHERE bestname IS NULL OR bestname = '';

UPDATE prot.sequences SET bestname = 'UNKNOWN' WHERE bestname IS NULL OR bestname = '';

/* ms2-2.34-2.35.sql */

-- Increase column size to accomodate long synonyms in recent GO files
ALTER TABLE prot.GoTermSynonym ALTER COLUMN TermSynonym TYPE VARCHAR(500);

/* ms2-8.10-8.20.sql */

/* ms2-8.10-8.11.sql */

CREATE INDEX IX_Fractions_MzXMLURL ON ms2.fractions(mzxmlurl);