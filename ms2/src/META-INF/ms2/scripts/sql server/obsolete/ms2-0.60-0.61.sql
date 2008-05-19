/*
 * Copyright (c) 2005-2008 Fred Hutchinson Cancer Research Center
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
DROP VIEW MS2Peptides
GO


-- Union of all MS2Peptides columns; alias Score1 to RawScore and SpScore, Score2 to DiffScore and DeltaCn, etc.
CREATE VIEW MS2Peptides AS
   SELECT
      MS2Fractions.Run, MS2PeptidesData.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore,
      Score1 As SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next,
      Score3 As B, Score4 As Y, Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 As Homology, IonPercent,
      MS2PeptidesData.Mass, DeltaMass, (MS2PeptidesData.Mass + DeltaMass) AS PrecursorMass,
      CASE WHEN MS2PeptidesData.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (MS2PeptidesData.Mass + (Charge - 1) * 1.00794)) END AS DeltaMassPPM,
      CASE WHEN Charge = 0 THEN 0 ELSE (MS2PeptidesData.Mass + DeltaMass + (Charge - 1) * 1.00794) / Charge END AS MZ,
      PeptideProphet, Peptide, ProteinHits, Protein, PrevAA, TrimmedPeptide,
      NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide, SequencePosition,
      ProtSequences.Description, ProtSequences.BestGeneName AS GeneName
   FROM MS2PeptidesData
      INNER JOIN
         MS2Fractions ON MS2PeptidesData.Fraction = MS2Fractions.Fraction 
      INNER JOIN
         MS2Runs ON MS2Fractions.Run = MS2Runs.Run 
      LEFT OUTER JOIN
         ProteinSequences ON 
            ProteinSequences.LookupString = MS2PeptidesData.Protein AND 
            ProteinSequences.DataBaseId = MS2Runs.DataBaseId 
      LEFT OUTER JOIN
            ProtSequences ON ProtSequences.SeqId = ProteinSequences.SeqId
GO