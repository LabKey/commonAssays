/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.labkey.api.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.SpecimenPropertyColumnDecorator;
import org.labkey.elispot.ElispotDataHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public PlateBasedAssayRunDataTable(final AssaySchema schema, final TableInfo table, ContainerFilter cf, final ExpProtocol protocol)
    {
        super(table, schema, cf);
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
                ExpRunTable expRunTable = AssayService.get().createRunTable(protocol, provider, schema.getUser(), schema.getContainer(), PlateBasedAssayRunDataTable.this.getContainerFilter());
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
        var materialColumn = getMutableColumn("SpecimenLsid"); //  new PropertyColumn(materialProperty, objectUriColumn, getContainer(), schema.getUser(), false);
        materialColumn.setLabel("Specimen");
        materialColumn.setHidden(true);
        materialColumn.setFk(new LookupForeignKey(cf,"LSID", null)
        {
            public TableInfo getLookupTableInfo()
            {
                ExpMaterialTable materials = ExperimentService.get().createMaterialTable(ExpSchema.TableType.Materials.toString(), schema, getLookupContainerFilter());
                if (sampleSet != null)
                {
                    materials.setSampleSet(sampleSet, true);
                }
                var propertyCol = materials.addColumn(ExpMaterialTable.Column.Property);
                if (hasMaterialSpecimenPropertyColumnDecorator && propertyCol.getFk() instanceof PropertyForeignKey)
                {
                    ((PropertyForeignKey)propertyCol.getFk()).addDecorator(new SpecimenPropertyColumnDecorator(provider, protocol, schema));
                    propertyCol.setDisplayColumnFactory(BaseColumnInfo.NOLOOKUP_FACTORY);
                }
                propertyCol.setHidden(false);
                materials.addColumn(ExpMaterialTable.Column.LSID).setHidden(true);
                return materials;
            }
        });

        ExprColumn runIdColumn = new ExprColumn(this, RUN_ID_COLUMN_NAME, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".RunID"), JdbcType.INTEGER);
        var addedRunIdColumn = addColumn(runIdColumn);
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
