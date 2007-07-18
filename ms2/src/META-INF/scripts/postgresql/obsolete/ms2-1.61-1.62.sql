-- Previous to CPAS 1.5, some runs ended up with PeptideCount = 0 & SpectrumCount = 0; this corrects those runs.
UPDATE ms2.MS2Runs SET
    PeptideCount = (SELECT COUNT(*) AS PepCount FROM ms2.MS2Peptides pep WHERE pep.run = ms2.MS2Runs.run),
    SpectrumCount = (SELECT COUNT(*) AS SpecCount FROM ms2.MS2Spectra spec WHERE spec.run = ms2.MS2Runs.run)
WHERE (PeptideCount = 0);

-- Index to speed up deletes from MS2PeptidesData
CREATE INDEX IX_MS2PeptideMemberships_PeptideId ON ms2.MS2PeptideMemberships(PeptideId);

-- Simplify MS2 table names
ALTER TABLE ms2.MS2Fractions RENAME TO Fractions;
ALTER TABLE ms2.MS2History RENAME TO History;
ALTER TABLE ms2.MS2Modifications RENAME TO Modifications;
ALTER TABLE ms2.MS2PeptideMemberships RENAME TO PeptideMemberships;
ALTER TABLE ms2.MS2PeptidesData RENAME TO PeptidesData;
ALTER TABLE ms2.MS2ProteinGroupMemberships RENAME TO ProteinGroupMemberships;
ALTER TABLE ms2.MS2ProteinGroups RENAME TO ProteinGroups;
ALTER TABLE ms2.MS2ProteinProphetFiles RENAME TO ProteinProphetFiles;
ALTER TABLE ms2.MS2Runs RENAME TO Runs;
ALTER TABLE ms2.MS2SpectraData RENAME TO SpectraData;

-- Rebuild all views to point to the right table names
DROP VIEW ms2.MS2Spectra;
DROP VIEW ms2.MS2ExperimentRuns;
DROP VIEW ms2.MS2Peptides;
DROP VIEW ms2.MS2SimplePeptides;
DROP VIEW prot.FastaAdmin;
DROP VIEW ms2.ProteinGroupsWithQuantitation;

CREATE VIEW ms2.Spectra AS
    SELECT f.Run AS Run, sd.*
    FROM ms2.SpectraData sd INNER JOIN
        ms2.Fractions f ON sd.Fraction = f.Fraction;

CREATE VIEW ms2.ExperimentRuns AS
    SELECT r.*, er.RowId as ExperimentRunRowId, ep.Name As ProtocolName
    FROM ms2.Runs r
        LEFT OUTER JOIN exp.ExperimentRun er ON er.LSID=r.ExperimentRunLSID
        LEFT OUTER JOIN exp.Protocol ep ON ep.LSID=er.ProtocolLSID;

CREATE VIEW ms2.SimplePeptides AS
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
    FROM ms2.PeptidesData pep
    JOIN ms2.Fractions frac ON pep.Fraction = frac.Fraction
    JOIN ms2.Runs run ON frac.Run = run.Run
    LEFT JOIN ms2.Quantitation quant ON pep.rowid=quant.peptideid
    LEFT JOIN ms2.PeptideProphetdata proph ON pep.rowid=proph.peptideid;

CREATE VIEW ms2.Peptides AS
    SELECT pep.*, seq.Description, seq.BestGeneName AS GeneName
    FROM ms2.SimplePeptides pep
    LEFT JOIN prot.Sequences seq ON seq.SeqId = pep.SeqId;

CREATE VIEW prot.FastaAdmin AS
    SELECT ff.FileName, ff.FastaId, ff.Loaded, runs.Runs
    FROM prot.FastaFiles ff LEFT OUTER JOIN
        (SELECT FastaId, COUNT(Run) AS Runs
        FROM ms2.Runs
        GROUP BY FastaId) runs ON runs.FastaId = ff.FastaId;

CREATE VIEW ms2.ProteinGroupsWithQuantitation AS
    SELECT * FROM ms2.ProteinGroups LEFT JOIN ms2.ProteinQuantitation ON ProteinGroupId = RowId;

