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

/* ms2-0.00-1.00.sql */

-- All tables used for MS2 data

CREATE SCHEMA prot;
SET search_path TO prot, public;

/****** ProtAnnotInsertions                                 */

CREATE TABLE ProtAnnotInsertions
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
CREATE TABLE ProtInfoSources
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
CREATE TABLE ProtAnnotationTypes
	(
	AnnotTypeId SERIAL NOT NULL,
	Name VARCHAR(50) NOT NULL,
	SourceId INT NULL,
	Description VARCHAR(200) NULL,
	EntryDate TIMESTAMP NOT NULL,
	ModDate TIMESTAMP NULL,
	Deleted INTEGER NOT NULL DEFAULT 0,

	CONSTRAINT PK_ProtAnnotationTypes PRIMARY KEY (AnnotTypeId),
    CONSTRAINT FK_ProtAnnotationTypes_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES ProtInfoSources (SourceId)
	);
CREATE UNIQUE INDEX UQ_ProtAnnotationTypes ON ProtAnnotationTypes(Name); 

/****** ProtIdentTypes                                      */
CREATE TABLE ProtIdentTypes
	(
	IdentTypeId SERIAL  NOT NULL ,
	Name VARCHAR(50) NOT NULL,
	CannonicalSourceId INT NULL,
	EntryDate TIMESTAMP NOT NULL,
	Description VARCHAR(200) NULL,
	Deleted INT NOT NULL DEFAULT 0,

	CONSTRAINT PK_ProtIdentTypes PRIMARY KEY (IdentTypeId),
    CONSTRAINT FK_ProtIdentTypes_ProtInfoSources FOREIGN KEY (CannonicalSourceId) REFERENCES ProtInfoSources (SourceId)
	);
CREATE UNIQUE INDEX UQ_ProtIdentTypes ON ProtIdentTypes(Name);


/****** ProtOrganisms                                       */
CREATE TABLE ProtOrganisms
	(
	OrgId SERIAL NOT NULL,
	CommonName VARCHAR(50) NULL,
	Genus VARCHAR(100) NOT NULL,
	Species VARCHAR(100)  NOT NULL,
	Comments VARCHAR(200) NULL,
	IdentId INT NULL, 
	Deleted INT NOT NULL DEFAULT 0,

	CONSTRAINT PK_ProtOrganisms PRIMARY KEY (OrgId)
	);
CREATE UNIQUE INDEX UQ_ProtOrganisms_Genus_Species ON ProtOrganisms(Genus,Species);


/****** ProtSequences                                       */  
CREATE TABLE ProtSequences
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
	CONSTRAINT FK_ProtSequences_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES ProtInfoSources (SourceId),
    CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES ProtOrganisms (OrgId)
	);

CREATE INDEX IX_ProtSequences_OrgId ON ProtSequences(OrgId); 
CREATE INDEX IX_ProtSequences_Hash ON  ProtSequences(Hash); 
CREATE INDEX IX_ProtSequences_BestGeneName ON ProtSequences(BestGeneName);
CREATE UNIQUE INDEX UQ_ProtSequences_Hash_OrgId ON ProtSequences(Hash, OrgId); 


/****** ProtIdentifiers                                     */ 
CREATE TABLE ProtIdentifiers
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
	CONSTRAINT FK_ProtIdentifiers_ProtIdentTypes FOREIGN KEY (IdentTypeId) REFERENCES ProtIdentTypes (IdentTypeId),
	CONSTRAINT FK_ProtIdentifiers_ProtSeqSources FOREIGN KEY (SourceId)    REFERENCES ProtInfoSources (SourceId),
	CONSTRAINT FK_ProtIdentifiers_ProtSequences  FOREIGN KEY (SeqId)       REFERENCES ProtSequences (SeqId)
	); 
CREATE INDEX IX_ProtIdentifiers_Identifier ON ProtIdentifiers(Identifier);
CREATE UNIQUE INDEX UQ_ProtIdentifiers_IdentTypeId_Identifier_SeqId ON ProtIdentifiers(IdentTypeId, Identifier, SeqId);
CREATE INDEX IX_ProtIdentifiers_IdentTypeId_Identifier_IdentId_SeqId ON ProtIdentifiers(IdentTypeId, Identifier, IdentId, SeqId); 

ALTER TABLE ProtOrganisms ADD CONSTRAINT FK_ProtOrganisms_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES ProtIdentifiers (IdentId);


/****** ProtAnnotations                                     */
CREATE TABLE ProtAnnotations
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
	CONSTRAINT FK_ProtAnnotations_ProtAnnotationTypes FOREIGN KEY (AnnotTypeId) REFERENCES ProtAnnotationTypes (AnnotTypeId),
	CONSTRAINT FK_ProtAnnotations_ProtIdentifiers FOREIGN KEY (AnnotIdent) REFERENCES ProtIdentifiers (IdentId),
	CONSTRAINT FK_ProtAnnotations_ProtSeqSources FOREIGN KEY (AnnotSourceId) REFERENCES ProtInfoSources (SourceId),
	CONSTRAINT FK_ProtAnnotations_ProtSequences FOREIGN KEY (SeqId) REFERENCES ProtSequences (SeqId)
	); 
CREATE INDEX IX_ProtAnnotations_SeqId_AnnotTypeId ON ProtAnnotations(SeqId, AnnotTypeId);
CREATE UNIQUE INDEX UQ_ProtAnnotations_AnnotTypeId_AnnotVal_SeqId_StartPos_EndPos ON ProtAnnotations(AnnotTypeId, AnnotVal, SeqId, StartPos, EndPos);


/****** ProtFastas                                          */
CREATE TABLE ProtFastas
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
 
	CONSTRAINT FK_ProtFastas_ProtSeqSources FOREIGN KEY (DbSource) REFERENCES ProtInfoSources (SourceId),
	CONSTRAINT PK_ProtFastas PRIMARY KEY (FastaId)
	);

/****** ProtSprotOrgMap                                  */
CREATE TABLE ProtSprotOrgMap
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

INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('Genbank','http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=protein&cmd=search&term={}','2005-03-04 12:08:10');
INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('NiceProt','http://au.expasy.org/cgi-bin/niceprot.pl?{}','2005-03-04 12:08:10');
INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('GeneCards','http://bioinfo.weizmann.ac.il/cards-bin/carddisp?_symbol={}','2005-03-04 12:08:10');
INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('NCBI Taxonomy','http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id={}','2005-03-04 12:08:10');
INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('GO','http://www.godatabase.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}','2005-03-04 12:08:52');

INSERT INTO ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_F',5,'2005-03-04 11:37:15');
INSERT INTO ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_P',5,'2005-03-04 11:37:15');
INSERT INTO ProtAnnotationTypes (Name,EntryDate) VALUES ('keyword','2005-03-04 11:37:15');
INSERT INTO ProtAnnotationTypes (Name,EntryDate) VALUES ('feature','2005-03-04 11:37:15');
INSERT INTO ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_C',5,'2005-03-04 11:38:13');
INSERT INTO ProtAnnotationTypes (Name,EntryDate) VALUES ('FullOrganismName',now());
INSERT INTO ProtAnnotationTypes (Name,EntryDate) VALUES ('LookupString',now());

INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('Genbank',1,'2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('SwissProt',2,'2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GeneName',3,'2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('NCBI Taxonomy',4,'2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('EMBL','2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('IntAct','2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Ensembl','2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('FlyBase','2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GO',5,'2005-03-04 11:37:14');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('InterPro','2005-03-04 11:37:15');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Pfam','2005-03-04 11:37:15');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PIR','2005-03-04 11:37:15');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Uniprot_keyword','2005-03-04 11:37:15');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SMART','2005-03-04 11:37:16');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('HSSP','2005-03-04 11:37:17');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ProDom','2005-03-04 11:37:17');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PROSITE','2005-03-04 11:37:17');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PRINTS','2005-03-04 11:37:19');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('TIGRFAMs','2005-03-04 11:37:22');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('EC','2005-03-04 11:37:22');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MaizeDB','2005-03-04 11:37:33');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('TRANSFAC','2005-03-04 11:37:34');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('WormBase','2005-03-04 11:37:38');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('WormPep','2005-03-04 11:37:39');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('COMPLUYEAST-2DPAGE','2005-03-04 11:37:39');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('DictyBase','2005-03-04 11:37:40');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Gramene','2005-03-04 11:37:45');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('OGP','2005-03-04 11:38:02');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Genew','2005-03-04 11:38:02');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('H-InvDB','2005-03-04 11:38:02');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MIM','2005-03-04 11:38:02');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MGD','2005-03-04 11:38:04');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('RGD','2005-03-04 11:38:06');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PDB','2005-03-04 11:38:10');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SWISS-2DPAGE','2005-03-04 11:38:33');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Aarhus/Ghent-2DPAGE','2005-03-04 11:38:33');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PMMA-2DPAGE','2005-03-04 11:38:45');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('TIGR','2005-03-04 11:38:49');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('TubercuList','2005-03-04 11:38:50');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Leproma','2005-03-04 11:39:05');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('GeneFarm','2005-03-04 11:39:35');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('GermOnline','2005-03-04 11:43:54');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SGD','2005-03-04 11:43:54');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('GeneDB_SPombe','2005-03-04 11:44:15');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PIRSF','2005-03-04 11:45:42');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('HAMAP','2005-03-04 11:46:49');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Reactome','2005-03-04 11:46:52');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ECO2DBASE','2005-03-04 11:46:55');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('EchoBASE','2005-03-04 11:46:55');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('EcoGene','2005-03-04 11:46:55');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SubtiList','2005-03-04 11:46:58');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ListiList','2005-03-04 11:47:14');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('GlycoSuiteDB','2005-03-04 11:47:44');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('StyGene','2005-03-04 11:51:59');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PHCI-2DPAGE','2005-03-04 11:52:19');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Siena-2DPAGE','2005-03-04 11:55:22');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('HSC-2DPAGE','2005-03-04 11:55:41');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MEROPS','2005-03-04 11:59:32');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('AGD','2005-03-04 12:14:40');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PhotoList','2005-03-04 12:15:22');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ZFIN','2005-03-04 12:15:39');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MypuList','2005-03-04 12:24:15');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SagaList','2005-03-04 12:25:40');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ANU-2DPAGE','2005-03-04 12:29:22');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Rat-heart-2DPAGE','2005-03-04 12:30:51');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PhosSite','2005-03-04 12:49:00');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('REBASE','2005-03-04 13:25:29');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Maize-2DPAGE','2005-03-04 15:10:53');
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('HIV','2005-03-04 22:13:40');

INSERT INTO ProtOrganisms (CommonName,Genus,Species,Comments) VALUES ('Unknown organism','Unknown','unknown','Organism is unknown');

CREATE TABLE ProteinDataBases
	(
	DataBaseId SERIAL,
	ProteinDataBase VARCHAR(400),
	Loaded TIMESTAMP,

	CONSTRAINT PK_ProteinDataBases PRIMARY KEY (DataBaseId)
	);


-- Special entry 0 for runs that contain no protein database
INSERT INTO ProteinDataBases (DataBaseId, ProteinDataBase, Loaded) VALUES (0, NULL, NULL);


CREATE TABLE ProteinSequences
	(
	DataBaseId INT NOT NULL,
	SequenceId SERIAL,
	SequenceMass REAL NOT NULL,
	Sequence TEXT NOT NULL,
	LookupString VARCHAR(200) NOT NULL,
    SeqId INT NULL,

	CONSTRAINT PK_ProteinSequences PRIMARY KEY (SequenceId),
	CONSTRAINT UQ_ProteinSequences_DataBaseId_LookupString UNIQUE (DataBaseId, LookupString),
    CONSTRAINT FK_ProteinSequences_ProtSequences FOREIGN KEY (SeqId) REFERENCES ProtSequences(SeqId)
	);

CREATE INDEX IX_ProteinSequences ON ProteinSequences (DataBaseId, LookupString);

CREATE TABLE ProteinNames
	(
	SequenceId INT NOT NULL,
	Description VARCHAR(1000) NOT NULL
	);

CREATE INDEX IX_ProteinNames ON ProteinNames (SequenceId);


CREATE SCHEMA ms2;
SET search_path TO ms2, public;

/**** MS2Runs                                           */
CREATE TABLE MS2Runs
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


CREATE TABLE MS2Fractions
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


CREATE TABLE MS2Modifications
	(
	Run INT NOT NULL,
	AminoAcid VARCHAR (1) NOT NULL,
	MassDiff REAL NOT NULL,
	Variable BOOLEAN NOT NULL,
	Symbol VARCHAR (1) NOT NULL,

	CONSTRAINT PK_MS2Modifications PRIMARY KEY (Run, AminoAcid, Symbol)
	);


CREATE TABLE MS2PeptidesData
	(
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

	CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (Fraction, Scan, Charge)
	);
CREATE INDEX IX_MS2PeptidesData_Protein ON MS2PeptidesData (Protein);


-- Union of all MS2Peptides columns; alias Score1 to RawScore and SpScore, Score2 to DiffScore and DeltaCn, etc.
-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE MS2SpectraData
	(
	Fraction INT NOT NULL,
	Scan INT NOT NULL,
	Spectrum BYTEA NOT NULL,

	CONSTRAINT PK_MS2SpectraData PRIMARY KEY (Fraction, Scan)
	);


CREATE TABLE MS2History
    (
    Date TIMESTAMP,
    Runs BIGINT,
    Peptides BIGINT,

    CONSTRAINT PK_MS2History PRIMARY KEY (Date)
    );


SET search_path TO prot, public;

-- ProteinDataBases with number of runs

/* ms2-1.00-1.10.sql */

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
(	Identifier varchar(50) NOT NULL,
	CommonName varchar(20) NULL,
	Genus varchar(100) NOT NULL,
	Species varchar(100) NOT NULL,
	OrgId int NULL,
	IdentId int NULL,
	IdentTypeId int NULL
)
;
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
;
UPDATE idents
	SET  IdentTypeId = (SELECT identtypeid FROM prot.ProtIdentTypes WHERE name='NCBI Taxonomy')
;
INSERT INTO prot.ProtOrganisms (CommonName, Genus, Species)
SELECT   CommonName, Genus, Species FROM idents
	WHERE NOT EXISTS
		(SELECT * FROM prot.ProtOrganisms PO INNER JOIN idents i ON (PO.Genus = i.Genus AND PO.Species = i.Species))
;
INSERT INTO prot.ProtIdentifiers (Identifier, IdentTypeId, entrydate)
	SELECT   Identifier, IdentTypeId , now() FROM idents
	WHERE NOT EXISTS
		(SELECT * FROM prot.ProtIdentifiers PI INNER JOIN idents i ON (PI.Identifier = i.Identifier AND PI.IdentTypeId = i.IdentTypeId))
;
UPDATE idents
	SET OrgId = PO.OrgId
	FROM prot.ProtOrganisms PO
	WHERE idents.Genus = PO.Genus AND idents.Species = PO.Species
;

UPDATE idents
	SET IdentId = PI.IdentId
	FROM prot.ProtIdentifiers PI
	WHERE idents.Identifier = PI.Identifier AND idents.IdentTypeId = PI.IdentTypeId
;
UPDATE prot.ProtOrganisms
	SET IdentId = i.IdentID
	FROM idents i
	WHERE i.OrgId = prot.ProtOrganisms.OrgId
;
--SELECT i.*, PO.orgid, PO.IdentID, PI.IdentId
-- FROM idents i
--INNER JOIN prot.ProtOrganisms PO ON (i.genus = PO.genus AND i.species = PO.species)
--INNER JOIN prot.ProtIdentifiers PI ON (i.Identifier = PI.Identifier AND i.IdentTypeID = PI.IdentTypeId)


DROP TABLE idents;

ALTER TABLE ms2.MS2PeptidesData RENAME TO MS2PeptidesDataOld;
ALTER INDEX ms2.PK_MS2PeptidesData RENAME TO PK_MS2PeptidesDataOld;
ALTER INDEX ms2.IX_MS2PeptidesData_Protein RENAME TO IX_MS2PeptidesData_ProteinOld;

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

INSERT INTO ms2.MS2PeptidesData (Fraction, Scan, Charge, Score1, Score2, Score3, Score4, Score5, IonPercent, Mass, DeltaMass,
		PeptideProphet, Peptide, PrevAA, TrimmedPeptide, NextAA, ProteinHits, SequencePosition, Protein, SeqId)
	SELECT ms2.MS2PeptidesDataOld.Fraction, Scan, Charge, Score1, Score2, Score3, Score4, Score5, IonPercent, Mass, DeltaMass,
		PeptideProphet, Peptide, PrevAA, TrimmedPeptide, NextAA, ProteinHits, SequencePosition, Protein,
		(SELECT SeqId FROM prot.ProteinSequences seq WHERE LookupString = Protein AND seq.DatabaseId = runs.DatabaseId) AS SeqId
	FROM ms2.MS2PeptidesDataOld
	INNER JOIN ms2.MS2Fractions frac ON ms2.MS2PeptidesDataOld.fraction = frac.fraction
	INNER JOIN ms2.MS2Runs runs ON frac.run = runs.run;

CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.MS2PeptidesData (Fraction, Scan, Charge);

ALTER TABLE ms2.MS2PeptidesData
    ADD CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (RowId);

CREATE INDEX IX_MS2PeptidesData_Protein ON ms2.MS2PeptidesData (Protein);

DROP TABLE ms2.MS2PeptidesDataOld;

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
	CONSTRAINT FK_MS2ProteinGroupMemberships_ProtSequences  FOREIGN KEY (SeqId) REFERENCES prot.ProtSequences (SeqId),
	CONSTRAINT FK_MS2ProteinGroupMembership_MS2ProteinGroups   FOREIGN KEY (ProteinGroupId) REFERENCES ms2.MS2ProteinGroups (RowId)
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

-- Index to speed up determining which SeqIds came from a given FASTA file (e.g., MS2 showAllProteins.view)
CREATE INDEX IX_ProteinSequences_SeqId ON prot.ProteinSequences(SeqId);


-- Update the sequence stored in prot.ProtSequences with the sequence from prot.ProteinSequences
-- in cases where the ProtSequences one was stored incorrectly

-- For performance, first create a SeqId -> SequenceId temporary lookup table
CREATE TABLE prot._collapseseqids (SeqId INT NOT NULL PRIMARY KEY, SequenceId INT);

INSERT INTO prot._collapseseqids
	SELECT SeqId, MIN(SequenceId)
		FROM prot.ProteinSequences
		WHERE SeqId IS NOT NULL
		GROUP BY SeqId
		ORDER BY SeqId;

-- Update the "bad" sequences
UPDATE prot.ProtSequences
    SET ProtSequence = ps.Sequence
    FROM prot.ProteinSequences ps INNER JOIN prot._collapseseqids c
        ON (ps.SequenceId = c.SequenceId)
    WHERE prot.ProtSequences.SeqId = c.SeqId
        AND prot.ProtSequences.ProtSequence SIMILAR TO '%[^A-Za-z]%';

-- Drop the temporary table
DROP TABLE prot._collapseseqids;

/* ms2-1.30-1.40.sql */

-- Store peptide and spectrum counts with each run to make computing stats much faster
ALTER TABLE ms2.MS2Runs
    ADD COLUMN PeptideCount INT NOT NULL DEFAULT 0,
    ADD COLUMN SpectrumCount INT NOT NULL DEFAULT 0;

-- Update counts for existing runs
UPDATE ms2.MS2Runs SET PeptideCount = PepCount FROM
    (SELECT Run, COUNT(*) AS PepCount FROM ms2.MS2PeptidesData pd INNER JOIN ms2.MS2Fractions f ON pd.Fraction = f.Fraction GROUP BY Run) x
WHERE ms2.MS2Runs.Run = x.Run;

UPDATE ms2.MS2Runs SET SpectrumCount = SpecCount FROM
    (SELECT Run, COUNT(*) AS SpecCount FROM ms2.MS2SpectraData sd INNER JOIN ms2.MS2Fractions f ON sd.Fraction = f.Fraction GROUP BY Run) x
WHERE ms2.MS2Runs.Run = x.Run;

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

-- Generate stub quantitation summaries for existing runs (must be xpress with
-- a default mass tolerance; other params unknown)
INSERT INTO ms2.QuantSummaries (Run, AnalysisType, MassTol)
  SELECT DISTINCT(F.Run), 'xpress', 1.0
    FROM ms2.MS2Fractions F
         INNER JOIN ms2.MS2PeptidesData P ON F.Fraction = P.Fraction
         INNER JOIN ms2.Quantitation Q ON P.RowId = Q.PeptideId;

-- Add a QuantId from these summaries to existing peptide quantitation records
UPDATE ms2.Quantitation
   SET QuantId = (SELECT S.QuantId FROM ms2.QuantSummaries S, ms2.MS2Runs R, ms2.MS2Fractions F, ms2.MS2PeptidesData P
   WHERE ms2.Quantitation.PeptideId = P.RowId
     AND P.Fraction = F.Fraction
     AND F.Run = R.Run
     AND S.Run = R.Run);

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
ALTER TABLE prot.ProteinSequences RENAME TO FastaSequences;
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
ALTER TABLE prot.FastaSequences RENAME DataBaseId TO FastaId;

-- Drop obsolete table, PK and columns
DROP TABLE prot.ProteinNames;

ALTER TABLE prot.FastaSequences DROP CONSTRAINT PK_ProteinSequences;
ALTER TABLE prot.FastaSequences
    DROP COLUMN SequenceId,
    DROP COLUMN SequenceMass,
    DROP COLUMN Sequence;

-- Add column for retention time
ALTER TABLE ms2.MS2PeptidesData
    ADD COLUMN RetentionTime REAL NULL;

/* ms2-1.60-1.70.sql */

UPDATE ms2.ms2proteingroups SET pctspectrumids = pctspectrumids / 100, percentcoverage = percentcoverage / 100;

-- Previous to CPAS 1.5, some runs ended up with PeptideCount = 0 & SpectrumCount = 0; this corrects those runs.

/* 7/7/08: COMMENT OUT THIS UPDATE AS PART OF VIEW REFACTORING
UPDATE ms2.MS2Runs SET
    PeptideCount = (SELECT COUNT(*) AS PepCount FROM ms2.MS2Peptides pep WHERE pep.run = ms2.MS2Runs.run),
    SpectrumCount = (SELECT COUNT(*) AS SpecCount FROM ms2.MS2Spectra spec WHERE spec.run = ms2.MS2Runs.run)
WHERE (PeptideCount = 0);
*/

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

-- Bug 2195 restructure prot.FastaSequences
    ALTER TABLE prot.FastaSequences RENAME TO FastaSequences_old;

CREATE TABLE prot.FastaSequences
(
    FastaId int NOT NULL,
    LookupString varchar (200) NOT NULL,
    SeqId int NULL
);

INSERT INTO prot.FastaSequences (FastaId, LookupString, SeqId)
    SELECT FastaId, LookupString,SeqId FROM prot.FastaSequences_old ORDER BY FastaId, LookupString;

ALTER TABLE prot.FastaSequences ADD CONSTRAINT PK_FastaSequences PRIMARY KEY (FastaId,LookupString);
ALTER TABLE prot.FastaSequences ADD CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId);
CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId);
CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId);
DROP TABLE prot.FastaSequences_old;

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

SELECT core.fn_dropifexists ('ProteinQuantitation', 'ms2', 'INDEX', 'IX_ProteinQuantitation_ProteinGroupId')
;
SELECT core.fn_dropifexists ('PeptideMemberships', 'ms2', 'INDEX', 'IX_Peptidemembership_ProteingroupId')
;
SELECT core.fn_dropifexists ('PeptidesData', 'ms2', 'INDEX', 'IX_PeptidesData_SeqId')
;
SELECT core.fn_dropifexists ('ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId')
;
-- redundant after the restructure of the UQ constraint
SELECT core.fn_dropifexists ('ProteinGroups', 'ms2', 'INDEX', 'ProteinGroups.IX_ProteinGroups_ProteinProphetFileId')
;

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId)
;
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId)
;

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId)

;
CREATE  INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability)
;

-- make PPfileid the left-most column in the index so that results by run can be found
ALTER TABLE ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups
;

ALTER TABLE ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE
	(
		ProteinProphetFileId,
		GroupNumber,
		IndistinguishableCollectionId
	)
;

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