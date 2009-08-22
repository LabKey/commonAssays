/*
 * Copyright (c) 2008 LabKey Corporation
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

/* SQLServer Version */

-- Clean up blank BestName entries from protein annotation loads in old versions

UPDATE prot.sequences SET bestname = (SELECT MIN(fs.lookupstring) FROM prot.fastasequences fs WHERE fs.seqid = prot.sequences.seqid) WHERE bestname IS NULL OR bestname = ''
GO

UPDATE prot.sequences SET bestname = (SELECT MIN(identifier) FROM prot.identifiers i WHERE i.seqid = prot.sequences.seqid) WHERE bestname IS NULL OR bestname = ''
GO

UPDATE prot.sequences SET bestname = 'UNKNOWN' WHERE bestname IS NULL OR bestname = ''
GO
