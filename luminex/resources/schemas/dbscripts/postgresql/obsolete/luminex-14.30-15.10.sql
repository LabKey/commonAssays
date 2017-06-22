/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

/* luminex-14.30-14.31.sql */

ALTER TABLE luminex.GuideSet ADD EC504PLEnabled BOOLEAN;
ALTER TABLE luminex.GuideSet ADD EC505PLEnabled BOOLEAN;
ALTER TABLE luminex.GuideSet ADD AUCEnabled BOOLEAN;
ALTER TABLE luminex.GuideSet ADD MaxFIEnabled BOOLEAN;
UPDATE luminex.GuideSet SET EC504PLEnabled=TRUE, EC505PLEnabled=TRUE, AUCEnabled=TRUE, MaxFIEnabled=TRUE;
ALTER TABLE luminex.GuideSet ALTER COLUMN EC504PLEnabled SET NOT NULL;
ALTER TABLE luminex.GuideSet ALTER COLUMN EC505PLEnabled SET NOT NULL;
ALTER TABLE luminex.GuideSet ALTER COLUMN AUCEnabled SET NOT NULL;
ALTER TABLE luminex.GuideSet ALTER COLUMN MaxFIEnabled SET NOT NULL;

/* luminex-14.31-14.32.sql */

ALTER TABLE luminex.GuideSet ALTER COLUMN EC504PLEnabled SET DEFAULT TRUE;
ALTER TABLE luminex.GuideSet ALTER COLUMN EC505PLEnabled SET DEFAULT TRUE;
ALTER TABLE luminex.GuideSet ALTER COLUMN AUCEnabled SET DEFAULT TRUE;
ALTER TABLE luminex.GuideSet ALTER COLUMN MaxFIEnabled SET DEFAULT TRUE;

/* luminex-14.32-14.33.sql */

CREATE INDEX IX_LuminexSinglePointControl_RunId ON luminex.SinglePointControl (RunId);
CREATE INDEX IDX_AnalyteSinglePointControl_AnalyteId ON luminex.AnalyteSinglePointControl(AnalyteId);

/* luminex-14.33-14.34.sql */

ALTER TABLE luminex.GuideSet ADD IsTitration BOOLEAN;
UPDATE luminex.GuideSet SET IsTitration =
  (
  /* Set the control type to single point control if the GuideSet is used in the AnalyteSinglePointControl table.
     Default to setting the control type to titration if not found. */
    CASE WHEN RowId IN (SELECT DISTINCT GuideSetId FROM luminex.AnalyteSinglePointControl WHERE GuideSetId IS NOT NULL) THEN FALSE ELSE TRUE END
  )
;
ALTER TABLE luminex.GuideSet ALTER COLUMN IsTitration SET NOT NULL;