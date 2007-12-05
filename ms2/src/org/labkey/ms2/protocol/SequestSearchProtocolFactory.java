package org.labkey.ms2.protocol;

import org.labkey.ms2.pipeline.BioMLInputParser;
import org.labkey.ms2.pipeline.SequestInputParser;

import java.io.File;

/**
 * User: billnelson@uky.edu
 * Date: Aug 25, 2006
 * Time: 10:59:40 AM
 */
public class SequestSearchProtocolFactory extends AbstractMS2SearchProtocolFactory<SequestSearchProtocol>
{
    public static SequestSearchProtocolFactory instance = new SequestSearchProtocolFactory();

    public static SequestSearchProtocolFactory get()
    {
        return instance;
    }

    public String getName()
    {
        return "sequest";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/SequestDefaults.xml";
    }

    public BioMLInputParser createInputParser()
    {
        return new SequestInputParser();
    }

    public SequestSearchProtocol createProtocolInstance(String name, String description, String[] dbNames, String xml)
    {
        return new SequestSearchProtocol(name, description, dbNames, xml);
    }

    public SequestSearchProtocol createProtocolInstance(String name, String description, File dirSeqRoot,
                                                        String dbPath, String[] dbNames, String xml)
    {
        if(dbNames == null || dbNames.length == 0)
            throw new IllegalArgumentException("A sequence database must be selected.");

        if (dbPath != null && dbPath.length() > 0)
        {
            String[] seqFullDBs = new String[dbNames.length];
            for (int i = 0; i < dbNames.length; i++)
                seqFullDBs[i] = dbPath + dbNames[i];
            dbNames = seqFullDBs;
        }

        File fileSequenceDB = new File(dirSeqRoot, dbNames[0]);
        if (!fileSequenceDB.exists())
            throw new IllegalArgumentException("Sequence database '" + dbNames[0] + "' is not found in local FASTA root.");

        return new SequestSearchProtocol(name, description, dbNames, xml);
    }

}
