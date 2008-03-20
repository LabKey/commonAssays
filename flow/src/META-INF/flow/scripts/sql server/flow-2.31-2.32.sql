drop index flow.object.flow_object_typeid
go
create index flow_object_typeid on flow.object (container, typeid, dataid)
go