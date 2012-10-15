package org.labkey.elisa.query;

import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayResultTable;
import org.labkey.api.study.assay.AssaySchema;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 10/14/12
 */
public class ElisaResultsTable extends AssayResultTable
{
    protected AssaySchema _schema;
    protected ExpProtocol _protocol;

    public ElisaResultsTable(AssaySchema schema, ExpProtocol protocol, AssayProvider provider, boolean includeCopiedToStudyColumns)
    {
        super(schema, protocol, provider, includeCopiedToStudyColumns);

        addPropertyColumns(schema, protocol, provider);
    }

    protected void addPropertyColumns(final AssaySchema schema, final ExpProtocol protocol, final AssayProvider provider)
    {
        // add material lookup columns to the view first, so they appear at the left:
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(protocol, AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
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
