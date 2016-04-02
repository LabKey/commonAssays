-- Issue 26074: Equals filter fails on protein.Sequence.ProtSequence column on SqlServer
ALTER TABLE prot.Sequences ALTER COLUMN ProtSequence VARCHAR(MAX);