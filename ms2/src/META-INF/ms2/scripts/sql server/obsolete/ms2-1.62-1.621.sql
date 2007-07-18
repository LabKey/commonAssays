-- More accurate column name
EXEC sp_rename
    @objname = 'ms2.Runs.SampleEnzyme',
    @newname = 'SearchEnzyme',
    @objtype = 'COLUMN'
GO

-- Rebuild view
DROP VIEW ms2.ExperimentRuns
GO

CREATE VIEW ms2.ExperimentRuns AS
    SELECT ms2.Runs.*, exp.ExperimentRun.RowId AS ExperimentRunRowId, exp.Protocol.Name AS ProtocolName
    FROM ms2.Runs
        LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.Runs.ExperimentRunLSID
        LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID
GO
