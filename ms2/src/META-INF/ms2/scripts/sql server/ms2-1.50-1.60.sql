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

-- Rename and rebuild Fasta admin view
DROP VIEW prot.ProteinDBs
GO

CREATE VIEW prot.FastaAdmin AS
    SELECT ff.FileName, ff.FastaId, ff.Loaded, runs.Runs
    FROM prot.FastaFiles ff LEFT OUTER JOIN
        (SELECT FastaId, COUNT(Run) AS Runs
        FROM ms2.MS2Runs
        GROUP BY FastaId) runs ON runs.FastaId = ff.FastaId
GO

-- Rebuild MS2ExperimentRuns view to pick up FastaId column name change
DROP VIEW ms2.MS2ExperimentRuns
GO

CREATE VIEW ms2.MS2ExperimentRuns AS
    SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId AS ExperimentRunRowId, exp.Protocol.Name AS ProtocolName
    FROM ms2.MS2Runs
        LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ExperimentRunLSID
        LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID
GO

-- Add column for retention time
ALTER TABLE ms2.MS2PeptidesData
    ADD RetentionTime REAL NULL
GO

-- Rebuild peptide views to join to new table name and use new column name
DROP VIEW ms2.MS2Peptides
DROP VIEW ms2.MS2SimplePeptides
GO

CREATE VIEW ms2.MS2SimplePeptides AS SELECT
    frac.Run, run.Description AS RunDescription, pep.Fraction, LEFT(frac.FileName, CHARINDEX('.', frac.FileName) - 1) AS FractionName, Scan,
    RetentionTime, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore, Score1 As SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank,
    Score1 As Hyper, Score2 As Next, Score3 As B, Score4 As Y, Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 AS Homology,
    IonPercent, pep.Mass, DeltaMass, (pep.Mass + DeltaMass) AS PrecursorMass, ABS(DeltaMass - ROUND(DeltaMass, 0)) AS FractionalDeltaMass,
    CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * ABS(DeltaMass - ROUND(DeltaMass, 0)) / (pep.Mass + (Charge - 1) * 1.007276)) END AS FractionalDeltaMassPPM,
	CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (pep.Mass + (Charge - 1) * 1.007276)) END AS DeltaMassPPM,
    CASE WHEN Charge = 0 THEN 0 ELSE (pep.Mass + DeltaMass + (Charge - 1) * 1.007276) / Charge END AS MZ, PeptideProphet, Peptide, ProteinHits,
    Protein, PrevAA, TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide,	SequencePosition, pep.SeqId, pep.RowId,
    quant.DecimalRatio, quant.Heavy2LightRatio, quant.HeavyArea, quant.HeavyFirstScan, quant.HeavyLastScan, quant.HeavyMass, quant.LightArea, quant.LightFirstScan, quant.LightLastScan, quant.LightMass, quant.Ratio,
    proph.ProphetFVal, proph.ProphetDeltaMass, proph.ProphetNumTrypticTerm, proph.ProphetNumMissedCleav
    FROM ms2.MS2PeptidesData pep
        INNER JOIN
            ms2.MS2Fractions frac ON pep.Fraction = frac.Fraction
        INNER JOIN
            ms2.MS2Runs run ON frac.Run = run.Run
    LEFT JOIN ms2.quantitation quant ON pep.rowid=quant.peptideid
    LEFT JOIN ms2.peptideprophetdata proph ON pep.rowid=proph.peptideid
GO

CREATE VIEW ms2.MS2Peptides AS
    SELECT pep.*, seq.description, seq.bestgenename AS genename
    FROM ms2.MS2SimplePeptides pep
    LEFT JOIN prot.sequences seq ON seq.seqid = pep.seqid
GO
