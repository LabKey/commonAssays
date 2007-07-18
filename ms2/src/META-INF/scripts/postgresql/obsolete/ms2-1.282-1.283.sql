ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetFValSeries TO FValSeries;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetObsSeries1 TO ObsSeries1;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetObsSeries2 TO ObsSeries2;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetObsSeries3 TO ObsSeries3;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelPosSeries1 TO ModelPosSeries1;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelPosSeries2 TO ModelPosSeries2;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelPosSeries3 TO ModelPosSeries3;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelNegSeries1 TO ModelNegSeries1;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelNegSeries2 TO ModelNegSeries2;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelNegSeries3 TO ModelNegSeries3;

ALTER TABLE ms2.PeptideProphetSummaries
    ADD MinProbSeries BYTEA NULL,
    ADD SensitivitySeries BYTEA NULL,
    ADD ErrorSeries BYTEA NULL;
