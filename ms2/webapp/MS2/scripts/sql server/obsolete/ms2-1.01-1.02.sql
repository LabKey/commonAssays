DROP VIEW ms2.MS2Peptides
GO

CREATE VIEW ms2.MS2Peptides AS
   SELECT
      ms2.MS2Fractions.Run, ms2.MS2PeptidesData.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore,
      Score1 AS SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next,
      Score3 AS B, Score4 As Y, Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 As Homology, IonPercent,
      ms2.MS2PeptidesData.Mass, DeltaMass, (ms2.MS2PeptidesData.Mass + DeltaMass) AS PrecursorMass,
      CASE WHEN ms2.MS2PeptidesData.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (ms2.MS2PeptidesData.Mass + (Charge - 1) * 1.00794)) END AS DeltaMassPPM,
      CASE WHEN Charge = 0 THEN 0 ELSE (ms2.MS2PeptidesData.Mass + DeltaMass + (Charge - 1) * 1.00794) / Charge END AS MZ,
      PeptideProphet, Peptide, ProteinHits, Protein, PrevAA, TrimmedPeptide,
      NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide, SequencePosition,
      prot.ProtSequences.Description, BestGeneName AS GeneName, prot.protsequences.seqid AS SeqId
   FROM ms2.MS2PeptidesData
      INNER JOIN
         ms2.MS2Fractions ON ms2.MS2PeptidesData.Fraction = ms2.MS2Fractions.Fraction
      INNER JOIN
         ms2.MS2Runs ON ms2.MS2Fractions.Run = ms2.MS2Runs.Run
      LEFT OUTER JOIN
         prot.ProteinSequences ON
            prot.ProteinSequences.LookupString = Protein AND
            prot.ProteinSequences.DataBaseId = ms2.MS2Runs.DataBaseId
      LEFT OUTER JOIN
            prot.ProtSequences ON prot.ProtSequences.SeqId = prot.ProteinSequences.SeqId
GO

CREATE INDEX IX_ProtOrganisms_Genus on prot.ProtOrganisms(Genus)
GO

CREATE INDEX IX_ProtOrganisms_Species on prot.ProtOrganisms(Species)
GO

