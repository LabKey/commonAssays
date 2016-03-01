/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
package org.labkey.ms2.pipeline.comet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.util.FileType;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.TPPTask;
import org.labkey.ms2.pipeline.sequest.AbstractSequestSearchTaskFactory;
import org.labkey.ms2.pipeline.sequest.SequestParamsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: jeckels
 * Date: 9/16/13
 */
public class CometSearchTask extends AbstractMS2SearchTask<CometSearchTask.Factory>
{
    public static final String COMET_PARAMS = "comet.params";
    public static final FileType COMET_PARAMS_FILE_TYPE = new FileType(".comet.params");

    private static final String COMET_ACTION_NAME = "Comet Search";

    public static class Factory extends AbstractSequestSearchTaskFactory
    {
        public Factory()
        {
            super(CometSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new CometSearchTask(this, job);
        }

        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(COMET_ACTION_NAME);
        }
    }

    protected CometSearchTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public CometPipelineJob getJob()
    {
        return (CometPipelineJob)super.getJob();
    }

    @NotNull
    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            // Copy the mzXML file to be local
            File fileMzXML = _factory.findInputFile(getJob());
            File localMzXML = _wd.inputFile(fileMzXML, true);

            // Write out comet.params file
            File fileWorkParams = _wd.newFile(COMET_PARAMS);

            // Default to 2015 params format, but allow for older setting
            String cometVersion = getJob().getParameters().get("comet, version");
            SequestParamsBuilder builder;
            if (cometVersion == null || cometVersion.contains("2015"))
            {
                builder = new Comet2015ParamsBuilder(getJob().getParameters(), getJob().getSequenceRootDirectory());
            }
            else
            {
                builder = new Comet2014ParamsBuilder(getJob().getParameters(), getJob().getSequenceRootDirectory());
            }
            builder.initXmlValues();
            builder.writeFile(fileWorkParams);

            // Perform Comet search
            List<String> args = new ArrayList<>();
            String cometPath = PipelineJobService.get().getExecutablePath("comet", null, "comet", null, getJob().getLogger());
            args.add(cometPath);
            args.add(localMzXML.getName());
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            getJob().runSubProcess(processBuilder, _wd.getDir());


            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(_wd.getDir(),
                                getJob().getBaseName(),
                                FileType.gzSupportLevel.NO_GZ);

            File pepXMLFile = TPPTask.FT_PEP_XML.getFile(_wd.getDir(), getJob().getBaseName());
            if (fileWorkPepXMLRaw.exists())
            {
                fileWorkPepXMLRaw.delete();
            }
            pepXMLFile.renameTo(fileWorkPepXMLRaw);

            try (WorkDirectory.CopyingResource ignored = _wd.ensureCopyingLock())
            {
                RecordedAction cometAction = new RecordedAction(COMET_ACTION_NAME);
                cometAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(args, " "));
                // Copy to a name that's unique to this file and won't conflict between searches in the same directory
                File jobSpecificCometParamsFile = COMET_PARAMS_FILE_TYPE.getFile(fileWorkParams.getParentFile(), getJob().getBaseName());
                FileUtils.moveFile(fileWorkParams, jobSpecificCometParamsFile);
                cometAction.addOutput(_wd.outputFile(jobSpecificCometParamsFile), "CometParams", true);
                cometAction.addOutput(_wd.outputFile(fileWorkPepXMLRaw), "RawPepXML", true);
                for (File file : getJob().getSequenceFiles())
                {
                    cometAction.addInput(file, FASTA_INPUT_ROLE);
                }
                cometAction.addInput(fileMzXML, SPECTRA_INPUT_ROLE);

                return new RecordedActionSet(cometAction);
            }
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
    }
}
