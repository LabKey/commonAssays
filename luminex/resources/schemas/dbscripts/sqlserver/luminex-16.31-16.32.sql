
DROP INDEX UQ_WellExclusion ON luminex.WellExclusion;
CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Dilution, Type, DataId);