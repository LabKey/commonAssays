package org.labkey.ms2.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.TaskFactory;
import org.labkey.api.pipeline.TaskFactorySettings;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.pipeline.WorkDirectoryTask;
import org.labkey.api.pipeline.cmd.TaskPath;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.util.FileType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: Jun 12, 2012
 */
public class Sqt2PinTask extends WorkDirectoryTask<Sqt2PinTask.Factory>
{
    public Sqt2PinTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        AbstractFileAnalysisJob job = (AbstractFileAnalysisJob) getJob();

        RecordedAction action = new RecordedAction(_factory.getStatusName());
        
        try
        {
            WorkDirectory.CopyingResource lock = null;
            try
            {
                lock = _wd.ensureCopyingLock();
                TaskPath targetListTP = new TaskPath(".target.list");
                TaskPath decoyListTP = new TaskPath(".decoy.list");
                File targetListFile = _wd.newWorkFile(WorkDirectory.Function.output, targetListTP, job.getBaseName());
                File decoyListFile = _wd.newWorkFile(WorkDirectory.Function.output, decoyListTP, job.getBaseName());
                BufferedWriter targetWriter = null;
                BufferedWriter decoyWriter = null;
                try
                {
                    targetWriter = new BufferedWriter(new FileWriter(targetListFile));
                    decoyWriter = new BufferedWriter(new FileWriter(decoyListFile));

                    FileType targetSQTFileType = new FileType(".sqt");
                    FileType decoySQTFileType = new FileType(".decoy.sqt");

                    int index = 1;
                    for (String inputBaseName :((AbstractFileAnalysisJob)getJob()).getSplitBaseNames())
                    {
                        String targetFileName = targetSQTFileType.getName(job.getAnalysisDirectory(), inputBaseName);
                        targetWriter.write(targetFileName);
                        String decoyFileName = decoySQTFileType.getName(job.getAnalysisDirectory(), inputBaseName);
                        decoyWriter.write(decoyFileName);

                        File inputTargetFile = new File(job.getAnalysisDirectory(), targetFileName);
                        _wd.inputFile(inputTargetFile, false);
                        File inputDecoyFile = new File(job.getAnalysisDirectory(), decoyFileName);
                        _wd.inputFile(inputDecoyFile, false);
                        action.addInput(inputTargetFile, "SQT" + (index == 1 ? "" : Integer.toString(index)));
                        action.addInput(inputDecoyFile, "DecoySQT" + (index == 1 ? "" : Integer.toString(index)));
                        index++;
                    }
                }
                finally
                {
                    if (targetWriter != null) { try { targetWriter.close(); } catch (IOException ignored) {} }
                    if (decoyWriter != null) { try { decoyWriter.close(); } catch (IOException ignored) {} }
                }

                List<String> args = new ArrayList<String>();
                args.add(PipelineJobService.get().getExecutablePath("sqt2pin", null, null, getJob().getLogger()));
                args.add(targetListFile.getName());
                args.add(decoyListFile.getName());

                ProcessBuilder pb = new ProcessBuilder(args);
                pb.directory(_wd.getDir());

                File output = new File(_wd.getDir(), job.getBaseName() + ".pin.xml");

                job.runSubProcess(pb, _wd.getDir(), output, 0, false);

                action.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(args, " "));

                action.addOutput(_wd.outputFile(output), "PinXML", false);
            }
            finally
            {
                if (lock != null) { lock.release(); }
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
