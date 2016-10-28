/*
 * Copyright (c) 2007-2016 LabKey Corporation
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
package org.labkey.ms2.pipeline.tandem;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.pipeline.file.FileAnalysisJobSupport;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.AbstractMS2SearchTaskFactory;
import org.labkey.ms2.pipeline.TPPTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts XTandem's .xtan.xml native output to pepXML. For typical searches this is handled
 * directly in XTandemSearchTask, but in some cases we also need it separately.
 */
public class XTandemToXMLTask extends AbstractMS2SearchTask<XTandemToXMLTask.Factory>
{
    public static class Factory extends AbstractMS2SearchTaskFactory<Factory>
    {
        public Factory()
        {
            super(XTandemToXMLTask.class);
        }

        @Override
        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new XTandemToXMLTask(this, job);
        }

        @Override
        public boolean isJobComplete(PipelineJob job)
        {
            FileAnalysisJobSupport support = (FileAnalysisJobSupport) job;
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            // The raw pepXML exists
            return NetworkDrive.exists(AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis, baseName));
        }

        @Override
        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(XTandemSearchTask.TANDEM2_XML_ACTION_NAME);
        }

        @Override
        public String getGroupParameterName()
        {
            return "xtandem";
        }
    }

    protected XTandemToXMLTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public FileAnalysisJobSupport getJobSupport()
    {
        return getJob().getJobSupport(FileAnalysisJobSupport.class);
    }

    @NotNull
    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            FileAnalysisJobSupport support = getJobSupport();
            String baseName = support.getBaseName();

            File fileOutputXML = XTandemSearchTask.getNativeOutputFile(support.getAnalysisDirectory(), baseName, FileType.gzSupportLevel.SUPPORT_GZ);
            if (!fileOutputXML.isFile())
                fileOutputXML = XTandemSearchTask.getNativeOutputFile(support.getDataDirectory(), baseName, FileType.gzSupportLevel.SUPPORT_GZ);
            File fileWorkOutputXML = _wd.inputFile(fileOutputXML, false);

            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(_wd.getDir(), baseName, support.getGZPreference());

            String ver = TPPTask.getTPPVersion(getJob());
            String exePath = PipelineJobService.get().getExecutablePath("Tandem2XML", null, "tpp", ver, getJob().getLogger());
            ProcessBuilder tandem2XmlPB = new ProcessBuilder(exePath,
                _wd.getRelativePath(fileWorkOutputXML),
                fileWorkPepXMLRaw.getName());
            getJob().runSubProcess(tandem2XmlPB,
                    _wd.getDir());

            // Move final outputs to analysis directory.
            File filePepXMLRaw;
            try (WorkDirectory.CopyingResource ignored = _wd.ensureCopyingLock())
            {
                filePepXMLRaw = _wd.outputFile(fileWorkPepXMLRaw);
            }

            List<RecordedAction> actions = new ArrayList<>();

            RecordedAction tandem2XmlAction = new RecordedAction(XTandemSearchTask.TANDEM2_XML_ACTION_NAME);
            tandem2XmlAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(tandem2XmlPB.command(), ' '));
            tandem2XmlAction.addInput(fileOutputXML, "TandemXML");
            tandem2XmlAction.addOutput(filePepXMLRaw, "RawPepXML", true);
            actions.add(tandem2XmlAction);

            return new RecordedActionSet(actions);
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
    }
}
