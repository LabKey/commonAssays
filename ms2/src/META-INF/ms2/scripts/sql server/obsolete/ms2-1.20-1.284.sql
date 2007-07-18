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

DROP VIEW ms2.MS2Peptides
GO

ALTER TABLE ms2.ms2peptidesdata
    ADD RowId BIGINT IDENTITY (1, 1) NOT NULL,
    CONSTRAINT UQ_PeptidesData UNIQUE (RowId)
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

CREATE TABLE ms2.ms2peptidememberships
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
  CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION
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

CREATE INDEX IX_MS2PeptidesData_Fraction ON ms2.MS2PeptidesData(Fraction)
GO

CREATE VIEW ms2.MS2SimplePeptides AS SELECT
    frac.Run, run.Description AS RunDescription, pep.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore, Score1 As SpScore,
    Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next, Score3 As B, Score4 As Y,
    Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 AS Homology, IonPercent, pep.Mass, DeltaMass,
    (pep.Mass + DeltaMass) AS PrecursorMass, ABS(DeltaMass - ROUND(DeltaMass, 0)) AS FractionalDeltaMass,
    CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * ABS(DeltaMass - ROUND(DeltaMass, 0)) / (pep.Mass + (Charge - 1) * 1.007276)) END AS FractionalDeltaMassPPM,
	CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (pep.Mass + (Charge - 1) * 1.007276)) END AS DeltaMassPPM,
    CASE WHEN Charge = 0 THEN 0 ELSE (pep.Mass + DeltaMass + (Charge - 1) * 1.007276) / Charge END AS MZ, PeptideProphet, Peptide,
	ProteinHits, Protein, PrevAA, TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide,
	SequencePosition, pep.SeqId, pep.RowId, quant.* FROM ms2.MS2PeptidesData pep
        INNER JOIN
            ms2.MS2Fractions frac ON pep.Fraction = frac.Fraction
        INNER JOIN
            ms2.MS2Runs run ON frac.Run = run.Run
   LEFT JOIN ms2.quantitation quant ON pep.rowid=quant.peptideid
GO

CREATE VIEW ms2.ms2peptides AS
 SELECT pep.*, seq.description, seq.bestgenename AS genename
   FROM ms2.ms2SimplePeptides pep
   LEFT JOIN prot.protsequences seq ON seq.seqid = pep.seqid
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

CREATE VIEW ms2.ProteinGroupsWithQuantitation AS
  SELECT *
    FROM ms2.ms2proteingroups
    LEFT JOIN ms2.proteinquantitation ON ProteinGroupId = RowId
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

-- No changes to this view, but we need to rebuild it since we added a column to MS2Runs
DROP VIEW ms2.MS2ExperimentRuns
GO

CREATE VIEW ms2.MS2ExperimentRuns AS
SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId AS ExperimentRunRowId, exp.Protocol.Name AS ProtocolName
FROM ms2.MS2Runs
LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ExperimentRunLSID
LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID
GO
