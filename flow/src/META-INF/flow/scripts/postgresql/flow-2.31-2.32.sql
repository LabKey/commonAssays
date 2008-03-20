drop index flow.flow_object_typeid;
create index flow_object_typeid on flow.object (container, typeid, dataid);