/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
        public String[] getCommand(Map<String, String> params, String pathMzXml, TPPTask.Factory factory, Pair<File, String> configFile) throws PipelineJobException
        {
            return new String[] { "-X" + StringUtils.join(getCommonXpressQ3Params(params, pathMzXml).iterator(), ' ') };
        }
    },
    q3
    {
        @Override
        public String[] getCommand(Map<String, String> params, String pathMzXml, TPPTask.Factory factory, Pair<File, String> configFile) throws PipelineJobException, FileNotFoundException
        {
            List<String> quantOpts = getCommonXpressQ3Params(params, pathMzXml);
            String paramMinPP = params.get("pipeline quantitation, min peptide prophet");
            if (paramMinPP != null)
                quantOpts.add("--minPeptideProphet=" + paramMinPP);
            String paramMaxDelta = params.get("pipeline quantitation, max fractional delta mass");
            if (paramMaxDelta != null)
                quantOpts.add("--maxFracDeltaMass=" + paramMaxDelta);
            String paramCompatQ3 = params.get("pipeline quantitation, q3 compat");
            if ("yes".equalsIgnoreCase(paramCompatQ3))
                quantOpts.add("--compat");

            String ver = params.get("pipeline, msinspect ver");

            // NOTE: The java command-line for msInspect gets passed as a
            // single argument to xinteract, and it the path to the java executable cannot contain spaces.
            // If it does contains spaces, then just use "java", and rely on it being on the path.
            String javaPath = PipelineJobService.get().getJavaPath();
            if (javaPath.indexOf(' ') >= 0)
                javaPath = "java";

            // We can support viewerPath.jar paths with spaces by wrapping in \" - not sure why this same
            // trick doesn't work with the path to the java executable, but it must be different xinteract internal
            // handling of arguments
            String viewerAppPath = PipelineJobService.get().getJarPath("viewerApp.jar", "msinspect", ver);
            if (viewerAppPath.contains(" "))
            {
                viewerAppPath = "\\\"" + viewerAppPath + "\\\"";
            }
            
            return new String[] {
                "-C1" + javaPath + " " +
                    (factory.getJavaVMOptions() == null ? "-Xmx1024M" : factory.getJavaVMOptions())
                    + " -jar " + viewerAppPath
                    + " --q3 " + StringUtils.join(quantOpts.iterator(), ' ')
                    ,
                    "-C2Q3ProteinRatioParser"
            };
        }
    },
    libra
    {
        @Override
        public String[] getCommand(Map<String, String> params, String pathMzXml, TPPTask.Factory factory, Pair<File, String> configFile) throws PipelineJobException, FileNotFoundException
        {
            String normalizationChannelString = params.get(TPPTask.LIBRA_NORMALIZATION_CHANNEL_PARAM);
            if (normalizationChannelString == null)
            {
                throw new PipelineJobException("Libra normalization channel must be specified using \"" + TPPTask.LIBRA_NORMALIZATION_CHANNEL_PARAM + "\"");
            }
            return new String[] { "-L" + configFile.getKey().getName() + "-" + normalizationChannelString};
        }

        @Override
        protected Pair<File, String> getConfigFile(Map<String, String> params, PipeRoot root, WorkDirectory wd) throws PipelineJobException, IOException
        {
            String libraConfigName = params.get(TPPTask.LIBRA_CONFIG_NAME_PARAM);
            if (libraConfigName == null)
            {
                throw new PipelineJobException("Name of Libra configuration must be specified using \"" + TPPTask.LIBRA_CONFIG_NAME_PARAM + "\"");
            }
            if (libraConfigName.indexOf(' ') != -1)
            {
                throw new PipelineJobException("Libra configuration files containing a space are not supported");
            }
            LibraProtocolFactory factory = new LibraProtocolFactory();
            File result = factory.getProtocolFile(root, libraConfigName);
            if (!NetworkDrive.exists(result))
            {
                throw new PipelineJobException("Libra config file does not exist: " + result);
            }
            wd.inputFile(result, true);
            return new Pair<File, String>(result, TPPTask.LIBRA_CONFIG_INPUT_ROLE);
    }};

    protected List<String> getCommonXpressQ3Params(Map<String, String> params, String pathMzXml)
    {
        List<String> quantOpts = new ArrayList<String>();

        String paramQuant = params.get("pipeline quantitation, residue label mass");
        if (paramQuant != null)
            getLabelOptions(paramQuant, quantOpts);

        paramQuant = params.get("pipeline quantitation, mass tolerance");
        if (paramQuant != null)
            quantOpts.add("-m" + paramQuant);

        paramQuant = params.get("pipeline quantitation, heavy elutes before light");
        if (paramQuant != null)
            if("yes".equalsIgnoreCase(paramQuant))
                quantOpts.add("-b");

        paramQuant = params.get("pipeline quantitation, fix");
        if (paramQuant != null)
        {
            if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-H");
            else if ("light".equalsIgnoreCase(paramQuant))
                quantOpts.add("-L");
        }

        paramQuant = params.get("pipeline quantitation, fix elution reference");
        if (paramQuant != null)
        {
            String refFlag = "-f";
            if ("peak".equalsIgnoreCase(paramQuant))
                refFlag = "-F";
            paramQuant = params.get("pipeline quantitation, fix elution difference");
            if (paramQuant != null)
                quantOpts.add(refFlag + paramQuant);
        }

        paramQuant = params.get("pipeline quantitation, metabolic search type");
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
