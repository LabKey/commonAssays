/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

import org.labkey.api.pipeline.PipelineProtocolFactory;
import org.labkey.api.util.XMLValidationParser;
import org.labkey.ms2.pipeline.MascotInputParser;

import java.net.URI;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * MascotSearchProtocolFactory class
 * <p/>
 * Created: Jun 6, 2006
 *
 * @author bmaclean
 */
public class MascotSearchProtocolFactory extends PipelineProtocolFactory<MascotSearchProtocol>
{
    public static MascotSearchProtocolFactory instance = new MascotSearchProtocolFactory();

    public static MascotSearchProtocolFactory get()
    {
        return instance;
    }

    public String getName()
    {
        return "mascot";
    }

    public MascotSearchProtocol load(URI uriRoot, String name) throws IOException
    {
        return loadInstance(getProtocolFile(uriRoot, name));
    }

    public MascotSearchProtocol loadInstance(File file) throws IOException
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

        MascotInputParser parser = new MascotInputParser();
        parser.parse(xmlBuffer.toString());
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
            {
                throw new IOException("Failed parsing Mascot input xml '" + file + "'.\n" +
                        err.getMessage());
            }
            else
            {
                throw new IOException("Failed parsing Mascot input xml '" + file + "'.\n" +
                        "Line " + err.getLine() + ": " + err.getMessage());
            }
        }

//TODO: WCH-check that we are using the same parameters set
        // Remove the pipeline specific parameters.
        String name = parser.removeInputParameter("pipeline, protocol name");
        String description = parser.removeInputParameter("pipeline, protocol description");
        String folder = parser.removeInputParameter("pipeline, load folder");
        String databases = parser.removeInputParameter("pipeline, database");
        String email = parser.removeInputParameter("pipeline, email address");
        String mascotServer = parser.removeInputParameter("pipeline, mascot server");
        String mascotHTTPProxy = parser.removeInputParameter("pipeline, mascot http proxy");

        // Remove the parameters set in the pipeline job.
        parser.removeInputParameter("list path, default parameters");
        parser.removeInputParameter("list path, taxonomy information");
        parser.removeInputParameter("protein, taxon");
        parser.removeInputParameter("spectrum, path");
        parser.removeInputParameter("output, path");
//END-TODO: WCH-check that we are using the same parameters set

        String[] dbNames = databases.split(";");

        MascotSearchProtocol instance = new MascotSearchProtocol(name, description, dbNames, parser.getXML());
        instance.setEmail(email);
        instance.setMascotServer(mascotServer);
        instance.setMascotHTTPProxy(mascotHTTPProxy);
        return instance;
    }
}
