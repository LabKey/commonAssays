/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
  -- FK between ProtSequences and ProtOrganisms was incorrect IN SQL Sever only.

  -- first set any unmatched orgids in ProtSeq to the 'unknown' orgid.  prevents failures later
  UPDATE prot.ProtSequences
      SET OrgId = (SELECT OrgId FROM prot.ProtOrganisms WHERE genus='Unknown' and species='unknown')
      WHERE OrgId NOT IN (SELECT OrgId FROM prot.ProtOrganisms)
  go
  -- drop the incorrect FK
  ALTER TABLE prot.ProtOrganisms
  	DROP CONSTRAINT FK_ProtSequences_ProtOrganisms
  go

  -- add the FK back on the correct table
  ALTER TABLE prot.ProtSequences
      WITH NOCHECK ADD	CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES prot.ProtOrganisms (OrgId)
  go

-- add most common ncbi Taxonomy id's

CREATE TABLE #idents
(	rowid int not null identity primary key,
	Identifier varchar(50) not null,
	CommonName varchar(20) null,
	Genus varchar(100) not null,
	Species varchar(100) not null,
	OrgId int null,
	IdentId int null,
	IdentTypeId int null
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
go
UPDATE #idents
	SET  IdentTypeId = (SELECT identtypeid FROM prot.ProtIdentTypes WHERE name='NCBI Taxonomy')
go
INSERT prot.ProtOrganisms (CommonName, Genus, Species)
SELECT   CommonName, Genus, Species FROM #idents
	WHERE NOT EXISTS
		(SELECT * FROM prot.ProtOrganisms PO INNER JOIN #idents i ON (PO.Genus = i.Genus AND PO.Species = i.Species))
go
INSERT prot.ProtIdentifiers (Identifier, IdentTypeId)
	SELECT   Identifier, IdentTypeId FROM #idents
	WHERE NOT EXISTS
		(SELECT * FROM prot.ProtIdentifiers PI INNER JOIN #idents i ON (PI.Identifier = i.Identifier AND PI.IdentTypeId = i.IdentTypeId))
go
UPDATE #idents
	SET OrgId = PO.OrgId
	FROM prot.ProtOrganisms PO
	WHERE #idents.Genus = PO.Genus AND #idents.Species = PO.Species
go

UPDATE #idents
	SET IdentId = PI.IdentId
	FROM prot.ProtIdentifiers PI
	WHERE #idents.Identifier = PI.Identifier and #idents.IdentTypeId = PI.IdentTypeId
go
UPDATE prot.ProtOrganisms
	SET IdentId = i.IdentID
	FROM #idents i
	WHERE i.OrgId = prot.ProtOrganisms.OrgId
go
--SELECT i.*, PO.orgid, PO.IdentID, PI.IdentId
-- FROM #idents i
--inner join prot.ProtOrganisms PO ON (i.genus = PO.genus and i.species = PO.species)
--inner join prot.ProtIdentifiers PI ON (i.Identifier = PI.Identifier AND i.IdentTypeID = PI.IdentTypeId)


drop table #idents
go

ALTER TABLE ms2.ms2peptidesdata ADD RowId INT IDENTITY (1, 1) NOT NULL
GO

ALTER TABLE ms2.ms2peptidesdata ADD CONSTRAINT UQ_PeptidesData UNIQUE (RowId)
GO

CREATE TABLE ms2.MS2ProteinProphetFiles
(
	RowId INT IDENTITY (1, 1) NOT NULL,
	FilePath VARCHAR(255) NOT NULL,

	CONSTRAINT PK_MS2ProteinProphetFiles PRIMARY KEY (RowId),
	CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (FilePath)
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
	PeptideId INT NOT NULL,
	ProteinGroupId INT NOT NULL,

	CONSTRAINT PK_MS2PeptideMemberships                PRIMARY KEY (ProteinGroupId, PeptideId),
	CONSTRAINT FK_MS2PeptideMembership_MS2PeptidesData        FOREIGN KEY (PeptideId)       REFERENCES ms2.MS2PeptidesData (RowId),
	CONSTRAINT FK_MS2PeptideMembership_MS2ProteinGroup   FOREIGN KEY (ProteinGroupId)  REFERENCES ms2.MS2ProteinGroups (RowId)
)
GO

DROP VIEW ms2.ms2peptides
GO

CREATE VIEW ms2.MS2Peptides AS SELECT
    frac.Run, run.Description AS RunDescription, pep.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore, Score1 As SpScore,
    Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next, Score3 As B, Score4 As Y,
    Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 AS Homology, IonPercent, pep.Mass, DeltaMass,
    (pep.Mass + DeltaMass) AS PrecursorMass, ABS(DeltaMass - ROUND(DeltaMass, 0)) AS FractionalDeltaMass,
    CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * ABS(DeltaMass - ROUND(DeltaMass, 0)) / (pep.Mass + (Charge - 1) * 1.007276)) END AS FractionalDeltaMassPPM,
	CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (pep.Mass + (Charge - 1) * 1.007276)) END AS DeltaMassPPM,
    CASE WHEN Charge = 0 THEN 0 ELSE (pep.Mass + DeltaMass + (Charge - 1) * 1.007276) / Charge END AS MZ, PeptideProphet, Peptide,
	ProteinHits, Protein, PrevAA, TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide,
	SequencePosition, pep.SeqId, seq.Description AS Description, BestGeneName AS GeneName, pep.RowId FROM ms2.MS2PeptidesData pep
        INNER JOIN
            ms2.MS2Fractions frac ON pep.Fraction = frac.Fraction
        INNER JOIN
            ms2.MS2Runs run ON frac.Run = run.Run
        LEFT OUTER JOIN
            prot.ProtSequences seq ON seq.SeqId = pep.SeqId
GO
