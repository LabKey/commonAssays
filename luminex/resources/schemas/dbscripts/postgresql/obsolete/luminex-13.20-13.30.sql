/*
 * Copyright (c) 2012-2016 LabKey Corporation
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

/* luminex-13.20-13.21.sql */

/* luminex-13.20-13.21.sql */
CREATE TABLE luminex.SinglePointControl
(
    RowId SERIAL NOT NULL,
    RunId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,

    CONSTRAINT PK_Luminex_SinglePointControl PRIMARY KEY (RowId),
    CONSTRAINT FK_Luminex_SinglePointControl_RunId FOREIGN KEY (RunId) REFERENCES exp.ExperimentRun(RowId),
    CONSTRAINT UQ_SinglePointControl UNIQUE (Name, RunId)
);

/* luminex-13.21-13.22.sql */

ALTER TABLE luminex.GuideSet RENAME COLUMN TitrationName TO ControlName;

ALTER TABLE luminex.GuideSet ADD COLUMN Titration BOOLEAN;

UPDATE luminex.GuideSet SET Titration = TRUE;

ALTER TABLE luminex.GuideSet ALTER COLUMN Titration SET NOT NULL;

/* luminex-13.22-13.23.sql */

CREATE TABLE luminex.AnalyteSinglePointControl
(
  	SinglePointControlId INT NOT NULL,
  	AnalyteId INT NOT NULL,
  	GuideSetId INT,
    IncludeInGuideSetCalculation BOOLEAN NOT NULL,

		CONSTRAINT PK_AnalyteSinglePointControl PRIMARY KEY (AnalyteId, SinglePointControlId),
  	CONSTRAINT FK_AnalyteSinglePointControl_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte (RowId),
  	CONSTRAINT FK_AnalyteSinglePointControl_SinglePointControlId FOREIGN KEY (SinglePointControlId) REFERENCES luminex.SinglePointControl (RowId),
  	CONSTRAINT FK_AnalyteSinglePointControl_GuideSetId FOREIGN KEY (GuideSetId) REFERENCES luminex.GuideSet (RowId)
);

CREATE INDEX IDX_AnalyteSinglePointControl_GuideSetId ON luminex.AnalyteSinglePointControl(GuideSetId);
CREATE INDEX IDX_AnalyteSinglePointControl_SinglePointControlId ON luminex.AnalyteSinglePointControl(SinglePointControlId);

ALTER TABLE luminex.GuideSet DROP COLUMN Titration;