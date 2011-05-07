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

ALTER TABLE luminex.datarow ADD COLUMN LSID LSIDtype;

UPDATE luminex.datarow SET LSID = 'urn:lsid:' || COALESCE ((SELECT p.value
FROM prop.properties p, prop.propertysets ps, core.containers c
WHERE
	p.name = 'defaultLsidAuthority' AND
	ps.set = p.set AND
	ps.category = 'SiteConfig' AND
	ps.objectid = c.entityid AND
	c.name IS NULL AND c.parent IS NULL), 'localhost') || ':LuminexDataRow:' || RowId;

ALTER TABLE luminex.datarow ALTER COLUMN LSID SET NOT NULL;

SELECT core.executeJavaUpgradeCode('addResultsDomain');

CREATE INDEX IX_LuminexDataRow_LSID ON luminex.Analyte (LSID);
