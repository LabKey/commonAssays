package org.labkey.ms2.pipeline.sequest;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.AbstractFileAnalysisProtocolFactory;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.net.URI;

/**
 * User: billnelson@uky.edu
 * Date: Aug 25, 2006
 * Time: 11:10:20 AM
 */
public class SequestSearchProtocol extends AbstractMS2SearchProtocol<SequestPipelineJob>
{
    private static Logger _log = Logger.getLogger(SequestSearchProtocol.class);

    public SequestSearchProtocol(String name, String description, String xml)
    {
        super(name, description, xml);
    }

    public AbstractFileAnalysisProtocolFactory getFactory()
    {
        return SequestSearchProtocolFactory.get();
    }

    public SequestPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                          File[] filesInput,
                                                          File fileParameters,
                                                          boolean fromCluster)
            throws SQLException, IOException
    {
        return new SequestPipelineJob(this, info, getName(), getDirSeqRoot(),
                filesInput, fileParameters, fromCluster);
    }

    public void validate(URI uriRoot) throws PipelineValidationException
    {
        String[] dbNames = getDbNames();
        String dbPath = getDbPath();
        if(dbNames == null || dbNames.length == 0)
            throw new IllegalArgumentException("A sequence database must be selected.");

        if (dbPath != null && dbPath.length() > 0)
        {
            String[] seqFullDBs = new String[dbNames.length];
            for (int i = 0; i < dbNames.length; i++)
                seqFullDBs[i] = dbPath + dbNames[i];
            setDbNames(seqFullDBs);
        }

        File fileSequenceDB = new File(getDirSeqRoot(), dbNames[0]);
        if (!fileSequenceDB.exists())
            throw new IllegalArgumentException("Sequence database '" + dbNames[0] + "' is not found in local FASTA root.");

        super.validate(uriRoot);
    }
}
