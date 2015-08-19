/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
package org.labkey.nab.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.SpecimenPropertyColumnDecorator;
import org.labkey.api.util.StringExpression;
import org.labkey.nab.SinglePlateNabDataHandler;

/**
 * User: davebradlee
 * Date: 2/20/13
 * Time: 6:36 PM
 */
public abstract class NabBaseTable extends FilteredTable<AssayProtocolSchema>
{
    protected final NabProtocolSchema _schema;
    protected final ExpProtocol _protocol;

    public NabBaseTable(final NabProtocolSchema schema, TableInfo table, final ExpProtocol protocol)
    {
        super(table, schema);
        _schema = schema;
        _protocol = protocol;
    }

    protected void addSpecimenColumn()
    {
        final AssayProvider provider = AssayService.get().getProvider(_protocol);
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(_protocol, AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);

        ColumnInfo specimenColumn = wrapColumn(getInputMaterialPropertyName(), _rootTable.getColumn("SpecimenLsid"));
        specimenColumn.setLabel("Specimen");
        specimenColumn.setKeyField(false);
        specimenColumn.setIsUnselectable(true);
        LookupForeignKey lfkSpecimen = new LookupForeignKey("LSID")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                ExpMaterialTable materials = ExperimentService.get().createMaterialTable(ExpSchema.TableType.Materials.toString(), _schema);
                // Make sure we are filtering to the same set of containers
                materials.setContainerFilter(getContainerFilter());
                if (sampleSet != null)
                {
                    materials.setSampleSet(sampleSet, true);
                }
                ColumnInfo propertyCol = materials.addColumn(ExpMaterialTable.Column.Property);
                if (propertyCol.getFk() instanceof PropertyForeignKey)
                {
                    ((PropertyForeignKey) propertyCol.getFk()).addDecorator(new SpecimenPropertyColumnDecorator(provider, _protocol, _schema));
                }
                propertyCol.setHidden(false);
                materials.addColumn(ExpMaterialTable.Column.LSID).setHidden(true);
                return materials;
            }
        };
        specimenColumn.setFk(lfkSpecimen);
        addColumn(specimenColumn);
    }

    protected void addRunColumn()
    {
        final AssayProvider provider = AssayService.get().getProvider(_protocol);
        ExprColumn runColumn = new ExprColumn(this, "Run", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".RunID"), JdbcType.INTEGER);
        runColumn.setFk(new LookupForeignKey("RowID")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable expRunTable = AssayService.get().createRunTable(_protocol, provider, _schema.getUser(), _schema.getContainer());
                expRunTable.setContainerFilter(getContainerFilter());
                return expRunTable;
            }

            @Override
            public StringExpression getURL(ColumnInfo parent)
            {
                return getURL(parent, true);
            }

        });
        addColumn(runColumn);
    }
    public String getInputMaterialPropertyName()
    {
        return SinglePlateNabDataHandler.DILUTION_INPUT_MATERIAL_DATA_PROPERTY;
    }

}

