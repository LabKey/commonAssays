/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

/* PostgreSQL Version */

CREATE INDEX IX_Annotations_IdentId ON prot.annotations(AnnotIdent);

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'SPROT_NAME' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt');

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'UPSP' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt');

DELETE FROM prot.identifiers WHERE UPPER(identifier) = 'UPTR' AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'SwissProt');

DELETE FROM prot.identifiers WHERE (identifier = '' OR identifier IS NULL) AND identtypeid IN (SELECT identtypeid FROM prot.identtypes WHERE name = 'GeneName');