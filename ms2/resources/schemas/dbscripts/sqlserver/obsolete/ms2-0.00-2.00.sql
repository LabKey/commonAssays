/*
 * Copyright (c) 2009 LabKey Corporation
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
	Url VARCHAR(1000) NULL ,
	ProcessToObtain BINARY(1000) NULL,
	LastUpdate DATETIME NULL,
	InsertDate DATETIME NULL DEFAULT (getdate()),
	ModDate DATETIME NULL ,
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
	IdentTypeId INT IDENTITY (1, 1) NOT NULL ,
	Name VARCHAR(50) NOT NULL,
	CannonicalSourceId INT NULL,
	EntryDate DATETIME NOT NULL DEFAULT (getdate()) ,
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
	Hash VARCHAR(100) NULL ,
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


CREATE TABLE prot.ProteinSequences
	(
	DataBaseId INT NOT NULL,
	SequenceId INT IDENTITY (1, 1) NOT NULL,
	SequenceMass REAL NOT NULL,
	Sequence TEXT NOT NULL,
	LookupString VARCHAR (200) NOT NULL,
	SeqId INT NULL,

	CONSTRAINT PK_ProteinSequences PRIMARY KEY (SequenceId),
	CONSTRAINT UQ_ProteinSequences_DataBaseId_LookupString UNIQUE (DataBaseId, LookupString),
	CONSTRAINT FK_ProteinSequences_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.ProtSequences (SeqId)
	)
CREATE INDEX IX_ProteinSequences ON prot.ProteinSequences (DataBaseId, LookupString)
GO


CREATE TABLE prot.ProteinNames
	(
	SequenceId INT NOT NULL,
	Description VARCHAR (1000) NOT NULL 
	)
CREATE INDEX IX_ProteinNames ON prot.ProteinNames (SequenceId)
GO


EXEC sp_addapprole 'ms2', 'password'
GO

CREATE TABLE ms2.MS2Runs
	(
	-- standard fields
	_ts TIMESTAMP,
	Run INT IDENTITY(1,1),
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


CREATE TABLE ms2.MS2PeptidesData
	(
	Fraction INT NOT NULL,
	Scan INT NOT NULL,
	Charge TINYINT NOT NULL,
	Score1 REAL NOT NULL DEFAULT 0,
	Score2 REAL NOT NULL DEFAULT 0,
	Score3 REAL NOT NULL DEFAULT 0,
	Score4 REAL NULL,
	Score5 REAL NULL,
	IonPercent REAL NOT NULL,
	Mass float NOT NULL,
	DeltaMass REAL NOT NULL,
	PeptideProphet REAL NOT NULL,
	Peptide varchar (200) NOT NULL,
	PrevAA char(1) NOT NULL DEFAULT '',
	TrimmedPeptide VARCHAR(200) NOT NULL DEFAULT '',
	NextAA char(1) NOT NULL DEFAULT '',
	ProteinHits SMALLINT NOT NULL,
	SequencePosition INT NOT NULL DEFAULT 0,
	Protein VARCHAR (100) NOT NULL,
	SeqId INT NULL,

	CONSTRAINT PK_MS2PeptidesData PRIMARY KEY CLUSTERED (Fraction, Scan, Charge)
	)
CREATE INDEX IX_MS2PeptidesData_Protein ON ms2.MS2PeptidesData (Protein)
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


-- Union of all MS2Peptides columns; alias Score1 to RawScore and SpScore, Score2 to DiffScore and DeltaCn, etc.
-- ProteinDataBases with some statistics (number of sequences, number of protein names, number of runs)

/* ms2-1.00-1.10.sql */

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
	rowid int NOT NULL identity PRIMARY KEY,
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
	SET  IdentTypeId = (SELECT identtypeid FROM prot.ProtIdentTypes WHERE name='NCBI Taxonomy')

INSERT prot.ProtOrganisms (CommonName, Genus, Species)
SELECT   CommonName, Genus, Species FROM #idents
	WHERE NOT EXISTS
		(SELECT * FROM prot.ProtOrganisms PO INNER JOIN #idents i ON (PO.Genus = i.Genus AND PO.Species = i.Species))

INSERT prot.ProtIdentifiers (Identifier, IdentTypeId)
	SELECT   Identifier, IdentTypeId FROM #idents
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

--SELECT i.*, PO.orgid, PO.IdentID, PI.IdentId
-- FROM #idents i
--INNER JOIN prot.ProtOrganisms PO ON (i.genus = PO.genus AND i.species = PO.species)
--INNER JOIN prot.ProtIdentifiers PI ON (i.Identifier = PI.Identifier AND i.IdentTypeID = PI.IdentTypeId)


DROP TABLE #idents
GO

-- Create new version of MS2PeptidesData table and copy existing data.  This is faster than modifying the existing table since it adds
-- the RowId column and updates the SeqId column in a single pass
EXEC sp_rename 'ms2.MS2PeptidesData', 'MS2PeptidesDataOld'
GO

EXEC sp_rename 'ms2.PK_MS2PeptidesData', 'PK_MS2PeptidesDataOld'
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

INSERT INTO ms2.MS2PeptidesData
    SELECT old.Fraction, Scan, Charge, Score1, Score2, Score3, Score4, Score5, IonPercent, Mass, DeltaMass,
        PeptideProphet, Peptide, PrevAA, TrimmedPeptide, NextAA, ProteinHits, SequencePosition, Protein,
	    (SELECT SeqId FROM prot.ProteinSequences seq WHERE LookupString = Protein AND seq.DatabaseId = runs.DatabaseId) AS SeqId
	FROM ms2.MS2PeptidesDataOld old
	INNER JOIN ms2.MS2Fractions frac ON old.fraction = frac.fraction
	INNER JOIN ms2.MS2Runs runs ON frac.run = runs.run
GO

CREATE UNIQUE CLUSTERED INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.MS2PeptidesData(Fraction, Scan, Charge)
GO

ALTER TABLE ms2.MS2PeptidesData
    ADD CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (RowId)
GO

CREATE INDEX IX_MS2PeptidesData_Protein ON ms2.MS2PeptidesData(Protein)
GO

DROP TABLE ms2.MS2PeptidesDataOld
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

-- Index to speed up determining which SeqIds came from a given FASTA file (e.g., MS2 showAllProteins.view)
CREATE INDEX IX_ProteinSequences_SeqId ON prot.ProteinSequences(SeqId)
GO

-- Update the sequence stored in prot.ProtSequences with the sequence from prot.ProteinSequences
-- in cases where the ProtSequences one was stored incorrectly

-- For performance, first create a SeqId -> SequenceId temporary lookup table
CREATE TABLE prot._collapseseqids (SeqId INT NOT NULL PRIMARY KEY, SequenceId INT)
GO

INSERT INTO prot._collapseseqids
	SELECT SeqId, MIN(SequenceId)
		FROM prot.ProteinSequences
		WHERE SeqId IS NOT NULL
		GROUP BY SeqId
		ORDER BY SeqId
GO

-- Update the "bad" sequences
UPDATE prot.ProtSequences
    SET prot.ProtSequences.ProtSequence = ps.Sequence
    FROM prot.ProteinSequences ps INNER JOIN prot._collapseseqids c
        ON (ps.SequenceId = c.SequenceId)
    WHERE prot.ProtSequences.SeqId = c.SeqId
        AND prot.ProtSequences.ProtSequence LIKE '%[^A-Za-z]%'
GO

-- Drop the temporary table
DROP TABLE prot._collapseseqids
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

-- Generate stub quantitation summaries for existing runs (must be xpress with
-- a default mass tolerance; other params unknown)
INSERT INTO ms2.QuantSummaries (Run, AnalysisType, MassTol)
  SELECT DISTINCT(F.Run), 'xpress', 1.0
    FROM ms2.MS2Fractions F
         INNER JOIN ms2.MS2PeptidesData P ON F.Fraction = P.Fraction
         INNER JOIN ms2.Quantitation Q ON P.RowId = Q.PeptideId
GO

-- Add a QuantId from these summaries to existing peptide quantitation records
UPDATE ms2.Quantitation
   SET QuantId = (SELECT S.QuantId FROM ms2.QuantSummaries S, ms2.MS2Runs R, ms2.MS2Fractions F, ms2.MS2PeptidesData P
   WHERE ms2.Quantitation.PeptideId = P.RowId
     AND P.Fraction = F.Fraction
     AND F.Run = R.Run
     AND S.Run = R.Run)
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
EXEC sp_rename 'prot.ProteinSequences', 'FastaSequences'
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
EXEC sp_rename 'prot.FastaSequences.DataBaseId', 'FastaId', 'COLUMN'
GO

-- Drop obsolete table, PK and columns
DROP TABLE prot.ProteinNames
GO

ALTER TABLE prot.FastaSequences DROP CONSTRAINT PK_ProteinSequences
GO

-- On very old CPAS installations, IX_ProteinSequence includes SequenceMass.  Only in that case,
-- rebuild the index with two columns so we can drop SequenceMass
DECLARE @idxid int
DECLARE @objid int
DECLARE @name varchar

SELECT @objid = object_id('prot.FastaSequences')
SELECT @idxid = indexproperty(@objid, 'IX_proteinsequences', 'IndexId')

IF (col_name(@objid, indexkey_property(@objid, @idxid, 3, 'ColumnId')) IS NOT NULL)
	BEGIN
		DROP INDEX prot.FastaSequences.IX_ProteinSequences
		CREATE INDEX IX_ProteinSequences ON prot.FastaSequences (FastaId, LookupString)
	END
GO

ALTER TABLE prot.FastaSequences DROP
    COLUMN SequenceId,
    COLUMN SequenceMass,
    COLUMN Sequence
GO

-- Add column for retention time
ALTER TABLE ms2.MS2PeptidesData
    ADD RetentionTime REAL NULL
GO

/* ms2-1.60-1.70.sql */

UPDATE ms2.ms2proteingroups SET pctspectrumids = pctspectrumids / 100, percentcoverage = percentcoverage / 100
GO

-- Previous to CPAS 1.5, some runs ended up with PeptideCount = 0 & SpectrumCount = 0; this corrects those runs.
-- Use old names here to allow running this easily on 1.6 installations.

/* 7/7/08: COMMENT OUT THIS UPDATE AS PART OF VIEW REFACTORING
UPDATE ms2.MS2Runs SET
    PeptideCount = (SELECT COUNT(*) AS PepCount FROM ms2.MS2Peptides pep WHERE pep.run = ms2.MS2Runs.run),
    SpectrumCount = (SELECT COUNT(*) AS SpecCount FROM ms2.MS2Spectra spec WHERE spec.run = ms2.MS2Runs.run)
WHERE (PeptideCount = 0)
*/

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
SET NOCOUNT ON
GO
DECLARE @errsave int
SELECT @errsave=0
EXEC sp_rename 'prot.FastaSequences', 'FastaSequences_old'

CREATE TABLE prot.FastaSequences (
    FastaId int NOT NULL ,
    LookupString varchar (200) NOT NULL ,
    SeqId int NULL
     )
SELECT @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

INSERT INTO prot.FastaSequences (FastaId, LookupString,SeqId)
SELECT FastaId, LookupString,SeqId FROM prot.FastaSequences_old ORDER BY FastaId, LookupString
SELECT @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

ALTER TABLE prot.FastaSequences ADD CONSTRAINT PK_FastaSequences PRIMARY KEY CLUSTERED(FastaId,LookupString)
SELECT @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

ALTER TABLE prot.FastaSequences WITH NOCHECK ADD CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId)
SELECT @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId)
SELECT @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId)
SELECT @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

IF (@errsave =0)
    DROP TABLE prot.FastaSequences_old
GO

SET NOCOUNT OFF
GO

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