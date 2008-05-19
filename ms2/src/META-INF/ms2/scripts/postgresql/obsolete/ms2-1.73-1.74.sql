/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
ALTER TABLE prot.FastaSequences DROP CONSTRAINT pk_fastasequences;

ALTER TABLE prot.FastaSequences ADD CONSTRAINT pk_fastasequences PRIMARY KEY(lookupstring, fastaid);

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval);


CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource);

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId);

CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident);


CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid);

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid);

CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId);

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid);

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId);

