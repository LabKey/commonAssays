package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;

/**
 * User: jeckels
 * Date: May 2, 2011
 */
public class LibraProtocolFactory extends AbstractFileAnalysisProtocolFactory
{
    @Override
    public AbstractFileAnalysisProtocol createProtocolInstance(String name, String description, String xml)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return "libra";
    }
}
