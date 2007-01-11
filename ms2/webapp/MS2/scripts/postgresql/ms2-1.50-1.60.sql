-- Simplify protein table names
ALTER TABLE prot.ProtAnnotations RENAME TO Annotations;
ALTER TABLE prot.ProtAnnotationTypes RENAME TO AnnotationTypes;
ALTER TABLE prot.ProtAnnotInsertions RENAME TO AnnotInsertions;
ALTER TABLE prot.ProteinDatabases RENAME TO FastaFiles;
ALTER TABLE prot.ProteinSequences RENAME TO FastaSequences;
ALTER TABLE prot.ProtFastas RENAME TO FastaLoads;
ALTER TABLE prot.ProtIdentifiers RENAME TO Identifiers;
ALTER TABLE prot.ProtIdentTypes RENAME TO IdentTypes;
ALTER TABLE prot.ProtInfoSources RENAME TO InfoSources;
ALTER TABLE prot.ProtOrganisms RENAME TO Organisms;
ALTER TABLE prot.ProtSequences RENAME TO Sequences;
ALTER TABLE prot.ProtSProtOrgMap RENAME TO SProtOrgMap;

-- Rename some columns
ALTER TABLE prot.FastaFiles RENAME DataBaseId TO FastaId;
ALTER TABLE prot.FastaFiles RENAME ProteinDataBase TO FileName;
ALTER TABLE ms2.MS2Runs RENAME DataBaseId TO FastaId;
ALTER TABLE prot.FastaSequences RENAME DataBaseId TO FastaId;

-- Drop obsolete table, PK and columns
DROP TABLE prot.ProteinNames;

ALTER TABLE prot.FastaSequences DROP CONSTRAINT PK_ProteinSequences;
ALTER TABLE prot.FastaSequences
    DROP COLUMN SequenceId,
    DROP COLUMN SequenceMass,
    DROP COLUMN Sequence;

-- Rename and rebuild Fasta admin view
DROP VIEW prot.ProteinDBs;

CREATE VIEW prot.FastaAdmin AS
    SELECT ff.FileName, ff.FastaId, ff.Loaded, runs.Runs
    FROM prot.FastaFiles ff LEFT OUTER JOIN
        (SELECT FastaId, COUNT(Run) AS Runs
        FROM ms2.MS2Runs
        GROUP BY FastaId) runs ON runs.FastaId = ff.FastaId;

-- Rebuild MS2ExperimentRuns view to pick up FastaId column name change
DROP VIEW ms2.MS2ExperimentRuns;

CREATE VIEW ms2.MS2ExperimentRuns AS
    SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId as ExperimentRunRowId, exp.Protocol.Name As ProtocolName
    FROM ms2.MS2Runs
        LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ExperimentRunLSID
        LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID;

-- Add column for retention time
ALTER TABLE ms2.MS2PeptidesData
    ADD COLUMN RetentionTime REAL NULL;

-- Rebuild peptide views to join to new table name and use new column name
DROP VIEW ms2.MS2Peptides;
DROP VIEW ms2.MS2SimplePeptides;

CREATE VIEW ms2.MS2SimplePeptides AS
    SELECT frac.run, run.description AS rundescription, pep.fraction, substring(frac.filename, 1, position('.' in frac.filename)-1) as fractionname, pep.scan,
    pep.retentiontime, pep.charge, pep.score1 AS rawscore, pep.score2 AS diffscore, pep.score3 AS zscore, pep.score1 AS spscore, pep.score2 AS deltacn, pep.score3 AS xcorr, pep.score4 AS sprank, pep.score1 AS hyper, pep.score2 AS "next", pep.score3 AS b, pep.score4 AS y, pep.score5 AS expect, pep.score1 AS ion, pep.score2 AS identity, pep.score3 AS homology, pep.ionpercent, pep.mass, pep.deltamass, pep.mass + pep.deltamass AS precursormass, abs(pep.deltamass - round(pep.deltamass::double precision)) AS fractionaldeltamass,
        CASE
            WHEN pep.mass = 0::double precision THEN 0::double precision
            ELSE abs(1000000::double precision * abs(pep.deltamass - round(pep.deltamass::double precision)) / (pep.mass + ((pep.charge - 1)::numeric * 1.007276)::double precision))
        END AS fractionaldeltamassppm,
        CASE
            WHEN pep.mass = 0::double precision THEN 0::double precision
            ELSE abs(1000000::double precision * pep.deltamass / (pep.mass + ((pep.charge - 1)::numeric * 1.007276)::double precision))
        END AS deltamassppm,
        CASE
            WHEN pep.charge = 0 THEN 0::double precision
            ELSE (pep.mass + pep.deltamass + ((pep.charge - 1)::numeric * 1.007276)::double precision) / pep.charge::double precision
        END AS mz, pep.peptideprophet, pep.peptide, pep.proteinhits, pep.protein, pep.prevaa, pep.trimmedpeptide, pep.nextaa, ltrim(rtrim((pep.prevaa::text || pep.trimmedpeptide::text) || pep.nextaa::text)) AS strippedpeptide, pep.sequenceposition, pep.seqid, pep.rowid,
        quant.DecimalRatio, quant.Heavy2LightRatio, quant.HeavyArea, quant.HeavyFirstScan, quant.HeavyLastScan, quant.HeavyMass, quant.LightArea, quant.LightFirstScan, quant.LightLastScan, quant.LightMass, quant.Ratio,
        proph.ProphetFVal, proph.ProphetDeltaMass, proph.ProphetNumTrypticTerm, proph.ProphetNumMissedCleav
   FROM ms2.ms2peptidesdata pep
   JOIN ms2.ms2fractions frac ON pep.fraction = frac.fraction
   JOIN ms2.ms2runs run ON frac.run = run.run
   LEFT JOIN ms2.quantitation quant ON pep.rowid=quant.peptideid
   LEFT JOIN ms2.peptideprophetdata proph ON pep.rowid=proph.peptideid;

CREATE VIEW ms2.MS2peptides AS
    SELECT pep.*, seq.description, seq.bestgenename AS genename
    FROM ms2.ms2SimplePeptides pep
    LEFT JOIN prot.sequences seq ON seq.seqid = pep.seqid;
