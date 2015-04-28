
/* ms2-15.11-15.12.sql */

ALTER TABLE ms2.ExpressionData ADD CONSTRAINT UQ_ExpressionData_DataId_SeqId_SampleId UNIQUE (DataId, SeqId, SampleId);

