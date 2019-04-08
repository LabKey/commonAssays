remove interface ContainerFilterable (migrate method to protected methods e.g. on FilteredTable)

remove class DelegatingContainerFilter

test ContainerFilter for ExperimentService.get().createQCFlagsTable

SpecimenForeignKey calls to getSpecimenTableInfo(null)

Find classes that override:
- UserSchema.createTable(String name)
- AssayProtocolSchema.createProviderTable(String name)
- AssayProtocolSchema.createDataTable(boolean includeCopiedToStudyColumns)
- AssayProtocolSchema.createRunsTable()
- AssayResultTable.AssayResultTable(AssayProtocolSchema schema, boolean includeCopiedToStudyColumns)

convert schemas: ms2, study, annoucements, flow

Find classes that use constructor
- SimpleTable(SchemaType schema, TableInfo table)
   
Who is still depending on AbstractExpSchema.setupTable(T table) to set containerFilter???

LineageTableInfo

More clean up of QueryForeignKey (Builder?)
More clean up of PdLookupForeignKey (.create()?)

QueryDefinitionImpl.getTable() remove usage of deprecated method w/o ContainerFilter

Issues.AllIssuesTable
IsuesTable.RelatedIssues lookup uses setFk() on locked table

search for "TODO ContainerFilter"

Check subclasses of LookupForeignKey.  getLookupTableInfo() should use getLookupContainerFilter()

update MultiValuedForeignKey to take CF, check usages of passed in FK to make sure they are configured with CF

Check that subclasses of QueryView initialize ContainerFilter when they construct TableInfo

How does setting effectiveContainer (when != source container) on QFK affect the ContainerFilter?  If effectiveContainer != sourceContainer
maybe it should have the same effect as setting lookupContainer?  see for instance, ExperimentsTable in oconnorexperiments