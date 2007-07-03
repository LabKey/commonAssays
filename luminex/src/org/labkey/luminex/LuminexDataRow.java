package org.labkey.luminex;

import org.labkey.api.study.Well;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * User: jeckels
 * Date: Jun 26, 2007
 */
public class LuminexDataRow
{
    private AnalyteInfo _analyteInfo;
    private final Map<String, String> _values;

    private String _sampleId;
    private Set<Well> _wells = new HashSet<Well>();
    private double _concInRange;
    private SpecimenInfo _specimen;

    public LuminexDataRow(AnalyteInfo analyteInfo, Map<String, String> values)
    {
        _analyteInfo = analyteInfo;
        _values = values;

        _sampleId = values.get("SpecimenID");
        for (String wellName : values.get("Well").split(","))
        {
            int row = wellName.charAt(0) - 'A' + 1;
            int col = Integer.parseInt(wellName.substring(1));
            _wells.add(new LuminexWell(row, col));
        }
        try
        {
            _concInRange = Double.parseDouble(values.get("Conc in Range"));
        }
        catch (NumberFormatException e)
        {
            _concInRange = Double.NaN;
        }
    }
    
    public AnalyteInfo getAnalyteInfo()
    {
        return _analyteInfo;
    }

    public Map<String, String> getValues()
    {
        return _values;
    }

    public String getSampleId()
    {
        return _sampleId;
    }

    public Set<Well> getWells()
    {
        return _wells;
    }
    
    public double getConcInRange()
    {
        return _concInRange;
    }

    public void setSpecimen(SpecimenInfo specimen)
    {
        _specimen = specimen;
    }
    
    public SpecimenInfo getSpecimen()
    {
        return _specimen;
    }
}
