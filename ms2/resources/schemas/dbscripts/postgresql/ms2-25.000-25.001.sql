-- This index overlaps with pk_proteinquantitation
DROP INDEX ms2.ix_proteinquantitation_proteingroupid;
-- This index overlaps with pk_ms2peptidememberships
DROP INDEX ms2.ix_peptidemembership_proteingroupid;
