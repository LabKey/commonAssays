/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
-- Eliminate unique requirement on Date column, which is unnecessary and causes constraint violations
ALTER TABLE ms2.MS2History DROP CONSTRAINT PK_MS2History;
CREATE INDEX IX_MS2History ON ms2.MS2History (Date);

-- Move SeqId into MS2PeptidesData to eliminate inefficient joins and add RunDescription column
ALTER TABLE ms2.MS2Fractions
    ADD COLUMN PepXmlDataLSID LSIDType NULL,
    ADD COLUMN MzXmlURL VARCHAR(400) NULL;

-- Add a column for the hash of the file

ALTER TABLE prot.proteindatabases
    ADD COLUMN FileChecksum varchar(50) NULL;
