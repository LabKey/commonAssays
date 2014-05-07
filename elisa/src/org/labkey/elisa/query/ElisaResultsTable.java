/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayResultTable;
import org.labkey.api.study.assay.SpecimenPropertyColumnDecorator;
import org.labkey.elisa.ElisaDataHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * User: klum
 * Date: 10/14/12
 */
public class ElisaResultsTable extends AssayResultTable
{
    public ElisaResultsTable(final AssayProtocolSchema schema, boolean includeCopiedToStudyColumns)
    {
        super(schema, includeCopiedToStudyColumns);

        List<FieldKey> visibleColumns = new ArrayList<>();

        // add material lookup columns to the view first, so they appear at the left:
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(schema.getProtocol(), AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);
        if (sampleSet != null)
        {
            for (DomainProperty pd : sampleSet.getType().getProperties())
            {
                visibleColumns.add(FieldKey.fromParts(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY,
                        ExpMaterialTable.Column.Property.toString(), pd.getName()));
            }
        }

        // add a lookup to the material table
        ColumnInfo specimenColumn = _columnMap.get(ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY);
        specimenColumn.setFk(new LookupForeignKey("LSID")
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
                if (propertyCol.getFk() instanceof PropertyForeignKey)
                {
                    ((PropertyForeignKey)propertyCol.getFk()).addDecorator(new SpecimenPropertyColumnDecorator(_provider, _protocol, schema));
                }
                propertyCol.setHidden(false);
                materials.addColumn(ExpMaterialTable.Column.LSID).setHidden(true);
                return materials;
            }
        });

        visibleColumns.addAll(getDefaultVisibleColumns());
        setDefaultVisibleColumns(visibleColumns);
    }
}
