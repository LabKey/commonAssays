/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

package org.labkey.elispot.query;

import org.labkey.api.data.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.*;
import org.labkey.api.study.assay.*;
import org.labkey.elispot.ElispotDataHandler;

import java.util.*;

/**
 * User: Karl Lum
 * Date: Jan 21, 2008
 */
public abstract class PlateBasedAssayRunDataTable extends FilteredTable<AssaySchema>
{
    protected final ExpProtocol _protocol;

    public static final String RUN_ID_COLUMN_NAME = "RunId";

    public String getInputMaterialPropertyName()
    {
        return ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY;
    }

    public PlateBasedAssayRunDataTable(final AssaySchema schema, final TableInfo table, final ExpProtocol protocol)
    {
        super(table, schema);
        _protocol = protocol;

        final AssayProvider provider = AssayService.get().getProvider(protocol);
        List<FieldKey> visibleColumns = new ArrayList<>();

        // add any property columns
        addPropertyColumns(schema, protocol, provider, visibleColumns);

        ExprColumn runColumn = new ExprColumn(this, "Run", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".RunID"), JdbcType.INTEGER);
        runColumn.setFk(new LookupForeignKey("RowID")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable expRunTable = AssayService.get().createRunTable(protocol, provider, schema.getUser(), schema.getContainer());
                expRunTable.setContainerFilter(getContainerFilter());
                return expRunTable;
            }
        });
        addColumn(runColumn);

//        ColumnInfo objectUriColumn = getColumn("ObjectUri");
//        Domain antigenDomain = ((ElispotAssayProvider)provider).getAntigenWellGroupDomain(protocol);
//        PropertyDescriptor materialProperty = antigenDomain.getPropertyByName("SpecimenLsid").getPropertyDescriptor();
        final boolean hasMaterialSpecimenPropertyColumnDecorator = hasMaterialSpecimenPropertyColumnDecorator();
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(protocol, AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);
        ColumnInfo materialColumn = getColumn("SpecimenLsid"); //  new PropertyColumn(materialProperty, objectUriColumn, getContainer(), schema.getUser(), false);
        materialColumn.setLabel("Specimen");
        materialColumn.setHidden(true);
        materialColumn.setFk(new LookupForeignKey("LSID")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpMaterialTable materials = ExperimentService.get().createMaterialTable(ExpSchema.TableType.Materials.toString(), schema);
                // Make sure we are filtering to the same set of containers
                materials.setContainerFilter(getContainerFilter());
                if (sampleSet != null)
                {
                    materials.setSampleSet(sampleSet, true);
                }
                ColumnInfo propertyCol = materials.addColumn(ExpMaterialTable.Column.Property);
                if (hasMaterialSpecimenPropertyColumnDecorator && propertyCol.getFk() instanceof PropertyForeignKey)
                {
                    ((PropertyForeignKey)propertyCol.getFk()).addDecorator(new SpecimenPropertyColumnDecorator(provider, protocol, schema));
                    propertyCol.setDisplayColumnFactory(ColumnInfo.NOLOOKUP_FACTORY);
                }
                propertyCol.setHidden(false);
                materials.addColumn(ExpMaterialTable.Column.LSID).setHidden(true);
                return materials;
            }
        });

        ExprColumn runIdColumn = new ExprColumn(this, RUN_ID_COLUMN_NAME, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".RunID"), JdbcType.INTEGER);
        ColumnInfo addedRunIdColumn = addColumn(runIdColumn);
        addedRunIdColumn.setHidden(true);

        Set<String> hiddenProperties = new HashSet<>();
        hiddenProperties.add(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
        hiddenProperties.add(AbstractAssayProvider.PARTICIPANT_VISIT_RESOLVER_PROPERTY_NAME);
        Domain runDomain = provider.getRunDomain(protocol);
        for (DomainProperty prop : runDomain.getProperties())
        {
            if (!hiddenProperties.contains(prop.getName()))
                visibleColumns.add(FieldKey.fromParts("Run", prop.getName()));
        }
        Domain uploadSetDomain = provider.getBatchDomain(protocol);
        for (DomainProperty prop : uploadSetDomain.getProperties())
        {
            if (!hiddenProperties.contains(prop.getName()))
                visibleColumns.add(FieldKey.fromParts("Run", AssayService.BATCH_COLUMN_NAME, prop.getName()));
        }

        SQLFragment protocolConditionSql = new SQLFragment("(SELECT d.ProtocolLsid FROM exp.ExperimentRun d WHERE d.RowId = RunId) = '" + _protocol.getLSID() + "'");
        addCondition(protocolConditionSql);

        setDefaultVisibleColumns(visibleColumns);
    }

    protected abstract void addPropertyColumns(final AssaySchema schema, final ExpProtocol protocol, final AssayProvider provider, List<FieldKey> visibleColumns);
    protected abstract boolean hasMaterialSpecimenPropertyColumnDecorator();

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        // There isn't a container column directly on this table so do a special filter
        if (getContainer() != null)
        {
            FieldKey containerColumn = FieldKey.fromParts("Run", "Folder");
            clearConditions(containerColumn);
            addCondition(filter.getSQLFragment(getSchema(), new SQLFragment("(SELECT d.Container FROM exp.ExperimentRun d WHERE d.RowId = RunId)"), getContainer()), containerColumn);
        }
    }

}
