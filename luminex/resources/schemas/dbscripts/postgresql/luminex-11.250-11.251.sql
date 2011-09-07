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

ALTER TABLE luminex.GuideSet ADD COLUMN TitrationName VARCHAR(255);
ALTER TABLE luminex.GuideSet ADD COLUMN Comment TEXT;
ALTER TABLE luminex.GuideSet ADD COLUMN CreatedBy USERID;
ALTER TABLE luminex.GuideSet ADD COLUMN Created TIMESTAMP;
ALTER TABLE luminex.GuideSet ADD COLUMN ModifiedBy USERID;
ALTER TABLE luminex.GuideSet ADD COLUMN Modified TIMESTAMP;

ALTER TABLE luminex.AnalyteTitration ADD COLUMN GuideSetId INT;
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalytTitration_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet(RowId);
CREATE INDEX IDX_AnalyteTitration_GuideSetId ON luminex.AnalyteTitration(GuideSetId);

ALTER TABLE luminex.AnalyteTitration ADD COLUMN IncludeInGuideSetCalculation BOOLEAN;
UPDATE luminex.AnalyteTitration SET IncludeInGuideSetCalculation = FALSE;
ALTER TABLE luminex.AnalyteTitration ALTER COLUMN IncludeInGuideSetCalculation SET NOT NULL;

ALTER TABLE luminex.Analyte DROP COLUMN GuideSetId;
ALTER TABLE luminex.Analyte DROP COLUMN IncludeInGuideSetCalculation;
