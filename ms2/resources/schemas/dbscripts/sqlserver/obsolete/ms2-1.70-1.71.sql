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
exec core.fn_dropifexists 'ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId'
go
	create index IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId)
go

exec core.fn_dropifexists 'ProteinGroups', 'ms2', 'INDEX', 'IX_ProteinGroups_ProteinProphetFileId'
go
	create index IX_ProteinGroups_ProteinProphetFileId ON ms2.ProteinGroups(ProteinProphetFileId)
go

exec core.fn_dropifexists 'Fractions', 'ms2', 'INDEX', 'IX_Fractions_Run_Fraction'
go
	create index IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction)
go

