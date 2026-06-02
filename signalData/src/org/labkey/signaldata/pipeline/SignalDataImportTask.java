package org.labkey.signaldata.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.assay.AssayService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.file.FileAnalysisJobSupport;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.DataLoaderFactory;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.vfs.FileLike;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SignalDataImportTask extends PipelineJob.Task<SignalDataImportTask.Factory>
{
    public static final String PROTOCOL_NAME_PROPERTY = "protocolName";

    private SignalDataImportTask(SignalDataImportTask.Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @NotNull
    @Override
    public RecordedActionSet run()
    {
        PipelineJob job = getJob();
        FileAnalysisJobSupport support = job.getJobSupport(FileAnalysisJobSupport.class);
        job.setLogFile(support.getDataDirectory().resolveChild(FileUtil.makeFileNameWithTimestamp("triggered_signaldata_import", "log")));
        job.setStatus("RELOADING", "Job started at: " + DateUtil.nowISO());
        Logger log = job.getLogger();

        // validate the protocol
        String protocolName = job.getParameters().get(PROTOCOL_NAME_PROPERTY);
        if (StringUtils.isBlank(protocolName))
        {
            log.error("Protocol name cannot be blank");
            return new RecordedActionSet();
        }

        ExpProtocol protocol = AssayService.get().getAssayProtocolByName(job.getContainer(), protocolName);
        if (protocol == null)
        {
            log.error("Could not resolve the specified protocol name : {}", protocolName);
            return new RecordedActionSet();
        }

        // guaranteed to only have a single file
        assert support.getInputFiles().size() == 1;
        FileLike dataFile = support.getInputFiles().getFirst();

        log.info("Loading {}", dataFile.getName());
        List<Map<String, Object>> dataRows = parseMetadata(dataFile, log);

        return new RecordedActionSet();
    }

    private List<Map<String, Object>> parseMetadata(FileLike dataFile, Logger log)
    {
        DataLoaderFactory dlf = DataLoader.get().findFactory(dataFile, null);
        if (null == dlf)
        {
            log.error("Unable to find a loader for file : {}", dataFile.getPath());
            return Collections.emptyList();
        }

        try (InputStream in = dataFile.openInputStream();
             DataLoader loader = dlf.createLoader(in, true))
        {
            return loader.load();
        }
        catch (Exception e)
        {
            log.error("Error parsing the metadata file : {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        public Factory()
        {
            super(SignalDataImportTask.class);
        }

        @Override
        public SignalDataImportTask createTask(PipelineJob job)
        {
            return new SignalDataImportTask(this, job);
        }

        @Override
        public List<FileType> getInputTypes()
        {
            return Collections.emptyList();
        }

        @Override
        public List<String> getProtocolActionNames()
        {
            return Collections.emptyList();
        }

        @Override
        public String getStatusName()
        {
            return "IMPORT SIGNAL DATA";
        }

        @Override
        public boolean isJobComplete(PipelineJob job)
        {
            return false;
        }
    }
}
