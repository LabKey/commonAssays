/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
-- Eliminate unique requirement on Date column, which is unnecessary and causes constraint violations
ALTER TABLE ms2.MS2History DROP CONSTRAINT PK_MS2History;
CREATE INDEX PK_MS2History ON ms2.MS2History (Date);

-- Move SeqId into MS2PeptidesData to eliminate inefficient joins
DROP VIEW ms2.MS2Peptides;

CREATE VIEW ms2.MS2Peptides AS SELECT
    frac.Run, pep.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore, Score1 As SpScore,
    Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next, Score3 As B, Score4 As Y,
    Score5 As Expect, Score1 As Ion, Score2 As Identity, Score3 AS Homology, IonPercent, pep.Mass, DeltaMass,
    (pep.Mass + DeltaMass) AS PrecursorMass,
	CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (pep.Mass + (Charge - 1) * 1.00794)) END AS DeltaMassPPM,
    CASE WHEN Charge = 0 THEN 0 ELSE (pep.Mass + DeltaMass + (Charge - 1) * 1.00794) / Charge END AS MZ, PeptideProphet, Peptide,
	ProteinHits, Protein, PrevAA, TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA || TrimmedPeptide || NextAA)) AS StrippedPeptide,
	SequencePosition, pep.SeqId, seq.Description AS Description, BestGeneName AS GeneName FROM ms2.MS2PeptidesData pep
        INNER JOIN
            ms2.MS2Fractions frac ON pep.Fraction = frac.Fraction
        LEFT OUTER JOIN
            prot.ProtSequences seq ON seq.SeqId = pep.SeqId;
