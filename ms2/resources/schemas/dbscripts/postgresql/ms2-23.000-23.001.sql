/*
 * Copyright (c) 2022-2023 LabKey Corporation
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

-- These PostgreSQL-only special indexes use 'varchar_pattern_ops' operator, which are used when queries have pattern expressions
-- using LIKE, however, they only work when using prefixes.
-- PostgreSQL doc :https://www.postgresql.org/docs/current/indexes-opclass.html

CREATE INDEX IF NOT EXISTS IX_ProtSequences_Identifier_VarcharPatternOps ON prot.Identifiers (lower(Identifier) varchar_pattern_ops);

CREATE INDEX IF NOT EXISTS IX_ProtAnnotations_AnnotVal_VarcharPatternOps ON prot.Annotations (lower(AnnotVal) varchar_pattern_ops);

CREATE INDEX IF NOT EXISTS IX_ProtFastaSequences_LookupString_VarcharPatternOps ON prot.FastaSequences (lower(LookupString) varchar_pattern_ops);

CREATE INDEX IF NOT EXISTS IX_ProtIdentifiers_BestName_VarcharPatternOps ON prot.Sequences (lower(BestName) varchar_pattern_ops);