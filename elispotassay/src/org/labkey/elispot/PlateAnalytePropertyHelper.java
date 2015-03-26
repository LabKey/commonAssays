package org.labkey.elispot;

import org.labkey.api.exp.DuplicateMaterialException;
import org.labkey.api.exp.SamplePropertyHelper;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.AbstractAssayProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by aaronr on 3/23/15.
 * Compare to PlateAntigenPropertyHelper
 */
public class PlateAnalytePropertyHelper extends SamplePropertyHelper<String>
{
    private List<String> _analyteNames;

    public PlateAnalytePropertyHelper(List<? extends DomainProperty> antigenDomainProperties)
    {
        super(antigenDomainProperties);
        _analyteNames = new ArrayList<>();

        // TODO: figure out defaulting/loading analyteNames
        _analyteNames = Arrays.asList("Analyte 1", "Analyte 2", "Analyte 3");
        /*
        if (template != null)
        {
            for (WellGroupTemplate wellgroup : template.getWellGroups())
            {
                if (wellgroup.getType() == WellGroup.Type.ANTIGEN)
                {
                    _analyteNames.add(wellgroup.getName());
                }
            }
        }
        */
    }

    @Override
    public List<String> getSampleNames()
    {
        return _analyteNames;
    }

    @Override
    protected String getObject(int index, Map<DomainProperty, String> sampleProperties) throws DuplicateMaterialException
    {
        String analyteName = _analyteNames.get(index);
        if (analyteName != null)
            return analyteName;

        /*
        int i = 0;
        for (WellGroupTemplate wellgroup : _template.getWellGroups())
        {
            if (wellgroup.getType() == WellGroup.Type.ANTIGEN)
            {
                if (i == index)
                {
                    return wellgroup.getName();
                }
                i++;
            }
        }
        */
        // todo clean up this message
        throw new IndexOutOfBoundsException("Requested #" + index + " but there were only " + _analyteNames.size() + " well group templates");
    }

    @Override
    protected boolean isCopyable(DomainProperty pd)
    {
        return !AbstractAssayProvider.SPECIMENID_PROPERTY_NAME.equals(pd.getName()) && !AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME.equals(pd.getName());
    }
}
