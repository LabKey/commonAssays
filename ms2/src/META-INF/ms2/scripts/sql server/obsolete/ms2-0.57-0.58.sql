/* Old script... for historical purposes only */
/* Add fourth & fifth MS2 score for SEQUEST & X!Tandem, add SpRank */

ALTER TABLE MS2PeptidesData DROP CONSTRAINT MS2Peptides_PK
go

ALTER TABLE MS2PeptidesData
    ADD Score4 REAL NULL, Score5 REAL NULL
go

ALTER TABLE MS2PeptidesData ADD CONSTRAINT MS2Peptides_PK PRIMARY KEY CLUSTERED (Fraction, Scan, Charge)
go

DROP VIEW MS2Peptides
go

-- Union of all MS2Peptides columns; alias Score1 to RawScore and SpScore, Score2 to DiffScore and DeltaCn, etc.
CREATE VIEW MS2Peptides AS
    SELECT Run, MS2PeptidesData.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore,
        Score1 As SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, IonPercent, Mass, DeltaMass, (Mass + DeltaMass) AS PrecursorMass,
        (Mass + DeltaMass + (Charge - 1) * 1.00794) / Charge AS MZ, PeptideProphet, Peptide, ProteinHits, Protein, PrevAA,
        TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide, SequencePosition
    FROM MS2PeptidesData INNER JOIN MS2Fractions ON MS2PeptidesData.Fraction = MS2Fractions.Fraction
go
