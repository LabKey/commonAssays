package org.labkey.ms2.pipeline.sequest;

/**
 * User: jeckels
 * Date: Jul 31, 2012
 */
public abstract class AbstractMultipleValueParamsValidator implements IParamsValidator
{
    private final int _valueCount;

    public AbstractMultipleValueParamsValidator(int valueCount)
    {
        _valueCount = valueCount;
    }

    public int getValueCount()
    {
        return _valueCount;
    }

    protected abstract String getValueDescription();

    public String validate(Param spp)
    {
        String value = spp.getValue();
        if (value == null)
        {
            return "\"" + spp.getInputXmlLabels().get(0) + "\" must have " + getValueCount() + " values. " + getValueDescription();
        }
        String[] values = value.split("\\s");
        if (values.length != getValueCount())
        {
            return "\"" + spp.getInputXmlLabels().get(0) + "\" must have " + getValueCount() + " values, but had " + values.length + ". " + getValueDescription();
        }

        for (String s : values)
        {
            try
            {
                String error = parseValue(s, spp);
                if (error != null)
                {
                    return error;
                }
            }
            catch (NumberFormatException e)
            {
                return "Could not parse value \"" + s + "\" for parameter \"" + spp.getInputXmlLabels().get(0) + "\". " + getValueDescription();
            }
        }

        return "";
    }

    protected abstract String parseValue(String value, Param spp);
}
