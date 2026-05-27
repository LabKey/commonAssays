/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- This index overlaps with pk_proteinquantitation
DROP INDEX ms2.ix_proteinquantitation_proteingroupid;
-- This index overlaps with pk_ms2peptidememberships
DROP INDEX ms2.ix_peptidemembership_proteingroupid;
