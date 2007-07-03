package org.labkey.luminex;

import java.util.*;

/**
 * User: jeckels
 * Date: Jun 26, 2007
 */
public class AnalyteInfo
{
    private String _analyteName;
    private List<LuminexDataRow> _values = new ArrayList<LuminexDataRow>();

    public AnalyteInfo(String name)
    {
        _analyteName = name;
    }

    public String getAnalyteName()
    {
        return _analyteName;
    }

    public void setAnalyteName(String analyteName)
    {
        _analyteName = analyteName;
    }

    public void addValue(Map<String, String> values)
    {
        _values.add(new LuminexDataRow(this, values));
    }

    public List<LuminexDataRow> getDataRows()
    {
        return _values;
    }

    public Set<String> getValueDescriptions()
    {
        return _values.get(0).getValues().keySet();
    }
}
