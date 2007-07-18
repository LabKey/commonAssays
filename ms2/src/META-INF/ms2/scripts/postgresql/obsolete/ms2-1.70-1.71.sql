select core.fn_dropifexists('ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId');
create index IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId);

select core.fn_dropifexists('ProteinGroups', 'ms2', 'INDEX', 'IX_ProteinGroups_ProteinProphetFileId');
create index IX_ProteinGroups_ProteinProphetFileId ON ms2.ProteinGroups(ProteinProphetFileId);

select core.fn_dropifexists('Fractions', 'ms2', 'INDEX', 'IX_Fractions_Run_Fraction');
create index IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction);

