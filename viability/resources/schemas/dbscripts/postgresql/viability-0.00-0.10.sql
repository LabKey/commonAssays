/*
 * Copyright (c) 2009 LabKey Corporation
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
CREATE SCHEMA viability;

CREATE TABLE viability.ResultSpecimens
(
  ResultID INT NOT NULL,
  SpecimenID TEXT NOT NULL,
  Index INT NOT NULL,

  CONSTRAINT PK_Viability_ResultSpecimens PRIMARY KEY (ResultID, SpecimenID, Index)
);

CREATE TABLE viability.Results
(
  RowID SERIAL NOT NULL,
  DataID INT NOT NULL,
  ObjectID INT NOT NULL,

  Date TIMESTAMP,
  VisitID FLOAT,
  ParticipantID VARCHAR(32),

  -- assay data
  PoolID VARCHAR(50) NOT NULL,
  TotalCells INT NOT NULL,
  ViableCells INT NOT NULL,

  CONSTRAINT PK_Viability_Results PRIMARY KEY (RowID),
  CONSTRAINT FK_Viability_DataID FOREIGN KEY (DataID) REFERENCES exp.Data(RowId),
  CONSTRAINT FK_Viability_ObjectID FOREIGN KEY (ObjectID) REFERENCES exp.Object(ObjectId)
);
