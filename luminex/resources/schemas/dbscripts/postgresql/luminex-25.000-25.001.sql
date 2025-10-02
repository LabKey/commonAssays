-- This index overlaps with pk_luminex_runexclusion
DROP INDEX luminex.idx_luminexrunexclusion_runid;
-- This index overlaps with uq_analyte_lsid
DROP INDEX luminex.ix_luminexdatarow_lsid;
-- This index overlaps with pk_luminex_analytetitration
DROP INDEX luminex.idx_luminexanalytetitration_analyteid;
-- This index overlaps with uq_curvefit
DROP INDEX luminex.idx_luminexcurvefit_analyteidtitrationid;
-- This index overlaps with pk_analytesinglepointcontrol
DROP INDEX luminex.idx_analytesinglepointcontrol_analyteid;
-- This index overlaps with pk_luminexwellexclusionanalyte
DROP INDEX luminex.idx_luminexwellexclusionanalyte_analyteid;
-- This index overlaps with pk_luminexrunexclusionanalyte
DROP INDEX luminex.idx_luminexrunexclusionanalyte_analyteid;

-- Previous index seems to be incorrect... this is the SQL Server version
CREATE INDEX IX_LuminexDataRow_LSID ON luminex.DataRow (LSID);
