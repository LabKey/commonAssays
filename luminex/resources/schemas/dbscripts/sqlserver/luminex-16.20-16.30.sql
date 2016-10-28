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

ALTER TABLE luminex.Analyte ADD BeadNumber NVARCHAR(50);
GO

-- parse out the bead number from the analyte name
UPDATE luminex.Analyte
  SET BeadNumber =
      CASE WHEN CHARINDEX(')', REVERSE(RTRIM(Name))) = 1 THEN
        RTRIM(LTRIM(SUBSTRING(Name, LEN(Name) - CHARINDEX('(', REVERSE(RTRIM(Name))) + 2, CHARINDEX('(', REVERSE(RTRIM(Name))) - CHARINDEX(')', REVERSE(RTRIM(Name))) - 1)))
      ELSE Name END,
  Name =
      CASE WHEN CHARINDEX(')', REVERSE(RTRIM(Name))) = 1 THEN
        SUBSTRING(Name, 1, LEN(Name) - CHARINDEX('(', REVERSE(RTRIM(Name))))
      ELSE Name END;

-- parse out the bead number from the guide set analyte name
UPDATE luminex.GuideSet SET AnalyteName =
  CASE WHEN CHARINDEX(')', REVERSE(RTRIM(AnalyteName))) = 1 THEN
    SUBSTRING(AnalyteName, 1, LEN(AnalyteName) - CHARINDEX('(', REVERSE(RTRIM(AnalyteName))))
  ELSE AnalyteName END;

UPDATE luminex.GuideSet set CurrentGuideSet = 0;
UPDATE luminex.GuideSet SET CurrentGuideSet = 1 WHERE rowId IN (SELECT MAX(rowId) FROM luminex.GuideSet GROUP BY analyteName, protocolId, conjugate, isotype, controlname);

/* luminex-16.21-16.22.sql */

-- need to parse the bead number off of the negative bead name and trim the analyte name
UPDATE luminex.Analyte SET Name = RTRIM(LTRIM(Name)),
  NegativeBead =
  CASE WHEN CHARINDEX(')', REVERSE(RTRIM(NegativeBead))) = 1 THEN
    RTRIM(LTRIM(SUBSTRING(NegativeBead, 1, LEN(NegativeBead) - CHARINDEX('(', REVERSE(RTRIM(NegativeBead))))))
  ELSE NegativeBead END;

-- trim the analyte name from the guide set table
UPDATE luminex.GuideSet SET AnalyteName = RTRIM(LTRIM(AnalyteName));