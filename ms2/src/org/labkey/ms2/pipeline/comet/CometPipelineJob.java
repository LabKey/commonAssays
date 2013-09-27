package org.labkey.ms2.pipeline.comet;

import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.sequest.SequestPipelineProvider;
import org.labkey.ms2.pipeline.sequest.SequestSearchTask;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * User: jeckels
 * Date: 9/16/13
 */
public class CometPipelineJob extends AbstractMS2SearchPipelineJob
{
    private static TaskId _tid = new TaskId(CometPipelineJob.class);

    public CometPipelineJob(CometSearchProtocol protocol,
                              ViewBackgroundInfo info,
                              PipeRoot root,
                              String name,
                              File dirSequenceRoot,
                              List<File> filesMzXML,
                              File fileInputXML
    ) throws IOException
    {
        super(protocol, SequestPipelineProvider.name, info, root, name, dirSequenceRoot, fileInputXML, filesMzXML);

        header("Comet search for " + getBaseName());
        writeInputFilesToLog();
    }

    public CometPipelineJob(CometPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);
    }

    public AbstractFileAnalysisJob createSingleFileJob(File file)
    {
        return new CometPipelineJob(this, file);
    }

    public TaskId getTaskPipelineId()
    {
        return _tid;
    }

    public boolean isRefreshRequired()
    {
        return true;
    }

    public File getSearchNativeOutputFile()
    {
        return SequestSearchTask.getNativeOutputFile(getAnalysisDirectory(), getBaseName(), getGZPreference());
    }

}
