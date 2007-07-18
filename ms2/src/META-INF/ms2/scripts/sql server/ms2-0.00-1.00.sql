/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

-- Tables and views used for Proteins and MS2 data

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


CREATE VIEW ms2.MS2Spectra AS
	SELECT ms2.MS2Fractions.Run AS Run, ms2.MS2SpectraData.*
	FROM ms2.MS2SpectraData INNER JOIN
	ms2.MS2Fractions ON ms2.MS2SpectraData.Fraction = ms2.MS2Fractions.Fraction
GO


-- Union of all MS2Peptides columns; alias Score1 to RawScore and SpScore, Score2 to DiffScore and DeltaCn, etc.
CREATE VIEW ms2.MS2Peptides AS
   SELECT
      ms2.MS2Fractions.Run, ms2.MS2PeptidesData.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore,
      Score1 AS SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next,
      Score3 AS B, Score4 As Y, Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 As Homology, IonPercent,
      ms2.MS2PeptidesData.Mass, DeltaMass, (ms2.MS2PeptidesData.Mass + DeltaMass) AS PrecursorMass,
      CASE WHEN ms2.MS2PeptidesData.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (ms2.MS2PeptidesData.Mass + (Charge - 1) * 1.00794)) END AS DeltaMassPPM,
      CASE WHEN Charge = 0 THEN 0 ELSE (ms2.MS2PeptidesData.Mass + DeltaMass + (Charge - 1) * 1.00794) / Charge END AS MZ,
      PeptideProphet, Peptide, ProteinHits, Protein, PrevAA, TrimmedPeptide,
      NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide, SequencePosition,
      prot.ProtSequences.Description, BestGeneName AS GeneName
   FROM ms2.MS2PeptidesData
      INNER JOIN
         ms2.MS2Fractions ON ms2.MS2PeptidesData.Fraction = ms2.MS2Fractions.Fraction
      INNER JOIN
         ms2.MS2Runs ON ms2.MS2Fractions.Run = ms2.MS2Runs.Run
      LEFT OUTER JOIN
         prot.ProteinSequences ON
            prot.ProteinSequences.LookupString = Protein AND
            prot.ProteinSequences.DataBaseId = ms2.MS2Runs.DataBaseId
      LEFT OUTER JOIN
            prot.ProtSequences ON prot.ProtSequences.SeqId = prot.ProteinSequences.SeqId
GO


CREATE VIEW ms2.MS2ExperimentRuns AS
SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId AS ExperimentRunRowId, exp.Protocol.Name AS ProtocolName
FROM ms2.MS2Runs
LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ApplicationLSID
LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID
GO


-- ProteinDataBases with some statistics (number of sequences, number of protein names, number of runs)
CREATE VIEW prot.ProteinDBs AS
	SELECT ProteinDataBase, prot.ProteinDataBases.DataBaseId, Loaded, X.Runs
	FROM prot.ProteinDataBases LEFT OUTER JOIN
		(SELECT DataBaseId, COUNT(Run) AS Runs
		FROM ms2.MS2Runs
		GROUP BY DataBaseId) X ON X.DataBaseId = prot.ProteinDataBases.DataBaseId
GO
