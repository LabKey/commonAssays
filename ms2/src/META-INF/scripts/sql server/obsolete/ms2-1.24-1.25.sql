ALTER TABLE ms2.ms2peptidememberships DROP CONSTRAINT FK_MS2PeptideMembership_MS2PeptidesData
GO
ALTER TABLE ms2.ms2peptidesdata DROP CONSTRAINT UQ_PeptidesData
GO

DROP VIEW ms2.ms2peptides
GO
DROP VIEW ms2.ms2simplepeptides
GO

ALTER TABLE ms2.ms2peptidesdata DROP COLUMN RowId
GO
ALTER TABLE ms2.ms2peptidesdata ADD RowId BIGINT IDENTITY (1, 1) NOT NULL
GO
ALTER TABLE ms2.ms2peptidesdata ADD CONSTRAINT UQ_PeptidesData UNIQUE (RowId)
GO

DROP TABLE ms2.MS2PeptideMemberships
GO
CREATE TABLE ms2.MS2PeptideMemberships
(
	PeptideId BIGINT NOT NULL,
	ProteinGroupId INT NOT NULL,

	CONSTRAINT PK_MS2PeptideMemberships                PRIMARY KEY (ProteinGroupId, PeptideId),
	CONSTRAINT FK_MS2PeptideMembership_MS2PeptidesData        FOREIGN KEY (PeptideId)       REFERENCES ms2.MS2PeptidesData (RowId),
	CONSTRAINT FK_MS2PeptideMembership_MS2ProteinGroup   FOREIGN KEY (ProteinGroupId)  REFERENCES ms2.MS2ProteinGroups (RowId)
)
GO

CREATE VIEW ms2.MS2SimplePeptides AS SELECT
    frac.Run, run.Description AS RunDescription, pep.Fraction, Scan, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore, Score1 As SpScore,
    Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 As Hyper, Score2 As Next, Score3 As B, Score4 As Y,
    Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 AS Homology, IonPercent, pep.Mass, DeltaMass,
    (pep.Mass + DeltaMass) AS PrecursorMass, ABS(DeltaMass - ROUND(DeltaMass, 0)) AS FractionalDeltaMass,
    CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * ABS(DeltaMass - ROUND(DeltaMass, 0)) / (pep.Mass + (Charge - 1) * 1.007276)) END AS FractionalDeltaMassPPM,
	CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (pep.Mass + (Charge - 1) * 1.007276)) END AS DeltaMassPPM,
    CASE WHEN Charge = 0 THEN 0 ELSE (pep.Mass + DeltaMass + (Charge - 1) * 1.007276) / Charge END AS MZ, PeptideProphet, Peptide,
	ProteinHits, Protein, PrevAA, TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide,
	SequencePosition, pep.SeqId, pep.RowId FROM ms2.MS2PeptidesData pep
        INNER JOIN
            ms2.MS2Fractions frac ON pep.Fraction = frac.Fraction
        INNER JOIN
            ms2.MS2Runs run ON frac.Run = run.Run
GO

CREATE VIEW ms2.ms2peptides AS
 SELECT pep.*, seq.description, seq.bestgenename AS genename
   FROM ms2.ms2SimplePeptides pep
   LEFT JOIN prot.protsequences seq ON seq.seqid = pep.seqid
GO
