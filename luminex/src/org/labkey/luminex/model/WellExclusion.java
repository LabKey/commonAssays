package org.labkey.luminex.model;

/**
 * Created by iansigmon on 1/27/17.
 */
public class WellExclusion
{
    private String _fileName;
    private String _analyte;
    private String _description;
    private String _type;
    private String _dilution;


    public String getDilution()
    {
        return _dilution;
    }
    public void setDilution(String dilution)
    {
        _dilution = dilution;
    }

    public String getType()
    {
        return _type;
    }
    public void setType(String type)
    {
        _type = type;
    }

    public String getDescription()
    {
        return _description;
    }
    public void setDescription(String description)
    {
        _description = description;
    }

    public String getAnalyte()
    {
        return _analyte;
    }
    public void setAnalyte(String analyte)
    {
        _analyte = analyte;
    }

    public String getFileName()
    {
        return _fileName;
    }
    public void setFileName(String fileName)
    {
        _fileName = fileName;
    }

    public boolean wouldExclude(String fileName, String analyteName, String description, String type, String dilution)
    {
        return getFileName().equals(fileName)
                && getAnalyte().equals(analyteName)
                && isNullOrValue(getDescription(), description)
                && isNullOrValue(getType(), type)
                && isNullOrValue(getDilution(), dilution);
    }

    private boolean isNullOrValue(String expected, String value)
    {
        return expected == null || expected.equals(value);
    }
}
