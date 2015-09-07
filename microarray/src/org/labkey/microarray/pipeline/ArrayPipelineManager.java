/*
 * Copyright (c) 2008-2015 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.reader.Readers;
import org.labkey.microarray.MicroarrayModule;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArrayPipelineManager
{
    protected static String _pipelineLogExt = ".log";
    protected static String _pipelineResultsExt = ".tgz";
    
    public static File getExtractionLog(File dirImages, String baseName)
    {
        if (null == baseName)
            return new File(dirImages, "Extraction" + _pipelineLogExt);
        return new File(dirImages, baseName + _pipelineLogExt);
    }
    
    public static File getResultsFile(File dir, String baseName)
    {
        return new File(dir, baseName + _pipelineResultsExt);
    }
    
    public static PipelineProvider.FileEntryFilter getImageFileFilter()
    {
        return new PipelineProvider.FileTypesEntryFilter(MicroarrayModule.TIFF_INPUT_TYPE.getFileType());
    }
    
    public static PipelineProvider.FileEntryFilter getMageFileFilter()
    {
        return new PipelineProvider.FileTypesEntryFilter(MicroarrayModule.MAGE_ML_INPUT_TYPE.getFileType());
    }
    
    public static File[] getImageFiles(File imageDir, FileStatus status, Container c) throws IOException
    {
        Map<File, FileStatus> imageFileStatus = getExtractionStatus(imageDir, c);
        List<File> fileList = new ArrayList<>();
        for (File imageFile : imageFileStatus.keySet())
        {
            if (status == null || status.equals(imageFileStatus.get(imageFile)))
                fileList.add(imageFile);
        }
        return fileList.toArray(new File[fileList.size()]);
    }
    
    public static Map<File, FileStatus> getExtractionStatus(File imageDir, Container c) throws IOException
    {
        Set<File> knownFiles = new HashSet<>();
        Set<File> checkedDirectories = new HashSet<>();
        
        File[] imageFiles = imageDir.listFiles(getImageFileFilter());

        Map<File, FileStatus> imageFileMap = new LinkedHashMap<>();
        if (imageFiles != null && imageFiles.length > 0)
        {
            Arrays.sort(imageFiles, (o1, o2) -> o1.getName().compareTo(o2.getName()));

            File logFile = getExtractionLog(imageDir, null);
            boolean logExists = exists(logFile, knownFiles, checkedDirectories);

            for (File file : imageFiles)
            {
                FileStatus status = FileStatus.UNKNOWN;
                if (logExists)
                {
                    // Check to see if images match what is being or has been processed by the pipeline.
                    PipelineStatusFile sf = PipelineService.get().getStatusFile(logFile);
                    if (null == sf || !sf.isActive())
                        status = FileStatus.COMPLETE;
                    else
                        status = FileStatus.RUNNING;
                    
                    try (BufferedReader logFileReader = Readers.getReader(logFile))
                    {
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
                }
                else
                {
                    imageFileMap.put(file, status);
                }
            }
        }
        return imageFileMap;
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
