package org.labkey.flow.data;

import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.property.SystemProperty;

abstract public class FlowProperty
{
    static final public String PROPERTY_BASE = "urn:flow.labkey.org/#";
    static public final SystemProperty SampleSetJoin = new SystemProperty(PROPERTY_BASE + "SampleSetJoin", PropertyType.STRING);
    static public final SystemProperty LogText = new SystemProperty(PROPERTY_BASE + "LogText", PropertyType.STRING);
    static public final SystemProperty FCSAnalysisName = new SystemProperty(PROPERTY_BASE + "FCSAnalysisName", PropertyType.STRING);
    static public final SystemProperty FCSAnalysisFilter = new SystemProperty(PROPERTY_BASE + "FCSAnalysisFilter", PropertyType.STRING);
    static public void register()
    {

    }
}
