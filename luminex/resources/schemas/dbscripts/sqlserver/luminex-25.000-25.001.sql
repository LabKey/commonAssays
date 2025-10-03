-- This index overlaps with PK_Luminex_RunExclusion
DROP INDEX IDX_LuminexRunExclusion_RunId ON luminex.RunExclusion;
-- This index overlaps with PK_LuminexWellExclusionAnalyte
DROP INDEX IDX_LuminexWellExclusionAnalyte_AnalyteId ON luminex.WellExclusionAnalyte;
-- This index overlaps with PK_Luminex_AnalyteTitration
DROP INDEX IDX_LuminexAnalyteTitration_AnalyteId ON luminex.AnalyteTitration;
-- This index overlaps with PK_AnalyteSinglePointControl
DROP INDEX IDX_AnalyteSinglePointControl_AnalyteId ON luminex.AnalyteSinglePointControl;
-- This index overlaps with UQ_CurveFit
DROP INDEX IDX_LuminexCurveFit_AnalyteIdTitrationId ON luminex.CurveFit;
-- This index overlaps with PK_LuminexRunExclusionAnalyte
DROP INDEX IDX_LuminexRunExclusionAnalyte_AnalyteId ON luminex.RunExclusionAnalyte;
