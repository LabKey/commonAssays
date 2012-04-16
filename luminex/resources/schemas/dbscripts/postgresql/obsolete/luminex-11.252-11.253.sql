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


ALTER TABLE luminex.DataRow ADD COLUMN Summary Boolean;
ALTER TABLE luminex.DataRow ADD COLUMN CV REAL;

UPDATE luminex.DataRow SET Summary = TRUE WHERE POSITION(',' IN Well) > 0;
UPDATE luminex.DataRow SET Summary = FALSE WHERE Summary IS NULL;

ALTER TABLE luminex.DataRow ALTER COLUMN Summary SET NOT NULL;

-- Calculate the StdDev for any summary rows that have already been uploaded
UPDATE luminex.DataRow dr1 SET StdDev =
	(SELECT STDDEV(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = dr1.description AND
		((dr2.dilution IS NULL AND dr1.dilution IS NULL) OR dr1.dilution = dr2.dilution) AND
		dr1.dataid = dr2.dataid AND
		dr1.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND dr1.expConc IS NULL) OR dr1.expConc = dr2.expConc))
	WHERE StdDev IS NULL;

-- Calculate the %CV for any summary rows that have already been uploaded
UPDATE luminex.DataRow dr1 SET CV =
	StdDev / (SELECT AVG(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = dr1.description AND
		((dr2.dilution IS NULL AND dr1.dilution IS NULL) OR dr1.dilution = dr2.dilution) AND
		dr1.dataid = dr2.dataid AND
		dr1.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND dr1.expConc IS NULL) OR dr1.expConc = dr2.expConc))
	WHERE StdDev IS NOT NULL AND
	(SELECT AVG(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = dr1.description AND
		((dr2.dilution IS NULL AND dr1.dilution IS NULL) OR dr1.dilution = dr2.dilution) AND
		dr1.dataid = dr2.dataid AND
		dr1.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND dr1.expConc IS NULL) OR dr1.expConc = dr2.expConc)) != 0;