/*
 * Copyright (c) 2019 LabKey Corporation
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

CREATE SCHEMA prot;
GO

/****** AnnotInsertions                                 */
CREATE TABLE prot.AnnotInsertions
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
);

ALTER TABLE prot.AnnotInsertions ALTER COLUMN FileName NVARCHAR(400);

/****** InfoSources                                     */
CREATE TABLE prot.InfoSources
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
 
    CONSTRAINT PK_SeqSources PRIMARY KEY CLUSTERED (SourceId)
);

INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('Genbank', 'http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=protein&cmd=search&term={}', '2005-03-04 12:08:10');
INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('NiceProt', 'http://au.expasy.org/cgi-bin/niceprot.pl?{}', '2005-03-04 12:08:10');
INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('GeneCards', 'http://www.genecards.org/cgi-bin/carddisp?{}&alias=yes', '2005-03-04 12:08:10');
INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('NCBI Taxonomy', 'http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id={}', '2005-03-04 12:08:10');
INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('GO', 'http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}', '2005-03-04 12:08:52');

UPDATE prot.InfoSources SET Url = 'http://www.genecards.org/cgi-bin/carddisp.pl?gene={}' WHERE Name = 'GeneCards';
UPDATE prot.InfoSources SET URL = 'http://www.uniprot.org/uniprot/{}' WHERE Name = 'NiceProt';
UPDATE prot.InfoSources SET URL = 'http://www.ncbi.nlm.nih.gov/protein/{}' WHERE Name = 'Genbank';
UPDATE prot.infosources SET url = 'http://amigo.geneontology.org/amigo/term/{}' WHERE Name = 'GO';
GO

/****** AnnotationTypes                                 */
CREATE TABLE prot.AnnotationTypes
(
    AnnotTypeId INT IDENTITY (1, 1) NOT NULL,
    Name VARCHAR(50) NOT NULL,
    SourceId INT NULL,
    Description VARCHAR(200) NULL,
    EntryDate DATETIME NOT NULL DEFAULT (getdate()),
    ModDate DATETIME NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtAnnotationTypes PRIMARY KEY CLUSTERED (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotationTypes_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.InfoSources (SourceId)
);
CREATE UNIQUE INDEX IX_ProtAnnotationTypes ON prot.AnnotationTypes(Name);

INSERT INTO prot.AnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_F',5,'2005-03-04 11:37:15');
INSERT INTO prot.AnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_P',5,'2005-03-04 11:37:15');
INSERT INTO prot.AnnotationTypes (Name,EntryDate) VALUES ('keyword','2005-03-04 11:37:15');
INSERT INTO prot.AnnotationTypes (Name,EntryDate) VALUES ('feature','2005-03-04 11:37:15');
INSERT INTO prot.AnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_C',5,'2005-03-04 11:38:13');
INSERT INTO prot.AnnotationTypes (Name) VALUES ('FullOrganismName');
INSERT INTO prot.AnnotationTypes (Name) VALUES ('LookupString');

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId);

/****** IdentTypes                                      */
CREATE TABLE prot.IdentTypes
(
    IdentTypeId INT IDENTITY (1, 1) NOT NULL,
    Name VARCHAR(50) NOT NULL,
    CannonicalSourceId INT NULL,
    EntryDate DATETIME NOT NULL DEFAULT (getdate()),
    Description VARCHAR(200) NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtIdentTypes PRIMARY KEY CLUSTERED (IdentTypeId),
    CONSTRAINT FK_ProtIdentTypes_ProtInfoSources FOREIGN KEY (CannonicalSourceId) REFERENCES prot.InfoSources (SourceId)
);
CREATE UNIQUE INDEX IX_ProtIdentTypes ON prot.IdentTypes(Name);

INSERT INTO prot.IdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('Genbank',1,'2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('SwissProt',2,'2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GeneName',3,'2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('NCBI Taxonomy',4,'2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('EMBL','2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('IntAct','2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Ensembl','2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('FlyBase','2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GO',5,'2005-03-04 11:37:14');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('InterPro','2005-03-04 11:37:15');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Pfam','2005-03-04 11:37:15');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PIR','2005-03-04 11:37:15');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Uniprot_keyword','2005-03-04 11:37:15');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('SMART','2005-03-04 11:37:16');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('HSSP','2005-03-04 11:37:17');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('ProDom','2005-03-04 11:37:17');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PROSITE','2005-03-04 11:37:17');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PRINTS','2005-03-04 11:37:19');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('TIGRFAMs','2005-03-04 11:37:22');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('EC','2005-03-04 11:37:22');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('MaizeDB','2005-03-04 11:37:33');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('TRANSFAC','2005-03-04 11:37:34');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('WormBase','2005-03-04 11:37:38');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('WormPep','2005-03-04 11:37:39');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('COMPLUYEAST-2DPAGE','2005-03-04 11:37:39');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('DictyBase','2005-03-04 11:37:40');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Gramene','2005-03-04 11:37:45');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('OGP','2005-03-04 11:38:02');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Genew','2005-03-04 11:38:02');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('H-InvDB','2005-03-04 11:38:02');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('MIM','2005-03-04 11:38:02');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('MGD','2005-03-04 11:38:04');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('RGD','2005-03-04 11:38:06');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PDB','2005-03-04 11:38:10');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('SWISS-2DPAGE','2005-03-04 11:38:33');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Aarhus/Ghent-2DPAGE','2005-03-04 11:38:33');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PMMA-2DPAGE','2005-03-04 11:38:45');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('TIGR','2005-03-04 11:38:49');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('TubercuList','2005-03-04 11:38:50');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Leproma','2005-03-04 11:39:05');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('GeneFarm','2005-03-04 11:39:35');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('GermOnline','2005-03-04 11:43:54');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('SGD','2005-03-04 11:43:54');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('GeneDB_SPombe','2005-03-04 11:44:15');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PIRSF','2005-03-04 11:45:42');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('HAMAP','2005-03-04 11:46:49');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Reactome','2005-03-04 11:46:52');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('ECO2DBASE','2005-03-04 11:46:55');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('EchoBASE','2005-03-04 11:46:55');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('EcoGene','2005-03-04 11:46:55');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('SubtiList','2005-03-04 11:46:58');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('ListiList','2005-03-04 11:47:14');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('GlycoSuiteDB','2005-03-04 11:47:44');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('StyGene','2005-03-04 11:51:59');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PHCI-2DPAGE','2005-03-04 11:52:19');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Siena-2DPAGE','2005-03-04 11:55:22');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('HSC-2DPAGE','2005-03-04 11:55:41');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('MEROPS','2005-03-04 11:59:32');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('AGD','2005-03-04 12:14:40');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PhotoList','2005-03-04 12:15:22');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('ZFIN','2005-03-04 12:15:39');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('MypuList','2005-03-04 12:24:15');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('SagaList','2005-03-04 12:25:40');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('ANU-2DPAGE','2005-03-04 12:29:22');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Rat-heart-2DPAGE','2005-03-04 12:30:51');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('PhosSite','2005-03-04 12:49:00');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('REBASE','2005-03-04 13:25:29');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('Maize-2DPAGE','2005-03-04 15:10:53');
INSERT INTO prot.IdentTypes (Name,EntryDate) VALUES ('HIV','2005-03-04 22:13:40');

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid);

/****** Sequences                                       */
CREATE TABLE prot.Sequences
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
    CONSTRAINT FK_ProtSequences_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.InfoSources (SourceId)
);
CREATE INDEX IX_SequencesOrg ON prot.Sequences(OrgId);
CREATE INDEX IX_ProtSequences_BestGeneName ON prot.Sequences(BestGeneName);
CREATE UNIQUE INDEX IX_ProtSequencesSurrogateKey ON prot.Sequences(Hash, OrgId);

-- Issue 26074: Equals filter fails on protein.Sequence.ProtSequence column on SqlServer
ALTER TABLE prot.Sequences ALTER COLUMN ProtSequence VARCHAR(MAX);

/****** Identifiers                                     */
CREATE TABLE prot.Identifiers
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
    CONSTRAINT FK_ProtIdentifiers_ProtIdentTypes FOREIGN KEY (IdentTypeId) REFERENCES prot.IdentTypes (IdentTypeId),
    CONSTRAINT FK_ProtIdentifiers_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.InfoSources (SourceId),
    CONSTRAINT FK_ProtIdentifiers_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId)
);
CREATE INDEX IX_Identifier ON prot.Identifiers(Identifier);
CREATE UNIQUE INDEX IX_ProtIdentifiers ON prot.Identifiers(IdentTypeId, Identifier, SeqId);
CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid);
CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId);

/****** Organisms                                       */
CREATE TABLE prot.Organisms
(
    OrgId INT IDENTITY (1, 1) NOT NULL,
    CommonName VARCHAR(100) NULL,
    Genus VARCHAR(100) NOT NULL,
    Species VARCHAR(100)  NOT NULL,
    Comments VARCHAR(200) NULL,
    IdentId INT NULL,
    Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtOrganisms PRIMARY KEY CLUSTERED (OrgId),
    CONSTRAINT FK_ProtOrganisms_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES prot.Identifiers (IdentId),
    CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES prot.Organisms (OrgId)
);
CREATE UNIQUE INDEX IX_ProtOrganismsSurrogateKey ON prot.Organisms(Genus, Species);

INSERT INTO prot.Organisms (CommonName, Genus, Species, Comments) VALUES ('Unknown organism', 'Unknown', 'unknown', 'Organism is unknown');

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId);

/****** Annotations                                     */
CREATE TABLE prot.Annotations
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
    CONSTRAINT FK_ProtAnnotations_ProtAnnotationTypes FOREIGN KEY (AnnotTypeId) REFERENCES prot.AnnotationTypes (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotations_ProtIdentifiers FOREIGN KEY (AnnotIdent) REFERENCES prot.Identifiers (IdentId),
    CONSTRAINT FK_ProtAnnotations_ProtSeqSources FOREIGN KEY (AnnotSourceId) REFERENCES prot.InfoSources (SourceId),
    CONSTRAINT FK_ProtAnnotations_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId)
)
CREATE INDEX IX_ProtAnnotations1 ON prot.Annotations(SeqId, AnnotTypeId)
CREATE UNIQUE INDEX IX_AnnotSurrogateKey ON prot.Annotations(AnnotTypeId, AnnotVal, SeqId, StartPos, EndPos);
CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval);
CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident);
CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid);
CREATE INDEX IX_Annotations_IdentId ON prot.annotations(AnnotIdent);

/****** FastaLoads                                          */
CREATE TABLE prot.FastaLoads
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
 
    CONSTRAINT FK_ProtFastas_ProtSeqSources FOREIGN KEY (DbSource) REFERENCES prot.InfoSources (SourceId),
    CONSTRAINT PK_ProtFastas PRIMARY KEY CLUSTERED (FastaId)
);

CREATE INDEX IX_FastaLoads_DBSource ON prot.FastaLoads(dbsource);

/****** SprotOrgMap                                  */
CREATE TABLE prot.SprotOrgMap
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

CREATE TABLE prot.FastaFiles
(
    FastaId INT IDENTITY (1, 1) NOT NULL,
    FileName NVARCHAR (400),
    Loaded DATETIME,
    FileChecksum VARCHAR(50) NULL,           -- Hash of the file
    ScoringAnalysis BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProteinDataBases PRIMARY KEY (FastaId)
);

-- All tables used for GO data
-- Data will change frequently, with updates from the GO consortium
-- See http://www.geneontology.org/GO.downloads.shtml

-- GO Terms

CREATE TABLE prot.GoTerm
(
    id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL DEFAULT '',
    termtype VARCHAR(55) NOT NULL DEFAULT '',
    acc VARCHAR(255) NOT NULL DEFAULT '',
    isobsolete INTEGER NOT NULL DEFAULT 0,
    isroot INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT PK_GoTerm PRIMARY KEY(id)
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

    CONSTRAINT PK_GoTerm2Term PRIMARY KEY(id)
);

CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id);
CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id);
CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id, term2Id);
CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId);
CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id, term2Id, relationshipTypeId);

-- Graph path

CREATE TABLE prot.GoGraphPath
(
    id INTEGER NOT NULL,
    term1Id INTEGER NOT NULL DEFAULT 0,
    term2Id INTEGER NOT NULL DEFAULT 0,
    distance INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT PK_GoGraphPath PRIMARY KEY(id)
);

CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id);
CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id);
CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id, term2Id);
CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id, distance);

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
    TermId INTEGER NOT NULL DEFAULT 0,
    TermSynonym VARCHAR(500) NULL DEFAULT NULL,
    AccSynonym VARCHAR(255) NULL DEFAULT NULL,
    SynonymTypeId INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(SynonymTypeId);
CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(TermId);
CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(TermSynonym);
CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(TermId, TermSynonym);

GO

CREATE PROCEDURE prot.create_go_indexes AS
BEGIN
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
    CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym);
END

GO

CREATE PROCEDURE prot.drop_go_indexes AS
BEGIN
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
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym';
END

GO

-- add most common ncbi Taxonomy id's
CREATE TABLE prot.idents
(
    RowId INT NOT NULL IDENTITY,
    Identifier VARCHAR(50) NOT NULL,
    CommonName VARCHAR(20) NULL,
    Genus VARCHAR(100) NOT NULL,
    Species VARCHAR(100) NOT NULL,
    OrgId INT NULL,
    IdentId INT NULL,
    IdentTypeId INT NULL
);

INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('chicken', 'Gallus', 'gallus', '9031');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('chimp', 'Pan', 'troglodytes', '9598');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('cow', 'Bos', 'taurus', '9913');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('dog', 'Canis', 'familiaris', '9615');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('ecoli', 'Escherichia', 'coli', '562');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('fruit fly', 'Drosophila', 'melanogaster', '7227');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('horse', 'Equus', 'caballus', '9796');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('human', 'Homo', 'sapiens', '9606');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('mouse', 'Mus', 'musculus', '10090');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('nematode', 'Caenorhabditis', 'elegans', '6239');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('pig', 'Sus', 'scrofa', '9823');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('rat', 'Rattus', 'norvegicus', '10116');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('yeast', 'Saccharomyces', 'cerevisiae', '4932');
INSERT prot.idents (CommonName, Genus, Species, Identifier)
VALUES ('zebrafish', 'Danio', 'rerio', '7955');

UPDATE prot.idents SET IdentTypeId = (SELECT identtypeid FROM prot.IdentTypes WHERE name='NCBI Taxonomy');

INSERT prot.Organisms (CommonName, Genus, Species)
    SELECT CommonName, Genus, Species FROM prot.idents
    WHERE NOT EXISTS
          (SELECT * FROM prot.Organisms PO INNER JOIN prot.idents i ON (PO.Genus = i.Genus AND PO.Species = i.Species));

INSERT prot.Identifiers (Identifier, IdentTypeId)
    SELECT Identifier, IdentTypeId FROM prot.idents
    WHERE NOT EXISTS
          (SELECT * FROM prot.Identifiers PI INNER JOIN prot.idents i ON (PI.Identifier = i.Identifier AND PI.IdentTypeId = i.IdentTypeId));

UPDATE prot.idents
    SET OrgId = PO.OrgId
    FROM prot.Organisms PO
    WHERE prot.idents.Genus = PO.Genus AND prot.idents.Species = PO.Species;

UPDATE prot.idents
    SET IdentId = PI.IdentId
    FROM prot.Identifiers PI
    WHERE prot.idents.Identifier = PI.Identifier AND prot.idents.IdentTypeId = PI.IdentTypeId;

UPDATE prot.Organisms
    SET IdentId = i.IdentID
    FROM prot.idents i
    WHERE i.OrgId = prot.Organisms.OrgId;

DROP TABLE prot.idents
GO

-- Bug 2195 restructure prot.FastaSequences
CREATE TABLE prot.FastaSequences
(
    FastaId INT NOT NULL,
    LookupString VARCHAR (200) NOT NULL,
    SeqId INT NULL,

    CONSTRAINT PK_FastaSequences PRIMARY KEY (LookupString, FastaId),
    CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId)
);

CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId)
CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId)

--Bug 2193
CREATE INDEX IX_SequencesSource ON prot.Sequences(SourceId);

CREATE TABLE prot.CustomAnnotationSet
(
    CustomAnnotationSetId INT IDENTITY(1, 1) NOT NULL,
    Container EntityId NOT NULL,
    Name VARCHAR(200) NOT NULL,
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,
    CustomAnnotationType VARCHAR(20) NOT NULL,
    Lsid lsidtype,

    CONSTRAINT PK_CustomAnnotationSet PRIMARY KEY (CustomAnnotationSetId),
    CONSTRAINT FK_CustomAnnotationSet_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT UQ_CustomAnnotationSet UNIQUE (Container, Name)
);

CREATE INDEX IX_CustomAnnotationSet_Container ON prot.CustomAnnotationSet(Container);

CREATE TABLE prot.CustomAnnotation
(
    CustomAnnotationId INT IDENTITY(1, 1) NOT NULL,
    CustomAnnotationSetId INT NOT NULL,
    ObjectURI LsidType NOT NULL,
    LookupString VARCHAR(200) NOT NULL,

    CONSTRAINT PK_CustomAnnotation PRIMARY KEY (CustomAnnotationId),
    CONSTRAINT fk_CustomAnnotation_CustomAnnotationSetId FOREIGN KEY (CustomAnnotationSetId) REFERENCES prot.CustomAnnotationSet(CustomAnnotationSetId),
    CONSTRAINT UQ_CustomAnnotation_LookupString_SetId UNIQUE (LookupString, CustomAnnotationSetId),
    CONSTRAINT UQ_CustomAnnotation_ObjectURI UNIQUE (ObjectURI)
);

CREATE INDEX IX_CustomAnnotation_CustomAnnotationSetId ON prot.CustomAnnotation(CustomAnnotationSetId);
GO
