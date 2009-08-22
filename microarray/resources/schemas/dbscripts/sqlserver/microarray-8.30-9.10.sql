/*
 * Copyright (c) 2009 LabKey Corporation
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

/* microarray-8.30-8.31.sql */

-- Add a single, empty data row for any microarray runs that don't already have one  
INSERT INTO exp.object (container, objecturi, ownerobjectid)
SELECT o.container, o.objecturi + '.DataRow-0', o.objectid FROM exp.object o
WHERE o.objecturi like '%MicroarrayAssayData.Folder-%' AND o.objecturi NOT LIKE '%MicroarrayAssayData.Folder-%DataRow-%'
AND o.objectid NOT IN (SELECT x.ownerobjectid FROM exp.object x WHERE x.objecturi LIKE '%MicroarrayAssayData.Folder-%DataRow-%')
GO