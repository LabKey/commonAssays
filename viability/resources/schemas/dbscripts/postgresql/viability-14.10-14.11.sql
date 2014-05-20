-- Add runid column
ALTER TABLE viability.results ADD runid INT;

UPDATE viability.results SET runid = (SELECT d.RunID FROM exp.data d WHERE d.RowID = DataID);

ALTER TABLE viability.results
  ALTER COLUMN runid SET NOT NULL;

ALTER TABLE viability.results
  ADD CONSTRAINT fk_results_runid FOREIGN KEY (runid) REFERENCES exp.experimentrun (rowid);


-- Last date the specimen aggregates were updated
ALTER TABLE viability.results ADD SpecimenAggregatesUpdated TIMESTAMP;

-- Count of specimens in the result row
ALTER TABLE viability.results ADD SpecimenCount INT;

UPDATE viability.results SET SpecimenCount =
    (SELECT COUNT(RS.specimenid) FROM viability.resultspecimens RS WHERE RowID = RS.ResultID);

-- Concatenated list of Specimen IDs
ALTER TABLE viability.results ADD SpecimenIDs VARCHAR(1000);

UPDATE viability.results SET SpecimenIDs =
    (SELECT array_to_string(array_agg(rs1.SpecimenID), ',')
      FROM (SELECT rs2.SpecimenID, rs2.ResultID FROM viability.ResultSpecimens rs2
        WHERE rs2.ResultID = RowID
        ORDER BY rs2.SpecimenIndex) AS rs1
      GROUP BY rs1.ResultID);


-- Count of specimens matched in the target study.  Calculated in ViabilityManager.updateSpecimenAggregates()
ALTER TABLE viability.results ADD SpecimenMatchCount INT;

-- Concatenated list of matched Specimen IDs in the target study.  Calculated in ViabilityManager.updateSpecimenAggregates()
ALTER TABLE viability.results ADD SpecimenMatches VARCHAR(1000);

-- Sum of cell counts in the matched Specimen vials in the target study.  Calculated in ViabilityManager.updateSpecimenAggregates()
ALTER TABLE viability.results ADD OriginalCells INT;

-- Schedule the viability aggregates to be updated after startup
SELECT core.executeJavaUpgradeCode('updateViabilitySpecimenAggregates');

