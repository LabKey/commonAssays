package org.labkey.signaldata.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayRunUploadContext;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.DefaultAssayRunCreator;
import org.labkey.api.assay.transform.DataTransformService;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.dataiterator.MapDataIterator;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.files.FileContentService;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.file.FileAnalysisJobSupport;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.DataLoaderFactory;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.webdav.WebdavResource;
import org.labkey.api.webdav.WebdavService;
import org.labkey.signaldata.assay.SignalDataAssayDataHandler;
import org.labkey.vfs.FileLike;
import org.labkey.vfs.FileSystemLike;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.labkey.api.files.FileContentService.UPLOADED_FILE;

public class SignalDataImportTask extends PipelineJob.Task<SignalDataImportTask.Factory>
{
    public static final String PROTOCOL_NAME_PROPERTY = "protocolName";

    // metadata file column names
    private static final String INPUT_NAME = "Name";
    private static final String INPUT_DATA_FILE = "DataFile";

    private String _folderName;

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
        Container container = job.getContainer();

        // validate the protocol
        String protocolName = job.getParameters().get(PROTOCOL_NAME_PROPERTY);
        if (StringUtils.isBlank(protocolName))
        {
            log.error("Protocol name cannot be blank");
            return new RecordedActionSet();
        }

        ExpProtocol protocol = AssayService.get().getAssayProtocolByName(container, protocolName);
        if (protocol == null)
        {
            log.error("Could not resolve the specified protocol name : {}", protocolName);
            return new RecordedActionSet();
        }

        // guaranteed to only have a single file
        if (support.getInputFiles().size() != 1)
        {
            log.error("Expecting a single input file but received {}", support.getInputFiles().size());
            return new RecordedActionSet();
        }
        FileLike dataFile = support.getInputFiles().getFirst();

        try
        {
            FileLike runRoot = getTargetFolder(container, log);
            if (runRoot == null)
                return new RecordedActionSet();

            log.info("Loading {}", dataFile.getName());
            List<Map<String, Object>> dataRows = parseMetadata(dataFile, log);
            List<Map<String, Object>> dataInputs = new ArrayList<>();

            for (Map<String, Object> row : dataRows)
            {
                // parse out the name and datafile properties
                String name = Objects.toString(row.get(INPUT_NAME), "");
                String dataFilePath = Objects.toString(row.get(INPUT_DATA_FILE), "").trim();

                // validate the existance of the datafile property and make a copy to the run root
                if (StringUtils.isBlank(dataFilePath))
                {
                    log.warn("Skipping row '{}' with blank DataFile property", name);
                    continue;
                }

                // If the value is just a filename (no directory separators), resolve it relative to
                // the metadata file's directory; otherwise treat it as a full server-side path
                String dataFileName = FileUtil.getFileName(Path.of(dataFilePath));
                FileLike sourceFile = null;
                if (dataFilePath.equals(dataFileName))
                {
                    String sourcePath = support.getParameters().get(DataTransformService.ORIGINAL_SOURCE_PATH);
                    if (StringUtils.isNotBlank(sourcePath))
                    {
                        FileLike originalSource = FileSystemLike.wrapFile(new File(sourcePath));
                        sourceFile = originalSource.getParent().resolveChild(dataFilePath);
                    }
                }
                else
                {
                    // check to see if it's a webdav url
                    WebdavResource resource = WebdavService.get().lookup(dataFilePath);
                    if (resource != null)
                    {
                        sourceFile = FileSystemLike.wrapFile(resource.getFile());
                    }

                    // check to see if it's a server-side path
                    if (sourceFile == null)
                    {
                        Path resolvedPath = Path.of(dataFilePath).toAbsolutePath().normalize();

                        if (!isUnderAnyPipelineRoot(resolvedPath))
                        {
                            log.error("DataFile '{}' is not under a server-managed pipeline root", dataFilePath);
                            row.remove(INPUT_DATA_FILE);
                            continue;
                        }
                        sourceFile = FileSystemLike.wrapFile(resolvedPath.toFile());
                    }
                }

                if (sourceFile != null && !sourceFile.exists())
                {
                    log.info("Data file not found: {}", sourceFile.getPath());
                    row.remove(INPUT_DATA_FILE);
                    continue;
                }

                // add a data input entry for the run
                Map<String, Object> dataInput = new CaseInsensitiveHashMap<>();
                dataInputs.add(dataInput);

                log.info("Copying {} to run folder", sourceFile.getName());
                FileLike destFile = runRoot.resolveChild(sourceFile.getName());
                FileUtil.copyFile(sourceFile, destFile);

                log.info("Ensuring input data is created for {}", destFile.getName());
                URI uri = FileContentService.get().getWebDavUrl(destFile, container, FileContentService.PathType.full);
                if (uri != null)
                {
                    WebdavResource resource = WebdavService.get().lookup(uri.getPath());
                    if (resource != null)
                    {
                        ExpData data = FileContentService.get().getDataObject(resource, container);
                        if (data == null)
                        {
                            // create the ExpData object for the input data
                            data = ExperimentService.get().createData(container, UPLOADED_FILE);
                            data.setName(destFile.getName());
                            data.setDataFileURI(destFile.toURI());
                            data.save(job.getUser());
                        }

                        FileLike d = FileUtil.getAbsoluteCaseSensitiveFile(destFile);
                        String url = d.toURI().toURL().toString();

                        dataInput.put(ExpDataTable.Column.Name.name(), data.getName());
                        dataInput.put(ExpDataTable.Column.DataFileUrl.name(), data.getDataFileUrl());

                        // file data type for this run data field, adjust the URL to be compatible
                        String dataFileUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
                        row.replace(INPUT_DATA_FILE, dataFileUrl.replace("file:", ""));
                    }
                }
            }

            // create and save the run
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider != null && !dataRows.isEmpty())
            {
                AssayRunUploadContext.Factory<?,?> runFactory = provider.createRunUploadFactory(protocol, job.getUser(), container);

                runFactory.setName(_folderName);
                runFactory.setLogger(log);
                runFactory.setRawData(MapDataIterator.of(dataRows));
                runFactory.setRunProperties(Map.of("RunIdentifier", _folderName));

                Map<ExpData, String> inputDatasMap = new HashMap<>();
                for (Map<String, Object> inputMap : dataInputs)
                {
                    String dataFileUrl = Objects.toString(inputMap.get(ExpDataTable.Column.DataFileUrl.name()), "");
                    if (!dataFileUrl.isEmpty())
                    {
                        ExpData expData = ExperimentService.get().getExpDataByURL(dataFileUrl, container);
                        if (expData != null)
                            inputDatasMap.put(expData, Objects.toString(inputMap.get(ExpDataTable.Column.Name.name()), ""));
                    }
                }
                if (!inputDatasMap.isEmpty())
                    runFactory.setInputDatas(inputDatasMap);

                // generate output data
                Map<Object, String> outputData = new HashMap<>();
                DefaultAssayRunCreator.generateResultData(job.getUser(), container, provider, dataRows, outputData, log);
                runFactory.setOutputDatas(outputData);

                try
                {
                    provider.getRunCreator().saveExperimentRun(runFactory.create(), null);
                }
                catch (ValidationException | ExperimentException e)
                {
                    log.error("Error saving assay run: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        }
        catch (Exception e)
        {
            log.error("Error importing data : {}", e.getMessage());
            throw new RuntimeException(e);
        }

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

    @Nullable
    private FileLike getTargetFolder(Container container, Logger log) throws IOException
    {
        PipeRoot root = PipelineService.get().findPipelineRoot(container);
        if (root != null)
        {
            _folderName = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy_M_d_H_m_s"));

            //Create folder if needed
            FileLike runRoot = root.getRootFileLike().resolveChild(SignalDataAssayDataHandler.NAMESPACE).resolveChild(_folderName);
            if (!runRoot.exists())
                runRoot.mkdirs();

            return runRoot;
        }
        else
            log.error("Unable to find a pipeline root for container : {}", container.getPath());

        return null;
    }

    /**
     * Determine whether the given path falls under a pipeline root for some container, using the same semantics as
     * {@link PipelineService#findPipelineRoot(Container)} (which includes the default file-root fallback, not just
     * explicitly configured pipeline roots). First try to resolve the path directly to its owning container(s); if
     * that comes up empty (e.g. a container with a custom, non-default file root that the path-resolution logic does
     * not yet handle), fall back to scanning every container's pipeline root.
     */
    private boolean isUnderAnyPipelineRoot(Path resolvedPath)
    {
        for (Container c : FileContentService.get().getContainersForFilePath(resolvedPath))
        {
            if (isUnderPipelineRoot(c, resolvedPath))
                return true;
        }

        // Path could not be resolved to a container directly; fall back to scanning all containers
        for (Container c : ContainerManager.getAllChildren(ContainerManager.getRoot()))
        {
            if (isUnderPipelineRoot(c, resolvedPath))
                return true;
        }

        return false;
    }

    private boolean isUnderPipelineRoot(Container container, Path resolvedPath)
    {
        PipeRoot root = PipelineService.get().findPipelineRoot(container);
        return root != null && root.isUnderRoot(resolvedPath);
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
