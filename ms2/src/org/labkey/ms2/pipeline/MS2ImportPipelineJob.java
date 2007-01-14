package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineStatusManager;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Importer;
import org.labkey.ms2.MS2Manager;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.exp.XarContext;

import java.io.File;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Mar 9, 2006
 */
public class MS2ImportPipelineJob extends PipelineJob
{
    private final File _file;
    private final String _description;
    private MS2Importer.RunInfo _runInfo;

    public MS2ImportPipelineJob(ViewBackgroundInfo info, File file, String description,
                                MS2Importer.RunInfo runInfo, boolean appendLog) throws SQLException
    {
        super(MS2PipelineProvider.name, info);
        _file = file;
        _description = description;
        _runInfo = runInfo;

        String basename = MS2PipelineManager.getBasename(_file);
        setLogFile(MS2PipelineManager.getLogFile(_file.getParentFile(), basename), appendLog);

        File fileStatus = MS2PipelineManager.getStatusFile(_file.getParentFile(), basename);
        if (NetworkDrive.exists(fileStatus))
            setStatusFile(fileStatus);

        // If there is an existing status file, make sure this job does not
        // overwrite the original provider.  Both Comet and X!Tandem legacy
        // pipeline create these jobs.
        String filePath = PipelineStatusManager.getStatusFilePath(getStatusFile().getAbsolutePath());
        PipelineStatusFile sf = PipelineStatusManager.getStatusFile(filePath);
        if (sf != null)
            _provider = sf.getProvider();
    }

    public ViewURLHelper getStatusHref()
    {
        return null;
    }

    public String getDescription()
    {
        return _description;
    }

    public void run()
    {
        setStatus("LOADING");

        boolean completeStatus = false;
        try
        {
            MS2Manager.addRun(this, _file, _runInfo, new XarContext());
            setStatus(PipelineJob.COMPLETE_STATUS);
            completeStatus = true;
        }
        catch (Exception e)
        {
            getLogger().error("MS2 import failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(PipelineJob.ERROR_STATUS);
            }
        }
    }
}
