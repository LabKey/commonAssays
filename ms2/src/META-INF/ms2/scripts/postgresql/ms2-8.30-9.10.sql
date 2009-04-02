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

/* ms2-8.30-8.31.sql */

UPDATE exp.DataInput SET Role = 'Spectra' WHERE Role = 'mzXML';

/* ms2-8.32-8.33.sql */

DROP INDEX prot.ix_protorganisms_genus;
DROP INDEX prot.ix_protorganisms_species;

DROP INDEX prot.ix_protidentifiers_identtypeid_identifier_identid_seqid;

/* ms2-8.33-8.34.sql */

ALTER TABLE prot.customannotationset DROP CONSTRAINT fk_customannotationset_createdby;
ALTER TABLE prot.customannotationset DROP CONSTRAINT fk_customannotationset_modifiedby;

/* ms2-8.34-8.35.sql */

-- Delete modification rows with bogus amino acids to avoid problems when wrapping associated runs
DELETE FROM ms2.Modifications WHERE AminoAcid < 'A' OR AminoAcid > 'Z';
SELECT core.executeJavaUpgradeCode('wrapRuns');