package org.labkey.luminex;

import java.util.List;

/**
* User: jeckels
* Date: Jun 9, 2011
*/
public enum LuminexOORIndicator
{
    IN_RANGE
    {
        public String getOORIndicator(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter)
        {
            return null;
        }
        public Double getValue(String value)
        {
            return LuminexDataHandler.parseDouble(value);
        }
        public Double getValue(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter, Analyte analyte)
        {
            return getValue(value);
        }
    },
    NOT_AVAILABLE
    {
        public String getOORIndicator(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter)
        {
            return "***";
        }
        public Double getValue(String value)
        {
            return null;
        }
        public Double getValue(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter, Analyte analyte)
        {
            return null;
        }
    },
    OUT_OF_RANGE_ABOVE
    {
        public String getOORIndicator(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter)
        {
            return ">>";
        }
        public Double getValue(String value)
        {
            return null;
        }
        public Double getValue(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter, Analyte analyte)
        {
            return LuminexDataHandler.getValidStandard(dataRows, getter, false, analyte);
        }
    },
    OUT_OF_RANGE_BELOW
    {
        public String getOORIndicator(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter)
        {
            return "<<";
        }
        public Double getValue(String value)
        {
            return null;
        }
        public Double getValue(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter, Analyte analyte)
        {
            return LuminexDataHandler.getValidStandard(dataRows, getter, true, analyte);
        }
    },
    BEYOND_RANGE
    {
        public String getOORIndicator(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter)
        {
            int lowerCount = 0;
            int higherCount = 0;
            double thisValue = Double.parseDouble(value.substring(1));
            for (LuminexDataRow dataRow : dataRows)
            {
                Double otherValue = getter.getValue(dataRow);
                if (otherValue != null)
                {
                    if (otherValue < thisValue)
                    {
                        lowerCount++;
                    }
                    else if (otherValue > thisValue)
                    {
                        higherCount++;
                    }
                }
            }
            if (lowerCount > higherCount)
            {
                return ">";
            }
            else if (lowerCount < higherCount)
            {
                return "<";
            }
            else
            {
                return "?";
            }
        }
        public Double getValue(String value)
        {
            return null;
        }
        public Double getValue(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter, Analyte analyte)
        {
            String oorIndicator = getOORIndicator(value, dataRows, getter);
            double thisValue = Double.parseDouble(value.substring(1));
            if ("<".equals(oorIndicator))
            {
                Double standardValue = OUT_OF_RANGE_BELOW.getValue(value, dataRows, getter, analyte);
                if (standardValue != null && standardValue > thisValue)
                {
                    return standardValue;
                }
                else
                {
                    return thisValue;
                }
            }
            else if (">".equals(oorIndicator))
            {
                Double standardValue = OUT_OF_RANGE_ABOVE.getValue(value, dataRows, getter, analyte);
                if (standardValue != null && standardValue < thisValue)
                {
                    return standardValue;
                }
                else
                {
                    return thisValue;
                }
            }
            else
            {
                return null;
            }
        }
    },
    ERROR
    {
        public String getOORIndicator(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter)
        {
            return "ParseError";
        }
        public Double getValue(String value)
        {
            return null;
        }
        public Double getValue(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter, Analyte analyte)
        {
            return null;
        }
    },
    OUTLIER
    {
        public String getOORIndicator(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter)
        {
            return "---";
        }
        public Double getValue(String value)
        {
            return null;
        }
        public Double getValue(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter, Analyte analyte)
        {
            return null;
        }
    };


    public abstract String getOORIndicator(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter);
    public abstract Double getValue(String value);
    public abstract Double getValue(String value, List<LuminexDataRow> dataRows, LuminexDataHandler.Getter getter, Analyte analyte);
}
