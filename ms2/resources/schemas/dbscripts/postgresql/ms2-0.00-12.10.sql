/*
 * Copyright (c) 2011-2014 LabKey Corporation
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

/* ms2-0.00-9.20.sql */

-- All tables used for MS2 data

CREATE SCHEMA prot;
CREATE SCHEMA ms2;

/****** AnnotInsertions                                 */

CREATE TABLE prot.AnnotInsertions
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

/****** InfoSources                                     */
CREATE TABLE prot.InfoSources
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

/*** Initializations */

INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('Genbank', 'http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=protein&cmd=search&term={}', '2005-03-04 12:08:10');
INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('NiceProt', 'http://au.expasy.org/cgi-bin/niceprot.pl?{}', '2005-03-04 12:08:10');
INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('GeneCards', 'http://www.genecards.org/cgi-bin/carddisp?{}&alias=yes', '2005-03-04 12:08:10');
INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('NCBI Taxonomy', 'http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id={}', '2005-03-04 12:08:10');
INSERT INTO prot.InfoSources (Name, Url, InsertDate) VALUES ('GO', 'http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}', '2005-03-04 12:08:52');

/****** AnnotationTypes                                 */
CREATE TABLE prot.AnnotationTypes
(
    AnnotTypeId SERIAL NOT NULL,
    Name VARCHAR(50) NOT NULL,
    SourceId INT NULL,
    Description VARCHAR(200) NULL,
    EntryDate TIMESTAMP NOT NULL,
    ModDate TIMESTAMP NULL,
    Deleted INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtAnnotationTypes PRIMARY KEY (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotationTypes_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.InfoSources (SourceId)
);
CREATE UNIQUE INDEX UQ_ProtAnnotationTypes ON prot.AnnotationTypes(Name);

INSERT INTO prot.AnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_F',5,'2005-03-04 11:37:15');
INSERT INTO prot.AnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_P',5,'2005-03-04 11:37:15');
INSERT INTO prot.AnnotationTypes (Name,EntryDate) VALUES ('keyword','2005-03-04 11:37:15');
INSERT INTO prot.AnnotationTypes (Name,EntryDate) VALUES ('feature','2005-03-04 11:37:15');
INSERT INTO prot.AnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_C',5,'2005-03-04 11:38:13');
INSERT INTO prot.AnnotationTypes (Name,EntryDate) VALUES ('FullOrganismName',now());
INSERT INTO prot.AnnotationTypes (Name,EntryDate) VALUES ('LookupString',now());

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId);

/****** IdentTypes                                      */
CREATE TABLE prot.IdentTypes
(
    IdentTypeId SERIAL NOT NULL,
    Name VARCHAR(50) NOT NULL,
    CannonicalSourceId INT NULL,
    EntryDate TIMESTAMP NOT NULL,
    Description VARCHAR(200) NULL,
    Deleted INT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtIdentTypes PRIMARY KEY (IdentTypeId),
    CONSTRAINT FK_ProtIdentTypes_ProtInfoSources FOREIGN KEY (CannonicalSourceId) REFERENCES prot.InfoSources (SourceId)
);

CREATE UNIQUE INDEX UQ_ProtIdentTypes ON prot.IdentTypes(Name);
CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid);

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

/****** Organisms                                       */
CREATE TABLE prot.Organisms
(
    OrgId SERIAL NOT NULL,
    CommonName VARCHAR(100) NULL,
    Genus VARCHAR(100) NOT NULL,
    Species VARCHAR(100) NOT NULL,
    Comments VARCHAR(200) NULL,
    IdentId INT NULL,
    Deleted INT NOT NULL DEFAULT 0,

    CONSTRAINT PK_ProtOrganisms PRIMARY KEY (OrgId)
);
CREATE UNIQUE INDEX UQ_ProtOrganisms_Genus_Species ON prot.Organisms(Genus, Species);

INSERT INTO prot.Organisms (CommonName,Genus,Species,Comments) VALUES ('Unknown organism','Unknown','unknown','Organism is unknown');

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId);

/****** Sequences                                       */
CREATE TABLE prot.Sequences
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
    CONSTRAINT FK_ProtSequences_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES prot.InfoSources (SourceId),
    CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES prot.Organisms (OrgId)
);

CREATE INDEX IX_ProtSequences_OrgId ON prot.Sequences(OrgId);
CREATE INDEX IX_ProtSequences_BestGeneName ON prot.Sequences(BestGeneName);
CREATE UNIQUE INDEX UQ_ProtSequences_Hash_OrgId ON prot.Sequences(Hash, OrgId);

--Bug 2193
CREATE INDEX IX_SequencesSource ON prot.Sequences(SourceId);

/****** Identifiers                                     */
CREATE TABLE prot.Identifiers
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
    CONSTRAINT FK_ProtIdentifiers_ProtIdentTypes FOREIGN KEY (IdentTypeId) REFERENCES prot.IdentTypes (IdentTypeId),
    CONSTRAINT FK_ProtIdentifiers_ProtSeqSources FOREIGN KEY (SourceId)    REFERENCES prot.InfoSources (SourceId),
    CONSTRAINT FK_ProtIdentifiers_ProtSequences  FOREIGN KEY (SeqId)       REFERENCES prot.Sequences (SeqId)
);
CREATE INDEX IX_ProtIdentifiers_Identifier ON prot.Identifiers(Identifier);
CREATE UNIQUE INDEX UQ_ProtIdentifiers_IdentTypeId_Identifier_SeqId ON prot.Identifiers(IdentTypeId, Identifier, SeqId);

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid);
CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId);

ALTER TABLE prot.Organisms ADD CONSTRAINT FK_ProtOrganisms_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES prot.Identifiers (IdentId);

/****** Annotations                                     */
CREATE TABLE prot.Annotations
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
    CONSTRAINT FK_ProtAnnotations_ProtAnnotationTypes FOREIGN KEY (AnnotTypeId) REFERENCES prot.AnnotationTypes (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotations_ProtIdentifiers FOREIGN KEY (AnnotIdent) REFERENCES prot.Identifiers (IdentId),
    CONSTRAINT FK_ProtAnnotations_ProtSeqSources FOREIGN KEY (AnnotSourceId) REFERENCES prot.InfoSources (SourceId),
    CONSTRAINT FK_ProtAnnotations_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId)
);
CREATE INDEX IX_ProtAnnotations_SeqId_AnnotTypeId ON prot.Annotations(SeqId, AnnotTypeId);
CREATE UNIQUE INDEX UQ_ProtAnnotations_AnnotTypeId_AnnotVal_SeqId_StartPos_EndPos ON prot.Annotations(AnnotTypeId, AnnotVal, SeqId, StartPos, EndPos);

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval);

CREATE INDEX IX_Annotations_AnnotIdent ON prot.Annotations(annotident);

CREATE INDEX IX_Annotations_annotsourceid ON prot.Annotations(annotsourceid);

CREATE INDEX IX_Annotations_IdentId ON prot.Annotations(AnnotIdent);

/****** FastaLoads                                          */
CREATE TABLE prot.FastaLoads
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
 
    CONSTRAINT FK_ProtFastas_ProtSeqSources FOREIGN KEY (DbSource) REFERENCES prot.InfoSources (SourceId),
    CONSTRAINT PK_ProtFastas PRIMARY KEY (FastaId)
);

CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource);

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
    FastaId SERIAL,
    FileName VARCHAR(400),
    Loaded TIMESTAMP,
    FileChecksum VARCHAR(50) NULL,         -- hash of the file
    ScoringAnalysis BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT PK_ProteinDataBases PRIMARY KEY (FastaId)
);

/**** Runs                                           */
CREATE TABLE ms2.Runs
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
    FastaId INT NOT NULL DEFAULT 0,
    SearchEnzyme VARCHAR(50),
    Deleted BOOLEAN NOT NULL DEFAULT '0',
    ExperimentRunLSID LSIDType NULL,
    HasPeptideProphet BOOLEAN NOT NULL DEFAULT '0',
    PeptideCount INT NOT NULL DEFAULT 0,    -- Store peptide and spectrum counts with each run to make computing stats much faster
    SpectrumCount INT NOT NULL DEFAULT 0,
    NegativeHitCount INT NOT NULL DEFAULT 0,
    
    CONSTRAINT PK_MS2Runs PRIMARY KEY (Run)
);

-- Create indexes on ms2 Runs table to support common operations in MS2Manager

CREATE INDEX MS2Runs_Stats ON ms2.Runs(PeptideCount, SpectrumCount, Deleted, StatusId);
CREATE INDEX MS2Runs_ExperimentRunLSID ON ms2.Runs(ExperimentRunLSID);
CREATE INDEX MS2Runs_Container ON ms2.Runs(Container);

CREATE TABLE ms2.Fractions
(
    Fraction SERIAL,
    Run INT NOT NULL,
    Description VARCHAR(300),
    FileName VARCHAR(300),
    HydroB0 REAL,
    HydroB1 REAL,
    HydroR2 REAL,
    HydroSigma REAL,
    PepXmlDataLSID LSIDType NULL,
    MzXmlURL VARCHAR(400) NULL,

    CONSTRAINT PK_MS2Fractions PRIMARY KEY (Fraction)
);

CREATE INDEX IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction);

CREATE INDEX IX_Fractions_MzXMLURL ON ms2.fractions(mzxmlurl);

CREATE TABLE ms2.Modifications
(
    Run INT NOT NULL,
    AminoAcid VARCHAR (1) NOT NULL,
    MassDiff REAL NOT NULL,
    Variable BOOLEAN NOT NULL,
    Symbol VARCHAR (1) NOT NULL,

    CONSTRAINT PK_MS2Modifications PRIMARY KEY (Run, AminoAcid, Symbol)
);

-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE ms2.SpectraData
(
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Spectrum BYTEA NOT NULL,

    CONSTRAINT PK_MS2SpectraData PRIMARY KEY (Fraction, Scan)
);

CREATE TABLE ms2.History
(
    Date TIMESTAMP,
    Runs BIGINT,
    Peptides BIGINT
);

CREATE INDEX IX_MS2History ON ms2.History (Date);

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

    CONSTRAINT pk_goterm PRIMARY KEY(id)
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

    CONSTRAINT pk_goterm2term PRIMARY KEY(id)
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

    CONSTRAINT pk_gographpath PRIMARY KEY(id)
);
 
CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id);
CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id);
CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id);
CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance);

-- GO term definitions

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
    termSynonym VARCHAR(500) NULL DEFAULT NULL,
    accSynonym VARCHAR(255) NULL DEFAULT NULL,
    synonymTypeId INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId);
CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId);
CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym);
CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym);

-- add most common ncbi Taxonomy id's

CREATE TEMPORARY TABLE idents
(
    Identifier VARCHAR(50) NOT NULL,
    CommonName VARCHAR(20) NULL,
    Genus VARCHAR(100) NOT NULL,
    Species VARCHAR(100) NOT NULL,
    OrgId INT NULL,
    IdentId INT NULL,
    IdentTypeId INT NULL
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
    SET IdentTypeId = (SELECT identtypeid FROM prot.IdentTypes WHERE name='NCBI Taxonomy');

INSERT INTO prot.Organisms (CommonName, Genus, Species)
    SELECT CommonName, Genus, Species FROM idents
        WHERE NOT EXISTS
            (SELECT * FROM prot.Organisms PO INNER JOIN idents i ON (PO.Genus = i.Genus AND PO.Species = i.Species));

INSERT INTO prot.Identifiers (Identifier, IdentTypeId, entrydate)
    SELECT Identifier, IdentTypeId , now() FROM idents
    WHERE NOT EXISTS
        (SELECT * FROM prot.Identifiers PI INNER JOIN idents i ON (PI.Identifier = i.Identifier AND PI.IdentTypeId = i.IdentTypeId));

UPDATE idents
    SET OrgId = PO.OrgId
    FROM prot.Organisms PO
    WHERE idents.Genus = PO.Genus AND idents.Species = PO.Species;

UPDATE idents
    SET IdentId = PI.IdentId
    FROM prot.Identifiers PI
    WHERE idents.Identifier = PI.Identifier AND idents.IdentTypeId = PI.IdentTypeId;

UPDATE prot.Organisms
    SET IdentId = i.IdentID
    FROM idents i
    WHERE i.OrgId = Organisms.OrgId;

DROP TABLE idents;

CREATE TABLE ms2.PeptidesData
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
    SeqId INT NULL,
    RetentionTime REAL NULL,
    PeptideProphetErrorRate REAL NULL,
    EndScan INT NULL,

    CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (RowId)
);

CREATE INDEX IX_MS2PeptidesData_Protein ON ms2.PeptidesData (Protein);
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId);
CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData (Fraction, Scan, EndScan, Charge);

CREATE INDEX IX_MS2PeptidesData_TrimmedPeptide ON ms2.PeptidesData(TrimmedPeptide);
CREATE INDEX IX_MS2PeptidesData_Peptide ON ms2.PeptidesData(Peptide);

CREATE TABLE ms2.ProteinProphetFiles
(
    RowId SERIAL NOT NULL,
    FilePath VARCHAR(255) NOT NULL,
    Run INT NOT NULL,
    UploadCompleted BOOLEAN NOT NULL DEFAULT '0',
    MinProbSeries BYTEA NULL,
    SensitivitySeries BYTEA NULL,
    ErrorSeries BYTEA NULL,
    PredictedNumberCorrectSeries BYTEA NULL,
    PredictedNumberIncorrectSeries BYTEA NULL,

    CONSTRAINT PK_MS2ProteinProphetFiles PRIMARY KEY (RowId),
    CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.Runs(Run),
    CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run)
);

CREATE TABLE ms2.ProteinGroups
(
    RowId SERIAL NOT NULL,
    GroupProbability REAL NOT NULL,
    ProteinProphetFileId INT NOT NULL,
    GroupNumber INT NOT NULL,
    IndistinguishableCollectionId INT NOT NULL,
    UniquePeptidesCount INT NOT NULL,
    TotalNumberPeptides INT NOT NULL,
    PctSpectrumIds REAL NULL,
    PercentCoverage REAL NULL,
    ProteinProbability REAL NOT NULL DEFAULT 0,
    ErrorRate REAL NULL,

    CONSTRAINT PK_MS2ProteinGroups PRIMARY KEY (RowId),
    CONSTRAINT UQ_MS2ProteinGroups UNIQUE (ProteinProphetFileId, GroupNumber, IndistinguishableCollectionId),
    CONSTRAINT FK_MS2ProteinGroup_MS2ProteinProphetFileId FOREIGN KEY (ProteinProphetFileId) REFERENCES ms2.ProteinProphetFiles(RowId)
);

CREATE TABLE ms2.ProteinGroupMemberships
(
    ProteinGroupId INT NOT NULL,
    SeqId INT NOT NULL,
    Probability REAL NOT NULL,

    CONSTRAINT PK_MS2ProteinGroupMemberships PRIMARY KEY (ProteinGroupId, SeqId),
    CONSTRAINT FK_MS2ProteinGroupMemberships_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId),
    CONSTRAINT FK_MS2ProteinGroupMembership_MS2ProteinGroups FOREIGN KEY (ProteinGroupId) REFERENCES ms2.ProteinGroups (RowId)
);

CREATE INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability);

CREATE TABLE ms2.PeptideMemberships
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
    CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.PeptidesData (rowid),
    CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ProteinGroups (rowid)
);

-- Index to speed up deletes from PeptidesData
CREATE INDEX IX_MS2PeptideMemberships_PeptideId ON ms2.PeptideMemberships(PeptideId);

CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId);

CREATE TABLE ms2.Quantitation
(
    PeptideId BIGINT NOT NULL,
    LightFirstscan INT NOT NULL,
    LightLastscan INT NOT NULL,
    LightMass REAL NOT NULL,
    HeavyFirstscan INT NOT NULL,
    HeavyLastscan INT NOT NULL,
    HeavyMass REAL NOT NULL,
    Ratio VARCHAR(20) NULL,                -- q3 does not generate string representations of ratios
    Heavy2lightRatio VARCHAR(20) NULL,     -- q3 does not generate string representations of ratios.
    LightArea REAL NOT NULL,
    HeavyArea REAL NOT NULL,
    DecimalRatio REAL NOT NULL,
    QuantId INT NOT NULL,                  -- QuantId must be non-null; eventually (PeptideId, QuantId) should become a compound PK

    CONSTRAINT PK_Quantitation PRIMARY KEY (PeptideId),
    CONSTRAINT FK_Quantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.PeptidesData(RowId)
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
    CONSTRAINT FK_ProteinQuantitation_ProteinGroup FOREIGN KEY (ProteinGroupId) REFERENCES ms2.ProteinGroups (RowId)
);

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId);

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
    CONSTRAINT FK_QuantSummaries_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.Runs (Run),
    -- Current restrictions allow only one quantitation analysis per run
    CONSTRAINT UQ_QuantSummaries UNIQUE (Run)
);

CREATE TABLE ms2.PeptideProphetData
(
    PeptideId BIGINT NOT NULL,
    ProphetFVal REAL NOT NULL,
    ProphetDeltaMass REAL NULL,
    ProphetNumTrypticTerm INT NULL,
    ProphetNumMissedCleav INT NULL,

    CONSTRAINT PK_PeptideProphetData PRIMARY KEY (PeptideId),
    CONSTRAINT FK_PeptideProphetData_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.PeptidesData(RowId)
);

CREATE TABLE prot.FastaSequences
(
    FastaId INT NOT NULL,
    LookupString VARCHAR (200) NOT NULL,
    SeqId INT NULL,

    CONSTRAINT PK_FastaSequences PRIMARY KEY(LookupString, FastaId),
    CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId)
);

CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId);
CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId);

CREATE TABLE prot.CustomAnnotationSet
(
    CustomAnnotationSetId SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    Name VARCHAR(200) NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP without time zone,
    ModifiedBy USERID,
    Modified TIMESTAMP without time zone,
    CustomAnnotationType VARCHAR(20) NOT NULL,
    Lsid lsidtype,

    CONSTRAINT pk_customannotationset PRIMARY KEY (CustomAnnotationSetId),
    CONSTRAINT fk_CustomAnnotationSet_Container FOREIGN KEY (container) REFERENCES core.containers(EntityId),
    CONSTRAINT uq_CustomAnnotationSet UNIQUE (Container, Name)
);

CREATE INDEX IX_CustomAnnotationSet_Container ON prot.CustomAnnotationSet(Container);

CREATE TABLE prot.CustomAnnotation
(
    CustomAnnotationId SERIAL NOT NULL,
    CustomAnnotationSetId INT NOT NULL,
    ObjectURI LsidType NOT NULL,
    LookupString VARCHAR(200) NOT NULL,

    CONSTRAINT pk_customannotation PRIMARY KEY (CustomAnnotationId),
    CONSTRAINT fk_CustomAnnotation_CustomAnnotationSetId FOREIGN KEY (CustomAnnotationSetId) REFERENCES prot.CustomAnnotationSet(CustomAnnotationSetId),
    CONSTRAINT UQ_CustomAnnotation_LookupString_SetId UNIQUE (LookupString, CustomAnnotationSetId),
    CONSTRAINT UQ_CustomAnnotation_ObjectURI UNIQUE (ObjectURI)
);

CREATE INDEX IX_CustomAnnotation_CustomAnnotationSetId ON prot.CustomAnnotation(CustomAnnotationSetId);

UPDATE exp.DataInput SET Role = 'Spectra' WHERE Role = 'mzXML';

-- Create functions to drop & create all GO indexes.  This helps with load performance.
CREATE FUNCTION prot.create_go_indexes() RETURNS void AS $$
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
    $$ LANGUAGE plpgsql;

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

/* ms2-9.20-9.30.sql */

UPDATE prot.InfoSources SET Url = 'http://www.genecards.org/cgi-bin/carddisp.pl?gene={}'
    WHERE Name = 'GeneCards';

ALTER TABLE ms2.peptidesdata ALTER score1 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score1 DROP DEFAULT;

ALTER TABLE ms2.peptidesdata ALTER score2 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score2 DROP DEFAULT;

ALTER TABLE ms2.peptidesdata ALTER score3 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score3 DROP DEFAULT;

/* ms2-10.20-10.30.sql */

ALTER TABLE ms2.Quantitation ADD COLUMN Invalidated BOOLEAN;

/* ms2-11.10-11.20.sql */

ALTER TABLE ms2.Fractions ADD COLUMN ScanCount INT;
ALTER TABLE ms2.Fractions ADD COLUMN MS1ScanCount INT;
ALTER TABLE ms2.Fractions ADD COLUMN MS2ScanCount INT;
ALTER TABLE ms2.Fractions ADD COLUMN MS3ScanCount INT;
ALTER TABLE ms2.Fractions ADD COLUMN MS4ScanCount INT;

/* ms2-11.20-11.30.sql */

CREATE TABLE ms2.iTraqPeptideQuantitation
(
    PeptideId BIGINT NOT NULL,
    TargetMass1 REAL,
    AbsoluteMass1 REAL,
    Normalized1 REAL,
    TargetMass2 REAL,
    AbsoluteMass2 REAL,
    Normalized2 REAL,
    TargetMass3 REAL,
    AbsoluteMass3 REAL,
    Normalized3 REAL,
    TargetMass4 REAL,
    AbsoluteMass4 REAL,
    Normalized4 REAL,
    TargetMass5 REAL,
    AbsoluteMass5 REAL,
    Normalized5 REAL,
    TargetMass6 REAL,
    AbsoluteMass6 REAL,
    Normalized6 REAL,
    TargetMass7 REAL,
    AbsoluteMass7 REAL,
    Normalized7 REAL,
    TargetMass8 REAL,
    AbsoluteMass8 REAL,
    Normalized8 REAL,

    CONSTRAINT PK_iTraqPeptideQuantitation PRIMARY KEY (PeptideId),
    CONSTRAINT FK_iTraqPeptideQuantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.PeptidesData(RowId)
);

CREATE TABLE ms2.iTraqProteinQuantitation
(
    ProteinGroupId BIGINT NOT NULL,
    Ratio1 REAL,
    Error1 REAL,
    Ratio2 REAL,
    Error2 REAL,
    Ratio3 REAL,
    Error3 REAL,
    Ratio4 REAL,
    Error4 REAL,
    Ratio5 REAL,
    Error5 REAL,
    Ratio6 REAL,
    Error6 REAL,
    Ratio7 REAL,
    Error7 REAL,
    Ratio8 REAL,
    Error8 REAL,

    CONSTRAINT PK_iTraqProteinQuantitation PRIMARY KEY (ProteinGroupId),
    CONSTRAINT FK_iTraqProteinQuantitation_ProteinGroups FOREIGN KEY (ProteinGroupId) REFERENCES ms2.ProteinGroups(RowId)
);

ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass1 TO AbsoluteIntensity1;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass2 TO AbsoluteIntensity2;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass3 TO AbsoluteIntensity3;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass4 TO AbsoluteIntensity4;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass5 TO AbsoluteIntensity5;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass6 TO AbsoluteIntensity6;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass7 TO AbsoluteIntensity7;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass8 TO AbsoluteIntensity8;
