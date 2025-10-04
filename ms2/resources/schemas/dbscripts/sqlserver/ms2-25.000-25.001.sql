-- This index overlaps with PK_ProteinQuantitation
DROP INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation;
-- This index overlaps with pk_ms2peptidememberships
DROP INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships;
