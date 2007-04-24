package org.labkey.nab;

import org.labkey.api.study.PlateTypeHandler;
import org.labkey.api.study.WellGroup;
import org.labkey.common.util.Pair;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Apr 23, 2007
 */
public class NabPlateTypeHandler implements PlateTypeHandler
{
    public Map<String, Object> getDefaultProperties()
    {
        Map<String, Object> result = new HashMap<String, Object>();
        for (NabManager.PlateProperty prop : NabManager.PlateProperty.values())
        {
            result.put(prop.toString(), null);
        }
        return result;
    }

    public List<Pair<WellGroup.Type, String>> getDefaultWellGroups()
    {
        List<Pair<WellGroup.Type, String>> result = new ArrayList<Pair<WellGroup.Type, String>>();
        result.add(new Pair<WellGroup.Type, String>(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE));
        result.add(new Pair<WellGroup.Type, String>(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE));
        return result;
    }

    public String getTypeName()
    {
        return "NAb";
    }
}
