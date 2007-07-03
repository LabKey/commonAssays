package org.labkey.luminex;

import org.labkey.api.study.Well;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.awt.*;

/**
 * User: jeckels
 * Date: Jun 26, 2007
 */
public class SpecimenInfo
{
    private String _name;
    private List<LuminexDataRow> _values = new ArrayList<LuminexDataRow>();
    private Color _color;

    public SpecimenInfo(String name)
    {
        _name = name;
    }

    public boolean isControl()
    {
        return _name.startsWith("S");
    }

    public String getName()
    {
        return _name;
    }

    public void addAnalyteValue(LuminexDataRow dataRow)
    {
        _values.add(dataRow);
    }
    
    public List<LuminexDataRow> getValues()
    {
        return _values;
    }

    public Set<Well> getWells()
    {
        Set<Well> result = new HashSet<Well>();
        for (LuminexDataRow row : _values)
        {
            result.addAll(row.getWells());
        }
        return result;
    }

    public void setColor(Color color)
    {
        _color = color;
    }
    
    public Color getColor()
    {
        return _color;
    }

    public String getColorAsString()
    {
        return "#" + Integer.toString(_color.getRed(), 16) + Integer.toString(_color.getGreen(), 16) + Integer.toString(_color.getBlue(), 16);
    }

    public class SpecimenValue
    {
        private LuminexDataRow _dataRow;
        private AnalyteInfo _analyte;
        private double _concInRange;

        public SpecimenValue(AnalyteInfo analyte, double concInRange)
        {
            _analyte = analyte;
            _concInRange = concInRange;
        }
        
        public AnalyteInfo getAnalyte()
        {
            return _analyte;
        }

        public double getConcInRange()
        {
            return _concInRange;
        }
    }
}
