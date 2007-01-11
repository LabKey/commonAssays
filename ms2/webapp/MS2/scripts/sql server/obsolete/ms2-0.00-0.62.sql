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

-- Tables and views used for MS2 data

EXEC sp_addtype 'ENTITYID', 'UNIQUEIDENTIFIER'
EXEC sp_addtype 'USERID', 'INT'
GO


/****** ProtAnnotInsertions                                 */
CREATE TABLE dbo.ProtAnnotInsertions
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
CREATE TABLE ProtInfoSources
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
CREATE TABLE ProtAnnotationTypes
	(
	AnnotTypeId INT IDENTITY (1, 1) NOT NULL,
	Name VARCHAR(50) NOT NULL,
	SourceId INT NULL,
	Description VARCHAR(200) NULL,
	EntryDate DATETIME NOT NULL DEFAULT (getdate()),
	ModDate DATETIME NULL,
	Deleted BIT NOT NULL DEFAULT 0,

	CONSTRAINT PK_ProtAnnotationTypes PRIMARY KEY CLUSTERED (AnnotTypeId),
        CONSTRAINT FK_ProtAnnotationTypes_ProtSeqSources FOREIGN KEY (SourceId) REFERENCES ProtInfoSources (SourceId)
	) 
CREATE UNIQUE INDEX IX_ProtAnnotationTypes ON ProtAnnotationTypes(Name) 
GO


/****** ProtIdentTypes                                      */
CREATE TABLE ProtIdentTypes
	(
	IdentTypeId INT IDENTITY (1, 1) NOT NULL ,
	Name VARCHAR(50) NOT NULL,
	CannonicalSourceId INT NULL,
	EntryDate DATETIME NOT NULL DEFAULT (getdate()) ,
	Description VARCHAR(200) NULL,
	Deleted BIT NOT NULL DEFAULT 0,

	CONSTRAINT PK_ProtIdentTypes PRIMARY KEY CLUSTERED (IdentTypeId),
        CONSTRAINT FK_ProtIdentTypes_ProtInfoSources FOREIGN KEY (CannonicalSourceId) REFERENCES ProtInfoSources (SourceId)
	) 
CREATE UNIQUE INDEX IX_ProtIdentTypes ON ProtIdentTypes(Name) 
GO


/****** ProtSequences                                       */  
CREATE TABLE ProtSequences
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
	CONSTRAINT FKProtSequencesProtSeqSources FOREIGN KEY (SourceId) REFERENCES ProtInfoSources (SourceId)
	) 
CREATE INDEX IX_SequencesOrg ON ProtSequences(OrgId) 
CREATE INDEX IX_SeqHash ON ProtSequences(Hash) 
CREATE INDEX IX_ProtSequences_BestGeneName ON ProtSequences(BestGeneName)
CREATE UNIQUE INDEX IX_ProtSequencesSurrogateKey ON ProtSequences(Hash, OrgId) 
GO


/****** ProtIdentifiers                                     */ 
CREATE TABLE ProtIdentifiers
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
	CONSTRAINT FKProtIdentifiersProtIdentTypes FOREIGN KEY (IdentTypeId) REFERENCES ProtIdentTypes (IdentTypeId),
	CONSTRAINT FKProtIdentifiersProtSeqSources FOREIGN KEY (SourceId) REFERENCES ProtInfoSources (SourceId),
	CONSTRAINT FKProtIdentifiersProtSequences FOREIGN KEY (SeqId) REFERENCES ProtSequences (SeqId) ON DELETE CASCADE
	) 
CREATE INDEX IX_Identifier on ProtIdentifiers(Identifier)
CREATE UNIQUE INDEX IX_ProtIdentifiers ON ProtIdentifiers(IdentTypeId, Identifier, SeqId) 
CREATE INDEX IX_ProtIdentifiers1 ON ProtIdentifiers(IdentTypeId, Identifier, IdentId, SeqId) 
GO


/****** ProtOrganisms                                       */
CREATE TABLE ProtOrganisms
	(
	OrgId INT IDENTITY (1, 1) NOT NULL,
	CommonName VARCHAR(50) NULL,
	Genus VARCHAR(100) NOT NULL,
	Species VARCHAR(100)  NOT NULL,
	Comments VARCHAR(200) NULL,
	IdentId INT NULL, 
	Deleted BIT NOT NULL DEFAULT 0,

	CONSTRAINT PK_Organism PRIMARY KEY CLUSTERED (OrgId),
	CONSTRAINT FK_Organism_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES ProtIdentifiers (IdentId),
	CONSTRAINT FKProtSequencesOrganism FOREIGN KEY (OrgId) REFERENCES ProtOrganisms (OrgId)
	) 
CREATE UNIQUE INDEX IX_ProtOrganismsSurrogateKey ON ProtOrganisms(Genus,Species) 
GO


/****** ProtAnnotations                                     */
CREATE TABLE ProtAnnotations
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
	CONSTRAINT FK_ProtAnnotations_ProtAnnotationTypes FOREIGN KEY (AnnotTypeId) REFERENCES ProtAnnotationTypes (AnnotTypeId),
	CONSTRAINT FKProtAnnotationsProtIdentifiers FOREIGN KEY (AnnotIdent) REFERENCES ProtIdentifiers (IdentId),
	CONSTRAINT FKProtAnnotationsProtSeqSources FOREIGN KEY (AnnotSourceId) REFERENCES ProtInfoSources (SourceId),
	CONSTRAINT FKProtAnnotationsProtSequences FOREIGN KEY (SeqId) REFERENCES ProtSequences (SeqId)
	) 
CREATE INDEX IX_ProtAnnotations1 ON ProtAnnotations(SeqId, AnnotTypeId) 
CREATE UNIQUE INDEX IX_AnnotSurrogateKey ON ProtAnnotations(AnnotTypeId, AnnotVal, SeqId, StartPos, EndPos) 
GO


/****** ProtFastas                                          */
CREATE TABLE dbo.ProtFastas
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
 
	CONSTRAINT FK_ProtFastas_ProtSeqSources FOREIGN KEY (DbSource) REFERENCES ProtInfoSources (SourceId),
	CONSTRAINT PK_ProtFastas PRIMARY KEY CLUSTERED (FastaId)
	)
GO

/****** ProtSprotOrgMap                                  */
CREATE TABLE dbo.ProtSprotOrgMap
	(
	SprotSuffix VARCHAR(50) COLLATE SQL_Latin1_General_CP437_CS_AS NOT NULL,
	OrgId INT NOT NULL,
	
	CONSTRAINT FK_ProtSprotOrgMap_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES dbo.ProtOrganisms (OrgId)
	)
CREATE INDEX IX_SprotSuffix ON ProtSprotOrgMap (SprotSuffix)
GO


INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('Genbank','http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=protein&cmd=search&term={}','2005-03-04 12:08:10')
INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('NiceProt','http://au.expasy.org/cgi-bin/niceprot.pl?{}','2005-03-04 12:08:10')
INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('GeneCards','http://bioinfo.weizmann.ac.il/cards-bin/carddisp?_symbol={}','2005-03-04 12:08:10')
INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('NCBI Taxonomy','http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id={}','2005-03-04 12:08:10')
INSERT INTO ProtInfoSources (Name,Url,InsertDate) VALUES ('GO','http://www.godatabase.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}','2005-03-04 12:08:52')
GO

INSERT INTO ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_F',5,'2005-03-04 11:37:15')
INSERT INTO ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_P',5,'2005-03-04 11:37:15')
INSERT INTO ProtAnnotationTypes (Name,EntryDate) VALUES ('keyword','2005-03-04 11:37:15')
INSERT INTO ProtAnnotationTypes (Name,EntryDate) VALUES ('feature','2005-03-04 11:37:15')
INSERT INTO ProtAnnotationTypes (Name,SourceId,EntryDate) VALUES ('GO_C',5,'2005-03-04 11:38:13')
INSERT INTO ProtAnnotationTypes (Name) VALUES ('FullOrganismName')
INSERT INTO ProtAnnotationTypes (Name) VALUES ('LookupString')
GO

INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('Genbank',1,'2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('SwissProt',2,'2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GeneName',3,'2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('NCBI Taxonomy',4,'2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('EMBL','2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('IntAct','2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Ensembl','2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('FlyBase','2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,CannonicalSourceId,EntryDate) VALUES ('GO',5,'2005-03-04 11:37:14')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('InterPro','2005-03-04 11:37:15')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Pfam','2005-03-04 11:37:15')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PIR','2005-03-04 11:37:15')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Uniprot_keyword','2005-03-04 11:37:15')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SMART','2005-03-04 11:37:16')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('HSSP','2005-03-04 11:37:17')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ProDom','2005-03-04 11:37:17')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PROSITE','2005-03-04 11:37:17')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PRINTS','2005-03-04 11:37:19')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('TIGRFAMs','2005-03-04 11:37:22')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('EC','2005-03-04 11:37:22')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MaizeDB','2005-03-04 11:37:33')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('TRANSFAC','2005-03-04 11:37:34')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('WormBase','2005-03-04 11:37:38')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('WormPep','2005-03-04 11:37:39')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('COMPLUYEAST-2DPAGE','2005-03-04 11:37:39')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('DictyBase','2005-03-04 11:37:40')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Gramene','2005-03-04 11:37:45')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('OGP','2005-03-04 11:38:02')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Genew','2005-03-04 11:38:02')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('H-InvDB','2005-03-04 11:38:02')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MIM','2005-03-04 11:38:02')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MGD','2005-03-04 11:38:04')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('RGD','2005-03-04 11:38:06')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PDB','2005-03-04 11:38:10')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SWISS-2DPAGE','2005-03-04 11:38:33')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Aarhus/Ghent-2DPAGE','2005-03-04 11:38:33')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PMMA-2DPAGE','2005-03-04 11:38:45')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('TIGR','2005-03-04 11:38:49')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('TubercuList','2005-03-04 11:38:50')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Leproma','2005-03-04 11:39:05')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('GeneFarm','2005-03-04 11:39:35')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('GermOnline','2005-03-04 11:43:54')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SGD','2005-03-04 11:43:54')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('GeneDB_SPombe','2005-03-04 11:44:15')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PIRSF','2005-03-04 11:45:42')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('HAMAP','2005-03-04 11:46:49')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Reactome','2005-03-04 11:46:52')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ECO2DBASE','2005-03-04 11:46:55')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('EchoBASE','2005-03-04 11:46:55')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('EcoGene','2005-03-04 11:46:55')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SubtiList','2005-03-04 11:46:58')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ListiList','2005-03-04 11:47:14')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('GlycoSuiteDB','2005-03-04 11:47:44')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('StyGene','2005-03-04 11:51:59')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PHCI-2DPAGE','2005-03-04 11:52:19')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Siena-2DPAGE','2005-03-04 11:55:22')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('HSC-2DPAGE','2005-03-04 11:55:41')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MEROPS','2005-03-04 11:59:32')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('AGD','2005-03-04 12:14:40')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PhotoList','2005-03-04 12:15:22')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ZFIN','2005-03-04 12:15:39')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('MypuList','2005-03-04 12:24:15')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('SagaList','2005-03-04 12:25:40')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('ANU-2DPAGE','2005-03-04 12:29:22')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Rat-heart-2DPAGE','2005-03-04 12:30:51')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('PhosSite','2005-03-04 12:49:00')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('REBASE','2005-03-04 13:25:29')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('Maize-2DPAGE','2005-03-04 15:10:53')
INSERT INTO ProtIdentTypes (Name,EntryDate) VALUES ('HIV','2005-03-04 22:13:40')
INSERT INTO ProtOrganisms (CommonName,Genus,Species,Comments) VALUES ('Unknown organism','Unknown','unknown','Organism is unknown')
GO


CREATE TABLE MS2Runs
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
	MassSpecType NVARCHAR(20),
	DataBaseId INT NOT NULL DEFAULT 0,
	SampleEnzyme NVARCHAR(50),
	Deleted BIT NOT NULL DEFAULT 0,
	ApplicationLSID LSIDType NULL,

	CONSTRAINT MS2Runs_PK PRIMARY KEY (Run)
	)
GO


CREATE TABLE MS2Fractions
	(
	Fraction INT IDENTITY(1,1),
	Run INT NOT NULL,
	Description NVARCHAR(300),
	FileName NVARCHAR(300),
	HydroB0 REAL,
	HydroB1 REAL,
	HydroR2 REAL,
	HydroSigma REAL,

	CONSTRAINT MS2Fractions_PK PRIMARY KEY (Fraction)
	)
GO


CREATE TABLE MS2Modifications
	(
	Run INT NOT NULL,
	AminoAcid VARCHAR (1) NOT NULL,
	MassDiff REAL NOT NULL,
	Variable BIT NOT NULL,
	Symbol VARCHAR (1) NOT NULL,

	CONSTRAINT MS2Modifications_PK PRIMARY KEY (Run, AminoAcid, Symbol)
	)
GO


CREATE TABLE MS2PeptidesData
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

	CONSTRAINT MS2Peptides_PK PRIMARY KEY CLUSTERED (Fraction, Scan, Charge),
	)
CREATE INDEX MS2PeptidesData_Protein ON MS2PeptidesData (Protein)
GO


-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE MS2SpectraData
	(
	Fraction INT NOT NULL,
	Scan INT NOT NULL,
	Spectrum IMAGE NOT NULL,

	CONSTRAINT MS2SpectraData_PK PRIMARY KEY (Fraction, Scan)
	)
GO


-- Create table for MS2 run & peptide history
CREATE TABLE MS2History
	(
	Date DATETIME,
	Runs BIGINT,
	Peptides BIGINT,

	CONSTRAINT MS2History_PK PRIMARY KEY CLUSTERED (Date)
	)
GO


CREATE TABLE ProteinDataBases
	(
	DataBaseId INT IDENTITY (0, 1) NOT NULL PRIMARY KEY,
	ProteinDataBase NVARCHAR (400),
	Loaded DATETIME
	)
GO


-- Special entry 0 for runs that contain no protein database
INSERT INTO ProteinDataBases (ProteinDataBase, Loaded) VALUES (NULL, NULL)
GO


CREATE TABLE ProteinSequences
	(
	DataBaseId INT NOT NULL,
	SequenceId INT IDENTITY (1, 1) NOT NULL PRIMARY KEY,
	SequenceMass REAL NOT NULL,
	Sequence TEXT NOT NULL,
	LookupString VARCHAR (200) NOT NULL,
	SeqId INT NULL,
	
	CONSTRAINT ProteinSequences_AK UNIQUE (DataBaseId, LookupString),
	CONSTRAINT FK_ProteinSequences_ProtSequences FOREIGN KEY (SeqId) REFERENCES ProtSequences (SeqId)
	)
CREATE INDEX IX_ProteinSequences ON ProteinSequences (DataBaseId, LookupString)
GO


CREATE TABLE ProteinNames
	(
	SequenceId INT NOT NULL,
	Description VARCHAR (1000) NOT NULL 
	)
CREATE INDEX IX_ProteinNames ON ProteinNames (SequenceId)
GO


CREATE VIEW MS2Spectra AS
	SELECT MS2Fractions.Run AS Run, MS2SpectraData.*
	FROM MS2SpectraData INNER JOIN
	MS2Fractions ON MS2SpectraData.Fraction = MS2Fractions.Fraction
GO


-- Union of all MS2Peptides columns; alias Score1 to RawScore and SpScore, Score2 to DiffScore and DeltaCn, etc.
CREATE VIEW MS2Peptides AS
   SELECT
      MS2Fractions.Run, MS2PeptidesData.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore,
      Score1 As SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next,
      Score3 As B, Score4 As Y, Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 As Homology, IonPercent,
      MS2PeptidesData.Mass, DeltaMass, (MS2PeptidesData.Mass + DeltaMass) AS PrecursorMass,
      CASE WHEN MS2PeptidesData.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (MS2PeptidesData.Mass + (Charge - 1) * 1.00794)) END AS DeltaMassPPM,
      CASE WHEN Charge = 0 THEN 0 ELSE (MS2PeptidesData.Mass + DeltaMass + (Charge - 1) * 1.00794) / Charge END AS MZ,
      PeptideProphet, Peptide, ProteinHits, Protein, PrevAA, TrimmedPeptide,
      NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide, SequencePosition,
      ProtSequences.Description, ProtSequences.BestGeneName AS GeneName
   FROM MS2PeptidesData
      INNER JOIN
         MS2Fractions ON MS2PeptidesData.Fraction = MS2Fractions.Fraction 
      INNER JOIN
         MS2Runs ON MS2Fractions.Run = MS2Runs.Run 
      LEFT OUTER JOIN
         ProteinSequences ON 
            ProteinSequences.LookupString = MS2PeptidesData.Protein AND 
            ProteinSequences.DataBaseId = MS2Runs.DataBaseId 
      LEFT OUTER JOIN
            ProtSequences ON ProtSequences.SeqId = ProteinSequences.SeqId
GO


-- Join ProteinDataBases, ProteinSequences, and ProteinNames into one table
CREATE VIEW Proteins AS
	SELECT ProteinDataBases.ProteinDataBase, ProteinSequences.DataBaseId, ProteinSequences.SequenceId, ProteinSequences.SequenceMass,
		ProteinSequences.Sequence, ProteinSequences.LookupString, ProteinNames.Description
	FROM ProteinSequences INNER JOIN
		ProteinNames ON ProteinSequences.SequenceId = ProteinNames.SequenceId INNER JOIN
		ProteinDataBases ON ProteinDataBases.DataBaseId = ProteinSequences.DataBaseId
GO


-- ProteinDataBases with some statistics (number of sequences, number of protein names, number of runs)
CREATE VIEW ProteinDBs AS
	SELECT ProteinDataBases.ProteinDataBase, ProteinDataBases.DataBaseId, ProteinDataBases.Loaded, X.Sequences, X.[Names], Y.Runs
	FROM ProteinDataBases LEFT OUTER JOIN
		(SELECT DataBaseId, COUNT(DISTINCT SequenceId) AS Sequences, COUNT(*) AS Names
		FROM Proteins
		GROUP BY DataBaseId) X ON X.DataBaseId = ProteinDataBases.DataBaseId LEFT OUTER JOIN
		(SELECT DataBaseId, COUNT(Run) AS Runs
		FROM MS2Runs
		GROUP BY DataBaseId) Y ON Y.DataBaseId = ProteinDataBases.DataBaseId
GO


-- Subset of MS2 Runs columns joined to container names and fasta names, plus number of scans & spectra
CREATE VIEW Runs AS
	SELECT MS2Runs.Run, MS2Runs.Description, MS2Runs.Path, MS2Runs.FileName, MS2Runs.Created, core..Containers.Name AS Container,
		MS2Runs.DataBaseId, ProteinDataBases.ProteinDataBase, X.Scans, Y.Spectra, MS2Runs.StatusId, MS2Runs.Status, MS2Runs.Deleted
	FROM MS2Runs LEFT OUTER JOIN
		core..Containers ON MS2Runs.Container = core..Containers.EntityId LEFT OUTER JOIN
		ProteinDataBases ON MS2Runs.DataBaseId = ProteinDataBases.DataBaseId LEFT OUTER JOIN
		(SELECT Run, COUNT(*) AS Scans
		FROM MS2Peptides
		GROUP BY Run) X ON MS2Runs.Run = X.Run LEFT OUTER JOIN
		(SELECT Run, COUNT(*) AS Spectra
		FROM MS2Spectra
		GROUP BY Run) Y ON MS2Runs.Run = Y.Run
GO