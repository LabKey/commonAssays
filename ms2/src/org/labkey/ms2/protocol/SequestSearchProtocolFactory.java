package org.labkey.ms2.protocol;

import org.labkey.ms2.pipeline.BioMLInputParser;
import org.labkey.ms2.pipeline.SequestInputParser;
import org.labkey.api.pipeline.PipelineProtocolFactory;
import org.labkey.api.util.XMLValidationParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

/**
 * User: billnelson@uky.edu
 * Date: Aug 25, 2006
 * Time: 10:59:40 AM
 */
public class SequestSearchProtocolFactory extends PipelineProtocolFactory<SequestSearchProtocol>
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

    public SequestSearchProtocol load(URI uriRoot, String name) throws IOException
    {
        return loadInstance(getProtocolFile(uriRoot, name));
    }

    public SequestSearchProtocol loadInstance(File file) throws IOException
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

        BioMLInputParser parser = new SequestInputParser();
        parser.parse(xmlBuffer.toString());
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
            {
                throw new IOException("Failed parsing Sequest input xml '" + file + "'.\n" +
                    err.getMessage());
            }
            else
            {
                throw new IOException("Failed parsing Sequest input xml '" + file + "'.\n" +
                    "Line " + err.getLine() + ": " + err.getMessage());
            }
        }

//TODO: WCH-check that we are using the same parameters set
        // Remove the pipeline specific parameters.
        String name = parser.removeInputParameter("pipeline, protocol name");
        String description = parser.removeInputParameter("pipeline, protocol description");
        String databases = parser.removeInputParameter("pipeline, database");
        String email = parser.removeInputParameter("pipeline, email address");

        // Remove the parameters set in the pipeline job.
        parser.removeInputParameter("list path, default parameters");
        parser.removeInputParameter("list path, taxonomy information");
        parser.removeInputParameter("protein, taxon");
        parser.removeInputParameter("spectrum, path");
        parser.removeInputParameter("output, path");
//END-TODO: WCH-check that we are using the same parameters set

        String[] dbNames = databases.split(";");

        SequestSearchProtocol instance = new SequestSearchProtocol(name, description, dbNames, parser.getXML());
        instance.setEmail(email);
        return instance;
    }
}
