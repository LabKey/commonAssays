package org.labkey.microarray.pipeline;

import org.apache.log4j.Logger;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpClient;
import org.labkey.api.microarray.FeatureExtractionClient;
import org.labkey.api.microarray.ExtractionException;
import org.labkey.api.microarray.ExtractionConfigException;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.exp.api.ExpData;
import org.labkey.microarray.MicroarrayModule;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;

import javax.xml.xpath.*;
import javax.xml.soap.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.io.*;


public class AgilentFeatureExtractionClientImpl implements FeatureExtractionClient
{
    private Logger _instanceLogger = null;
    private String _url;
    private String _taskId;
    private static volatile int _lastWorkingSet = 0;
    private static volatile String _lastWorkingUrl = "";
    private static volatile String _lastProvidedUrl = "";

    public AgilentFeatureExtractionClientImpl(String url)
    {
        this(url, Logger.getLogger("null"));
    }

    public AgilentFeatureExtractionClientImpl(String url, Logger instanceLogger)
    {
        _url = url;
        if (instanceLogger == null)
        {
            _instanceLogger = Logger.getLogger(AgilentFeatureExtractionClientImpl.class);
        }
        else
        {
            _instanceLogger = instanceLogger;
        }
        _instanceLogger = instanceLogger;

        // initalize a TaskID to submit the job for this client
        _instanceLogger.info("Creating FeatureExtraction client taskId...");
        _taskId = Long.toString(new Date().getTime());
    }

    public boolean setProxyURL(String proxyURL)
    {
        return false;
    }

    public void testConnectivity() throws ExtractionException
    {
        // to test and report connectivity problem
        startSession();
    }

    public void findWorkableSettings() throws ExtractionConfigException
    {
        if (_lastWorkingSet > 0)
        {
            if (_lastProvidedUrl.equals(_url))
            {
                _url = _lastWorkingUrl;
                return;
            }

            _lastWorkingSet = 0;
        }

        // we have to figure out which is the workable settings from what are given
        _lastWorkingUrl = "";
        String originalUrl = _url;
        try
        {
            URL url;
            if (!_url.startsWith("http://"))
                url = new URL("http://" + _url);
            else
                url = new URL(_url);


            List<String> possibleURLs = new ArrayList<String>();
            // user provided a http://host/path, we shall test this first
            if (!"".equals(url.getPath()))
                possibleURLs.add(_url);

            StringBuffer alternativeLink;
            alternativeLink = new StringBuffer("http://");
            alternativeLink.append(url.getHost());
            if (80 != url.getPort() && -1 != url.getPort())
            {
                alternativeLink.append(":");
                alternativeLink.append(url.getPort());
            }
            String alternativeLinkPrefix = alternativeLink.toString();
            String alternativeUrl = "/FeatureExtractionQueue/";
            if (!alternativeUrl.equals(url.getPath()))
                possibleURLs.add(alternativeLinkPrefix + alternativeUrl);

            for (String testUrl : possibleURLs)
            {
                _url = testUrl;
                try
                {
                    startSessionInternal();

                    _lastWorkingSet = 2;
                    _lastWorkingUrl = testUrl;
                    _lastProvidedUrl = originalUrl;

                    if (!testUrl.equals(originalUrl))
                    {
                        throw new ExtractionConfigException("Test passed ONLY when feature extraction server is set to " + alternativeLink);
                    }

                    break;
                }
                catch (ExtractionException e)
                {
                    
                }
            }
            if (_lastWorkingSet > 0)
                _url = _lastWorkingUrl;
        }
        catch (MalformedURLException x)
        {
            throw new ExtractionConfigException("Failed to parse FeatureExtraction Server URL" + _url, x);
        }
    }

    public void startSession() throws ExtractionException
    {
        findWorkableSettings();
        startSessionInternal();
    }

    private void startSessionInternal() throws ExtractionException
    {
        Properties parameters = new Properties();
        request(parameters);
    }

    public String getTaskId()
    {
        return _taskId;
    }

    protected String getTaskStatus(String taskId) throws ExtractionException
    {
        if ("".equals(taskId))
            return "";

        Properties parameters = new Properties();
        parameters.setProperty("cmd", "status");
        parameters.setProperty("taskId", taskId);
        Properties results = request(parameters);
        return results.getProperty("HTTPContent", "");
    }

    public int run(File[] imageFiles) throws ExtractionException
    {
        _taskId = "1199843396163";
        return 0;
/*        _instanceLogger.info("Creating FeatureExtraction session...");
        startSession();

        int returnCode = 0;
        final int delayAfterSubmitSec = 30;

        // submit job to feature extraction server
        _instanceLogger.info("Submitting job to FeatureExtraction server (taskId=" + _taskId + ").");
        if (!submitFiles(_taskId, imageFiles))
        {
            _instanceLogger.info("Failed to submit job to Feature Extraction server.");
            _instanceLogger.info("Retrieving remote log file.");
            getLogFile(_taskId);
            _instanceLogger.info("Finished retrieving remote log file.");
            returnCode = 3;
        }
        else
        {
            String prevExtractionStatus = null;
            String extractionStatus;
            File resultsFile = ArrayPipelineManager.getResultsFile(imageFiles[0].getParentFile(), _taskId);
            int retryCount = 0;
            while (retryCount++ < 3)
            {
                if (retryCount > 1)
                    _instanceLogger.warn("Trying to download results file again; try number " + retryCount);
                while (true)
                {
                    try
                    {
                        Thread.sleep(delayAfterSubmitSec * 1000);
                    }
                    catch (InterruptedException e)
                    {
                    }

                    extractionStatus = getTaskStatus(_taskId);
                    if (null == prevExtractionStatus || !extractionStatus.equals(prevExtractionStatus))
                        _instanceLogger.info("Feature Extraction job status: " + extractionStatus);
                    prevExtractionStatus = extractionStatus;
                    if (!extractionStatus.toLowerCase().contains("waiting") &&
                            !extractionStatus.toLowerCase().contains("extracting"))
                    {
                        break;
                    }
                }
                if (!extractionStatus.toLowerCase().contains("complete"))
                {
                    _instanceLogger.info("Bad status returned '" + extractionStatus + "'.");
                    _instanceLogger.info("Retrieving remote log file.");
                    getLogFile(_taskId);
                    _instanceLogger.info("Finished retrieving remote log file.");
                    returnCode = 4;
                }
                else
                {
                    _instanceLogger.info("Retrieving FeatureExtraction job result...");
                    try
                    {
                        getResultFile(_taskId, resultsFile);
                        _instanceLogger.info("FeatureExtraction job results retrieved.");
                        _instanceLogger.info("Retrieving remote log file.");
                        getLogFile(_taskId);
                        _instanceLogger.info("Finished retrieving remote log file.");
                        _instanceLogger.info("Cleaning extraction files from remote feature extraction server.");
                        try
                        {
                            clean(_taskId);
                        }
                        // Remove this catch for production
                        catch (ExtractionException e2) {}
                        break;
                    }
                    catch (ExtractionException e)
                    {
                        _instanceLogger.info("Attempt to retrieve results file failed.", e);
                        _instanceLogger.info("Retreiving remote log file.");
                        try
                        {
                            getLogFile(_taskId);
                        }
                        // Remove this catch for production
                        catch (ExtractionException e2) {}
                        _instanceLogger.info("Finished retrieving remote log file.");
                        returnCode = 5;
                    }
                }
            }
        }
        _instanceLogger.info("Feature Extraction session ended.");
        return returnCode;
*/
    }

    protected boolean submitFiles(String taskId, File[] imageFiles)
    {
        if ("".equals(taskId) || null == imageFiles || imageFiles.length == 0)
            return false;

        int partCount = 1 + imageFiles.length;
        int count = 0;
        Part[] parts = new Part[partCount];

        parts[count++] = new StringPart("taskId", taskId);

        for (File image : imageFiles)
        {
            try
            {
                parts[count++] = new FilePart("imageFile", image);
            }
            catch (FileNotFoundException err)
            {
                _instanceLogger.error("Cannot find the  image file '" + image.getPath() + "'.\n");
                return false;
            }
        }

        StringBuffer urlSB = new StringBuffer(_url);
        if (!_url.endsWith("/"))
            urlSB.append("/");
        urlSB.append("FeatureExtractionQueue");
        String feRequestURL = urlSB.toString();
        _instanceLogger.info("Feature Extraction Service URL ='" + feRequestURL);
        PostMethod post = new PostMethod(feRequestURL);
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
        HttpClient client = new HttpClient();

        int statusCode = -1;
        int attempt = 0;
        // We will retry up to 3 times.
        final int maxAttempt = 3;
        while (statusCode == -1 && attempt < maxAttempt)
        {
            try
            {
                statusCode = client.executeMethod(post);
            }
            catch (IOException err)
            {
                _instanceLogger.error("Failed to submit FeatureExtraction query '" + feRequestURL + " on attempt#" +
                        Integer.toString(attempt + 1) + ".\n", err);
                attempt = maxAttempt;
            }
            attempt++;
        }
        // Check that we didn't run out of retries.
        if (statusCode == -1)
        {
            post.releaseConnection();
            return false;
        }

        boolean uploadFinished = false;
        try
        {
            // handle response.
            final String endOfUploadMarker = "success";
            BufferedReader in = new BufferedReader(new InputStreamReader(post.getResponseBodyAsStream()));
            StringBuffer sb = new StringBuffer();
            String str;
            while ((str = in.readLine()) != null)
            {
                sb.append(str);
            }
            if (sb.indexOf(endOfUploadMarker) > -1)
            {
                uploadFinished = true;
                _instanceLogger.info("Feature Extraction service task status: query upload completed");
            }
            else
            {
                _instanceLogger.info(
                        "Feature Extraction service task status: query upload failed with this error: " + sb.toString());
            }
            in.close();
        }
        catch (IOException err)
        {
            _instanceLogger.error("Failed to get response from Feature Extraction query '" + feRequestURL + "' for jobId" +
                    taskId + " on attempt#" + Integer.toString(attempt + 1) + ".\n", err);
        }
        finally
        {
            post.releaseConnection();
        }

        return uploadFinished;
    }

    protected void getResultFile(String taskId, File resultsFile) throws ExtractionException
    {
        if ("".equals(taskId))
            return;

        Properties parameters = new Properties();
        parameters.setProperty("cmd", "retrieve");
        parameters.setProperty("fileType", "results");
        parameters.setProperty("taskId", taskId);
        InputStream in = getRequestResultStream(parameters);

        BufferedInputStream reader = new BufferedInputStream(in);
        FileOutputStream out = null;

        try
        {
            out = new FileOutputStream(resultsFile);
            byte[] buf = new byte[4 * 1024];  // 4K buffer
            int bytesRead;
            while ((bytesRead = reader.read(buf)) != -1)
            {
                out.write(buf, 0, bytesRead);
            }
            in.close();
            reader.close();
            out.close();
        }
        catch (IOException e)
        {
            // a read or write error occurred
            throw new ExtractionException(e);
        }
        finally
        {
            try
            {
                if (in != null) in.close();
                if (reader != null) reader.close();
                if (out != null) out.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    protected void getLogFile(String taskId) throws ExtractionException
    {
        //sessionID is optional
        if ("".equals(taskId))
            return;

        Properties parameters = new Properties();
        parameters.setProperty("cmd", "retrieve");
        parameters.setProperty("fileType", "log");
        parameters.setProperty("taskId", taskId);

        // Open a stream to the file using the URL.
        InputStream in = getRequestResultStream(parameters);

        BufferedReader dis =
                new BufferedReader(new InputStreamReader(in));
        boolean ioError = false;
        try
        {
            String line;
            while ((line = dis.readLine()) != null)
            {
                _instanceLogger.info(line);
            }
        }
        catch (IOException e)
        {
            // a read or write error occurred
            throw new ExtractionException(e);
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    public int saveProcessedRuns(User u, Container c, File outputDir)
    {
        File[] mageFiles = outputDir.listFiles(ArrayPipelineManager.getMageFileFilter());
        File[] featureFiles = outputDir.listFiles(ArrayPipelineManager.getFeatureFileFilter());
        File[] alignmentFiles = outputDir.listFiles(ArrayPipelineManager.getAlignmentFileFilter());
        int runs = 0;
        int retVal = 0;

        for (File mage : mageFiles)
        {
            try
            {
                ExpData data = AbstractAssayProvider.createData(c, mage, MicroarrayModule.MAGE_ML_DATA_TYPE);
                data.save(u);

//                FeatureExtractionRun feRun = new FeatureExtractionRun();

//                Node descriptionParentNode = (Node) xPathDescriptionNode.evaluate(input, XPathConstants.NODE);
//                Node softwareApplicationParentNode = (Node) xPathSoftwareApplicationsNode.evaluate(input, XPathConstants.NODE);

                String fileBase = mage.getAbsolutePath().substring(0,
                        mage.getAbsolutePath().lastIndexOf(ArrayPipelineManager.MAGE_EXTENSION));
                File loResImage = new File(fileBase + ArrayPipelineManager._pipelineLoResImageExt);
                File qcReport = new File(fileBase + ArrayPipelineManager._pipelineQCReportExt);
                File feature = featureFiles[0];
                File alignment = alignmentFiles[0];

//                feRun.setDescription(description);
//                feRun.setProtocol(protocol);
//                feRun.setGrid(grid);
//                feRun.setBarcode(barcode);
//                feRun.setStatusId(1);
//
//                if (null != mage && mage.exists())
//                    feRun.setPath(mage.getParent());
//                feRun.setMageML(mage.getName());
//                if (null != loResImage && loResImage.exists())
//                    feRun.setLowResImage(loResImage.getName());
//                if (null != qcReport && qcReport.exists())
//                    feRun.setQcReport(qcReport.getName());
//                if (null != feature && feature.exists())
//                    feRun.setFeature(feature.getName());
//                if (null != alignment && alignment.exists())
//                    feRun.setAlignment(alignment.getName());
//
//                MicroarrayManager.get().saveRun(u, null);
                
                runs++;
            }
            catch (Exception e)
            {
                _instanceLogger.error("Unable to save extraction set associated with the following MAGE file: " + mage.getAbsolutePath());
                _instanceLogger.error("Check to ensure all associated resources are present before attempting to load this resource through the pipeline.", e);
                retVal = 1;
            }
        }

        _instanceLogger.info("Successfully processed " + runs + " extraction sets for this run.");
        return retVal;
    }

    protected String clean(String taskId) throws ExtractionException
    {
        if ("".equals(taskId))
            return "";

        Properties parameters = new Properties();
        parameters.setProperty("cmd", "clean");
        parameters.setProperty("taskId", taskId);
        Properties results = request(parameters);
        String statusString = results.getProperty("HTTPContent", "");
        if (statusString.contains("="))
        {
            results.remove("HTTPContent");
            String[] contentLines = statusString.split("\n");
            for (String contentLine : contentLines)
            {
                if (contentLine.contains("="))
                {
                    String[] parts = contentLine.split("=");
                    if (2 == parts.length)
                        if (!"".equals(parts[0]))
                            results.put(parts[0], parts[1]);
                }
            }
            if (results.contains("error") && !"0".equals(results.getProperty("error", "-1")))
            {
                // fall thru', return the full HTTP Content as we need the full text for diagnosis
                _instanceLogger.info("Feature Extraction search task status error: (" + results.getProperty("error", "-1") + ") " +
                        results.getProperty("errorstring", ""));
            }
            else
                statusString = results.getProperty("running", "");
        }

        return statusString;
    }


    public String getEnvironmentConf() throws ExtractionException
    {
        // retrieve the the configuation of FeatureExtractionServer
        Properties parameters = new Properties();
        parameters.setProperty("cmd", "admin");
        String results = request(parameters).getProperty("HTTPContent");

        if (!results.contains("----FEATURE EXTRACTION EXECUTABLE----"))
        {
            throw new ExtractionException("Failed to interact with FeatureExtractionQueue application: Unexpected content returned.");
        }
        return results;
    }

    private String requestURL(Properties parameters)
    {
        StringBuffer requestURLLSB = new StringBuffer(_url);
        if (parameters.size() > 0)
            requestURLLSB.append("/FeatureExtractionQueue?");
        boolean firstEntry = true;
        for (Enumeration e = parameters.propertyNames(); e.hasMoreElements();)
        {
            String s = (String) e.nextElement();
            if (firstEntry)
            {
                firstEntry = false;
            }
            else
            {
                requestURLLSB.append("&");
            }
            try
            {
                requestURLLSB.append(URLEncoder.encode(s, "UTF-8"));
            }
            catch (UnsupportedEncodingException x)
            {
                requestURLLSB.append(s);
            }
            String val = parameters.getProperty(s);
            requestURLLSB.append("=");
            try
            {
                requestURLLSB.append(URLEncoder.encode(val, "UTF-8"));
            }
            catch (UnsupportedEncodingException x)
            {
                requestURLLSB.append(val);
            }
        }
        return requestURLLSB.toString();
    }

    private Properties request(Properties parameters) throws ExtractionException
    {
        // connect to the FeatureExtraction Server to send request
        // report the results as a property set, i.e. key=value pairs

        Properties results = new Properties();
        String feRequestURL = requestURL(parameters);
        _instanceLogger.info("Submitting URL '" + feRequestURL + "'.");
        try
        {
            URL feURL = new URL(feRequestURL);

            BufferedReader in = new BufferedReader(new InputStreamReader(feURL.openStream()));
            String str;
            StringBuffer reply = new StringBuffer();
            while ((str = in.readLine()) != null)
            {
                reply.append(str);
                reply.append("\n");
            }
            results.setProperty("HTTPContent", reply.toString());
            in.close();
        }
        catch (MalformedURLException x)
        {
            throw new ExtractionConfigException(x);
        }
        catch (Exception x)
        {
            throw new ExtractionException(x);
        }
        return results;
    }

    private InputStream getRequestResultStream(Properties parameters) throws ExtractionException
    {
        // connect to the FeatureExtraction Server to send request
        // return the reply as a stream

        String feRequestURL = requestURL(parameters);
        try
        {
            URL feURL = new URL(feRequestURL);
            return feURL.openStream();
        }
        catch (MalformedURLException x)
        {
            throw new ExtractionConfigException("Failed for URL" + feRequestURL, x);
        }
        catch (Exception x)
        {
            throw new ExtractionException("Failed for URL " + feRequestURL, x);
        }
    }
}
