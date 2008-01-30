package org.labkey.ms2.pipeline.sequest;

import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;

/**
 * User: billnelson@uky.edu
 * Date: Aug 25, 2006
 * Time: 10:59:40 AM
 */
public class SequestSearchProtocolFactory extends AbstractMS2SearchProtocolFactory
{
    public static SequestSearchProtocolFactory instance = new SequestSearchProtocolFactory();

    public static SequestSearchProtocolFactory get()
    {
        return instance;
    }

    private SequestSearchProtocolFactory()
    {
        // Use the get() function.
    }

    public String getName()
    {
        return "sequest";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/sequest/SequestDefaults.xml";
    }

    public AbstractMS2SearchProtocol createProtocolInstance(String name, String description, String xml)
    {
        return new SequestSearchProtocol(name, description, xml);
    }

}
