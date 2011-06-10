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

-- Add the Unknown type for a titration
ALTER TABLE luminex.Titration ADD COLUMN "unknown" BOOLEAN;

UPDATE luminex.Titration SET "unknown" = FALSE;

ALTER TABLE luminex.Titration ALTER COLUMN "unknown" SET NOT NULL;

-- Make sure we have a row in exp.Object for all Luminex data files so that we can migrate the Excel run properties to it
INSERT INTO exp.Object (ObjectURI, Container)
	SELECT d.LSID, d.Container
	FROM exp.Data d, exp.ExperimentRun r, exp.ProtocolApplication pa
	WHERE d.SourceApplicationId = pa.RowId and r.RowId = pa.RunId AND r.ProtocolLSID LIKE '%:LuminexAssayProtocol.Folder-%' AND d.LSID NOT IN (SELECT ObjectURI FROM exp.Object);

-- Clean up run field values that were orphaned when we failed to delete them as part of deleting a Luminex run
DELETE FROM exp.ObjectProperty WHERE ObjectId IN (SELECT o.ObjectId FROM exp.Object o LEFT OUTER JOIN exp.ExperimentRun r ON o.ObjectURI = r.LSID WHERE o.ObjectURI like '%:LuminexAssayRun.Folder-%' AND r.LSID IS NULL);

-- Migrate Excel-based values from run to data 
UPDATE exp.ObjectProperty SET ObjectId =
	(SELECT MIN(dataO.ObjectId) FROM exp.Object dataO, exp.Object runO, exp.Data d, exp.ExperimentRun r, exp.ProtocolApplication pa
	WHERE dataO.ObjectURI = d.LSID AND runO.ObjectURI = r.LSID AND runO.ObjectId = exp.ObjectProperty.ObjectId AND d.SourceApplicationId = pa.RowId AND pa.RunId = r.RowId AND r.ProtocolLSID LIKE '%:LuminexAssayProtocol.Folder-%')
WHERE PropertyId IN (SELECT pd.PropertyId FROM exp.PropertyDescriptor p, exp.DomainDescriptor d, exp.PropertyDomain pd WHERE p.PropertyId = pd.PropertyId AND d.domainuri LIKE '%:AssayDomain-ExcelRun.Folder-%' AND pd.DomainId = d.DomainId);
