/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayResultTable;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 10/14/12
 */
public class ElisaResultsTable extends AssayResultTable
{
    public ElisaResultsTable(AssayProtocolSchema schema, boolean includeCopiedToStudyColumns)
    {
        super(schema, includeCopiedToStudyColumns);

        addPropertyColumns(schema);
    }

    protected void addPropertyColumns(final AssayProtocolSchema schema)
    {
        // add material lookup columns to the view first, so they appear at the left:
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(schema.getProtocol(), AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);
        if (sampleSet != null)
        {
            for (DomainProperty pd : sampleSet.getPropertiesForType())
            {
/*
                visibleColumns.add(FieldKey.fromParts("Properties", ElisaDataHandler.ELISA_INPUT_MATERIAL_DATA_PROPERTY,
                        ExpMaterialTable.Column.Property.toString(), pd.getName()));
*/
            }
        }
    }
}
