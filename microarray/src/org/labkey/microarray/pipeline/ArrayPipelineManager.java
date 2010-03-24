/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.microarray.pipeline;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.*;
import org.labkey.api.util.FileUtil;
import org.labkey.microarray.MicroarrayModule;


public class ArrayPipelineManager {
    
    private static Logger _log = Logger.getLogger(MicroarrayPipelineProvider.class);
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
            String name = f.getName().toLowerCase();
            return (name.endsWith(".tif") || name.endsWith(".tiff")) && f.isFile();
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
            return MicroarrayModule.MAGE_ML_INPUT_TYPE.getFileType().isType(f);
        }
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
    
    
    public static File[] getImageFiles(File imageDir, FileStatus status, Container c) throws IOException, SQLException
    {
        Map<File, FileStatus> imageFileStatus = getExtractionStatus(imageDir, c);
        List<File> fileList = new ArrayList<File>();
        for (File imageFile : imageFileStatus.keySet())
        {
            if (status == null || status.equals(imageFileStatus.get(imageFile)))
                fileList.add(imageFile);
        }
        return fileList.toArray(new File[fileList.size()]);
    }
    
    public static File[] getMageFiles(URI uriData, FileStatus status, Container c) throws IOException, SQLException
    {
        Map<File, FileStatus> mageFileStatus = getExperimentRunStatus(uriData, c);
        List<File> fileList = new ArrayList<File>();
        for (File mageFile : mageFileStatus.keySet())
        {
            if (status == null || status.equals(mageFileStatus.get(mageFile)))
                fileList.add(mageFile);
        }
        return fileList.toArray(new File[fileList.size()]);
    }
    
    public static Map<File, FileStatus> getExtractionStatus(File imageDir, Container c) throws IOException, SQLException
    {
        Set<File> knownFiles = new HashSet<File>();
        Set<File> checkedDirectories = new HashSet<File>();
        
        File[] imageFiles = imageDir.listFiles(getImageFileFilter());

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

            File logFile = getExtractionLog(imageDir, null);
            boolean logExists = exists(logFile, knownFiles, checkedDirectories);

            for (File file : imageFiles)
            {
                FileStatus status = FileStatus.UNKNOWN;
                if (logExists)
                {
                    // Check to see if images match what is being or has been processed by the pipeline.
                    PipelineStatusFile sf = PipelineService.get().getStatusFile(logFile.getAbsolutePath());
                    if (null == sf || !sf.isActive())
                        status = FileStatus.COMPLETE;
                    else
                        status = FileStatus.RUNNING;
                    
                    BufferedReader logFileReader = null;
                    try
                    {
                        logFileReader = new BufferedReader(new FileReader(logFile));
                        String line;
                        while ((line = logFileReader.readLine()) != null)
                        {
                            if (line.endsWith("EXTRACTING"))
                                 break;
                            
                            if (line.endsWith(file.getName()))
                            {
                                imageFileMap.put(file, status);
                                break;
                            }
                        }
                        if (!imageFileMap.containsKey(file))
                        {
                            imageFileMap.put(file, FileStatus.UNKNOWN);
                        }
                    }
                    finally
                    {
                        try
                        {
                            logFileReader.close();
                        }
                        catch (Exception e) {}
                    }
                }
                else
                {
                    imageFileMap.put(file, status);
                }
            }
        }
        return imageFileMap;
    }
    
    public static Map<File, FileStatus> getExperimentRunStatus(URI uriData, Container c) throws IOException, SQLException
    {
        Set<File> knownFiles = new HashSet<File>();
        Set<File> checkedDirectories = new HashSet<File>();
        
        File dirData = FileUtil.getAbsoluteCaseSensitiveFile(new File(uriData));
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
                    PipelineStatusFile sf = PipelineService.get().getStatusFile(logFile.getCanonicalPath());
                    if (null == sf || !sf.isActive())
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
                    knownFiles.addAll(Arrays.asList(files));
                }
                checkedDirectories.add(parent);
            }
            return knownFiles.contains(file);
        }
        return file.exists();
    }
    
}
