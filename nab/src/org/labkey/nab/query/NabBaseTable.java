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
package org.labkey.nab.query;

import org.labkey.api.assay.plate.AbstractPlateBasedAssayProvider;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.SampleTypeService;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.assay.AbstractAssayProvider;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayService;
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

    public NabBaseTable(final NabProtocolSchema schema, TableInfo table, ContainerFilter cf, final ExpProtocol protocol)
    {
        super(table, schema, cf);
        _schema = schema;
        _protocol = protocol;
    }

    protected void addSpecimenColumn()
    {
        final AssayProvider provider = AssayService.get().getProvider(_protocol);
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(_protocol, AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleType sampleType = SampleTypeService.get().getSampleType(sampleDomainURI);

        var specimenColumn = wrapColumn(getInputMaterialPropertyName(), _rootTable.getColumn("SpecimenLsid"));
        specimenColumn.setLabel("Specimen");
        specimenColumn.setKeyField(false);
        specimenColumn.setIsUnselectable(true);
        LookupForeignKey lfkSpecimen = new LookupForeignKey(getContainerFilter(), "LSID", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                ExpMaterialTable materials = ExperimentService.get().createMaterialTable(ExpSchema.TableType.Materials.toString(), _schema, getLookupContainerFilter());
                if (sampleType != null)
                {
                    materials.setSampleType(sampleType, true);
                }
                var propertyCol = materials.addColumn(ExpMaterialTable.Column.Property);
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
        runColumn.setFk(new LookupForeignKey(getContainerFilter(), "RowID", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable expRunTable = AssayService.get().createRunTable(_protocol, provider, _schema.getUser(), _schema.getContainer(), getLookupContainerFilter());
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

