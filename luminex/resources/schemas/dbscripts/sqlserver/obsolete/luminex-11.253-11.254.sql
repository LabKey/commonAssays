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


ALTER TABLE luminex.WellExclusion ADD Type VARCHAR(10)
GO

-- Populate the WellExclusion Type column based on the value in the DataRow table for the given DataId/Description/Dilution
UPDATE luminex.WellExclusion SET Type =
	(SELECT types.Type FROM (SELECT dr.DataId, dr.Dilution, dr.Description, min(dr.Type) AS Type
			FROM luminex.DataRow dr
			GROUP BY dr.DataId, dr.Dilution, dr.Description) AS types
		WHERE luminex.WellExclusion.DataId = types.DataId
		AND ((luminex.WellExclusion.Dilution IS NULL AND types.Dilution IS NULL) OR luminex.WellExclusion.Dilution = types.Dilution)
		AND ((luminex.WellExclusion.Description IS NULL AND types.Description IS NULL) OR luminex.WellExclusion.Description = types.Description))
	WHERE Type IS NULL
GO

DROP INDEX UQ_WellExclusion ON luminex.WellExclusion
GO

CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Type, DataId)
GO

ALTER TABLE luminex.WellExclusion DROP COLUMN Dilution
GO