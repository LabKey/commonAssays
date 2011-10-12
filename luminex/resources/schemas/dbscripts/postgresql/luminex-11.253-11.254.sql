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


ALTER TABLE luminex.wellexclusion ADD COLUMN "type" character varying(10);

-- Populate the WellExclusion Type column based on the value in the DataRow table for the given DataId/Description/Dilution
UPDATE luminex.wellexclusion we SET "type" =
	(SELECT types."type" FROM (SELECT dr.dataid, dr.dilution, dr.description, min(dr."type") AS "type"
			FROM luminex.datarow AS dr
			GROUP BY dr.dataid, dr.dilution, dr.description) AS types
		WHERE we.dataid = types.dataid
		AND ((we.dilution IS NULL AND types.dilution IS NULL) OR we.dilution = types.dilution)
		AND ((we.description IS NULL AND types.description IS NULL) OR we.description = types.description))
	WHERE "type" IS NULL;

DROP INDEX luminex.UQ_WellExclusion;

CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Type, DataId);	

ALTER TABLE luminex.wellexclusion DROP COLUMN dilution;

