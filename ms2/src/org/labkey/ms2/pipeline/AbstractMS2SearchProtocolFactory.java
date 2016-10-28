/*
 * Copyright (c) 2008-2016 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.ParamParser;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocolFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * <code>AbstractMS2SearchProtocolFactory</code>
 */
abstract public class AbstractMS2SearchProtocolFactory extends AbstractFileAnalysisProtocolFactory<AbstractMS2SearchProtocol>
{
    public static final String SEQUENCE_FILE_SEPARATOR = ";";

    @NotNull
    public static String[] splitSequenceFiles(String concatenatedFiles)
    {
        return concatenatedFiles.split(SEQUENCE_FILE_SEPARATOR);
    }

    public static String joinSequenceFiles(String... databases)
    {
        return StringUtils.join(databases, SEQUENCE_FILE_SEPARATOR);
    }

    protected AbstractMS2SearchProtocol createProtocolInstance(ParamParser parser)
    {
        // Get the pipeline specific parameters.
        String databases = parser.getInputParameter("pipeline, database");

        // Remove the parameters set in the pipeline job.
        parser.removeInputParameter("list path, default parameters");
        parser.removeInputParameter("list path, taxonomy information");
        parser.removeInputParameter("spectrum, path");
        parser.removeInputParameter("output, path");

        String[] dbNames;
        if (databases == null)
        {
            dbNames = new String[0];
        }
        else
        {
            dbNames = splitSequenceFiles(databases);
        }

        AbstractMS2SearchProtocol instance = super.createProtocolInstance(parser);

        instance.setDbNames(dbNames);

        return instance;
    }

    public abstract String getDefaultParametersResource();

    public String getDefaultParametersXML(PipeRoot root) throws IOException
    {
        String xml = super.getDefaultParametersXML(root);
        if (xml != null)
            return xml;
        return new ResourceDefaultsReader().readXML();
    }

    protected class ResourceDefaultsReader extends DefaultsReader
    {
        public Reader createReader() throws IOException
        {
            String resourceStream = getDefaultParametersResource();
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourceStream);
            return new InputStreamReader(is);
        }
    }
}
