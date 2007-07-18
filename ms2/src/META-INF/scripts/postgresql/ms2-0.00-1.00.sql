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

-- All tables and views used for MS2 data

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
CREATE VIEW MS2Peptides AS
    SELECT
        MS2Fractions.Run, MS2PeptidesData.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore,
        Score1 As SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next,
        Score3 As B, Score4 As Y, Score5 As Expect, Score1 As Ion, Score2 As Identity, Score3 AS Homology, IonPercent,
        MS2PeptidesData.Mass, DeltaMass, (MS2PeptidesData.Mass + DeltaMass) AS PrecursorMass,
        CASE WHEN MS2PeptidesData.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (MS2PeptidesData.Mass + (Charge - 1) * 1.00794)) END AS DeltaMassPPM,
        CASE WHEN Charge = 0 THEN 0 ELSE (MS2PeptidesData.Mass + DeltaMass + (Charge - 1) * 1.00794) / Charge END AS MZ,
        PeptideProphet, Peptide, ProteinHits, Protein, PrevAA, TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA ||
        TrimmedPeptide || NextAA)) AS StrippedPeptide, SequencePosition, ProtSequences.Description AS Description,
        ProtSequences.BestGeneName AS GeneName
    FROM MS2PeptidesData
        INNER JOIN
            MS2Fractions ON MS2PeptidesData.Fraction = MS2Fractions.Fraction
        INNER JOIN
            MS2Runs ON MS2Fractions.Run = MS2Runs.Run
        LEFT OUTER JOIN
            prot.ProteinSequences ON
                ProteinSequences.LookupString = MS2PeptidesData.Protein AND ProteinSequences.DataBaseId = MS2Runs.DataBaseId
        LEFT OUTER JOIN
            prot.ProtSequences ON ProtSequences.SeqId = ProteinSequences.SeqId;


-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE MS2SpectraData
	(
	Fraction INT NOT NULL,
	Scan INT NOT NULL,
	Spectrum BYTEA NOT NULL,

	CONSTRAINT PK_MS2SpectraData PRIMARY KEY (Fraction, Scan)
	);


CREATE VIEW MS2Spectra AS
    SELECT MS2Fractions.Run AS Run, MS2SpectraData.*
    FROM MS2SpectraData INNER JOIN
    MS2Fractions ON MS2SpectraData.Fraction = MS2Fractions.Fraction;


CREATE TABLE MS2History
    (
    Date TIMESTAMP,
    Runs BIGINT,
    Peptides BIGINT,

    CONSTRAINT PK_MS2History PRIMARY KEY (Date)
    );


CREATE VIEW ms2.MS2ExperimentRuns AS
SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId as ExperimentRunRowId, exp.Protocol.Name As ProtocolName
FROM ms2.MS2Runs
LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ApplicationLSID
LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID;


SET search_path TO prot, public;

-- ProteinDataBases with number of runs
CREATE VIEW ProteinDBs AS
    SELECT ProteinDataBases.ProteinDataBase, ProteinDataBases.DataBaseId, ProteinDataBases.Loaded, X.Runs
    FROM ProteinDataBases LEFT OUTER JOIN
        (SELECT DataBaseId, COUNT(Run) AS Runs
        FROM ms2.MS2Runs
        GROUP BY DataBaseId) X ON X.DataBaseId = ProteinDataBases.DataBaseId;
