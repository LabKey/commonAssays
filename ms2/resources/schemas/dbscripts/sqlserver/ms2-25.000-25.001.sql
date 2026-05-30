/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- This index overlaps with PK_ProteinQuantitation
DROP INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation;
-- This index overlaps with pk_ms2peptidememberships
DROP INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships;
