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

/* ms2-2.30-2.31.sql */

EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX','IX_MS2PeptidesData_TrimmedPeptide'
GO

CREATE INDEX IX_MS2PeptidesData_TrimmedPeptide ON
    ms2.PeptidesData(TrimmedPeptide)
GO

EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX','IX_MS2PeptidesData_Peptide'
GO

CREATE INDEX IX_MS2PeptidesData_Peptide ON
    ms2.PeptidesData(Peptide)
GO

/* ms2-2.31-2.32.sql */

CREATE INDEX IX_Annotations_IdentId ON prot.annotations(AnnotIdent)
GO

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'SPROT_NAME' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt')
GO

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'UPSP' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt')
GO

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'UPTR' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt')
GO

DELETE FROM prot.identifiers WHERE (identifier = '' OR identifier IS NULL) AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'GeneName')
GO

/* ms2-2.32-2.33.sql */

DELETE FROM prot.FastaFiles where FastaId = 0
GO

/* ms2-2.33-2.34.sql */

-- Clean up blank BestName entries from protein annotation loads in old versions

UPDATE prot.sequences SET bestname = (SELECT MIN(fs.lookupstring) FROM prot.fastasequences fs WHERE fs.seqid = prot.sequences.seqid) WHERE bestname IS NULL OR bestname = ''
GO

UPDATE prot.sequences SET bestname = (SELECT MIN(identifier) FROM prot.identifiers i WHERE i.seqid = prot.sequences.seqid) WHERE bestname IS NULL OR bestname = ''
GO

UPDATE prot.sequences SET bestname = 'UNKNOWN' WHERE bestname IS NULL OR bestname = ''
GO

/* ms2-2.34-2.35.sql */

-- Increase column size to accomodate long synonyms in recent GO files
ALTER TABLE prot.GoTermSynonym ALTER COLUMN TermSynonym VARCHAR(500)
GO