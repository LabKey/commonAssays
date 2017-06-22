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

/* luminex-14.20-14.21.sql */

ALTER TABLE luminex.Titration ADD OtherControl BIT;
GO
UPDATE luminex.Titration SET OtherControl=0;
ALTER TABLE luminex.Titration ALTER COLUMN OtherControl BIT NOT NULL;

/* luminex-14.21-14.22.sql */

ALTER TABLE luminex.Analyte ADD NegativeBead VARCHAR(50);
GO

-- assume that any run/dataid that has a "Blank" analyte was using that as the Negative Control bead
UPDATE luminex.analyte SET NegativeBead = (
  SELECT x.Name FROM luminex.analyte AS a
    JOIN (SELECT DISTINCT DataId, Name FROM luminex.analyte WHERE Name LIKE 'Blank %') AS x ON a.DataId = x.DataId
  WHERE a.DataId IN (SELECT DataId FROM luminex.analyte WHERE Name LIKE 'Blank %') AND a.Name NOT LIKE 'Blank %'
        AND luminex.analyte.RowId = a.RowId)
WHERE NegativeBead IS NULL;

/* luminex-14.22-14.23.sql */

ALTER TABLE luminex.datarow ADD SinglePointControlId INT;
GO
ALTER TABLE luminex.datarow ADD CONSTRAINT FK_Luminex_DataRow_SinglePointControlId FOREIGN KEY (SinglePointControlId) REFERENCES luminex.SinglePointControl(RowId);
CREATE INDEX IX_LuminexDataRow_SinglePointControlId ON luminex.DataRow (SinglePointControlId);

UPDATE luminex.datarow SET SinglePointControlId =
  (SELECT s.rowid FROM luminex.singlepointcontrol s, exp.data d, exp.protocolapplication pa
    WHERE pa.runid = s.runid AND d.sourceapplicationid = pa.rowid AND dataid = d.rowid AND description = s.name)
WHERE SinglePointControlId IS NULL;