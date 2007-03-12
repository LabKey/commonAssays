package org.labkey.ms2.protocol;

import org.labkey.api.pipeline.PipelineProtocolFactory;
import org.labkey.api.data.Container;
import org.labkey.ms2.pipeline.SequestInputParser;
import org.labkey.ms2.pipeline.BioMLInputParser;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

/**
 * User: billnelson@uky.edu
 * Date: Aug 25, 2006
 * Time: 11:10:20 AM
 */
public class SequestSearchProtocol extends MS2SearchPipelineProtocol
{
    private static Logger _log = Logger.getLogger(SequestSearchProtocol.class);

    public SequestSearchProtocol(String name, String description, String[] dbNames, String xml)
    {
        super(name, description, dbNames, xml);
    }

    public PipelineProtocolFactory getFactory()
    {
        return SequestSearchProtocolFactory.get();
    }

    public void saveInstance(File file, Container c) throws IOException
    {
        Map<String, String> addParams = new HashMap<String, String>();
        addParams.put("pipeline, load folder", c.getPath());
        addParams.put("pipeline, email address", email);
        save(file, addParams);
    }

    protected BioMLInputParser createInputParser()
    {
        return new SequestInputParser();
    }
}
