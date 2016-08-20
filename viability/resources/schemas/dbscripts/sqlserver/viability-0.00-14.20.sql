/*
 * Copyright (c) 2009-2012 LabKey Corporation
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

/* viability-0.00-9.30.sql */

CREATE SCHEMA viability;
GO

CREATE TABLE viability.Results
(
    RowID INT IDENTITY(1,1) NOT NULL,
    Container EntityID NOT NULL,
    ProtocolID INT NOT NULL,
    DataID INT NOT NULL,
    ObjectID INT NOT NULL,

    Date DATETIME NULL,
    VisitID FLOAT,
    ParticipantID VARCHAR(32),

    -- assay data
    SampleNum INT NOT NULL DEFAULT 0,
    PoolID VARCHAR(50) NOT NULL,
    TotalCells INT NOT NULL,
    ViableCells INT NOT NULL,

    CONSTRAINT PK_Viability_Results PRIMARY KEY (RowID),
    CONSTRAINT FK_Results_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityID),
    CONSTRAINT FK_Results_ProtocolID FOREIGN KEY (ProtocolID) REFERENCES exp.Protocol (RowID),
    CONSTRAINT FK_Viability_DataID FOREIGN KEY (DataID) REFERENCES exp.Data(RowId),
    CONSTRAINT FK_Viability_ObjectID FOREIGN KEY (ObjectID) REFERENCES exp.Object(ObjectId)
);
CREATE INDEX IDX_Results_Container_ProtocolID ON viability.Results(Container, ProtocolID);

CREATE TABLE viability.ResultSpecimens
(
    ResultID INT NOT NULL,
    SpecimenID VARCHAR(32) NOT NULL,
    SpecimenIndex INT NOT NULL,

    CONSTRAINT PK_Viability_ResultSpecimens PRIMARY KEY (ResultID, SpecimenIndex),
    CONSTRAINT UQ_Viability_ResultSpecimens_ResultIDSpecimenID UNIQUE (ResultID, SpecimenID),
    CONSTRAINT FK_ResultSpecimens_ResultID FOREIGN KEY (ResultID) REFERENCES viability.Results(RowId)
);

/* viability-14.10-14.20.sql */

-- Add runid column
ALTER TABLE viability.results ADD runid INT;
GO

UPDATE viability.results SET runid = (SELECT d.RunID FROM exp.data d WHERE d.RowID = DataID);

ALTER TABLE viability.results
  ALTER COLUMN runid INT NOT NULL;

ALTER TABLE viability.results
  ADD CONSTRAINT fk_results_runid FOREIGN KEY (runid) REFERENCES exp.experimentrun (rowid);


-- Last date the specimen aggregates were updated
ALTER TABLE viability.results ADD SpecimenAggregatesUpdated DATETIME;

-- Count of specimens in the result row
ALTER TABLE viability.results ADD SpecimenCount INT;
GO

UPDATE viability.results SET SpecimenCount =
    (SELECT COUNT(RS.specimenid) FROM viability.resultspecimens RS WHERE RowID = RS.ResultID);

-- Concatenated list of Specimen IDs
ALTER TABLE viability.results ADD SpecimenIDs VARCHAR(1000);
GO

UPDATE viability.results SET SpecimenIDs =
    (SELECT core.GROUP_CONCAT_DS(SpecimenId, ',', 1) FROM viability.ResultSpecimens RS WHERE RowID = RS.ResultID);


-- Count of specimens matched in the target study.  Calculated in ViabilityManager.updateSpecimenAggregates()
ALTER TABLE viability.results ADD SpecimenMatchCount INT;

-- Concatenated list of matched Specimen IDs in the target study.  Calculated in ViabilityManager.updateSpecimenAggregates()
ALTER TABLE viability.results ADD SpecimenMatches VARCHAR(1000);

-- Sum of cell counts in the matched Specimen vials in the target study.  Calculated in ViabilityManager.updateSpecimenAggregates()
ALTER TABLE viability.results ADD OriginalCells INT;

ALTER TABLE viability.results ADD TargetStudy ENTITYID;
