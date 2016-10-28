/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

/* luminex-14.10-14.11.sql */

ALTER TABLE luminex.GuideSet ADD ValueBased BIT;
GO
UPDATE luminex.GuideSet SET ValueBased = 0;
ALTER TABLE luminex.GuideSet ALTER COLUMN ValueBased BIT NOT NULL;

ALTER TABLE luminex.GuideSet ADD EC504PLAverage REAL;
ALTER TABLE luminex.GuideSet ADD EC504PLStdDev REAL;
ALTER TABLE luminex.GuideSet ADD EC505PLAverage REAL;
ALTER TABLE luminex.GuideSet ADD EC505PLStdDev REAL;
ALTER TABLE luminex.GuideSet ADD AUCAverage REAL;
ALTER TABLE luminex.GuideSet ADD AUCStdDev REAL;
ALTER TABLE luminex.GuideSet ADD MaxFIAverage REAL;
ALTER TABLE luminex.GuideSet ADD MaxFIStdDev REAL;

/* luminex-14.11-14.12.sql */

ALTER TABLE luminex.GuideSet ALTER COLUMN EC504PLAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ALTER COLUMN EC504PLStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ALTER COLUMN EC505PLAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ALTER COLUMN EC505PLStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ALTER COLUMN AUCAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ALTER COLUMN AUCStdDev DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ALTER COLUMN MaxFIAverage DOUBLE PRECISION;
ALTER TABLE luminex.GuideSet ALTER COLUMN MaxFIStdDev DOUBLE PRECISION;

/* luminex-14.12-14.13.sql */

-- Clear out empty strings that were imported instead of treating them as null values. See issue 19837.
-- OntologyManager doesn't bother storing NULL values, so just delete the rows from exp.ObjectProperty

DELETE FROM exp.objectproperty
WHERE
  PropertyId IN (
    SELECT
      PropertyId
    FROM
      exp.propertydescriptor
    WHERE
      PropertyURI LIKE '%:AssayDomain-ExcelRun.%' AND
      RangeURI = 'http://www.w3.org/2001/XMLSchema#string'
  ) AND
  StringValue = '' AND
  MVIndicator IS NULL;