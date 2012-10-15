package org.labkey.elisa.actions;

import org.labkey.api.exp.SamplePropertyHelper;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.assay.AbstractAssayProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 10/14/12
 */
public class PlateConcentrationPropertyHelper extends SamplePropertyHelper<WellGroupTemplate>
{
    private Set<String> _controlNames;
    private final PlateTemplate _template;

    public PlateConcentrationPropertyHelper(DomainProperty[] domainProperties, PlateTemplate template)
    {
        super(domainProperties);
        _template = template;
        _controlNames = new HashSet<String>();

        if (template != null)
        {
            Map<String, Position> controls = new HashMap<String, Position>();
            for (WellGroupTemplate group : template.getWellGroups())
            {
                if (group.getType() == WellGroup.Type.CONTROL)
                {
                    for (Position position : group.getPositions())
                        controls.put(position.getDescription(), position);
                }
            }

            for (WellGroupTemplate group : template.getWellGroups())
            {
                if (group.getType() == WellGroup.Type.REPLICATE)
                {
                    for (Position position : group.getPositions())
                    {
                        if (controls.containsKey(position.getDescription()))
                            _controlNames.add(group.getPositionDescription());
                    }
                }
            }
        }
    }

    protected WellGroupTemplate getObject(int index, Map<DomainProperty, String> sampleProperties)
    {
        int i = 0;
        for (WellGroupTemplate wellgroup : _template.getWellGroups())
        {
            if (wellgroup.getType() == WellGroup.Type.CONTROL)
            {
                if (i == index)
                {
                    return wellgroup;
                }
                i++;
            }
        }
        throw new IndexOutOfBoundsException("Requested #" + index + " but there were only " + i + " well group templates");
    }

    protected boolean isCopyable(DomainProperty pd)
    {
        return !AbstractAssayProvider.SPECIMENID_PROPERTY_NAME.equals(pd.getName()) && !AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME.equals(pd.getName());
    }


    public List<String> getSampleNames()
    {
        return Arrays.asList(_controlNames.toArray(new String[_controlNames.size()]));
    }
}
