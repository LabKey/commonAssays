ALTER TABLE ms2.MS2Runs
    ADD COLUMN HasPeptideProphet BOOLEAN NOT NULL DEFAULT '0';

CREATE TABLE ms2.PeptideProphetSummaries
(
    RunId INT NOT NULL,
    PepProphetFValSeries BYTEA NULL,
    PepProphetObsSeries1 BYTEA NULL,
    PepProphetObsSeries2 BYTEA NULL,
    PepProphetObsSeries3 BYTEA NULL,
    PepProphetModelPosSeries1 BYTEA NULL,
    PepProphetModelPosSeries2 BYTEA NULL,
    PepProphetModelPosSeries3 BYTEA NULL,
    PepProphetModelNegSeries1 BYTEA NULL,
    PepProphetModelNegSeries2 BYTEA NULL,
    PepProphetModelNegSeries3 BYTEA NULL,

    CONSTRAINT PK_PeptideProphetSummmaries PRIMARY KEY (RunId)
);

DROP VIEW ms2.MS2ExperimentRuns;

-- No changes to this view, but we need to rebuild it since we added a column to MS2Runs
CREATE OR REPLACE VIEW ms2.MS2ExperimentRuns AS
SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId as ExperimentRunRowId, exp.Protocol.Name As ProtocolName
FROM ms2.MS2Runs
LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ExperimentRunLSID
LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID;

