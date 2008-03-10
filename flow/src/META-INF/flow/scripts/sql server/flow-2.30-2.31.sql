ALTER TABLE flow.object ADD container entityid
go
UPDATE flow.object SET container = (select container from exp.data where exp.data.rowid = flow.object.dataid)
go
ALTER TABLE flow.object ALTER COLUMN container entityid NOT NULL
go
-- UNDONE: PK_Object is clustered even though it would make more sense to cluster this index
CREATE INDEX flow_object_typeid ON flow.object (container, typeid)
go

ALTER TABLE flow.object ADD compid int;
ALTER TABLE flow.object ADD scriptid int;
ALTER TABLE flow.object ADD fcsid int;
go

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
go