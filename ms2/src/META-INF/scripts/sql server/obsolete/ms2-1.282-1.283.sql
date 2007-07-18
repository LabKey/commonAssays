EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetFValSeries', @newname = 'FValSeries', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetObsSeries1', @newname = 'ObsSeries1', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetObsSeries2', @newname = 'ObsSeries2', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetObsSeries3', @newname = 'ObsSeries3', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelPosSeries1', @newname = 'ModelPosSeries1', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelPosSeries2', @newname = 'ModelPosSeries2', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelPosSeries3', @newname = 'ModelPosSeries3', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelNegSeries1', @newname = 'ModelNegSeries1', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelNegSeries2', @newname = 'ModelNegSeries2', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelNegSeries3', @newname = 'ModelNegSeries3', @objtype = 'COLUMN'
GO

ALTER TABLE ms2.PeptideProphetSummaries ADD
    MinProbSeries IMAGE NULL,
    SensitivitySeries IMAGE NULL,
    ErrorSeries IMAGE NULL
GO
