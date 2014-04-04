/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

CREATE TABLE luminex.AnalyteSinglePointControl
(
    RowId INT IDENTITY(1,1) NOT NULL,
  	SinglePointControlId INT NOT NULL,
  	AnalyteId INT NOT NULL,
  	GuideSetId INT,
    IncludeInGuideSetCalculation BIT NOT NULL,

		CONSTRAINT PK_AnalyteSinglePointControl PRIMARY KEY (AnalyteId, SinglePointControlId),
  	CONSTRAINT FK_AnalyteSinglePointControl_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte (RowId),
  	CONSTRAINT FK_AnalyteSinglePointControl_SinglePointControlId FOREIGN KEY (SinglePointControlId) REFERENCES luminex.SinglePointControl (RowId),
  	CONSTRAINT FK_AnalyteSinglePointControl_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet (RowId)
);

CREATE INDEX IDX_AnalyteSinglePointControl_GuideSetId ON luminex.AnalyteSinglePointControl(GuideSetId);
CREATE INDEX IDX_AnalyteSinglePointControl_SinglePointControlId ON luminex.AnalyteSinglePointControl(SinglePointControlId);

ALTER TABLE luminex.GuideSet DROP COLUMN Titration;