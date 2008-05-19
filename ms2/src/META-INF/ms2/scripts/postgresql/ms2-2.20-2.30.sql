/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
select core.fn_dropifexists('ProteinGroupMembers', 'cabig', 'VIEW', NULL);
select core.fn_dropifexists('protsequences', 'cabig', 'VIEW', NULL);

ALTER TABLE prot.organisms ALTER COLUMN CommonName TYPE varchar(100);

-- Create indexes on ms2 Runs table to support common operations in MS2Manager

CREATE INDEX MS2Runs_Stats ON ms2.Runs(PeptideCount, SpectrumCount, Deleted, StatusId);
CREATE INDEX MS2Runs_ExperimentRunLSID ON ms2.Runs(ExperimentRunLSID);

-- Use fn_dropifexists to make drop GO indexes function more reliable
CREATE OR REPLACE FUNCTION prot.drop_go_indexes() RETURNS void AS $$
    BEGIN
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Constraint', 'pk_goterm');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'IX_GoTerm_Name');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'IX_GoTerm_TermType');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'UQ_GoTerm_Acc');

        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Constraint', 'pk_goterm2term');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term2Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1_2_Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_relationshipTypeId');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'UQ_GoTerm2Term_1_2_R');

        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Constraint', 'pk_gographpath');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term2Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1_2_Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_t1_distance');

        PERFORM core.fn_dropifexists('gotermdefinition', 'prot', 'Index', 'IX_GoTermDefinition_dbXrefId');
        PERFORM core.fn_dropifexists('gotermdefinition', 'prot', 'Index', 'UQ_GoTermDefinition_termId');

        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_SynonymTypeId');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_TermId');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_termSynonym');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym');
    END;
	$$ LANGUAGE plpgsql;

SELECT core.fn_dropifexists('Runs', 'ms2', 'INDEX','MS2Runs_Container');

CREATE INDEX MS2Runs_Container ON ms2.Runs(Container);

DROP VIEW ms2.Peptides;
DROP VIEW ms2.SimplePeptides;

-- Add EndScan to PeptidesData table
DROP INDEX ms2.UQ_MS2PeptidesData_FractionScanCharge;

ALTER TABLE ms2.PeptidesData
    ADD EndScan INT NULL;

CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData (Fraction, Scan, EndScan, Charge);

CREATE VIEW ms2.SimplePeptides AS
    SELECT frac.run, run.description AS rundescription, pep.fraction, substring(frac.filename, 1, position('.' in frac.filename)-1) as fractionname, pep.scan, pep.endscan, pep.retentiontime, pep.charge,
    pep.score1 AS rawscore, pep.score2 AS diffscore, pep.score3 AS zscore, pep.score1 AS spscore, pep.score2 AS deltacn, pep.score3 AS xcorr, pep.score4 AS sprank, pep.score1 AS hyper, pep.score2 AS "next", pep.score3 AS b, pep.score4 AS y, pep.score5 AS expect, pep.score1 AS ion, pep.score2 AS identity, pep.score3 AS homology, pep.score1 AS origscore,
    pep.ionpercent, pep.mass, pep.deltamass, pep.mass + pep.deltamass AS precursormass, abs(pep.deltamass - round(pep.deltamass::double precision)) AS fractionaldeltamass,
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
        END AS mz, pep.peptideprophet, pep.PeptideProphetErrorRate, pep.peptide, pep.proteinhits, pep.protein, pep.prevaa, pep.trimmedpeptide, pep.nextaa, ltrim(rtrim((pep.prevaa::text || pep.trimmedpeptide::text) || pep.nextaa::text)) AS strippedpeptide, pep.sequenceposition, pep.seqid, pep.rowid,
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

