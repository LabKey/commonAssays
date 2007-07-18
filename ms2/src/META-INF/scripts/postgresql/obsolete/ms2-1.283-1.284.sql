ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN MinProbSeries BYTEA NULL;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN SensitivitySeries BYTEA NULL;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN ErrorSeries BYTEA NULL;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN PredictedNumberCorrectSeries BYTEA NULL;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN PredictedNumberIncorrectSeries BYTEA NULL;
