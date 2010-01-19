/*
 * Copyright (c) 2008-2010 LabKey Corporation
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
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.labkey.api.microarray.FeatureExtractionClient;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.api.ExperimentService;
import com.ice.tar.TarInputStream;
import com.ice.tar.TarEntry;


public class FeatureExtractionPipelineJob extends PipelineJob
{
    protected Integer _extractionRowId;
    protected List<File> _imageFiles;
    protected String _protocol;
    private String _extractionEngine;

    public FeatureExtractionPipelineJob(ViewBackgroundInfo info,
                                        String protocol,
                                        List<File> imageFiles, String extractionEngine) throws SQLException
    {
        super(MicroarrayPipelineProvider.NAME, info);

        _protocol = protocol;
        _extractionEngine = extractionEngine;
        _imageFiles = imageFiles;
        if (_imageFiles.isEmpty())
        {
            throw new IllegalArgumentException("No image files to be processed");
        }
        File dirImages = getImagesDir();
        setLogFile(ArrayPipelineManager.getExtractionLog(dirImages, null));
        header("Feature extraction for folder " + dirImages.getAbsolutePath());
    }

    private File getImagesDir()
    {
        return _imageFiles.get(0).getParentFile();
    }

    public ActionURL getStatusHref()
    {
        if (_extractionRowId != null)
        {
            return PageFlowUtil.urlProvider(ExperimentUrls.class).getExperimentDetailsURL(getContainer(), ExperimentService.get().getExpExperiment(_extractionRowId.intValue()));
        }
        return null;
    }

    public String getDescription()
    {
        String dataName = "";
        File dirImages = getImagesDir();
        if (dirImages != null)
        {
            dataName = dirImages.getName();
            if ("xml".equals(dataName))
            {
                File dirData = dirImages.getParentFile();
                if (dirData != null)
                    dataName = dirData.getName();
            }
        }

        StringBuilder description = new StringBuilder("TIFF processing for ");
        description.append(dataName);
        if (_protocol != null)
        {
            description.append(" (").append(_protocol).append(")");
        }
        return description.toString();
    }

    public void run()
    {
        setStatus("RUNNING");

        boolean completeStatus = false;
        try
        {
            AppProps appProps = AppProps.getInstance();
            if ("agilent".equalsIgnoreCase(_extractionEngine) && appProps.getMicroarrayFeatureExtractionServer() == null)
                throw new IllegalArgumentException("Feature extraction server has not been specified in site customization.");

            File[] unprocessedFile = ArrayPipelineManager.getImageFiles(getImagesDir(), FileStatus.UNKNOWN, getContainer());
            List<File> imageFileList = new ArrayList<File>();
            imageFileList.addAll(Arrays.asList(unprocessedFile));
            imageFileList.retainAll(_imageFiles);
            if (imageFileList.isEmpty())
                throw new IllegalArgumentException("Feature extraction for this protocol is already complete.");

            info("Image files included in this extraction job:");
            for (File image : imageFileList)
            {
                info(image.getName());
            }

            int iReturn;
            FeatureExtractionClient extractionClient;
            // Here is where there is a specific reference to the Agilent FE Client Implementation
            if ("agilent".equalsIgnoreCase(_protocol))
            {
                extractionClient = new AgilentFeatureExtractionClientImpl(appProps.getMicroarrayFeatureExtractionServer(), getLogger());
            }
            else
            {
                // we take Agilent as the default case
                extractionClient = new AgilentFeatureExtractionClientImpl(appProps.getMicroarrayFeatureExtractionServer(), getLogger());
            }

            info("Initiating feature extraction process...");
            File resultsFile = extractionClient.run(imageFileList);

            if (!resultsFile.exists())
            {
                error("Failed running Feature Extraction process, results file was expected at " + resultsFile);
                return;
            }

            info("Extracting data from results file archive.");
            GZIPInputStream zIn = null;
            TarInputStream tIn = null;
            try
            {
                zIn = new GZIPInputStream(new FileInputStream(resultsFile));
                tIn = new TarInputStream(zIn);
                TarEntry entry;
                while((entry = tIn.getNextEntry()) != null)
                {
                    File extractedFile = new File(getImagesDir(), entry.getName());
                    if (entry.isDirectory())
                    {
                        extractedFile.mkdirs();
                    }
                    else
                    {
                        FileOutputStream fOut = null;
                        try
                        {
                            fOut = new FileOutputStream(extractedFile);
                            byte[] b = new byte[4096];
                            int count;
                            while ((count = tIn.read(b, 0, b.length)) >= 0)
                            {
                                fOut.write(b, 0, count);
                            }
                        }
                        finally
                        {
                            if (fOut != null) { try { fOut.close(); } catch (IOException e) {} }
                        }
                    }
                }
            }
            finally
            {
                if (tIn != null) { try { tIn.close(); } catch (IOException e) {} }
                if (zIn != null) { try { zIn.close(); } catch (IOException e) {} }
            }

            info("Deleting results archive '" + resultsFile.getAbsolutePath() + "'");
            if (!resultsFile.delete())
            {
                warn("Unable to delete results tgz file '" + resultsFile.getAbsolutePath() + "'.");
                warn("Delete this file manually to recover disk space.");
            }

//            As requested by Jon on 2/27/08, don't delete the original TIFF
//            for (File image : imageFiles)
//            {
//                boolean removed = image.delete();
//                if (!removed)
//                {
//                    warn("Unable to delete processed image file '" + image.getAbsolutePath() + "'.");
//                    warn("Delete this image manually to recover disk space.");
//                }
//            }

            iReturn = extractionClient.saveProcessedRuns(getUser(), getContainer(), new File(getImagesDir(), extractionClient.getTaskId()));

            if (iReturn != 0)
            {
                throw new IOException("Failed or partially failed saving processed runs to the database. Check log file for further details.");
            }
            setStatus(COMPLETE_STATUS);
            completeStatus = true;
        }
        catch (Exception e)
        {
            error("Feature Extraction processing exception", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(ERROR_STATUS);
            }
        }

    }

}
