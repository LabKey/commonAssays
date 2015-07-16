/*
 * Copyright (c) 2015 LabKey Corporation
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

ALTER TABLE luminex.GuideSet ADD IsTitration BIT;
GO
UPDATE luminex.GuideSet SET IsTitration =
  (
    /* Set the control type to single point control if the GuideSet is used in the AnalyteSinglePointControl table.
       Default to setting the control type to titration if not found. */
    CASE WHEN RowId IN (SELECT DISTINCT GuideSetId FROM luminex.AnalyteSinglePointControl WHERE GuideSetId IS NOT NULL) THEN 0 ELSE 1 END
  )
;
ALTER TABLE luminex.GuideSet ALTER COLUMN IsTitration BIT NOT NULL;
