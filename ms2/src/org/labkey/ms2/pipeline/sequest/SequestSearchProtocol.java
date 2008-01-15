package org.labkey.ms2.pipeline.sequest;

import org.apache.log4j.Logger;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.sequest.SequestPipelineJob;
import org.labkey.ms2.pipeline.MS2SearchPipelineProtocol;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

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

    public AbstractMS2SearchProtocolFactory getFactory()
    {
        return SequestSearchProtocolFactory.get();
    }

    public AbstractMS2SearchPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                          File dirSequenceRoot,
                                                          File[] mzXMLFiles,
                                                          File fileParameters,
                                                          boolean fromCluster)
            throws SQLException, IOException
    {
        return new SequestPipelineJob(info, getName(), dirSequenceRoot, mzXMLFiles, fileParameters, fromCluster);
    }
}
