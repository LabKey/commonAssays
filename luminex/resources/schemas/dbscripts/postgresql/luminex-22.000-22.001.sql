
-- add missing FK constraint and indices for luminex.AnalyteTitration
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalyteTitration_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.Analyte(RowId);
CREATE INDEX IDX_LuminexAnalyteTitration_AnalyteId ON luminex.AnalyteTitration(AnalyteId);
ALTER TABLE luminex.AnalyteTitration ADD CONSTRAINT FK_AnalyteTitration_TitrationId FOREIGN KEY (TitrationId) REFERENCES luminex.Titration(RowId);
CREATE INDEX IDX_LuminexAnalyteTitration_TitrationId ON luminex.AnalyteTitration(TitrationId);

-- add missing indices for FK constraints on existing table columns
CREATE INDEX IDX_LuminexCurveFit_AnalyteIdTitrationId ON luminex.CurveFit(AnalyteId, TitrationId);
CREATE INDEX IDX_LuminexRunExclusion_RunId ON luminex.RunExclusion(RunId);
CREATE INDEX IDX_LuminexRunExclusionAnalyte_AnalyteId ON luminex.RunExclusionAnalyte(AnalyteId);
CREATE INDEX IDX_LuminexWellExclusion_DataId ON luminex.WellExclusion(DataId);
CREATE INDEX IDX_LuminexWellExclusionAnalyte_AnalyteId ON luminex.WellExclusionAnalyte(AnalyteId);