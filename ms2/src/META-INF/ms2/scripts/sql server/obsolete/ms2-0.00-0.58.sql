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
go

/*
IF OBJECT_ID('MS2Runs','U') IS NOT NULL
	DROP TABLE MS2Runs
IF OBJECT_ID('MS2Modifications','U') IS NOT NULL
	DROP TABLE MS2Modifications
IF OBJECT_ID('MS2Fractions','U') IS NOT NULL
	DROP TABLE MS2Fractions
IF OBJECT_ID('MS2PeptidesData','U') IS NOT NULL
	DROP TABLE MS2PeptidesData
IF OBJECT_ID('MS2SpectraData','U') IS NOT NULL
	DROP TABLE MS2SpectraData
IF OBJECT_ID('MS2Peptides', 'V') IS NOT NULL
	DROP VIEW MS2Peptides
IF OBJECT_ID('MS2Spectra', 'V') IS NOT NULL
	DROP VIEW MS2Spectra
IF OBJECT_ID('ProteinDataBases','U') IS NOT NULL
	DROP TABLE ProteinDataBases
IF OBJECT_ID('ProteinSequences','U') IS NOT NULL
	DROP TABLE ProteinSequences
IF OBJECT_ID('ProteinNames','U') IS NOT NULL
	DROP TABLE ProteinNames
*/

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

	CONSTRAINT MS2Runs_PK PRIMARY KEY (Run)
	)
go


CREATE TABLE MS2Fractions
	(
	Fraction INT IDENTITY(1,1),
	Run INT NOT NULL,
	Description NVARCHAR(300),
	FileName NVARCHAR(300),
	HydroB0 real,
	HydroB1 real,
	HydroR2 real,
	HydroSigma real,

	CONSTRAINT MS2Fractions_PK PRIMARY KEY (Fraction)
	)
go


CREATE TABLE MS2Modifications
	(
	Run INT NOT NULL,
	AminoAcid VARCHAR (1) NOT NULL,
	MassDiff REAL NOT NULL,
	Variable BIT NOT NULL,
	Symbol VARCHAR (1) NOT NULL,

	CONSTRAINT MS2Modifications_PK PRIMARY KEY (Run, AminoAcid, Symbol)
	)
go


CREATE TABLE MS2PeptidesData
	(
	Fraction int NOT NULL,
	Scan int NOT NULL,
	Charge tinyint NOT NULL,
	Score1 real NOT NULL DEFAULT 0,
	Score2 real NOT NULL DEFAULT 0,
	Score3 real NOT NULL DEFAULT 0,
	Score4 real NULL,
	Score5 real NULL,
	IonPercent real NOT NULL,
	Mass float NOT NULL,
	DeltaMass real NOT NULL,
	PeptideProphet real NOT NULL,
	Peptide varchar (200) NOT NULL,
	PrevAA char(1) NOT NULL DEFAULT '',
	TrimmedPeptide varchar(200) NOT NULL DEFAULT '',
	NextAA char(1) NOT NULL DEFAULT '',
	ProteinHits smallint NOT NULL,
	SequencePosition int NOT NULL DEFAULT 0,
	Protein varchar (100) NOT NULL,

	CONSTRAINT MS2Peptides_PK PRIMARY KEY CLUSTERED (Fraction, Scan, Charge)
	)
go

CREATE INDEX MS2PeptidesData_Protein ON MS2PeptidesData (Protein)
go


-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE MS2SpectraData
	(
	Fraction int NOT NULL,
	Scan int NOT NULL,
	Spectrum image NOT NULL,

	CONSTRAINT MS2SpectraData_PK PRIMARY KEY (Fraction, Scan)
	)
go


CREATE VIEW MS2Spectra AS
	SELECT MS2Fractions.Run AS Run, MS2SpectraData.*
	FROM MS2SpectraData INNER JOIN
	MS2Fractions ON MS2SpectraData.Fraction = MS2Fractions.Fraction
go


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
      NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide, SequencePosition
   FROM MS2PeptidesData
      INNER JOIN
         MS2Fractions ON MS2PeptidesData.Fraction = MS2Fractions.Fraction 
GO


CREATE TABLE ProteinDataBases
	(
	DataBaseId INT IDENTITY (0, 1) NOT NULL PRIMARY KEY,
	ProteinDataBase NVARCHAR (400),
	Loaded DATETIME
	)
go

-- Special entry 0 for runs that contain no protein database
INSERT INTO ProteinDataBases (ProteinDataBase, Loaded) VALUES (NULL, NULL)
go

CREATE TABLE ProteinSequences
	(
	DataBaseId INT NOT NULL,
	SequenceId INT IDENTITY (1, 1) NOT NULL PRIMARY KEY,
	SequenceMass REAL NOT NULL,
	Sequence TEXT NOT NULL,
	LookupString VARCHAR (200) NOT NULL,

	CONSTRAINT ProteinSequences_AK UNIQUE (DataBaseId, LookupString)
	)
go

CREATE INDEX IX_ProteinSequences ON ProteinSequences (DataBaseId, LookupString)

CREATE TABLE ProteinNames (
	SequenceId INT NOT NULL,
	Description VARCHAR (1000) NOT NULL 
)
go

CREATE INDEX IX_ProteinNames ON ProteinNames (SequenceId)
go


-- Join ProteinDataBases, ProteinSequences, and ProteinNames into one table
CREATE VIEW Proteins AS
	SELECT ProteinDataBases.ProteinDataBase, ProteinSequences.DataBaseId, ProteinSequences.SequenceId, ProteinSequences.SequenceMass,
		ProteinSequences.Sequence, ProteinSequences.LookupString, ProteinNames.Description
	FROM ProteinSequences INNER JOIN
		ProteinNames ON ProteinSequences.SequenceId = ProteinNames.SequenceId INNER JOIN
		ProteinDataBases ON ProteinDataBases.DataBaseId = ProteinSequences.DataBaseId
go

-- ProteinDataBases with some statistics (number of sequences, number of protein names, number of runs)
CREATE VIEW ProteinDBs AS
	SELECT ProteinDataBases.ProteinDataBase, ProteinDataBases.DataBaseId, ProteinDataBases.Loaded, X.Sequences, X.[Names], Y.Runs
	FROM ProteinDataBases LEFT OUTER JOIN
		(SELECT DataBaseId, COUNT(DISTINCT sequenceid) AS Sequences, COUNT(*) AS Names
		FROM Proteins
		GROUP BY databaseid) X ON X.DataBaseId = ProteinDataBases.DataBaseId LEFT OUTER JOIN
		(SELECT DataBaseId, COUNT(Run) AS Runs
		FROM MS2Runs
		GROUP BY DataBaseId) Y ON Y.DataBaseId = ProteinDataBases.DataBaseId
go

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
go