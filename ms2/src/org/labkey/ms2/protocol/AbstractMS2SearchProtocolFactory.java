/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms2.protocol;

import org.apache.log4j.Logger;
import org.labkey.api.pipeline.PipelineProtocolFactory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.XMLValidationParser;
import org.labkey.ms2.pipeline.BioMLInputParser;
import org.labkey.ms2.pipeline.MS2SearchPipelineProvider;

import java.io.*;
import java.net.URI;
import java.util.List;

/**
 * <code>AbstractMS2SearchProtocolFactory</code>
 */
abstract public class AbstractMS2SearchProtocolFactory<T extends MS2SearchPipelineProtocol> extends PipelineProtocolFactory<T>
{
    private static Logger _log = Logger.getLogger(AbstractMS2SearchProtocolFactory.class);
    
    /**
     * Get the file name used for MS2 search parameters in analysis directories.
     *
     * @return file name
     */
    public String getParametersFileName()
    {
        return getName() + ".xml";
    }

    /**
     * Get the file name for the default MS2 search parameters for all protocols of this type.
     * 
     * @return file name
     */
    public String getDefaultParametersFileName()
    {
        return getName() + "_default_input.xml";
    }

    /**
     * Get the analysis directory location, given a directory containing the mass spec data.
     *
     * @param dirData mass spec data directory
     * @param protocolName name of protocol for analysis
     * @return analysis directory
     */
    public File getAnalysisDir(File dirData, String protocolName)
    {
        return new File(new File(dirData, getName()), protocolName);
    }

    /**
     * Returns true if the file uses the type of protocol created by this factory.
     */
    public boolean isProtocolTypeFile(File file)
    {
        return NetworkDrive.exists(new File(file.getParent(), getParametersFileName()));
    }

    /**
     * Get the parameters file location, given a directory containing the mass spec data.
     *
     * @param dirData mass spec data directory
     * @param protocolName name of protocol for analysis
     * @return parameters file
     */
    public File getParametersFile(File dirData, String protocolName)
    {
        return new File(getAnalysisDir(dirData, protocolName), getParametersFileName());
    }

    /**
     * Get the default parameters file, given the pipeline root directory.
     *
     * @param dirRoot pipeline root directory
     * @return default parameters file
     */
    public File getDefaultParametersFile(File dirRoot)
    {
        return new File(dirRoot, getDefaultParametersFileName());
    }

    /**
     * Make sure default parameters for this protocol type exist.
     *
     * @param dirRoot pipeline root directory
     */
    public void ensureDefaultParameters(File dirRoot) throws IOException
    {
        if (!NetworkDrive.exists(getDefaultParametersFile(dirRoot)))
            setDefaultParametersXML(dirRoot, getDefaultParametersXML(dirRoot));
    }

    public abstract String getDefaultParametersResource();

    public abstract BioMLInputParser createInputParser();

    public abstract T createProtocolInstance(String name, String description, String[] dbNames, String xml);

    public abstract T createProtocolInstance(String name, String description, File dirSeqRoot,
                                                       String dbPath, String[] dbNames, String xml);

    public T load(URI uriRoot, String name) throws IOException
    {
        return loadInstance(getProtocolFile(uriRoot, name));
    }

    public T loadInstance(File file) throws IOException
    {
        BufferedReader inputReader = null;
        StringBuffer xmlBuffer = new StringBuffer();
        try
        {
            inputReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = inputReader.readLine()) != null)
                xmlBuffer.append(line).append("\n");
        }
        catch (IOException eio)
        {
            throw new IOException("Failed to load protocol file '" + file + "'.");
        }
        finally
        {
            if (inputReader != null)
            {
                try
                {
                    inputReader.close();
                }
                catch (IOException eio)
                {
                }
            }
        }

        BioMLInputParser parser = createInputParser();
        parser.parse(xmlBuffer.toString());
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
            {
                throw new IOException("Failed parsing input parameters '" + file + "'.\n" +
                        err.getMessage());
            }
            else
            {
                throw new IOException("Failed parsing input parameters '" + file + "'.\n" +
                        "Line " + err.getLine() + ": " + err.getMessage());
            }
        }

        // Remove the pipeline specific parameters.
        String name = parser.removeInputParameter("pipeline, protocol name");
        String description = parser.removeInputParameter("pipeline, protocol description");
        String folder = parser.removeInputParameter("pipeline, load folder");
        String databases = parser.removeInputParameter("pipeline, database");
        String email = parser.removeInputParameter("pipeline, email address");

        // Remove the parameters set in the pipeline job.
        parser.removeInputParameter("list path, default parameters");
        parser.removeInputParameter("list path, taxonomy information");
        parser.removeInputParameter("protein, taxon");
        parser.removeInputParameter("spectrum, path");
        parser.removeInputParameter("output, path");

        String[] dbNames;
        if (databases == null)
        {
            dbNames = new String[0];
        }
        else
        {
            dbNames = databases.split(";");
        }

        T instance = createProtocolInstance(name, description, dbNames, parser.getXML());
        instance.setEmail(email);
        return instance;
    }

    public String getDefaultParametersXML(File dirRoot) throws FileNotFoundException, IOException
    {
        BufferedReader reader = null;
        try
        {
            File fileDefault = getDefaultParametersFile(dirRoot);

            if (fileDefault.exists())
            {
                reader = new BufferedReader(new FileReader(fileDefault));
            }
            else
            {
                String resourceStream = getDefaultParametersResource();
                InputStream is = getClass().getClassLoader().getResourceAsStream(resourceStream);
                reader = new BufferedReader(new InputStreamReader(is));
            }

            StringBuffer defaults = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null)
            {
                defaults.append(line).append("\n");
            }
            return defaults.toString();
        }
        catch (FileNotFoundException enf)
        {
            _log.error("Default parameters file missing. Check product setup.", enf);
            throw enf;
        }
        catch (IOException eio)
        {
            _log.error("Error reading default parameters file.", eio);
            throw eio;
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException eio)
                {
                }
            }
        }
    }

    public void setDefaultParametersXML(File dirRoot, String xml) throws IOException
    {
        if (xml == null || xml.length() == 0)
            throw new IllegalArgumentException("You must supply default parameters for " + getName() + ".");

        BioMLInputParser parser = createInputParser();
        parser.parse(xml);
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
                throw new IllegalArgumentException(err.getMessage());
            else
                throw new IllegalArgumentException("Line " + err.getLine() + ": " + err.getMessage());
        }

        File fileDefault = getDefaultParametersFile(dirRoot);

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(fileDefault));
            writer.write(xml, 0, xml.length());
        }
        catch (IOException eio)
        {
            _log.error("Error writing default parameters file.", eio);
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
                    _log.error("Error writing default parameters file.", eio);
                    throw eio;
                }
            }
        }
    }

    public static AbstractMS2SearchProtocolFactory fromFile(File file)
    {
        List<PipelineProvider> providers = PipelineService.get().getPipelineProviders();
        for (PipelineProvider provider : providers)
        {
            if (!(provider instanceof MS2SearchPipelineProvider))
                continue;

            MS2SearchPipelineProvider mprovider = (MS2SearchPipelineProvider) provider;
            if (mprovider.getProtocolFactory().isProtocolTypeFile(file))
                return mprovider.getProtocolFactory();
        }

        return XTandemSearchProtocolFactory.get();
    }
}
