/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.elisa.query;

import org.labkey.api.assay.AbstractAssayProvider;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.assay.AssayResultTable;
import org.labkey.api.assay.plate.AbstractPlateBasedAssayProvider;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpSampleType;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.SampleTypeService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.study.assay.SpecimenPropertyColumnDecorator;
import org.labkey.elisa.ElisaDataHandler;

import java.util.LinkedHashSet;
import java.util.Set;

public class ElisaResultsTable extends AssayResultTable
{
    public ElisaResultsTable(final AssayProtocolSchema schema, ContainerFilter cf, boolean includeLinkedToStudyColumns)
    {
        super(schema, cf, includeLinkedToStudyColumns);

        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(schema.getProtocol(), AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleType sampleType = SampleTypeService.get().getSampleType(sampleDomainURI);
        Set<FieldKey> visibleColumns = new LinkedHashSet<>();

        // add material lookup columns to the view first, so they appear at the left:
        if (sampleType != null)
        {
            for (DomainProperty pd : sampleType.getDomain().getProperties())
            {
                visibleColumns.add(FieldKey.fromParts(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY,
                        ExpMaterialTable.Column.Property.toString(), pd.getName()));
            }
        }

        // add a lookup to the material table
        assert initialColumnsAreAdded(); // super() above should have initialized the columns
        BaseColumnInfo specimenColumn = (BaseColumnInfo)_columnMap.get(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY);
        if (specimenColumn != null)
        {
            specimenColumn.setLabel("Specimen");
            specimenColumn.setHidden(false);

            specimenColumn.setFk(new LookupForeignKey(getContainerFilter(), "LSID", null)
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    ExpMaterialTable materials = ExperimentService.get().createMaterialTable(schema, getLookupContainerFilter(), sampleType);
                    var propertyCol = materials.addColumn(ExpMaterialTable.Column.Property);
                    if (propertyCol.getFk() instanceof PropertyForeignKey)
                    {
                        ((PropertyForeignKey) propertyCol.getFk()).addDecorator(new SpecimenPropertyColumnDecorator(_provider, _protocol, schema));
                    }
                    propertyCol.setHidden(false);
                    materials.addColumn(ExpMaterialTable.Column.LSID).setHidden(true);

                    return materials;
                }
            });
        }

        visibleColumns.addAll(getDefaultVisibleColumns());
        setDefaultVisibleColumns(visibleColumns);
    }
}
