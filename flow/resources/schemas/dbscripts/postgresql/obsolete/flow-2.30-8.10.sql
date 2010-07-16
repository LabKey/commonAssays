/*
 * Copyright (c) 2008 LabKey Corporation
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
/* flow-2.30-2.32.sql */

ALTER TABLE flow.object ADD COLUMN container entityid;
UPDATE flow.object SET container = (SELECT container FROM exp.data WHERE exp.data.rowid = flow.object.dataid);
ALTER TABLE flow.object ALTER COLUMN container SET NOT NULL;
CREATE INDEX flow_object_typeid ON flow.object (container, typeid, dataid);

ALTER TABLE flow.object ADD COLUMN compid int4;
ALTER TABLE flow.object ADD COLUMN scriptid int4;
ALTER TABLE flow.object ADD COLUMN fcsid int4;

UPDATE flow.object
SET
compid =(
 SELECT DI.dataid
 FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid
 WHERE D.rowid = flow.object.dataid AND INPUT.typeid=4
 ),
fcsid =(
 SELECT DI.dataid
 FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid
 WHERE D.rowid = flow.object.dataid AND INPUT.typeid=1
 ),
scriptid =(
 SELECT DI.dataid
 FROM flow.object INPUT INNER JOIN exp.datainput DI ON INPUT.dataid=DI.dataid INNER JOIN exp.data D ON D.sourceapplicationid=DI.targetapplicationid
 WHERE D.rowid = flow.object.dataid AND INPUT.typeid IN (5,7)
 )
WHERE flow.object.typeid=3;

CLUSTER flow_object_typeid ON flow.object;
VACUUM ANALYZE flow.object;