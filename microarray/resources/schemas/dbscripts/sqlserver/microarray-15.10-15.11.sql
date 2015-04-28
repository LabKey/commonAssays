
/* microarray-15.10-15.11.sql */

ALTER TABLE microarray.FeatureData ADD CONSTRAINT UQ_FeatureData_DataId_FeatureId_SampleId UNIQUE (DataId, FeatureId, SampleId);

