/*
 * Copyright (c) 2010 LabKey Corporation
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

/* ms2-0.00-8.20.sql */

/* ms2-0.00-2.30.sql */

/* ms2-0.00-2.00.sql */

/* ms2-0.00-1.00.sql */

-- Tables used for Proteins and MS2 data

EXEC sp_addapprole 'prot', 'password'
GO

/****** ProtAnnotInsertions                                 */
CREATE TABLE prot.ProtAnnotInsertions
(
    InsertId INT IDENTITY (1, 1) NOT NULL,
    FileName VARCHAR(200) NULL,
    FileType VARCHAR(50) NULL,
    Comment VARCHAR(200) NULL,
    InsertDate DATETIME NULL DEFAULT (getdate()),
    ChangeDate DATETIME NULL DEFAULT (getdate()),
    Mouthsful INT NULL DEFAULT 0,
    RecordsProcessed INT NULL DEFAULT 0,
    CompletionDate DATETIME NULL,
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

    CONSTRAINT PK_ProtAnnotInsertions PRIMARY KEY CLUSTERED (InsertId)
)
GO

/****** ProtInfoSources                                     */
CREATE TABLE prot.ProtInfoSources
(
    SourceId INT IDENTITY (1, 1) NOT NULL,
    Name VARCHAR(50) NOT NULL,
    CurrentVersion VARCHAR(50) NULL,
    CurrentVersionDate DATETIME NULL,
    Url VARCHAR(1000) NULL,
    ProcessToObtain BINARY(1000) NULL,
    LastUpdate DATETIME NULL,
    InsertDate DATETIME NULL DEFAULT (getdate()),
    ModDate DATETIME NULL,
    Deleted BIT NOT NULL DEFAULT 0,
 
    CONSTRAINT PK_ProtSeqSources PRIMARY KEY CLUSTERED (SourceId)
)
GO

/****** ProtAnnotationTypes                                 */  
CREATE TABLE prot.ProtAnnotationTypes
(
    AnnotTypeId INT IDENTITY (1, 1) NOT NULL,
    Name VARCHAR(50) NOT NULL,
    SourceId INT NULL,
    Description VARCHAR(200) NULL,
    EntryDate DATETIME NOT NULL DEFAULT (getdate()),
    ModDate DATETIME NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtAnnotationTypes PRIMARY KEY CLUSTERED (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotationTypes_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.ProtInfoSources (SourceId)
)
CREATE UNIQUE INDEX IX_ProtAnnotationTypes ON prot.ProtAnnotationTypes(Name) 
GO


/****** ProtIdentTypes                                      */
CREATE TABLE prot.ProtIdentTypes
(
    IdentTypeId INT IDENTITY (1, 1) NOT NULL,
    Name VARCHAR(50) NOT NULL,
    CannonicalSourceId INT NULL,
    EntryDate DATETIME NOT NULL DEFAULT (getdate()),
    Description VARCHAR(200) NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtIdentTypes PRIMARY KEY CLUSTERED (IdentTypeId),
    CONSTRAINT FK_ProtIdentTypes_ProtInfoSources FOREIGN KEY (CannonicalSourceId) REFERENCES prot.ProtInfoSources (SourceId)
)
CREATE UNIQUE INDEX IX_ProtIdentTypes ON prot.ProtIdentTypes(Name) 
GO


/****** ProtSequences                                       */  
CREATE TABLE prot.ProtSequences
(
    SeqId INT IDENTITY (1, 1) NOT NULL,
    ProtSequence TEXT NULL,
    Hash VARCHAR(100) NULL,
    Description VARCHAR(200) NULL,
    SourceId INT NULL,
    SourceVersion VARCHAR(50) NULL,
    InsertDate DATETIME NULL DEFAULT (getdate()),
    ChangeDate DATETIME NULL,
    SourceChangeDate DATETIME NULL,
    SourceInsertDate DATETIME NULL,
    OrgId INT NULL,
    Mass FLOAT NULL,
    BestName VARCHAR(50) NULL,
    BestGeneName VARCHAR(50) NULL,
    Length INT NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtSequences PRIMARY KEY CLUSTERED (SeqId),
    CONSTRAINT FK_ProtSequences_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.ProtInfoSources (SourceId)
)
CREATE INDEX IX_SequencesOrg ON prot.ProtSequences(OrgId) 
CREATE INDEX IX_SeqHash ON prot.ProtSequences(Hash) 
CREATE INDEX IX_ProtSequences_BestGeneName ON prot.ProtSequences(BestGeneName)
CREATE UNIQUE INDEX IX_ProtSequencesSurrogateKey ON prot.ProtSequences(Hash, OrgId) 
GO


/****** ProtIdentifiers                                     */ 
CREATE TABLE prot.ProtIdentifiers
(
    IdentId INT IDENTITY (1, 1) NOT NULL,
    IdentTypeId INT NOT NULL,
    Identifier VARCHAR(50) NOT NULL,
    SeqId INT NULL,
    SourceId INT NULL,
    EntryDate DATETIME NOT NULL DEFAULT (getdate()),
    SourceVersion VARCHAR(50) NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtIdentifiers PRIMARY KEY CLUSTERED (IdentId),
    CONSTRAINT FK_ProtIdentifiers_ProtIdentTypes FOREIGN KEY (IdentTypeId) REFERENCES prot.ProtIdentTypes (IdentTypeId),
    CONSTRAINT FK_ProtIdentifiers_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.ProtInfoSources (SourceId),
    CONSTRAINT FK_ProtIdentifiers_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.ProtSequences (SeqId)
)
CREATE INDEX IX_Identifier ON prot.ProtIdentifiers(Identifier)
CREATE UNIQUE INDEX IX_ProtIdentifiers ON prot.ProtIdentifiers(IdentTypeId, Identifier, SeqId) 
CREATE INDEX IX_ProtIdentifiers1 ON prot.ProtIdentifiers(IdentTypeId, Identifier, IdentId, SeqId) 
GO


/****** ProtOrganisms                                       */
CREATE TABLE prot.ProtOrganisms
(
    OrgId INT IDENTITY (1, 1) NOT NULL,
    CommonName VARCHAR(50) NULL,
    Genus VARCHAR(100) NOT NULL,
    Species VARCHAR(100)  NOT NULL,
    Comments VARCHAR(200) NULL,
    IdentId INT NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtOrganisms PRIMARY KEY CLUSTERED (OrgId),
    CONSTRAINT FK_ProtOrganisms_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES prot.ProtIdentifiers (IdentId),
    CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES prot.ProtOrganisms (OrgId)
)
CREATE UNIQUE INDEX IX_ProtOrganismsSurrogateKey ON prot.ProtOrganisms(Genus,Species) 
GO


/****** ProtAnnotations                                     */
CREATE TABLE prot.ProtAnnotations
(
    AnnotId INT IDENTITY (1, 1) NOT NULL,
    AnnotTypeId INT NOT NULL,
    AnnotVal VARCHAR(200) NULL,
    AnnotIdent INT NULL,
    SeqId INT NULL,
    AnnotSourceId INT NULL,
    AnnotSourceVersion VARCHAR(50) NULL,
    InsertDate DATETIME NOT NULL DEFAULT (getdate()),
    ModDate DATETIME NULL,
    StartPos INT NULL,
    EndPos INT NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtAnnotations PRIMARY KEY CLUSTERED (AnnotId),
    CONSTRAINT FK_ProtAnnotations_ProtAnnotationTypes FOREIGN KEY (AnnotTypeId) REFERENCES prot.ProtAnnotationTypes (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotations_ProtIdentifiers FOREIGN KEY (AnnotIdent) REFERENCES prot.ProtIdentifiers (IdentId),
    CONSTRAINT FK_ProtAnnotations_ProtSeqSources FOREIGN KEY (AnnotSourceId) REFERENCES prot.ProtInfoSources (SourceId),
    CONSTRAINT FK_ProtAnnotations_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.ProtSequences (SeqId)
)
CREATE INDEX IX_ProtAnnotations1 ON prot.ProtAnnotations(SeqId, AnnotTypeId) 
CREATE UNIQUE INDEX IX_AnnotSurrogateKey ON prot.ProtAnnotations(AnnotTypeId, AnnotVal, SeqId, StartPos, EndPos) 
GO


/****** ProtFastas                                          */
CREATE TABLE prot.ProtFastas
(
    FastaId INT IDENTITY (1, 1) NOT NULL,
    FileName VARCHAR(200) NOT NULL,
    FileChecksum VARCHAR(50) NULL,
    Comment VARCHAR(200) NULL,
    InsertDate DATETIME NULL DEFAULT (getdate()),
    DbName VARCHAR(100) NULL,
    DbVersion VARCHAR(100) NULL,
    DbSource INT NULL,
    DbDate DATETIME NULL,
    Reference VARCHAR(200) NULL,
    NSequences INT NULL,
    Sequences IMAGE NULL,
 
    CONSTRAINT FK_ProtFastas_ProtSeqSources FOREIGN KEY (DbSource) REFERENCES prot.ProtInfoSources (SourceId),
    CONSTRAINT PK_ProtFastas PRIMARY KEY CLUSTERED (FastaId)
)
GO

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
)
GO


INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('Genbank','http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=protein&cmd=search&term={}','2005-03-04 12:08:10')
INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('NiceProt','http://au.expasy.org/cgi-bin/niceprot.pl?{}','2005-03-04 12:08:10')
INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('GeneCards','http://bioinfo.weizmann.ac.il/cards-bin/carddisp?_symbol={}','2005-03-04 12:08:10')
INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('NCBI Taxonomy','http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id={}','2005-03-04 12:08:10')
INSERT INTO prot.ProtInfoSources (Name,Url,InsertDate) VALUES ('GO','http://www.godatabase.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}','2005-03-04 12:08:52')
GO

INSERT INTO prot.ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_F',5,'2005-03-04 11:37:15')
INSERT INTO prot.ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_P',5,'2005-03-04 11:37:15')
INSERT INTO prot.ProtAnnotationTypes (Name,EntryDate) VALUES ('keyword','2005-03-04 11:37:15')
INSERT INTO prot.ProtAnnotationTypes (Name,EntryDate) VALUES ('feature','2005-03-04 11:37:15')
INSERT INTO prot.ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_C',5,'2005-03-04 11:38:13')
INSERT INTO prot.ProtAnnotationTypes (Name) VALUES ('FullOrganismName')
INSERT INTO prot.ProtAnnotationTypes (Name) VALUES ('LookupString')
GO

INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('Genbank',1,'2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('SwissProt',2,'2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GeneName',3,'2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('NCBI Taxonomy',4,'2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('EMBL','2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('IntAct','2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Ensembl','2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('FlyBase','2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GO',5,'2005-03-04 11:37:14')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('InterPro','2005-03-04 11:37:15')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Pfam','2005-03-04 11:37:15')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PIR','2005-03-04 11:37:15')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Uniprot_keyword','2005-03-04 11:37:15')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SMART','2005-03-04 11:37:16')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('HSSP','2005-03-04 11:37:17')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ProDom','2005-03-04 11:37:17')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PROSITE','2005-03-04 11:37:17')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PRINTS','2005-03-04 11:37:19')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('TIGRFAMs','2005-03-04 11:37:22')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('EC','2005-03-04 11:37:22')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MaizeDB','2005-03-04 11:37:33')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('TRANSFAC','2005-03-04 11:37:34')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('WormBase','2005-03-04 11:37:38')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('WormPep','2005-03-04 11:37:39')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('COMPLUYEAST-2DPAGE','2005-03-04 11:37:39')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('DictyBase','2005-03-04 11:37:40')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Gramene','2005-03-04 11:37:45')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('OGP','2005-03-04 11:38:02')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Genew','2005-03-04 11:38:02')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('H-InvDB','2005-03-04 11:38:02')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MIM','2005-03-04 11:38:02')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MGD','2005-03-04 11:38:04')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('RGD','2005-03-04 11:38:06')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PDB','2005-03-04 11:38:10')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SWISS-2DPAGE','2005-03-04 11:38:33')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Aarhus/Ghent-2DPAGE','2005-03-04 11:38:33')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PMMA-2DPAGE','2005-03-04 11:38:45')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('TIGR','2005-03-04 11:38:49')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('TubercuList','2005-03-04 11:38:50')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Leproma','2005-03-04 11:39:05')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('GeneFarm','2005-03-04 11:39:35')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('GermOnline','2005-03-04 11:43:54')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SGD','2005-03-04 11:43:54')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('GeneDB_SPombe','2005-03-04 11:44:15')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PIRSF','2005-03-04 11:45:42')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('HAMAP','2005-03-04 11:46:49')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Reactome','2005-03-04 11:46:52')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ECO2DBASE','2005-03-04 11:46:55')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('EchoBASE','2005-03-04 11:46:55')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('EcoGene','2005-03-04 11:46:55')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SubtiList','2005-03-04 11:46:58')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ListiList','2005-03-04 11:47:14')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('GlycoSuiteDB','2005-03-04 11:47:44')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('StyGene','2005-03-04 11:51:59')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PHCI-2DPAGE','2005-03-04 11:52:19')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Siena-2DPAGE','2005-03-04 11:55:22')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('HSC-2DPAGE','2005-03-04 11:55:41')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MEROPS','2005-03-04 11:59:32')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('AGD','2005-03-04 12:14:40')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PhotoList','2005-03-04 12:15:22')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ZFIN','2005-03-04 12:15:39')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('MypuList','2005-03-04 12:24:15')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('SagaList','2005-03-04 12:25:40')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('ANU-2DPAGE','2005-03-04 12:29:22')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Rat-heart-2DPAGE','2005-03-04 12:30:51')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('PhosSite','2005-03-04 12:49:00')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('REBASE','2005-03-04 13:25:29')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('Maize-2DPAGE','2005-03-04 15:10:53')
INSERT INTO prot.ProtIdentTypes (Name,EntryDate) VALUES ('HIV','2005-03-04 22:13:40')

INSERT INTO prot.ProtOrganisms (CommonName,Genus,Species,Comments) VALUES ('Unknown organism','Unknown','unknown','Organism is unknown')
GO


CREATE TABLE prot.ProteinDataBases
(
    DataBaseId INT IDENTITY (0, 1) NOT NULL,
    ProteinDataBase NVARCHAR (400),
    Loaded DATETIME,

    CONSTRAINT PK_ProteinDataBases PRIMARY KEY (DataBaseId)
)
GO


-- Special entry 0 for runs that contain no protein database
INSERT INTO prot.ProteinDataBases (ProteinDataBase, Loaded) VALUES (NULL, NULL)
GO


EXEC sp_addapprole 'ms2', 'password'
GO

CREATE TABLE ms2.MS2Runs
(
    -- standard fields
    _ts TIMESTAMP,
    Run INT IDENTITY(1, 1),
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,
    Owner USERID NULL,

    Container ENTITYID NOT NULL,
    EntityId ENTITYID DEFAULT NEWID(),
    Description NVARCHAR(300),
    Path NVARCHAR(500),
    FileName NVARCHAR(300),
    Status NVARCHAR(200),
    StatusId INT NOT NULL DEFAULT 0,
    Type NVARCHAR(30),
    SearchEngine NVARCHAR(20),
    MassSpecType NVARCHAR(200),
    DataBaseId INT NOT NULL DEFAULT 0,
    SampleEnzyme NVARCHAR(50),
    Deleted BIT NOT NULL DEFAULT 0,
    ApplicationLSID LSIDType NULL,

    CONSTRAINT PK_MS2Runs PRIMARY KEY (Run)
)
GO


CREATE TABLE ms2.MS2Fractions
(
    Fraction INT IDENTITY(1,1),
    Run INT NOT NULL,
    Description NVARCHAR(300),
    FileName NVARCHAR(300),
    HydroB0 REAL,
    HydroB1 REAL,
    HydroR2 REAL,
    HydroSigma REAL,

    CONSTRAINT PK_MS2Fractions PRIMARY KEY (Fraction)
)
GO


CREATE TABLE ms2.MS2Modifications
(
    Run INT NOT NULL,
    AminoAcid VARCHAR (1) NOT NULL,
    MassDiff REAL NOT NULL,
    Variable BIT NOT NULL,
    Symbol VARCHAR (1) NOT NULL,

    CONSTRAINT PK_MS2Modifications PRIMARY KEY (Run, AminoAcid, Symbol)
)
GO


-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE ms2.MS2SpectraData
(
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Spectrum IMAGE NOT NULL,

    CONSTRAINT PK_MS2SpectraData PRIMARY KEY (Fraction, Scan)
)
GO


-- Create table for MS2 run & peptide history
CREATE TABLE ms2.MS2History
(
    Date DATETIME,
    Runs BIGINT,
    Peptides BIGINT,

    CONSTRAINT PK_MS2History PRIMARY KEY CLUSTERED (Date)
)
GO


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

CREATE TABLE prot.GoTerm2Term
(
    id INTEGER NOT NULL,
    relationshipTypeId INTEGER NOT NULL DEFAULT 0,
    term1Id INTEGER NOT NULL DEFAULT 0,
    term2Id INTEGER NOT NULL DEFAULT 0,
    complete INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT goterm2term_pkey PRIMARY KEY(id)
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

CREATE TABLE prot.GoGraphPath
(
    id INTEGER NOT NULL,
    term1Id INTEGER NOT NULL DEFAULT 0,
    term2Id INTEGER NOT NULL DEFAULT 0,
    distance INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT gographpath_pkey PRIMARY KEY(id)
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

CREATE TABLE prot.GoTermDefinition
(
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

CREATE TABLE prot.GoTermSynonym
(
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

/* ms2-1.10-1.20.sql */

-- Eliminate unique requirement on Date column, which is unnecessary and causes constraint violations
ALTER TABLE ms2.MS2History DROP CONSTRAINT PK_MS2History
GO

CREATE INDEX IX_MS2History ON ms2.MS2History (Date)
GO

-- Move SeqId into MS2PeptidesData to eliminate inefficient joins and add RunDescription column
ALTER TABLE ms2.MS2Fractions ADD
    PepXmlDataLSID LSIDType NULL,
    MzXmlURL VARCHAR(400) NULL
GO

-- Add a column for the hash of the file
ALTER TABLE prot.proteindatabases ADD
    FileChecksum varchar(50) NULL
GO

/* ms2-1.20-1.30.sql */

-- FK between ProtSequences and ProtOrganisms was incorrect IN SQL Sever only.

-- first set any unmatched orgids in ProtSeq to the 'unknown' orgid.  prevents failures later
UPDATE prot.ProtSequences
    SET OrgId = (SELECT OrgId FROM prot.ProtOrganisms WHERE genus='Unknown' AND species='unknown')
    WHERE OrgId NOT IN (SELECT OrgId FROM prot.ProtOrganisms)
GO

-- drop the incorrect FK
ALTER TABLE prot.ProtOrganisms
    DROP CONSTRAINT FK_ProtSequences_ProtOrganisms
GO

-- add the FK back on the correct table
ALTER TABLE prot.ProtSequences
    WITH NOCHECK ADD CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES prot.ProtOrganisms (OrgId)
GO

-- add most common ncbi Taxonomy id's

CREATE TABLE #idents
(
    rowid int NOT NULL identity,
    Identifier varchar(50) NOT NULL,
    CommonName varchar(20) NULL,
    Genus varchar(100) NOT NULL,
    Species varchar(100) NOT NULL,
    OrgId int NULL,
    IdentId int NULL,
    IdentTypeId int NULL
)

INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('chicken', 'Gallus', 'gallus', '9031')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('chimp', 'Pan', 'troglodytes', '9598')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('cow', 'Bos', 'taurus', '9913')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('dog', 'Canis', 'familiaris', '9615')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('ecoli', 'Escherichia', 'coli', '562')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('fruit fly', 'Drosophila', 'melanogaster', '7227')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('horse', 'Equus', 'caballus', '9796')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('human', 'Homo', 'sapiens', '9606')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('mouse', 'Mus', 'musculus', '10090')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('nematode', 'Caenorhabditis', 'elegans', '6239')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('pig', 'Sus', 'scrofa', '9823')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('rat', 'Rattus', 'norvegicus', '10116')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('yeast', 'Saccharomyces', 'cerevisiae', '4932')
INSERT #idents (CommonName, Genus, Species, Identifier)
    VALUES ('zebrafish', 'Danio', 'rerio', '7955')

UPDATE #idents
    SET IdentTypeId = (SELECT identtypeid FROM prot.ProtIdentTypes WHERE name='NCBI Taxonomy')

INSERT prot.ProtOrganisms (CommonName, Genus, Species)
SELECT CommonName, Genus, Species FROM #idents
    WHERE NOT EXISTS
        (SELECT * FROM prot.ProtOrganisms PO INNER JOIN #idents i ON (PO.Genus = i.Genus AND PO.Species = i.Species))

INSERT prot.ProtIdentifiers (Identifier, IdentTypeId)
    SELECT Identifier, IdentTypeId FROM #idents
    WHERE NOT EXISTS
        (SELECT * FROM prot.ProtIdentifiers PI INNER JOIN #idents i ON (PI.Identifier = i.Identifier AND PI.IdentTypeId = i.IdentTypeId))

UPDATE #idents
    SET OrgId = PO.OrgId
    FROM prot.ProtOrganisms PO
    WHERE #idents.Genus = PO.Genus AND #idents.Species = PO.Species


UPDATE #idents
    SET IdentId = PI.IdentId
    FROM prot.ProtIdentifiers PI
    WHERE #idents.Identifier = PI.Identifier AND #idents.IdentTypeId = PI.IdentTypeId

UPDATE prot.ProtOrganisms
    SET IdentId = i.IdentID
    FROM #idents i
    WHERE i.OrgId = prot.ProtOrganisms.OrgId

DROP TABLE #idents
GO


CREATE TABLE ms2.MS2PeptidesData
(
    RowId BIGINT IDENTITY (1, 1) NOT NULL,
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Charge TINYINT NOT NULL,
    Score1 REAL NOT NULL DEFAULT 0,
    Score2 REAL NOT NULL DEFAULT 0,
    Score3 REAL NOT NULL DEFAULT 0,
    Score4 REAL NULL,
    Score5 REAL NULL,
    IonPercent REAL NOT NULL,
    Mass FLOAT NOT NULL,    -- Store mass as high-precision real
    DeltaMass REAL NOT NULL,
    PeptideProphet REAL NOT NULL,
    Peptide varchar (200) NOT NULL,
    PrevAA char(1) NOT NULL DEFAULT '',
    TrimmedPeptide VARCHAR(200) NOT NULL DEFAULT '',
    NextAA char(1) NOT NULL DEFAULT '',
    ProteinHits SMALLINT NOT NULL,
    SequencePosition INT NOT NULL DEFAULT 0,
    Protein VARCHAR (100) NOT NULL,
    SeqId INT NULL
)
GO

CREATE UNIQUE CLUSTERED INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.MS2PeptidesData(Fraction, Scan, Charge)
GO

ALTER TABLE ms2.MS2PeptidesData
    ADD CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (RowId)
GO

CREATE INDEX IX_MS2PeptidesData_Protein ON ms2.MS2PeptidesData(Protein)
GO

CREATE TABLE ms2.MS2ProteinProphetFiles
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    FilePath VARCHAR(255) NOT NULL,
    Run INT NOT NULL,
    Container EntityId NOT NULL,
    UploadCompleted BIT DEFAULT 0 NOT NULL,
    MinProbSeries IMAGE NULL,
    SensitivitySeries IMAGE NULL,
    ErrorSeries IMAGE NULL,
    PredictedNumberCorrectSeries IMAGE NULL,
    PredictedNumberIncorrectSeries IMAGE NULL,

    CONSTRAINT PK_MS2ProteinProphetFiles PRIMARY KEY (RowId),
    CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs(Run),
    CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run)
)
GO

CREATE TABLE ms2.MS2ProteinGroups
(
    RowId INT IDENTITY (1, 1) NOT NULL,
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
)
GO

CREATE TABLE ms2.MS2ProteinGroupMemberships
(
    ProteinGroupId INT NOT NULL,
    SeqId INT NOT NULL,
    Probability REAL NOT NULL,

    CONSTRAINT PK_MS2ProteinGroupMemberships PRIMARY KEY (ProteinGroupId, SeqId),
    CONSTRAINT FK_MS2ProteinGroupMemberships_ProtSequences  FOREIGN KEY (SeqId)       REFERENCES prot.ProtSequences (SeqId),
    CONSTRAINT FK_MS2ProteinGroupMembership_MS2ProteinGroups   FOREIGN KEY (ProteinGroupId)       REFERENCES ms2.MS2ProteinGroups (RowId)
)
GO

CREATE TABLE ms2.MS2PeptideMemberships
(
  PeptideId BIGINT NOT NULL,
  ProteinGroupId INT NOT NULL,
  NSPAdjustedProbability REAL NOT NULL,
  Weight REAL NOT NULL,
  NondegenerateEvidence BIT NOT NULL,
  EnzymaticTermini INT NOT NULL,
  SiblingPeptides REAL NOT NULL,
  SiblingPeptidesBin INT NOT NULL,
  Instances INT NOT NULL,
  ContributingEvidence BIT NOT NULL,
  CalcNeutralPepMass REAL NOT NULL,
  CONSTRAINT pk_ms2peptidememberships PRIMARY KEY (proteingroupid, peptideid),
  CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid),
  CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid)
)
GO

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
)
GO

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
)
GO

ALTER TABLE ms2.MS2Runs
    ADD HasPeptideProphet BIT NOT NULL DEFAULT '0'


CREATE TABLE ms2.PeptideProphetSummaries
(
    Run INT NOT NULL,
    FValSeries IMAGE NULL,
    ObsSeries1 IMAGE NULL,
    ObsSeries2 IMAGE NULL,
    ObsSeries3 IMAGE NULL,
    ModelPosSeries1 IMAGE NULL,
    ModelPosSeries2 IMAGE NULL,
    ModelPosSeries3 IMAGE NULL,
    ModelNegSeries1 IMAGE NULL,
    ModelNegSeries2 IMAGE NULL,
    ModelNegSeries3 IMAGE NULL,
    MinProbSeries IMAGE NULL,
    SensitivitySeries IMAGE NULL,
    ErrorSeries IMAGE NULL,

    CONSTRAINT PK_PeptideProphetSummmaries PRIMARY KEY (Run)
)
GO

-- Update GeneCards URL
UPDATE prot.ProtInfoSources SET Url = 'http://www.genecards.org/cgi-bin/carddisp?{}&alias=yes'
    WHERE Name = 'GeneCards'
GO

/* ms2-1.30-1.40.sql */

-- Store peptide and spectrum counts with each run to make computing stats much faster
ALTER TABLE ms2.MS2Runs ADD
    PeptideCount INT NOT NULL DEFAULT 0,
    SpectrumCount INT NOT NULL DEFAULT 0
GO

-- Update counts for existing runs
UPDATE ms2.MS2Runs SET PeptideCount = PepCount FROM
    (SELECT Run, COUNT(*) AS PepCount FROM ms2.MS2PeptidesData pd INNER JOIN ms2.MS2Fractions f ON pd.Fraction = f.Fraction GROUP BY Run) x
WHERE ms2.MS2Runs.Run = x.Run

UPDATE ms2.MS2Runs SET SpectrumCount = SpecCount FROM
    (SELECT Run, COUNT(*) AS SpecCount FROM ms2.MS2SpectraData sd INNER JOIN ms2.MS2Fractions f ON sd.Fraction = f.Fraction GROUP BY Run) x
WHERE ms2.MS2Runs.Run = x.Run
GO

-- Relax contraints on quantitation result columns; q3 does not generate string representations of ratios.
ALTER TABLE ms2.Quantitation ALTER COLUMN Ratio VARCHAR(20) NULL
ALTER TABLE ms2.Quantitation ALTER COLUMN Heavy2lightRatio VARCHAR(20) NULL
GO

-- Add a quantitation summary table
CREATE TABLE ms2.QuantSummaries
(
    QuantId INT IDENTITY(1,1) NOT NULL,
    Run INT NOT NULL,
    AnalysisType NVARCHAR(20) NOT NULL,
    AnalysisTime DATETIME NULL,
    Version NVARCHAR(80) NULL,
    LabeledResidues NVARCHAR(20) NULL,
    MassDiff NVARCHAR(80) NULL,
    MassTol REAL NOT NULL,
    SameScanRange CHAR(1) NULL,
    XpressLight INT NULL,

    CONSTRAINT PK_QuantSummaries PRIMARY KEY (QuantId),
    CONSTRAINT FK_QuantSummaries_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs (Run),
    -- Current restrictions allow only one quantitation analysis per run
    CONSTRAINT UQ_QuantSummaries UNIQUE (Run)
)
GO

-- Add a QuantId column to ms2.Quantitation to allow multiple results for each peptide
ALTER TABLE ms2.Quantitation ADD QuantId INT
GO

-- QuantId must be non-null; eventually (PeptideId, QuantId) should become a compound PK
ALTER TABLE ms2.Quantitation ALTER COLUMN QuantId INT NOT NULL
GO

/* ms2-1.40-1.50.sql */

-- Store reverse peptide counts to enable scoring analysis UI.
ALTER TABLE ms2.MS2Runs ADD
    NegativeHitCount INT NOT NULL DEFAULT 0
GO

CREATE TABLE ms2.PeptideProphetData
(
    PeptideId BIGINT NOT NULL,
    ProphetFVal REAL NOT NULL,
    ProphetDeltaMass REAL NULL,
    ProphetNumTrypticTerm INT NULL,
    ProphetNumMissedCleav INT NULL,

    CONSTRAINT PK_PeptideProphetData PRIMARY KEY (PeptideId),
    CONSTRAINT FK_PeptideProphetData_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.MS2PeptidesData(RowId)
)
GO

ALTER TABLE prot.ProteinDataBases ADD
    ScoringAnalysis bit NOT NULL DEFAULT 0
GO

/* ms2-1.50-1.60.sql */

-- Simplify protein table names
EXEC sp_rename 'prot.ProtAnnotations', 'Annotations'
EXEC sp_rename 'prot.ProtAnnotationTypes', 'AnnotationTypes'
EXEC sp_rename 'prot.ProtAnnotInsertions', 'AnnotInsertions'
EXEC sp_rename 'prot.ProteinDatabases', 'FastaFiles'
EXEC sp_rename 'prot.ProtFastas', 'FastaLoads'
EXEC sp_rename 'prot.ProtIdentifiers', 'Identifiers'
EXEC sp_rename 'prot.ProtIdentTypes', 'IdentTypes'
EXEC sp_rename 'prot.ProtInfoSources', 'InfoSources'
EXEC sp_rename 'prot.ProtOrganisms', 'Organisms'
EXEC sp_rename 'prot.ProtSequences', 'Sequences'
EXEC sp_rename 'prot.ProtSProtOrgMap', 'SProtOrgMap'
GO

-- Rename some columns
EXEC sp_rename 'prot.FastaFiles.DataBaseId', 'FastaId', 'COLUMN'
EXEC sp_rename 'prot.FastaFiles.ProteinDataBase', 'FileName', 'COLUMN'
EXEC sp_rename 'ms2.MS2Runs.DataBaseId', 'FastaId', 'COLUMN'
GO

-- Add column for retention time
ALTER TABLE ms2.MS2PeptidesData
    ADD RetentionTime REAL NULL
GO

/* ms2-1.60-1.70.sql */

UPDATE ms2.ms2proteingroups SET pctspectrumids = pctspectrumids / 100, percentcoverage = percentcoverage / 100
GO

-- Index to speed up deletes from MS2PeptidesData.  Use old names here to allow running this on 1.6 installations.
IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE name = 'IX_MS2PeptideMemberships_PeptideId' AND id = object_id('ms2.MS2PeptideMemberships'))
    CREATE INDEX IX_MS2PeptideMemberships_PeptideId ON ms2.MS2PeptideMemberships(PeptideId)
GO

-- Simplify MS2 table names
EXEC sp_rename 'ms2.MS2Fractions', 'Fractions'
EXEC sp_rename 'ms2.MS2History', 'History'
EXEC sp_rename 'ms2.MS2Modifications', 'Modifications'
EXEC sp_rename 'ms2.MS2PeptideMemberships', 'PeptideMemberships'
EXEC sp_rename 'ms2.MS2PeptidesData', 'PeptidesData'
EXEC sp_rename 'ms2.MS2ProteinGroupMemberships', 'ProteinGroupMemberships'
EXEC sp_rename 'ms2.MS2ProteinGroups', 'ProteinGroups'
EXEC sp_rename 'ms2.MS2ProteinProphetFiles', 'ProteinProphetFiles'
EXEC sp_rename 'ms2.MS2Runs', 'Runs'
EXEC sp_rename 'ms2.MS2SpectraData', 'SpectraData'
GO

-- More accurate column name
EXEC sp_rename
    @objname = 'ms2.Runs.SampleEnzyme',
    @newname = 'SearchEnzyme',
    @objtype = 'COLUMN'
GO

-- Bug 2195 restructure prot.FastaSequences
CREATE TABLE prot.FastaSequences
(
    FastaId int NOT NULL,
    LookupString varchar (200) NOT NULL,
    SeqId int NULL
)

ALTER TABLE prot.FastaSequences ADD CONSTRAINT PK_FastaSequences PRIMARY KEY CLUSTERED(FastaId,LookupString)
ALTER TABLE prot.FastaSequences WITH NOCHECK ADD CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId)
CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId)
CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId)

--Bug 2193
CREATE INDEX IX_SequencesSource ON prot.Sequences(SourceId)
GO
IF EXISTS (SELECT * FROM dbo.sysindexes WHERE name = 'IX_SeqHash' AND id = object_id('prot.Sequences'))
    DROP INDEX prot.Sequences.IX_SeqHash
GO

/* ms2-1.70-2.00.sql */

EXEC core.fn_dropifexists 'ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId'
GO
CREATE INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId)
GO

EXEC core.fn_dropifexists 'ProteinGroups', 'ms2', 'INDEX', 'IX_ProteinGroups_ProteinProphetFileId'
GO
CREATE INDEX IX_ProteinGroups_ProteinProphetFileId ON ms2.ProteinGroups(ProteinProphetFileId)
GO

EXEC core.fn_dropifexists 'Fractions', 'ms2', 'INDEX', 'IX_Fractions_Run_Fraction'
GO
CREATE INDEX IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction)
GO

ALTER TABLE ms2.ProteinGroups
    ADD ErrorRate REAL NULL
GO

ALTER TABLE ms2.PeptidesData
    ADD PeptideProphetErrorRate REAL NULL
GO

EXEC core.fn_dropifexists 'ProteinQuantitation', 'ms2', 'INDEX', 'IX_ProteinQuantitation_ProteinGroupId'
GO
EXEC core.fn_dropifexists 'PeptideMemberships', 'ms2', 'INDEX', 'IX_Peptidemembership_ProteingroupId'
GO
EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX', 'IX_PeptidesData_SeqId'
GO
EXEC core.fn_dropifexists 'ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId'
GO
-- redundant after the restructure of the UQ constraint
EXEC core.fn_dropifexists 'ProteinGroups', 'ms2', 'INDEX', 'ProteinGroups.IX_ProteinGroups_ProteinProphetFileId'
GO

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId)
GO
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId)
GO

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId)
GO
CREATE  INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability)
GO

-- make PPfileid the left-most column in the index so that results by run can be found
ALTER TABLE ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups
GO

ALTER TABLE ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE  NONCLUSTERED
(
    ProteinProphetFileId,
    GroupNumber,
    IndistinguishableCollectionId
)
GO

ALTER TABLE prot.FastaSequences DROP CONSTRAINT pk_fastasequences
GO

ALTER TABLE prot.FastaSequences ADD CONSTRAINT pk_fastasequences PRIMARY KEY(lookupstring, fastaid)
GO

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval)
GO

CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource)
GO

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId)
GO

CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident)
GO

CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid)
GO

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid)
GO

CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId)
GO

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid)
GO

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId)
GO

ALTER TABLE ms2.ProteinProphetFiles DROP COLUMN Container
GO

UPDATE prot.InfoSources SET Url = 'http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}'
    WHERE Name = 'GO'
GO

/* ms2-2.00-2.10.sql */

CREATE TABLE prot.CustomAnnotationSet
(
    CustomAnnotationSetId INT IDENTITY(1, 1) NOT NULL,
    Container EntityId NOT NULL,
    Name VARCHAR(200) NOT NULL,
    CreatedBy UserId,
    Created DATETIME,
    ModifiedBy userid,
    Modified DATETIME,
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
    CustomAnnotationId INT IDENTITY(1, 1) NOT NULL,
    CustomAnnotationSetId INT NOT NULL,
    ObjectURI LsidType NOT NULL,
    LookupString VARCHAR(200) NOT NULL,

    CONSTRAINT customannotation_pkey PRIMARY KEY (CustomAnnotationId),
    CONSTRAINT fk_CustomAnnotation_CustomAnnotationSetId FOREIGN KEY (CustomAnnotationSetId) REFERENCES prot.CustomAnnotationSet(CustomAnnotationSetId),
    CONSTRAINT UQ_CustomAnnotation_LookupString_SetId UNIQUE (LookupString, CustomAnnotationSetId),
    CONSTRAINT UQ_CustomAnnotation_ObjectURI UNIQUE (ObjectURI)
);

CREATE INDEX IX_CustomAnnotation_CustomAnnotationSetId ON prot.CustomAnnotation(CustomAnnotationSetId);

ALTER TABLE ms2.ProteinGroups ALTER COLUMN pctspectrumids REAL NULL
GO

ALTER TABLE ms2.ProteinGroups ALTER COLUMN percentcoverage REAL NULL
GO

/* ms2-2.10-2.20.sql */

-- TODO: We should now just name all the PKs what we want in the CREATE statements above
CREATE PROCEDURE core.Rename_Primary_Key(@SchemaName VARCHAR(255), @TableName VARCHAR(255)) AS
    DECLARE @name VARCHAR(200)
    DECLARE @sql VARCHAR(4000)

    SELECT @name = so.name FROM sysobjects so INNER JOIN sysobjects parent ON parent.id = so.parent_obj WHERE so.xtype = 'PK' AND parent.xtype = 'U' AND parent.name = @TableName
    SELECT @sql = 'EXEC sp_rename ''' + @SchemaName + '.' + @name + ''', ''PK_' + @TableName + ''', ''OBJECT'''
    EXEC sp_sqlexec @sql
GO

-- Replace auto-generated primary key names with standard names
EXEC core.Rename_Primary_Key 'prot', 'GoTerm'
EXEC core.Rename_Primary_Key 'prot', 'GoTerm2Term'
EXEC core.Rename_Primary_Key 'prot', 'GoGraphPath'
EXEC core.Rename_Primary_Key 'prot', 'CustomAnnotationSet'
EXEC core.Rename_Primary_Key 'prot', 'CustomAnnotation'
GO

DROP PROCEDURE core.Rename_Primary_Key
GO

-- Create functions to drop & create all GO indexes.  This helps with load performance.
CREATE PROCEDURE prot.drop_go_indexes AS
    ALTER TABLE prot.goterm DROP CONSTRAINT PK_GoTerm
    DROP INDEX prot.GoTerm.IX_GoTerm_Name
    DROP INDEX prot.GoTerm.IX_GoTerm_TermType
    DROP INDEX prot.GoTerm.UQ_GoTerm_Acc

    ALTER TABLE prot.goterm2term DROP CONSTRAINT PK_GoTerm2Term
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term1Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term2Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term1_2_Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_relationshipTypeId
    DROP INDEX prot.goterm2term.UQ_GoTerm2Term_1_2_R

    ALTER TABLE prot.gographpath DROP CONSTRAINT PK_GoGraphPath
    DROP INDEX prot.gographpath.IX_GoGraphPath_term1Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_term2Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_term1_2_Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_t1_distance

    DROP INDEX prot.GoTermDefinition.IX_GoTermDefinition_dbXrefId
    DROP INDEX prot.GoTermDefinition.UQ_GoTermDefinition_termId

    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_SynonymTypeId
    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_TermId
    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_termSynonym
    DROP INDEX prot.GoTermSynonym.UQ_GoTermSynonym_termId_termSynonym
GO

CREATE PROCEDURE prot.create_go_indexes AS
    ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id)
    CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name)
    CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype)
    CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc)

    ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id)
    CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id)
    CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id)
    CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id,term2Id)
    CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId)
    CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId)

    ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id)
    CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id)
    CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id)
    CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id)
    CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance)

    CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId)
    CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId)

    CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId)
    CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId)
    CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym)
    CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym)
GO

/* ms2-2.20-2.30.sql */

ALTER TABLE prot.organisms ALTER COLUMN CommonName varchar(100)
GO

-- Create indexes on ms2 Runs table to support common operations in MS2Manager
CREATE INDEX MS2Runs_Stats ON ms2.Runs(PeptideCount, SpectrumCount, Deleted, StatusId)
GO
CREATE INDEX MS2Runs_ExperimentRunLSID ON ms2.Runs(ExperimentRunLSID)
GO

-- Use fn_dropifexists to make GO index drop function more reliable
DROP PROCEDURE prot.drop_go_indexes
GO

CREATE PROCEDURE prot.drop_go_indexes AS
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Constraint', 'PK_GoTerm'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'IX_GoTerm_Name'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'IX_GoTerm_TermType'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'UQ_GoTerm_Acc'

    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Constraint', 'PK_GoTerm2Term'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term2Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1_2_Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_relationshipTypeId'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'UQ_GoTerm2Term_1_2_R'

    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Constraint', 'PK_GoGraphPath'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term2Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1_2_Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_t1_distance'

    EXEC core.fn_dropifexists 'gotermdefinition', 'prot', 'Index', 'IX_GoTermDefinition_dbXrefId'
    EXEC core.fn_dropifexists 'gotermdefinition', 'prot', 'Index', 'UQ_GoTermDefinition_termId'

    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_SynonymTypeId'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_TermId'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_termSynonym'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym'
GO

EXEC core.fn_dropifexists 'Runs', 'ms2', 'INDEX','MS2Runs_Container'
GO
CREATE INDEX MS2Runs_Container ON ms2.Runs(Container)
GO

-- Add EndScan to PeptidesData table
DROP INDEX ms2.PeptidesData.UQ_MS2PeptidesData_FractionScanCharge
GO

ALTER TABLE ms2.PeptidesData
    ADD EndScan INT NULL
GO

CREATE UNIQUE CLUSTERED INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge)
GO

/* ms2-2.30-8.10.sql */

/* ms2-2.30-2.31.sql */

EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX','IX_MS2PeptidesData_TrimmedPeptide'
GO

CREATE INDEX IX_MS2PeptidesData_TrimmedPeptide ON
    ms2.PeptidesData(TrimmedPeptide)
GO

EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX','IX_MS2PeptidesData_Peptide'
GO

CREATE INDEX IX_MS2PeptidesData_Peptide ON
    ms2.PeptidesData(Peptide)
GO

/* ms2-2.31-2.32.sql */

CREATE INDEX IX_Annotations_IdentId ON prot.annotations(AnnotIdent)
GO

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'SPROT_NAME' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt')
GO

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'UPSP' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt')
GO

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'UPTR' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt')
GO

DELETE FROM prot.identifiers WHERE (identifier = '' OR identifier IS NULL) AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'GeneName')
GO

/* ms2-2.32-2.33.sql */

DELETE FROM prot.FastaFiles WHERE FastaId = 0
GO

/* ms2-2.33-2.34.sql */

-- Clean up blank BestName entries from protein annotation loads in old versions

UPDATE prot.sequences SET bestname = (SELECT MIN(fs.lookupstring) FROM prot.fastasequences fs WHERE fs.seqid = prot.sequences.seqid) WHERE bestname IS NULL OR bestname = ''
GO

UPDATE prot.sequences SET bestname = (SELECT MIN(identifier) FROM prot.identifiers i WHERE i.seqid = prot.sequences.seqid) WHERE bestname IS NULL OR bestname = ''
GO

UPDATE prot.sequences SET bestname = 'UNKNOWN' WHERE bestname IS NULL OR bestname = ''
GO

/* ms2-2.34-2.35.sql */

-- Increase column size to accomodate long synonyms in recent GO files
ALTER TABLE prot.GoTermSynonym ALTER COLUMN TermSynonym VARCHAR(500)
GO

/* ms2-8.10-8.20.sql */

/* ms2-8.10-8.11.sql */

CREATE INDEX IX_Fractions_MzXMLURL ON ms2.fractions(mzxmlurl)
GO

/* ms2-8.30-9.10.sql */

/* ms2-8.30-8.31.sql */

UPDATE exp.DataInput SET Role = 'Spectra' WHERE Role = 'mzXML'
GO

/* ms2-8.32-8.33.sql */

DROP INDEX prot.organisms.ix_protorganisms_genus
GO

DROP INDEX prot.organisms.ix_protorganisms_species
GO

DROP INDEX prot.identifiers.IX_ProtIdentifiers1
GO

/* ms2-8.33-8.34.sql */

ALTER TABLE prot.customannotationset DROP CONSTRAINT fk_customannotationset_createdby
GO
ALTER TABLE prot.customannotationset DROP CONSTRAINT fk_customannotationset_modifiedby
GO

/* ms2-8.34-8.35.sql */

-- Delete modification rows with bogus amino acids to avoid problems when wrapping associated runs
DELETE FROM ms2.Modifications WHERE AminoAcid < 'A' OR AminoAcid > 'Z'
GO
