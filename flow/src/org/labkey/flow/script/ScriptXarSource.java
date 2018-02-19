/*
 * Copyright (c) 2005-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.flow.script;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.labkey.api.data.Container;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.exp.XarSource;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ScriptXarSource extends XarSource
{
    private static final Logger _log = Logger.getLogger(ScriptXarSource.class);
    File _root;
    File _workingDirectory;
    File _logFile;
    ExperimentArchiveDocument _doc;

    public ScriptXarSource(ExperimentArchiveDocument doc, File root, File workingDirectory, PipelineJob job)
    {
        super(job);
        _root = root;
        _doc = doc;
        _workingDirectory = workingDirectory;
        _logFile = new File(_workingDirectory, "flow.xar.log");

        // For informational purposes, write out the XAR file.
        try
        {
            File xarfile = new File(_workingDirectory, "flow.xar.xml");

            try (FileWriter writer = new FileWriter(xarfile))
            {
                writer.write(doc.toString());
            }
        }
        catch (Exception e)
        {
            _log.error("Error writing XAR file", e);
        }
    }

    public String canonicalizeDataFileURL(String dataFileURL) throws XarFormatException
    {
        return FileUtil.getAbsoluteCaseSensitivePathString(getXarContext().getContainer(), FileUtil.createUri(dataFileURL));   //new File(dataFileURL).toURI().toString();
    }

    public File getRoot()
    {
        return _root;
    }

    public Path getRootPath()
    {
        return null != getRoot() ? getRoot().toPath() : null;
    }

    public boolean allowImport(PipeRoot pr, Container container, File file)
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
}
