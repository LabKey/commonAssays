/*
 * Copyright (c) 2016 LabKey Corporation
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
/* luminex-16.20-16.21.sql */

ALTER TABLE luminex.Analyte ADD BeadNumber VARCHAR(50);

-- parse out the bead number from the analyte name
UPDATE luminex.Analyte
  SET BeadNumber =
    CASE WHEN STRPOS(REVERSE(RTRIM(Name)), ')') = 1 THEN
      TRIM(both FROM (SUBSTRING(Name FROM (LENGTH(Name) - STRPOS(REVERSE(Name), '(') + 2) FOR (STRPOS(REVERSE(Name), '(') - STRPOS(REVERSE(Name), ')') - 1))))
    ELSE Name END,
  Name =
    CASE WHEN STRPOS(REVERSE(RTRIM(Name)), ')') = 1 THEN
      SUBSTRING(Name, 1, LENGTH(Name) - STRPOS(REVERSE(Name), '('))
    ELSE Name END;

-- parse out the bead number from the guide set analyte name
UPDATE luminex.GuideSet SET AnalyteName =
CASE WHEN STRPOS(REVERSE(RTRIM(AnalyteName)), ')') = 1 THEN
  SUBSTRING(AnalyteName, 1, LENGTH(AnalyteName) - STRPOS(REVERSE(AnalyteName), '('))
ELSE AnalyteName END;

UPDATE luminex.GuideSet set CurrentGuideSet = false;
UPDATE luminex.GuideSet SET CurrentGuideSet = true WHERE rowId IN (SELECT MAX(rowId) FROM luminex.GuideSet GROUP BY analyteName, protocolId, conjugate, isotype, controlname);

/* luminex-16.21-16.22.sql */

-- need to parse the bead number off of the negative bead name and trim the analyte name
UPDATE luminex.Analyte SET Name = TRIM(both FROM Name),
  NegativeBead =
    CASE WHEN STRPOS(REVERSE(RTRIM(NegativeBead)), ')') = 1 THEN
      TRIM(both FROM SUBSTRING(NegativeBead, 1, LENGTH(NegativeBead) - STRPOS(REVERSE(NegativeBead), '(')))
    ELSE NegativeBead END;

-- trim the analyte name from the guide set table
UPDATE luminex.GuideSet SET AnalyteName = TRIM(both FROM AnalyteName);