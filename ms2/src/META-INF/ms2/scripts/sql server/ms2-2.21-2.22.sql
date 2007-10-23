-- Create indexes on ms2 Runs table to support common operations in MS2Manager

CREATE INDEX MS2Runs_Stats ON ms2.Runs(PeptideCount, SpectrumCount, Deleted, StatusId)
go
CREATE INDEX MS2Runs_ExperimentRunLSID ON ms2.Runs(ExperimentRunLSID)
go

