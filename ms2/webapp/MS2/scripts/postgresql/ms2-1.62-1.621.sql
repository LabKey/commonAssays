-- More accurate column name
ALTER TABLE ms2.Runs RENAME COLUMN SampleEnzyme TO SearchEnzyme;

-- Rebuild view
DROP VIEW ms2.ExperimentRuns;

CREATE VIEW ms2.ExperimentRuns AS
    SELECT r.*, er.RowId as ExperimentRunRowId, ep.Name As ProtocolName
    FROM ms2.Runs r
        LEFT OUTER JOIN exp.ExperimentRun er ON er.LSID=r.ExperimentRunLSID
        LEFT OUTER JOIN exp.Protocol ep ON ep.LSID=er.ProtocolLSID;

