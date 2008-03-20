ALTER TABLE flow.object ADD COLUMN container entityid;
UPDATE flow.object SET container = (select container from exp.data where exp.data.rowid = flow.object.dataid);
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
 WHERE D.rowid = flow.object.dataid AND INPUT.typeid in (5,7)
 )
WHERE flow.object.typeid=3;

CLUSTER flow_object_typeid ON flow.object;
VACUUM ANALYZE flow.object;
