package org.labkey.ms1;

/**
 * Represents a parameter setting for a software package used to create an MS1 data file
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 10, 2007
 * Time: 2:31:27 PM
 */
public class SoftwareParam
{

    public int getSoftwareId()
    {
        return SoftwareId;
    }

    public void setSoftwareId(int softwareId)
    {
        SoftwareId = softwareId;
    }

    public String getName()
    {
        return Name;
    }

    public void setName(String name)
    {
        Name = name;
    }

    public String getValue()
    {
        return Value;
    }

    public void setValue(String value)
    {
        Value = value;
    }

    protected int SoftwareId = -1;
    protected String Name;
    protected String Value;
}
