package org.labkey.microarray.pipeline;

import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectoryTask;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.util.FileType;
import org.labkey.microarray.MicroarrayModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: Dec 28, 2010
 */
public class FeatureExtractorTask extends WorkDirectoryTask<FeatureExtractorTask.Factory>
{
    private static final String ACTION_NAME = "Feature Extraction";

    public FeatureExtractorTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @Override
    public AbstractFileAnalysisJob getJob()
    {
        return (AbstractFileAnalysisJob)super.getJob();
    }

    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            File protocolFile = _wd.newFile("protocol.fep");
            ProtocolFileBuilder builder = new ProtocolFileBuilder();
            builder.build(protocolFile, getJob().getInputFiles());

            List<String> args = new ArrayList<String>();
            args.add("c:\\Program Files (x86)\\Agilent\\MicroArray\\FeatureExtraction\\FeNoWindows.exe");
            args.add(protocolFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(args);

            getJob().runSubProcess(pb, _wd.getDir());

            _wd.discardFile(protocolFile);
            _wd.remove();
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
        return new RecordedActionSet();
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        public Factory()
        {
            super(FeatureExtractorTask.class);
        }

        public Factory(String name)
        {
            super(FeatureExtractorTask.class, name);    
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new FeatureExtractorTask(this, job);
        }

        public List<FileType> getInputTypes()
        {
            return Collections.singletonList(MicroarrayModule.TIFF_INPUT_TYPE.getFileType());
        }

        public String getStatusName()
        {
            return "FEATURE EXTRACTION";
        }

        public boolean isJobComplete(PipelineJob job)
        {
            // TODO - look for real file
            return false;
//            JobSupport support = job.getJobSupport(JobSupport.class);
//            String baseName = support.getBaseName();
//            File dirAnalysis = support.getAnalysisDirectory();
//
//            return NetworkDrive.exists(getPepXMLFile(dirAnalysis, baseName)));
        }

        public List<String> getProtocolActionNames()
        {
            return Collections.singletonList(ACTION_NAME);
        }

        public String getGroupParameterName()
        {
            return "featureextraction";
        }
    }
}
