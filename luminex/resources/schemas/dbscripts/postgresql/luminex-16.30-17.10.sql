/* luminex-16.30-16.31.sql */

ALTER TABLE luminex.WellExclusion ADD COLUMN Dilution REAL;

/* luminex-16.31-16.32.sql */

DROP INDEX luminex.UQ_WellExclusion;
CREATE UNIQUE INDEX UQ_WellExclusion ON luminex.WellExclusion(Description, Dilution, Type, DataId);