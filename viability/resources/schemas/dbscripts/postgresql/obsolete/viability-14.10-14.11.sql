/*
 * Copyright (c) 2014 LabKey Corporation
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


