package org.labkey.microarray.pipeline;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.*;


public class ArrayPipelineManager {
    
    private static Logger _log = Logger.getLogger(ArrayPipeline.class);
    protected static String _pipelineAgilentXML = "agilent.xml";
    protected static String _pipelineLogExt = ".log";
    protected static String MAGE_EXTENSION = "_MAGEML.xml";
    protected static String _pipelineLoResImageExt = ".jpg";
    protected static String _pipelineQCReportExt = ".pdf";
    protected static String _pipelineFeatureExt = "_feat.csv";
    protected static String _pipelineAlignmentExt = "_grid.csv";
    protected static String _pipelineResultsExt = ".tgz";
    
    public static File getExtractionLog(File dirImages, String baseName)
    {
        if (null == baseName)
            return new File(dirImages, "Extraction" + _pipelineLogExt);
        return new File(dirImages, baseName + _pipelineLogExt);
    }
    
    public static File getExtractionLog(URI uriRoot, String logPath)
    {
        return new File(uriRoot.resolve(logPath));
    }
    
    public static File getExperimentRunLog(File dirMage, String baseName)
    {
        if (null == baseName)
            return new File(dirMage, "ExperimentRun" + _pipelineLogExt);
        return new File(dirMage, baseName + _pipelineLogExt);
    }
    
    public static File getExperimentRunLog(URI uriRoot, String logPath)
    {
        return new File(uriRoot.resolve(logPath));
    }
    
    public static File getResultsFile(File dir, String baseName)
    {
        return new File(dir, baseName + _pipelineResultsExt);
    }
    
    public static class ImageFileFilter extends PipelineProvider.FileEntryFilter
    {
        public boolean accept(File f)
        {
            return (f.getName().endsWith(".tif") || f.getName().endsWith(".tiff")) && f.isFile();
        }
    }

    public static PipelineProvider.FileEntryFilter getImageFileFilter()
    {
        return new ImageFileFilter();
    }
    
    public static class MageFileFilter extends PipelineProvider.FileEntryFilter
    {
        public boolean accept(File f)
        {
            return isMageFile(f);
        }

    }

    private static final Pattern MAGE_REGEX = Pattern.compile("(.*)(\\Q.mage\\E|\\QMAGE-ML.xml\\E|\\Q_MAGEML.xml\\E)", Pattern.CASE_INSENSITIVE);

    public static boolean isMageFile(File f)
    {
        return MAGE_REGEX.matcher(f.getName()).matches() && f.isFile();
    }

    public static String getBaseMageName(String filename)
    {
        Matcher matcher = MAGE_REGEX.matcher(filename);
        if (matcher.matches() && matcher.groupCount() > 0)
        {
            return matcher.group(1);
        }
        return null;
    }

    public static PipelineProvider.FileEntryFilter getMageFileFilter()
    {
        return new MageFileFilter();
    }
    
    public static class FeatureFileFilter extends PipelineProvider.FileEntryFilter
    {
        public boolean accept(File f)
        {
            return f.getName().endsWith(_pipelineFeatureExt) && f.isFile();
        }
    }

    public static PipelineProvider.FileEntryFilter getFeatureFileFilter()
    {
        return new FeatureFileFilter();
    }
    
    public static class AlignmentFileFilter extends PipelineProvider.FileEntryFilter
    {
        public boolean accept(File f)
        {
            return f.getName().endsWith(_pipelineAlignmentExt) && f.isFile();
        }
    }

    public static PipelineProvider.FileEntryFilter getAlignmentFileFilter()
    {
        return new AlignmentFileFilter();
    }
    
    
    public static File[] getImageFiles(URI uriData, String protocolName, FileStatus status, Container c, String extractionEngine) throws IOException
    {
        Map<File, FileStatus> imageFileStatus = getExtractionStatus(uriData, protocolName, c, extractionEngine);
        List<File> fileList = new ArrayList<File>();
        for (File imageFile : imageFileStatus.keySet())
        {
            if (status == null || status.equals(imageFileStatus.get(imageFile)))
                fileList.add(imageFile);
        }
        return fileList.toArray(new File[fileList.size()]);
    }
    
    public static File[] getMageFiles(URI uriData, String protocolName, FileStatus status, Container c) throws IOException
    {
        Map<File, FileStatus> mageFileStatus = getExperimentRunStatus(uriData, protocolName, c);
        List<File> fileList = new ArrayList<File>();
        for (File mageFile : mageFileStatus.keySet())
        {
            if (status == null || status.equals(mageFileStatus.get(mageFile)))
                fileList.add(mageFile);
        }
        return fileList.toArray(new File[fileList.size()]);
    }
    
    public static Map<File, FileStatus> getExtractionStatus(URI uriData, String protocolName, Container c, String extractionEngine) throws IOException
    {
        Set<File> knownFiles = new HashSet<File>();
        Set<File> checkedDirectories = new HashSet<File>();
        
        File dirData = new File(uriData).getCanonicalFile();
        String dirDataURL = dirData.toURI().toURL().toString();
        File[] imageFiles = dirData.listFiles(getImageFileFilter());

        Map<File, FileStatus> imageFileMap = new LinkedHashMap<File, FileStatus>();
        if (imageFiles != null && imageFiles.length > 0)
        {
            Arrays.sort(imageFiles, new Comparator<File>()
            {
                public int compare(File o1, File o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            File logFile = getExtractionLog(dirData, null);
            boolean logExists = exists(logFile, knownFiles, checkedDirectories);

            for (File file : imageFiles)
            {
                FileStatus status = FileStatus.UNKNOWN;
                if (logExists) {//Check to see if images match what is being or has been processed by the pipeline.
                    PipelineJob job = PipelineService.get().getPipelineQueue().findJob(c, logFile.getAbsolutePath());
                    if (null == job || job.isDone())
                        status = FileStatus.COMPLETE;
                    else
                        status = FileStatus.RUNNING;
                    
                    BufferedReader logFileReader = null;
                    try {
                        logFileReader = new BufferedReader(new FileReader(logFile));
                        String line;
                        while (logFileReader.ready())
                        {
                            line = logFileReader.readLine();
                            if (line.endsWith("EXTRACTING"))
                                 break;
                            
                            if (line.endsWith(file.getName())) {
                                imageFileMap.put(file, status);
                                break;
                            }
                        }
                        logFileReader.close();
                    }
                    catch (IOException ioe) {
                        _log.error("Error encountered when attempting to read pipeline log file in method getExtractionStatus...",ioe);
                    }
                    finally {
                        try {
                            logFileReader.close();
                        }
                        catch (Exception e) {}
                    }
                } else {
                    imageFileMap.put(file, status);
                }
            }
        }
        return imageFileMap;
    }
    
    public static Map<File, FileStatus> getExperimentRunStatus(URI uriData, String protocolName, Container c) throws IOException
    {
        Set<File> knownFiles = new HashSet<File>();
        Set<File> checkedDirectories = new HashSet<File>();
        
        File dirData = new File(uriData).getCanonicalFile();
        String dirDataURL = dirData.toURI().toURL().toString();
        File[] mageFiles = dirData.listFiles(getMageFileFilter());

        Map<File, FileStatus> mageFileMap = new LinkedHashMap<File, FileStatus>();
        if (mageFiles != null && mageFiles.length > 0)
        {
            Arrays.sort(mageFiles, new Comparator<File>()
            {
                public int compare(File o1, File o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            File logFile = getExperimentRunLog(dirData, null);
            boolean logExists = exists(logFile, knownFiles, checkedDirectories);

            for (File file : mageFiles)
            {
                FileStatus status = FileStatus.UNKNOWN;
                if (logExists) {//Check to see if files match what is being or has been processed by the pipeline.
                    PipelineJob job = PipelineService.get().getPipelineQueue().findJob(c, logFile.getAbsolutePath());
                    if (null == job || job.isDone())
                        status = FileStatus.COMPLETE;
                    else
                        status = FileStatus.RUNNING;
                    
                    BufferedReader logFileReader = null;
                    try {
                        logFileReader = new BufferedReader(new FileReader(logFile));
                        String line;
                        while (logFileReader.ready())
                        {
                            line = logFileReader.readLine();
                            if (line.endsWith("EXTRACTING"))
                                 break;
                            
                            if (line.endsWith(file.getName())) {
                                mageFileMap.put(file, status);
                                break;
                            }
                        }
                        logFileReader.close();
                    }
                    catch (IOException ioe) {
                        _log.error("Error encountered when attempting to read pipeline log file in method getExperimentRunStatus...",ioe);
                    }
                    finally {
                        try {
                            logFileReader.close();
                        }
                        catch (Exception e) {}
                    }
                } else {
                    mageFileMap.put(file, status);
                }
            }
        }
        return mageFileMap;
    }
    
    public static String getDataDescription(File dirData, String baseName, String protocolName)
    {
        String dataName = "";
        if (dirData != null)
        {
            dataName = dirData.getName();
            if ("xml".equals(dataName))
            {
                dirData = dirData.getParentFile();
                if (dirData != null)
                    dataName = dirData.getName();
            }
        }

        StringBuffer description = new StringBuffer(dataName);
        if (baseName != null)
        {
            if (description.length() > 0)
                description.append("/");
            description.append(baseName);
        }
        description.append(" (").append(protocolName).append(")");
        return description.toString();
    }
    
/*    public static void runFeatureExtraction(ViewBackgroundInfo info,
                                   URI uriRoot,
                                   URI uriData,
                                   PipelineProtocol protocol,
                                   String extractionEngine) throws IOException, SQLException
    {
        String protocolName = protocol.getName();
        File dirData = new File(uriData);
        if (!dirData.exists())
        {
            throw new IllegalArgumentException("The specified data directory does not exist.");
        }

        AppProps appProps = AppProps.getInstance();
        if ("agilent".equalsIgnoreCase(extractionEngine) && appProps.getFeatureExtractionServer() == null)
            throw new IllegalArgumentException("Feature extraction server has not been specified in site customization.");

        File[] unprocessedFile = getImageFiles(uriData, protocolName, FileStatus.UNKNOWN, info.getContainer(), extractionEngine);
        List<File> imageFileList = new ArrayList<File>();
        imageFileList.addAll(Arrays.asList(unprocessedFile));
        File[] imageFiles = imageFileList.toArray(new File[imageFileList.size()]);
        if (imageFiles.length == 0)
            throw new IllegalArgumentException("Feature extraction for this protocol is already complete.");

        PipelineService service = PipelineService.get();
        PipelineJob job = null;
        job = new FeatureExtractionPipelineJob(info, protocolName, uriRoot, uriData, imageFiles);
        
        service.queueJob(job);
    }
    
    public static void runMageLoader(ViewBackgroundInfo info,
                                   URI uriRoot,
                                   URI uriData,
                                   PipelineProtocol protocol) throws IOException, SQLException
    {
        String protocolName = protocol.getName();
        File dirData = new File(uriData);
        if (!dirData.exists())
        {
            throw new IllegalArgumentException("The specified data directory does not exist.");
        }

        AppProps appProps = AppProps.getInstance();
        File[] unprocessedFile = getMageFiles(uriData, protocolName, FileStatus.UNKNOWN, info.getContainer());
        List<File> mageFileList = new ArrayList<File>();
        mageFileList.addAll(Arrays.asList(unprocessedFile));
        File[] mageFiles = mageFileList.toArray(new File[mageFileList.size()]);
        if (mageFiles.length == 0)
            throw new IllegalArgumentException("Experiment Run creation for this protocol is already complete.");

        PipelineService service = PipelineService.get();
        PipelineJob job = null;
        job = new FeatureExtractionPipelineJob(info, protocolName, uriRoot, uriData, mageFiles);
        
        service.queueJob(job);
    }
  */  
    public static boolean exists(File file, Set<File> knownFiles, Set<File> checkedDirectories)
    {
        File parent = file.getParentFile();
        if (parent != null)
        {
            if (!checkedDirectories.contains(parent))
            {
                File[] files = parent.listFiles();
                if (files != null)
                {
                    for (File f : files)
                    {
                        knownFiles.add(f);
                    }
                }
                checkedDirectories.add(parent);
            }
            return knownFiles.contains(file);
        }
        return file.exists();
    }
    
}
