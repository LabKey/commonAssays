/*
 * Copyright (c) 2011 LabKey Corporation
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

-- Don't allow AnalyteId to be NULL in the exclusion tables

DROP INDEX UQ_WellExclusionAnalyte ON luminex.WellExclusionAnalyte
GO

ALTER TABLE luminex.WellExclusionAnalyte ALTER COLUMN AnalyteId INT NOT NULL
GO

ALTER TABLE luminex.WellExclusionAnalyte ADD CONSTRAINT PK_LuminexWellExclusionAnalyte PRIMARY KEY (AnalyteId, WellExclusionId)
GO

CREATE INDEX IDX_LuminexWellExclusionAnalyte_WellExclusionID ON luminex.WellExclusionAnalyte(WellExclusionId)
GO

DROP INDEX UQ_RunExclusionAnalyte ON luminex.RunExclusionAnalyte
GO

ALTER TABLE luminex.RunExclusionAnalyte ALTER COLUMN AnalyteId INT NOT NULL
GO

ALTER TABLE luminex.RunExclusionAnalyte ADD CONSTRAINT PK_LuminexRunExclusionAnalyte PRIMARY KEY (AnalyteId, RunId)
GO

CREATE INDEX IDX_LuminexRunExclusionAnalyte_RunID ON luminex.RunExclusionAnalyte(RunId)
GO

