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

-- Rebuild peptide view to join to new table name
DROP VIEW ms2.MS2Peptides
GO

CREATE VIEW ms2.MS2Peptides AS
    SELECT pep.*, seq.description, seq.bestgenename AS genename
    FROM ms2.MS2SimplePeptides pep
    LEFT JOIN prot.sequences seq ON seq.seqid = pep.seqid
GO

