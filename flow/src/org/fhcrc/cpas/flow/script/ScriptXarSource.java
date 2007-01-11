package org.fhcrc.cpas.flow.script;

import org.fhcrc.cpas.exp.StringXarSource;
import org.fhcrc.cpas.exp.XarFormatException;
import org.fhcrc.cpas.exp.XarSource;
import org.fhcrc.cpas.exp.ExperimentException;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.fhcrc.cpas.pipeline.PipelineService;
import org.fhcrc.cpas.data.Container;
import org.fhcrc.cpas.flow.FlowSettings;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ScriptXarSource extends XarSource
{
    private static final Logger _log = Logger.getLogger(ScriptXarSource.class);
    File _root;
    File _workingDirectory;
    File _xarFile;
    File _logFile;
    ExperimentArchiveDocument _doc;

    public ScriptXarSource(ExperimentArchiveDocument doc, File root, File workingDirectory) throws Exception
    {
        _root = root;
        _doc = doc;
        _workingDirectory = workingDirectory;
        _logFile = new File(_workingDirectory, "flow.xar.log");

        // For informational purposes, write out the XAR file.
        try
        {
            File xarfile = new File(_workingDirectory, "flow.xar.xml");
            FileWriter writer = new FileWriter(xarfile);
            writer.write(doc.toString());
            writer.close();
        }
        catch (Exception e)
        {
            _log.error("Error writing XAR file", e);
        }
    }

    public String canonicalizeDataFileURL(String dataFileURL) throws XarFormatException
    {
        return dataFileURL;
    }

    public File getRoot()
    {
        return _root;
    }

    public boolean isUnderPipelineRoot(PipelineService.PipeRoot pr, Container container, File file) throws Exception
    {
        return true;
    }

    public boolean shouldIgnoreDataFiles()
    {
        return false;
    }

    public ExperimentArchiveDocument getDocument() throws XmlException, IOException
    {
        return _doc;
    }

    public File getLogFile() throws IOException
    {
        return _logFile;
    }

    public void init() throws IOException, ExperimentException
    {
    }
}
