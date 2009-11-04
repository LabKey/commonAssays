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

-- Add denormalized data to improve query performance
ALTER TABLE viability.Results ADD COLUMN Container UniqueIdentifier;
ALTER TABLE viability.Results ADD COLUMN ProtocolID INT;

-- Set the values for the new columns in existing rows
UPDATE viability.Results SET Container = (SELECT container FROM exp.data d WHERE d.rowid = dataid);
UPDATE viability.Results SET ProtocolID =
  (SELECT p.RowId FROM exp.experimentrun r, exp.data d, exp.protocol p
    WHERE d.rowid = dataid AND r.rowid = d.runid AND r.protocollsid = p.lsid);

-- Lock down the values in the columns
ALTER TABLE viability.Results ALTER COLUMN Container SET NOT NULL;
ALTER TABLE viability.Results ALTER COLUMN ProtocolID SET NOT NULL;

ALTER TABLE viability.Results
	ADD CONSTRAINT FK_Results_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityID);

ALTER TABLE viability.Results
	ADD CONSTRAINT FK_Results_ProtocolID FOREIGN KEY (ProtocolID) REFERENCES exp.Protocol (RowID);

CREATE INDEX IDX_Results_Container_ProtocolID ON viability.Results(Container, ProtocolID);
ANALYZE viability.Results;

ALTER TABLE viability.ResultSpecimens ADD CONSTRAINT FK_ResultSpecimens_ResultID FOREIGN KEY (ResultID) REFERENCES viability.Results(RowId);
ANALYZE viability.ResultSpecimens;
