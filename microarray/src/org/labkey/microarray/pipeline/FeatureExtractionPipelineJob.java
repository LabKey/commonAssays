package org.labkey.microarray.pipeline;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.labkey.api.microarray.FeatureExtractionClient;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.AppProps;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ActionURL;
import com.ice.tar.TarInputStream;
import com.ice.tar.TarEntry;


public class FeatureExtractionPipelineJob extends PipelineJob
{
    protected Integer _extractionRowId;
    protected File _dirImages;
    protected String _protocol;
    private final URI _uriData;
    private final String _extractionEngine;

    public FeatureExtractionPipelineJob(ViewBackgroundInfo info,
                                        String protocol,
                                        URI uriData, String extractionEngine) throws SQLException
    {
        super(MicroarrayPipelineProvider.NAME, info);

        _protocol = protocol;
        _uriData = uriData;
        _extractionEngine = extractionEngine;
        _dirImages = new File(uriData).getParentFile();
        setLogFile(ArrayPipelineManager.getExtractionLog(_dirImages, null), false);
        header("Feature extraction for folder " + _dirImages.getAbsolutePath());
    }

    public ActionURL getStatusHref()
    {
        if (_extractionRowId != null)
        {
            ActionURL ret = getActionURL().clone();
            ret.setPageFlow("Experiment");
            ret.setAction("details");
            ret.setExtraPath(getContainer().getPath());
            ret.deleteParameters();
            ret.addParameter("rowId", _extractionRowId.toString());
            return ret;
        }
        return null;
    }

    public String getDescription()
    {
        String baseName = null;
        String dataName = "";
        if (_dirImages != null)
        {
            dataName = _dirImages.getName();
            if ("xml".equals(dataName))
            {
                File dirData = _dirImages.getParentFile();
                if (dirData != null)
                    dataName = dirData.getName();
            }
        }

        StringBuilder description = new StringBuilder("TIFF processing for ");
        description.append(dataName);
        if (baseName != null)
        {
            if (description.length() > 0)
                description.append("/");
            description.append(baseName);
        }
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
            File dirData = new File(_uriData);
            if (!dirData.exists())
            {
                throw new FileNotFoundException("The specified data directory, " + dirData + ", does not exist.");
            }

            AppProps appProps = AppProps.getInstance();
            if ("agilent".equalsIgnoreCase(_extractionEngine) && appProps.getMicroarrayFeatureExtractionServer() == null)
                throw new IllegalArgumentException("Feature extraction server has not been specified in site customization.");

            File[] unprocessedFile = ArrayPipelineManager.getImageFiles(_uriData, _protocol, FileStatus.UNKNOWN, getContainer(), _extractionEngine);
            List<File> imageFileList = new ArrayList<File>();
            imageFileList.addAll(Arrays.asList(unprocessedFile));
            File[] imageFiles = imageFileList.toArray(new File[imageFileList.size()]);
            if (imageFiles.length == 0)
                throw new IllegalArgumentException("Feature extraction for this protocol is already complete.");

            info("Image files included in this extraction job:");
            for (File image : imageFiles)
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
            iReturn = extractionClient.run(imageFiles);

            File resultsFile = ArrayPipelineManager.getResultsFile(_dirImages, extractionClient.getTaskId());
            if (iReturn != 0 || !resultsFile.exists())
            {
                error("Failed running Feature Extraction process, error code " + iReturn);
                return;
            }

            if (resultsFile.exists())
            {
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
                        File extractedFile = new File(_dirImages, entry.getName());
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
            }

            if (!resultsFile.exists())
            {
                error("Failed to extract results archive. Results file does not exist.");
                return;
            }

            info("Deleting results archive '" + resultsFile.getAbsolutePath() + "'");
            if (!resultsFile.delete())
            {
                warn("Unable to delete results tgz file '" + resultsFile.getAbsolutePath() + "'.");
                warn("Delete this file manually to recover disk space.");
            }

            for (File image : imageFiles)
            {
                boolean removed = image.delete();
                if (!removed)
                {
                    warn("Unable to delete processed image file '" + image.getAbsolutePath() + "'.");
                    warn("Delete this image manually to recover disk space.");
                }
            }

            iReturn = extractionClient.saveProcessedRuns(getUser(), getContainer(), new File(_dirImages, extractionClient.getTaskId()));

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
