/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

ALTER TABLE luminex.datarow ADD COLUMN NewOutlier INT;

UPDATE luminex.datarow set NewOutlier = 3 WHERE Outlier = TRUE;

UPDATE luminex.datarow set NewOutlier = 0 WHERE NewOutlier IS NULL;

ALTER TABLE luminex.datarow DROP COLUMN Outlier;

ALTER TABLE luminex.datarow RENAME COLUMN NewOutlier TO Outlier;

-- Correctly migrate existing Luminex records to the right outlier code
UPDATE luminex.datarow set Outlier = 1 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND dr.fioorindicator = '---' AND d.datafileurl like '%Summary%');

UPDATE luminex.datarow set Outlier = 2 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND dr.fioorindicator IS NULL AND d.datafileurl like '%Summary%');

UPDATE luminex.datarow set Outlier = 1 WHERE RowId IN
  (SELECT dr.RowId FROM luminex.datarow dr, exp.data d WHERE
      d.rowid = dr.dataid AND dr.outlier = 3 AND d.datafileurl like '%Raw%');

-- Trim leading spaces from the extraspecimeninfo column
UPDATE luminex.datarow set extraspecimeninfo = substring(extraspecimeninfo from 2) WHERE strpos(extraspecimeninfo, ' ') = 1;