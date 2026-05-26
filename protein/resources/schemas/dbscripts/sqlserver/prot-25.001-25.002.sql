/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Move foreign key to the correct table. Issue 53523.
ALTER TABLE prot.Organisms DROP CONSTRAINT FK_ProtSequences_ProtOrganisms;
ALTER TABLE prot.Sequences ADD CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES prot.Organisms (OrgId);
