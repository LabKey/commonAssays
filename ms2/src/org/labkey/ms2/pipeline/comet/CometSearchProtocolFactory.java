package org.labkey.ms2.pipeline.comet;

import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;

/**
 * User: jeckels
 * Date: 9/16/13
 */
public class CometSearchProtocolFactory extends AbstractMS2SearchProtocolFactory
{
    private static final CometSearchProtocolFactory INSTANCE = new CometSearchProtocolFactory();

    public static CometSearchProtocolFactory get()
    {
        return INSTANCE;
    }

    private CometSearchProtocolFactory()
    {
        // Use the get() function.
    }

    public String getName()
    {
        return "comet";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/comet/CometDefaults.xml";
    }

    public AbstractMS2SearchProtocol createProtocolInstance(String name, String description, String xml)
    {
        return new CometSearchProtocol(name, description, xml);
    }
}
