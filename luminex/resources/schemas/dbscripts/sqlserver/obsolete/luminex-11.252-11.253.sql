/*
 * Copyright (c) 2011-2012 LabKey Corporation
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
 
ALTER TABLE luminex.DataRow ADD Summary BIT
GO
ALTER TABLE luminex.DataRow ADD CV REAL
GO

UPDATE luminex.DataRow SET Summary = 1 WHERE patindex('%,%', Well) > 0
GO
UPDATE luminex.DataRow SET Summary = 0 WHERE Summary IS NULL
GO

ALTER TABLE luminex.DataRow ALTER COLUMN Summary BIT NOT NULL
GO

-- Calculate the StdDev for any summary rows that have already been uploaded
UPDATE luminex.DataRow SET StdDev =
	(SELECT STDEV(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = luminex.DataRow.description AND
		((dr2.dilution IS NULL AND luminex.DataRow.dilution IS NULL) OR luminex.DataRow.dilution = dr2.dilution) AND
		luminex.DataRow.dataid = dr2.dataid AND
		luminex.DataRow.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND luminex.DataRow.expConc IS NULL) OR luminex.DataRow.expConc = dr2.expConc))
	WHERE StdDev IS NULL
GO

-- Calculate the %CV for any summary rows that have already been uploaded
UPDATE luminex.DataRow SET CV =
	StdDev / (SELECT AVG(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = luminex.DataRow.description AND
		((dr2.dilution IS NULL AND luminex.DataRow.dilution IS NULL) OR luminex.DataRow.dilution = dr2.dilution) AND
		luminex.DataRow.dataid = dr2.dataid AND
		luminex.DataRow.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND luminex.DataRow.expConc IS NULL) OR luminex.DataRow.expConc = dr2.expConc))
	WHERE StdDev IS NOT NULL AND
	(SELECT AVG(FI) FROM luminex.DataRow dr2 WHERE
		dr2.description = luminex.DataRow.description AND
		((dr2.dilution IS NULL AND luminex.DataRow.dilution IS NULL) OR luminex.DataRow.dilution = dr2.dilution) AND
		luminex.DataRow.dataid = dr2.dataid AND
		luminex.DataRow.analyteid = dr2.analyteid AND
		((dr2.expConc IS NULL AND luminex.DataRow.expConc IS NULL) OR luminex.DataRow.expConc = dr2.expConc)) != 0
GO
