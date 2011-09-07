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

ALTER TABLE luminex.GuideSet ADD TitrationName VARCHAR(255)
GO
ALTER TABLE luminex.GuideSet ADD Comment TEXT
GO
ALTER TABLE luminex.GuideSet ADD CreatedBy USERID
GO
ALTER TABLE luminex.GuideSet ADD Created DATETIME
GO
ALTER TABLE luminex.GuideSet ADD ModifiedBy USERID
GO
ALTER TABLE luminex.GuideSet ADD Modified DATETIME
GO

ALTER TABLE luminex.AnalyteTitration ADD GuideSetId INT
GO
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalytTitration_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet(RowId)
GO

ALTER TABLE luminex.AnalyteTitration ADD IncludeInGuideSetCalculation BIT
GO
UPDATE luminex.AnalyteTitration SET IncludeInGuideSetCalculation = 0
GO
ALTER TABLE luminex.AnalyteTitration ALTER COLUMN IncludeInGuideSetCalculation BIT NOT NULL
GO

CREATE INDEX IDX_AnalyteTitration_GuideSetId ON luminex.AnalyteTitration(GuideSetId)
GO

DROP INDEX IDX_Analyte_GuideSetId ON luminex.Analyte
GO
ALTER TABLE luminex.Analyte DROP CONSTRAINT FK_Analyte_GuideSetId
GO
ALTER TABLE luminex.Analyte DROP COLUMN GuideSetId
GO
ALTER TABLE luminex.Analyte DROP COLUMN IncludeInGuideSetCalculation
GO
