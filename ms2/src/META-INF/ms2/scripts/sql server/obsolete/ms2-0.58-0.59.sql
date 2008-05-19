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


IF OBJECT_ID('ProtOrganisms','U') IS NOT NULL
	DROP TABLE ProtOrganisms
IF OBJECT_ID('ProtAnnotations','U') IS NOT NULL
	DROP TABLE ProtAnnotations
IF OBJECT_ID('ProtIdentifiers','U') IS NOT NULL
	DROP TABLE ProtIdentifiers
IF OBJECT_ID('ProtSequences','U') IS NOT NULL
	DROP TABLE ProtSequences
IF OBJECT_ID('ProtAnnotationTypes','U') IS NOT NULL
	DROP TABLE ProtAnnotationTypes
IF OBJECT_ID('ProtIdentTypes', 'U') IS NOT NULL
	DROP TABLE ProtIdentTypes
IF OBJECT_ID('ProtInfoSources', 'U') IS NOT NULL
	DROP TABLE ProtInfoSources
IF OBJECT_ID('ProtAnnotInsertions', 'U') IS NOT NULL
	DROP TABLE ProtAnnotInsertions
IF OBJECT_ID('ProtFastas','U') IS NOT NULL
    DROP TABLE ProtFastas
IF OBJECT_ID('ProtSprotOrgMap','U') IS NOT NULL
    DROP TABLE ProtSprotOrgMap

GO

/****** ProtAnnotInsertions                                 */
CREATE TABLE dbo.ProtAnnotInsertions (
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
CREATE TABLE ProtInfoSources (
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
CREATE TABLE ProtAnnotationTypes (
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
GO

/****** ProtIdentTypes                                      */
CREATE TABLE ProtIdentTypes (
	IdentTypeId INT IDENTITY (1, 1) NOT NULL ,
	Name VARCHAR(50) NOT NULL,
	CannonicalSourceId INT NULL,
	EntryDate DATETIME NOT NULL DEFAULT (getdate()) ,
	Description VARCHAR(200) NULL,
	Deleted BIT NOT NULL DEFAULT 0,

	CONSTRAINT PK_ProtIdentTypes PRIMARY KEY CLUSTERED (IdentTypeId),
        CONSTRAINT FK_ProtIdentTypes_ProtInfoSources FOREIGN KEY (CannonicalSourceId) REFERENCES ProtInfoSources (SourceId)
) 
GO

/****** ProtSequences                                       */  
CREATE TABLE ProtSequences (
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
GO

/****** ProtIdentifiers                                     */ 
CREATE TABLE ProtIdentifiers (
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
GO

/****** ProtOrganisms                                       */
CREATE TABLE ProtOrganisms (
	OrgId INT IDENTITY (1, 1) NOT NULL,
	CommonName VARCHAR(50) NULL,
	Genus VARCHAR(100) NOT NULL,
	Species VARCHAR(100)  NOT NULL,
	Comments VARCHAR(200) NULL,
	IdentId INT NULL, 
	Deleted BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_Organism PRIMARY KEY CLUSTERED (OrgId)
) 
GO

/****** ProtAnnotations                                     */
CREATE TABLE ProtAnnotations (
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
	CONSTRAINT FKProtAnnotationsProtSequences FOREIGN KEY (SeqId) REFERENCES ProtSequences (SeqId) ON DELETE CASCADE
) 
GO

/****** ProtFastas                                          */
CREATE TABLE dbo.ProtFastas (
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
CREATE TABLE dbo.ProtSprotOrgMap (
   SprotSuffix VARCHAR(50) COLLATE SQL_Latin1_General_CP437_CS_AS NOT NULL,
   OrgId INT NOT NULL
)
GO

ALTER TABLE dbo.ProtSprotOrgMap
   ADD CONSTRAINT FK_ProtSprotOrgMap_ProtOrganisms FOREIGN KEY
	(OrgId) REFERENCES dbo.ProtOrganisms (OrgId)
GO

CREATE INDEX IX_SprotSuffix ON protSprotOrgMap (SprotSuffix)
GO

ALTER TABLE ProtOrganisms ADD
        CONSTRAINT FK_Organism_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES ProtIdentifiers (IdentId) ON UPDATE CASCADE 
GO

ALTER TABLE ProtOrganisms ADD
	CONSTRAINT FKProtSequencesOrganism FOREIGN KEY (OrgId) REFERENCES ProtOrganisms (OrgId)
GO

CREATE UNIQUE INDEX IX_ProtAnnotationTypes ON ProtAnnotationTypes(Name) 
GO

CREATE UNIQUE INDEX IX_ProtIdentTypes ON ProtIdentTypes(Name) 
GO

CREATE UNIQUE INDEX IX_ProtIdentifiers ON ProtIdentifiers(IdentTypeId, Identifier, SeqId) 
GO

CREATE UNIQUE INDEX IX_ProtOrganismsSurrogateKey ON ProtOrganisms(Genus,Species) 
GO

CREATE INDEX IX_ProtAnnotations1 ON ProtAnnotations(SeqId, AnnotTypeId) 
GO

CREATE UNIQUE INDEX IX_annotSurrogateKey ON ProtAnnotations(AnnotTypeId, AnnotVal, SeqId, StartPos, EndPos) 
GO

CREATE INDEX IX_ProtIdentifiers1 ON ProtIdentifiers(IdentTypeId, Identifier, IdentId, SeqId) 
GO

CREATE INDEX IX_Identifier on ProtIdentifiers(Identifier)
GO

CREATE UNIQUE INDEX IX_ProtSequencesSurrogateKey ON ProtSequences(Hash, OrgId) 
GO

CREATE  INDEX IX_SeqHash ON ProtSequences(Hash) 
GO

CREATE  INDEX IX_SequencesOrg ON ProtSequences(OrgId) 
GO

CREATE INDEX IX_ProtSequences_BestGeneName ON ProtSequences(BestGeneName)
GO

ALTER TABLE ProteinSequences
    ADD SeqId INT NULL FOREIGN KEY REFERENCES ProtSequences(SeqId)
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
INSERT INTO ProtOrganisms (CommonName,Genus,Species,Comments) VALUES
('Unknown organism','Unknown','unknown','Organism is unknown')
GO