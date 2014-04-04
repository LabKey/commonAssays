/*
 * Copyright (c) 2011-2014 LabKey Corporation
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

-- Duplicate ms2.Peptides custom views into the search-engine specific flavors of the peptides table
INSERT INTO query.customview
(
	entityid,
	created,
	createdby,
	modified,
	modifiedby,
	schema,
	queryname,
	container,
	name,
	customviewowner,
	columns,
	filter,
	flags
)
(
	SELECT
    -- Hack up the existing entityid into something that should be unique
	CASE
		WHEN substring(cv.entityid, 1, 1) >= '0' AND substring(cv.entityid, 1, 1) <= '5'
		THEN CAST((x.position + 5) || SUBSTRING(cv.entityid, 2, 1000) AS EntityId)
		ELSE CAST(x.position || SUBSTRING(cv.entityid, 2, 1000) AS EntityId)
	END as entityid,
	NOW() as created,
	cv.createdby,
	NOW() as modified,
	cv.modifiedby,
	cv.schema,
	x.newname as queryname,
	cv.container,
	cv.name,
	cv.customviewowner,
	cv.columns,
	cv.filter,
	cv.flags FROM query.customview cv,
	(
		SELECT 'XTandemPeptides' AS newname, 1 AS position UNION
		SELECT 'SequestPeptides' AS newname, 2 AS position UNION
		SELECT 'MascotPeptides' AS newname, 3 AS position UNION
		SELECT 'PhenyxPeptides' AS newname, 4 AS position
	) x WHERE LOWER(schema) = 'ms2' and LOWER(queryname) = 'peptides'
);