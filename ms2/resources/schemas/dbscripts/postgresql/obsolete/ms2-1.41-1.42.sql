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
-- Store peptide and spectrum counts with each run to make computing stats much faster
ALTER TABLE ms2.MS2Runs
    ADD COLUMN NegativeHitCount INT NOT NULL DEFAULT 0;

-- Update counts for existing runs
--UPDATE ms2.MS2Runs SET NegativeHitCount = NegHitCount FROM
--    (SELECT Run, COUNT(*) AS NegHitCount FROM ms2.MS2PeptidesData pd INNER JOIN ms2.MS2Fractions f ON pd.Fraction = f.Fraction WHERE pd.Protein LIKE 'rev_%' GROUP BY Run) x
--WHERE ms2.MS2Runs.Run = x.Run;

-- No changes to this view, but we need to rebuild it since we added two columns to MS2Runs
DROP VIEW ms2.MS2ExperimentRuns;

CREATE OR REPLACE VIEW ms2.MS2ExperimentRuns AS
SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId as ExperimentRunRowId, exp.Protocol.Name As ProtocolName
FROM ms2.MS2Runs
LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ExperimentRunLSID
LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID;

CREATE TABLE ms2.PeptideProphetData
(
	PeptideId BIGINT NOT NULL,
    ProphetFVal REAL NOT NULL,
    ProphetDeltaMass REAL NULL,
    ProphetNumTrypticTerm INT NULL,
    ProphetNumMissedCleav INT NULL,

    CONSTRAINT PK_PeptideProphetData PRIMARY KEY (PeptideId),
    CONSTRAINT FK_PeptideProphetData_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.MS2PeptidesData(RowId)
);

DROP VIEW ms2.MS2Peptides;
DROP VIEW ms2.MS2SimplePeptides;

CREATE OR REPLACE VIEW ms2.ms2SimplePeptides AS
 SELECT frac.run, run.description AS rundescription, pep.fraction, substring(frac.filename, 1, position('.' in frac.filename)-1) as fractionname, pep.scan, pep.charge, pep.score1 AS rawscore, pep.score2 AS diffscore, pep.score3 AS zscore, pep.score1 AS spscore, pep.score2 AS deltacn, pep.score3 AS xcorr, pep.score4 AS sprank, pep.score1 AS hyper, pep.score2 AS "next", pep.score3 AS b, pep.score4 AS y, pep.score5 AS expect, pep.score1 AS ion, pep.score2 AS identity, pep.score3 AS homology, pep.ionpercent, pep.mass, pep.deltamass, pep.mass + pep.deltamass AS precursormass, abs(pep.deltamass - round(pep.deltamass::double precision)) AS fractionaldeltamass,
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

CREATE OR REPLACE VIEW ms2.ms2peptides AS
 SELECT pep.*, seq.description, seq.bestgenename AS genename
   FROM ms2.ms2SimplePeptides pep
   LEFT JOIN prot.protsequences seq ON seq.seqid = pep.seqid;

ALTER TABLE prot.ProteinDataBases
    ADD ScoringAnalysis boolean NOT NULL DEFAULT FALSE;
