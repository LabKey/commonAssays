-- Store peptide and spectrum counts with each run to make computing stats much faster
ALTER TABLE ms2.MS2Runs ADD
    PeptideCount INT NOT NULL DEFAULT 0,
    SpectrumCount INT NOT NULL DEFAULT 0
GO

-- Update counts for existing runs
UPDATE ms2.MS2Runs SET PeptideCount = PepCount FROM
    (SELECT Run, COUNT(*) AS PepCount FROM ms2.MS2PeptidesData pd INNER JOIN ms2.MS2Fractions f ON pd.Fraction = f.Fraction GROUP BY Run) x
WHERE ms2.MS2Runs.Run = x.Run

UPDATE ms2.MS2Runs SET SpectrumCount = SpecCount FROM
    (SELECT Run, COUNT(*) AS SpecCount FROM ms2.MS2SpectraData sd INNER JOIN ms2.MS2Fractions f ON sd.Fraction = f.Fraction GROUP BY Run) x
WHERE ms2.MS2Runs.Run = x.Run
GO
