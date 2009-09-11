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

-- Relax contraints on quantitation result columns; q3 does not generate string representations of ratios.
ALTER TABLE ms2.Quantitation ALTER COLUMN Ratio VARCHAR(20) NULL
ALTER TABLE ms2.Quantitation ALTER COLUMN Heavy2lightRatio VARCHAR(20) NULL
GO

-- Add a quantitation summary table
CREATE TABLE ms2.QuantSummaries
(
    QuantId INT IDENTITY(1,1) NOT NULL,
    Run INT NOT NULL,
    AnalysisType NVARCHAR(20) NOT NULL,
    AnalysisTime DATETIME NULL,
    Version NVARCHAR(80) NULL,
    LabeledResidues NVARCHAR(20) NULL,
    MassDiff NVARCHAR(80) NULL,
    MassTol REAL NOT NULL,
    SameScanRange CHAR(1) NULL,
    XpressLight INT NULL,

    CONSTRAINT PK_QuantSummaries PRIMARY KEY (QuantId),
    CONSTRAINT FK_QuantSummaries_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs (Run),
    -- Current restrictions allow only one quantitation analysis per run
    CONSTRAINT UQ_QuantSummaries UNIQUE (Run)
)
GO

-- Add a QuantId column to ms2.Quantitation to allow multiple results for each peptide
ALTER TABLE ms2.Quantitation ADD QuantId INT
GO

-- Generate stub quantitation summaries for existing runs (must be xpress with
-- a default mass tolerance; other params unknown)
INSERT INTO ms2.QuantSummaries (Run, AnalysisType, MassTol)
  SELECT DISTINCT(F.Run), 'xpress', 1.0
    FROM ms2.MS2Fractions F
         INNER JOIN ms2.MS2PeptidesData P ON F.Fraction = P.Fraction
         INNER JOIN ms2.Quantitation Q ON P.RowId = Q.PeptideId
GO

-- Add a QuantId from these summaries to existing peptide quantitation records
UPDATE ms2.Quantitation
   SET QuantId = (SELECT S.QuantId FROM ms2.QuantSummaries S, ms2.MS2Runs R, ms2.MS2Fractions F, ms2.MS2PeptidesData P
   WHERE ms2.Quantitation.PeptideId = P.RowId
     AND P.Fraction = F.Fraction
     AND F.Run = R.Run
     AND S.Run = R.Run)
GO

-- QuantId must be non-null; eventually (PeptideId, QuantId) should become a compound PK
ALTER TABLE ms2.Quantitation ALTER COLUMN QuantId INT NOT NULL
GO

