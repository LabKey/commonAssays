package org.labkey.ms2.protocol;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.XMLValidationParser;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.BioMLInputParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jeckels
 * Date: Dec 7, 2006
 */
public abstract class MS2SearchPipelineProtocol extends PipelineProtocol
{
    private static Logger _log = Logger.getLogger(MS2SearchPipelineProtocol.class);

    protected String description;
    protected String[] dbNames;
    protected String xml;

    protected String email;

    public MS2SearchPipelineProtocol(String name, String description, String[] dbNames, String xml)
    {
        super(name);
        
        this.description = description;
        this.dbNames = dbNames;
        this.xml = xml;
    }

    public void validate(URI uriRoot) throws PipelineValidationException
    {
        super.validate(uriRoot);

        if (AppProps.getInstance().hasPipelineCluster())
        {
            // TODO(brendanx): Fix this limitations (post 1.4)
            if (getName().indexOf(' ') != -1)
            {
                throw new PipelineValidationException("The cluster pipeline does not currently support spaces in"
                        + " search protocol names.");
            }
        }

        if (dbNames.length == 0)
            throw new PipelineValidationException("Select at least one protein database.");
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String[] getDbNames()
    {
        return dbNames;
    }

    public void setDbNames(String[] dbNames)
    {
        this.dbNames = dbNames;
    }

    public String getXml()
    {
        return xml;
    }

    public void setXml(String xml)
    {
        this.xml = xml;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public abstract AbstractMS2SearchProtocolFactory getFactory();

    public File getAnalysisDir(File dirData)
    {
        return getFactory().getAnalysisDir(dirData, getName());
    }

    public File getParametersFile(File dirData)
    {
        return getFactory().getParametersFile(dirData, getName());
    }

    public void saveDefinition(URI uriRoot) throws IOException
    {
        save(getFactory().getProtocolFile(uriRoot, getName()), null);
    }

    public void saveInstance(File file, Container c) throws IOException
    {
        Map<String, String> addParams = new HashMap<String, String>();
        addParams.put("pipeline, load folder", c.getPath());
        addParams.put("pipeline, email address", email);
        save(file, addParams);
    }

    protected void save(File file, Map<String, String> addParams) throws IOException
    {
        if (xml == null || xml.length() == 0)
        {
            xml = "<?xml version=\"1.0\"?>\n" +
                    "<bioml>\n" +
                    "</bioml>";
        }

        BioMLInputParser parser = getFactory().createInputParser();
        parser.parse(xml);
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
                throw new IllegalArgumentException(err.getMessage());
            else
                throw new IllegalArgumentException("Line " + err.getLine() + ": " + err.getMessage());
        }

        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Failed to create directory '" + dir + "'.");

        parser.setInputParameter("pipeline, protocol name", getName());
        parser.setInputParameter("pipeline, protocol description", description);

        StringBuffer dbs = new StringBuffer();
        for (String dbName : dbNames)
        {
            if (dbs.length() > 0)
                dbs.append(';');
            dbs.append(dbName);
        }
        parser.setInputParameter("pipeline, database", dbs.toString());

        if (addParams != null)
        {
            for (String name : addParams.keySet())
                parser.setInputParameter(name, addParams.get(name));
        }

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(file));
            xml = parser.getXML();
            if (xml == null)
                throw new IOException("Error writing input XML.");
            writer.write(xml, 0, xml.length());
        }
        catch (IOException eio)
        {
            _log.error("Error writing input XML.", eio);
            throw eio;
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException eio)
                {
                    _log.error("Error writing input XML.", eio);
                }
            }
        }
    }

    public abstract AbstractMS2SearchPipelineJob createPipelineJob(ViewBackgroundInfo info,
                                                                   File dirSequenceRoot,
                                                                   File[] mzXMLFiles,
                                                                   File fileParameters,
                                                                   boolean fromCluster)
            throws SQLException, IOException;
}
