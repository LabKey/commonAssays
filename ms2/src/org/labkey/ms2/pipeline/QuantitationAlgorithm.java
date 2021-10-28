/*
 * Copyright (c) 2011-2018 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.Pair;
import org.labkey.ms2.pipeline.client.ParameterNames;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
* User: jeckels
* Date: May 21, 2011
*/
public enum QuantitationAlgorithm
{
    xpress
    {
        @Override
        public String[] getCommand(Map<String, String> params, String pathMzXml, TPPTask.Factory factory, Pair<File, String> configFile)
        {
            return new String[] { "-X" + StringUtils.join(getCommonXpressQ3Params(params, pathMzXml).iterator(), ' ') };
        }
    },
    libra
    {
        @Override
        public String[] getCommand(Map<String, String> params, String pathMzXml, TPPTask.Factory factory, Pair<File, String> configFile) throws PipelineJobException
        {
            String normalizationChannelString = params.get(ParameterNames.LIBRA_NORMALIZATION_CHANNEL_PARAM);
            if (normalizationChannelString == null)
            {
                throw new PipelineJobException("Libra normalization channel must be specified using \"" + ParameterNames.LIBRA_NORMALIZATION_CHANNEL_PARAM + "\"");
            }
            return new String[] { "-L" + configFile.getKey().getName() + "-" + normalizationChannelString};
        }

        @Override
        protected Pair<File, String> getConfigFile(Map<String, String> params, PipeRoot root, WorkDirectory wd) throws PipelineJobException, IOException
        {
            String libraConfigName = params.get(ParameterNames.LIBRA_CONFIG_NAME_PARAM);
            if (libraConfigName == null)
            {
                throw new PipelineJobException("Name of Libra configuration must be specified using \"" + ParameterNames.LIBRA_CONFIG_NAME_PARAM + "\"");
            }
            if (libraConfigName.indexOf(' ') != -1)
            {
                throw new PipelineJobException("Libra configuration files containing a space are not supported");
            }
            LibraProtocolFactory factory = new LibraProtocolFactory();
            Path result = factory.getProtocolFile(root, libraConfigName, false);
            if (!NetworkDrive.exists(result))
            {
                throw new PipelineJobException("Libra config file does not exist: " + result);
            }
            wd.inputFile(result.toFile(), true);
            return new Pair<>(result.toFile(), TPPTask.LIBRA_CONFIG_INPUT_ROLE);
    }};

    protected List<String> getCommonXpressQ3Params(Map<String, String> params, String pathMzXml)
    {
        List<String> quantOpts = new ArrayList<>();

        String paramQuant = params.get(ParameterNames.QUANTITATION_RESIDUE_LABEL_MASS);
        if (paramQuant != null)
            getLabelOptions(paramQuant, quantOpts);

        paramQuant = params.get(ParameterNames.QUANTITATION_MASS_TOLERANCE);
        if (paramQuant != null)
            quantOpts.add("-m" + paramQuant);

        paramQuant = params.get(ParameterNames.PIPELINE_QUANT_PREFIX + "heavy elutes before light");
        if (paramQuant != null)
            if("yes".equalsIgnoreCase(paramQuant))
                quantOpts.add("-b");

        paramQuant = params.get(ParameterNames.PIPELINE_QUANT_PREFIX + "fix");
        if (paramQuant != null)
        {
            if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-H");
            else if ("light".equalsIgnoreCase(paramQuant))
                quantOpts.add("-L");
        }

        paramQuant = params.get(ParameterNames.PIPELINE_QUANT_PREFIX + "fix elution reference");
        if (paramQuant != null)
        {
            String refFlag = "-f";
            if ("peak".equalsIgnoreCase(paramQuant))
                refFlag = "-F";
            paramQuant = params.get(ParameterNames.PIPELINE_QUANT_PREFIX + "fix elution difference");
            if (paramQuant != null)
                quantOpts.add(refFlag + paramQuant);
        }

        paramQuant = params.get(ParameterNames.PIPELINE_QUANT_PREFIX + "metabolic search type");
        if (paramQuant != null)
        {
            if ("normal".equalsIgnoreCase(paramQuant))
                quantOpts.add("-M");
            else if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-N");
        }

        quantOpts.add("-d\"" + pathMzXml + "\"");
        return quantOpts;
    }

    private void getLabelOptions(String paramQuant, List<String> quantOpts)
    {
        String[] quantSpecs = paramQuant.split(",");
        for (String spec : quantSpecs)
        {
            String[] specVals = spec.split("@");
            if (specVals.length != 2)
                continue;
            String mass = specVals[0].trim();
            String aa = specVals[1].trim();
            if ("[".equals(aa))
            {
                aa = "n";
            }
            else if ("]".equals(aa))
            {
                aa = "c";
            }
            quantOpts.add("-n" + aa + "," + mass);
        }
    }

    /** @return Libra config file, in its original location, or null if we're not using Libra */
    protected @Nullable
    Pair<File, String> getConfigFile(Map<String, String> params, PipeRoot root, WorkDirectory wd) throws PipelineJobException, IOException
    {
        return null;
    }

    public abstract String[] getCommand(Map<String, String> params, String pathMzXml, TPPTask.Factory factory, Pair<File, String> configFile) throws PipelineJobException, FileNotFoundException;
}
