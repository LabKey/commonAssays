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
ALTER TABLE prot.organisms ALTER COLUMN CommonName varchar(100)
GO

-- Create indexes on ms2 Runs table to support common operations in MS2Manager
CREATE INDEX MS2Runs_Stats ON ms2.Runs(PeptideCount, SpectrumCount, Deleted, StatusId)
GO
CREATE INDEX MS2Runs_ExperimentRunLSID ON ms2.Runs(ExperimentRunLSID)
GO

-- Use fn_dropifexists to make GO index drop function more reliable
DROP PROCEDURE prot.drop_go_indexes
GO

CREATE PROCEDURE prot.drop_go_indexes AS
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Constraint', 'PK_GoTerm'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'IX_GoTerm_Name'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'IX_GoTerm_TermType'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'UQ_GoTerm_Acc'

    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Constraint', 'PK_GoTerm2Term'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term2Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1_2_Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_relationshipTypeId'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'UQ_GoTerm2Term_1_2_R'

    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Constraint', 'PK_GoGraphPath'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term2Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1_2_Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_t1_distance'

    EXEC core.fn_dropifexists 'gotermdefinition', 'prot', 'Index', 'IX_GoTermDefinition_dbXrefId'
    EXEC core.fn_dropifexists 'gotermdefinition', 'prot', 'Index', 'UQ_GoTermDefinition_termId'

    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_SynonymTypeId'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_TermId'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_termSynonym'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym'
GO

EXEC core.fn_dropifexists 'Runs', 'ms2', 'INDEX','MS2Runs_Container'
GO
CREATE INDEX MS2Runs_Container ON ms2.Runs(Container)
GO

DROP VIEW ms2.Peptides
DROP VIEW ms2.SimplePeptides
GO

-- Add EndScan to PeptidesData table
DROP INDEX ms2.PeptidesData.UQ_MS2PeptidesData_FractionScanCharge
GO

ALTER TABLE ms2.PeptidesData
    ADD EndScan INT NULL
GO

CREATE UNIQUE CLUSTERED INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge)
GO

CREATE VIEW ms2.SimplePeptides AS SELECT
    frac.Run, run.Description AS RunDescription, pep.Fraction, LEFT(frac.FileName, CHARINDEX('.', frac.FileName) - 1) AS FractionName, Scan, EndScan,
    RetentionTime, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore, Score1 As SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 AS OrigScore,
    Score1 As Hyper, Score2 As Next, Score3 As B, Score4 As Y, Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 AS Homology,
    IonPercent, pep.Mass, DeltaMass, (pep.Mass + DeltaMass) AS PrecursorMass, ABS(DeltaMass - ROUND(DeltaMass, 0)) AS FractionalDeltaMass,
    CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * ABS(DeltaMass - ROUND(DeltaMass, 0)) / (pep.Mass + (Charge - 1) * 1.007276)) END AS FractionalDeltaMassPPM,
	CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (pep.Mass + (Charge - 1) * 1.007276)) END AS DeltaMassPPM,
    CASE WHEN Charge = 0 THEN 0 ELSE (pep.Mass + DeltaMass + (Charge - 1) * 1.007276) / Charge END AS MZ, PeptideProphet, PeptideProphetErrorRate, Peptide, ProteinHits,
    Protein, PrevAA, TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide,	SequencePosition, pep.SeqId, pep.RowId,
    quant.DecimalRatio, quant.Heavy2LightRatio, quant.HeavyArea, quant.HeavyFirstScan, quant.HeavyLastScan, quant.HeavyMass, quant.LightArea, quant.LightFirstScan, quant.LightLastScan, quant.LightMass, quant.Ratio,
    proph.ProphetFVal, proph.ProphetDeltaMass, proph.ProphetNumTrypticTerm, proph.ProphetNumMissedCleav
    FROM ms2.PeptidesData pep
        INNER JOIN
            ms2.Fractions frac ON pep.Fraction = frac.Fraction
        INNER JOIN
            ms2.Runs run ON frac.Run = run.Run
    LEFT JOIN ms2.quantitation quant ON pep.rowid=quant.peptideid
    LEFT JOIN ms2.peptideprophetdata proph ON pep.rowid=proph.peptideid
GO

CREATE VIEW ms2.Peptides AS
    SELECT pep.*, seq.description, seq.bestgenename AS genename
    FROM ms2.SimplePeptides pep
    LEFT JOIN prot.sequences seq ON seq.seqid = pep.seqid
GO

