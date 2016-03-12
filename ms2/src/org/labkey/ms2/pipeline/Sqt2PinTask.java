/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.pipeline.WorkDirectoryTask;
import org.labkey.api.pipeline.cmd.TaskPath;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.util.FileType;
import org.labkey.api.writer.PrintWriters;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts from .sqt to .pin file format, using the sqt2pin command-line tool.
 * Used by the MacCoss proteomics analysis workflow.
 * User: jeckels
 * Date: Jun 12, 2012
 */
public class Sqt2PinTask extends WorkDirectoryTask<Sqt2PinTask.Factory>
{
    public Sqt2PinTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @NotNull
    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        AbstractFileAnalysisJob job = (AbstractFileAnalysisJob) getJob();

        RecordedAction action = new RecordedAction(_factory.getStatusName());
        
        try
        {
            try (WorkDirectory.CopyingResource ignored = _wd.ensureCopyingLock())
            {
                TaskPath targetListTP = new TaskPath(".target.list");
                TaskPath decoyListTP = new TaskPath(".decoy.list");
                File targetListFile = _wd.newWorkFile(WorkDirectory.Function.output, targetListTP, job.getBaseName());
                File decoyListFile = _wd.newWorkFile(WorkDirectory.Function.output, decoyListTP, job.getBaseName());

                try (PrintWriter targetWriter = PrintWriters.getPrintWriter(targetListFile); PrintWriter decoyWriter = PrintWriters.getPrintWriter(decoyListFile))
                {
                    FileType targetSQTFileType = new FileType(".sqt");
                    FileType decoySQTFileType = new FileType(".decoy.sqt");

                    int index = 1;
                    for (String inputBaseName : ((AbstractFileAnalysisJob) getJob()).getSplitBaseNames())
                    {
                        String targetFileName = targetSQTFileType.getName(job.getAnalysisDirectory(), inputBaseName);
                        targetWriter.write(targetFileName);
                        targetWriter.write("\n");
                        String decoyFileName = decoySQTFileType.getName(job.getAnalysisDirectory(), inputBaseName);
                        decoyWriter.write(decoyFileName);
                        decoyWriter.write("\n");

                        File inputTargetFile = new File(job.getAnalysisDirectory(), targetFileName);
                        _wd.inputFile(inputTargetFile, false);
                        File inputDecoyFile = new File(job.getAnalysisDirectory(), decoyFileName);
                        _wd.inputFile(inputDecoyFile, false);
                        action.addInput(inputTargetFile, "SQT" + (index == 1 ? "" : Integer.toString(index)));
                        action.addInput(inputDecoyFile, "DecoySQT" + (index == 1 ? "" : Integer.toString(index)));
                        index++;
                    }
                }

                File output = new File(_wd.getDir(), job.getBaseName() + ".pin.xml");

                List<String> args = new ArrayList<>();
                String version = getJob().getParameters().get("sqt2pin, version");
                args.add(PipelineJobService.get().getExecutablePath("sqt2pin_v" + PipelineJobService.VERSION_SUBSTITUTION, null, null, version, getJob().getLogger()));
                args.add("-M");
                args.add("-p");
                args.add("*:1:#:2:@:3:^:4:~:5:$:6");
                args.add("-o");
                args.add(output.getName());
                args.add(targetListFile.getName());
                args.add(decoyListFile.getName());

                ProcessBuilder pb = new ProcessBuilder(args);
                pb.directory(_wd.getDir());

                job.runSubProcess(pb, _wd.getDir());

                action.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(args, " "));

                action.addOutput(_wd.outputFile(output), "PinXML", false);
                action.addOutput(_wd.outputFile(targetListFile), "SQTFileList", false);
                action.addOutput(_wd.outputFile(decoyListFile), "DecoySQTFileList", false);
            }
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
        return new RecordedActionSet(action);
    }

    public static class FactorySettings extends AbstractTaskFactorySettings
    {
        private String _cloneName;

        public FactorySettings(String name)
        {
            super(Sqt2PinTask.class, name);
        }

        public TaskId getCloneId()
        {
            return new TaskId(Sqt2PinTask.class, _cloneName);
        }

        public String getCloneName()
        {
            return _cloneName;
        }

        public void setCloneName(String cloneName)
        {
            _cloneName = cloneName;
        }
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        public Factory()
        {
            super(Sqt2PinTask.class);
        }

        public String getStatusName()
        {
            return "SQT2PIN";
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new Sqt2PinTask(this, job);
        }

        @Override
        public boolean isJoin()
        {
            return true;
        }

        public List<FileType> getInputTypes()
        {
            return Collections.singletonList(AbstractMS2SearchProtocol.FT_MZXML);
        }

        public boolean isJobComplete(PipelineJob job)
        {
            return false;
        }

        public List<String> getProtocolActionNames()
        {
            return Collections.singletonList(getStatusName());
        }
    }
}
