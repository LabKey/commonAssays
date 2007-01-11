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

