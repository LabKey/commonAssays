package org.labkey.ms2.pipeline.sequest;

import org.apache.commons.collections.list.TreeList;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.log4j.Logger;
import org.labkey.api.ms2.SearchClient;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Dec 13, 2006
 * Time: 4:35:42 PM
 */
public class SequestClientImpl implements SearchClient
{
    private static Logger _log = Logger.getLogger(SequestClientImpl.class);
    
    private Logger _instanceLogger = null;
    private String _url;
    private int errorCode = 0;
    private String errorString = "";
    private static volatile int _lastWorkingSet = 0;
    private static volatile String _lastWorkingUrl = "";
    private static volatile String _lastProvidedUrl = "";

    public SequestClientImpl(String url)
    {
        this(url, null);
    }

    public SequestClientImpl(String url, Logger instanceLogger)
    {
        _url = url;
        _instanceLogger = (instanceLogger == null ? _log : instanceLogger);
        errorCode = 0;
        errorString = "";
    }

    public int getErrorCode()
    {
        return errorCode;
    }

    public String getErrorString()
    {
        return errorString;
    }

    public boolean setProxyURL(String proxyURL)
    {
        return false;
    }

    public String testConnectivity()
    {
        // to test and report connectivity problem
        errorCode = 0;
        errorString = "";
        startSession();
        if (0 == errorCode)
        {
            return "";
        }
        else
        {
            return (("".equals(errorString)) ? "Fail to contact Sequest server at " + _url : errorString);
        }
    }
    public void  findWorkableSettings(boolean notUsed)
    {
      findWorkableSettings();
    }
    public void findWorkableSettings()
    {
        errorCode = 0;
        errorString = "";

        if (_lastWorkingSet>0)
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
                url = new URL("http://"+_url);
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
            String alternativeUrl = "/SequestQueue/";
            if (!alternativeUrl.equals(url.getPath()))
                possibleURLs.add(alternativeLinkPrefix + alternativeUrl);

            for (String testUrl : possibleURLs)
            {
                _url = testUrl;
                startSessionInternal();
                int attemptStatus = getErrorCode();
                String attemptMessage = getErrorString();

                errorCode = attemptStatus;
                errorString = attemptMessage;
                if (!(1 == attemptStatus || 2 == attemptStatus))
                {
                    if (0 == attemptStatus)
                    {
                        if (!url.equals(testUrl))
                            errorString = "Test passed ONLY when sequest server is set to " + alternativeLink.toString();

                        _lastWorkingSet = 2;
                        _lastWorkingUrl = testUrl;
                        _lastProvidedUrl = originalUrl;
                        break;
                    }
                    else
                    {
                        errorCode = attemptStatus;
                        errorString = "Sequest server responded on " + testUrl + " with \"" + attemptMessage + "\"";

                        _lastWorkingSet = 1;
                        _lastWorkingUrl = testUrl;
                        _lastProvidedUrl = originalUrl;
                    }
                }
            }
            if (_lastWorkingSet>0)
                _url = _lastWorkingUrl;
        }
        catch (MalformedURLException x)
        {
            _instanceLogger.error("connect("+_url +")", x);
            //Fail to parse Sequest Server URL
            errorCode = 1;
            errorString = "Fail to parse Sequest Server URL";
        }
    }

    public String startSession()
    {
        findWorkableSettings();

        if (0 == errorCode)
            return startSessionInternal();
        else
            return "";
    }

    private String startSessionInternal()
    {
        Properties results;

        errorCode = 0;
        errorString = "";
        Properties parameters = new Properties();
        results = request(parameters);
        if ("0".equals(results.getProperty("error","0")))
            return "";
        else
        {
            if (results.containsKey("error"))
                errorCode = Integer.parseInt(results.getProperty("error", "0"));
            return "";
        }
    }

    protected String getTaskID ()
    {
        errorCode = 0;
        errorString = "";
        Date date = new Date();
        return Long.toString(date.getTime());
    }

    protected String getTaskStatus (String taskId)
    {
        errorCode = 0;
        errorString = "";

        if ("".equals(taskId))
            return "";

        Properties parameters = new Properties();
        parameters.setProperty("cmd", "status");
        parameters.setProperty("taskId", taskId);
        Properties results = request (parameters);
        return results.getProperty("HTTPContent", "");
    }

    public Map<String, String[]> getSequenceDBNamesMap(String directory,Map<String, String[]> result)
    {
        errorCode = 0;
        errorString = "";
        Properties results;
        List<String> dbFilesList = new ArrayList<String>();

        findWorkableSettings();
        if (0 == errorCode )
            results = getDbNamesResults(directory);
        else
            results = new Properties();


        String dbsString = results.getProperty("HTTPContent", "");
        String[] contentLines = dbsString.split("\n");
        for (String contentLine : contentLines)
        {
            if (!"".equals(contentLine))
            {
                dbFilesList.add(contentLine);
            }
        }
        if(dbFilesList.size() == 0) return result;

        TreeList listNames = new TreeList();
        TreeList listSubDirs = new TreeList();
        for(String db:dbFilesList)
        {
            if(db.endsWith("/")) listSubDirs.add(db);
            else listNames.add(db);  
        }

        if(listNames.size() > 0)
            result.put(directory,(String[])listNames.toArray(new String[listNames.size()]));
        for(Object subDir:listSubDirs)
        {
            getSequenceDBNamesMap((String)subDir, result);
        }
        return result;
    }

    public int search(String databaseDir,
                      String sequestParamFile,
                      String mzXmlFile,
                      String resultFile,
                      Collection<String> mzXmlCommand)
    {
        errorCode = 0;
        errorString = "";

        _instanceLogger.info("Creating Sequest session...");
        startSession();
        if (0 != getErrorCode())
        {
            _instanceLogger.info("Failed to start Sequest session");
            return 2;
        }

        int returnCode = 0;
        final int delayAfterSubmitSec = 30;

        // get a TaskID to submit the job
        _instanceLogger.info("Creating Sequest search task...");
        String taskId = getTaskID();

        // submit job to sequest server
        _instanceLogger.info("Submitting search to Sequest server (taskId=" + taskId + ").");
        if (!submitFile(databaseDir, taskId, sequestParamFile, mzXmlFile, mzXmlCommand))
        {
            _instanceLogger.info("Failed to submit search to Sequest server.");
            _instanceLogger.info("Retreiving remote log file.");
            getLogFile( taskId);
            _instanceLogger.info("Finished retreiving remote log file.");
            returnCode = 3;
        }
        else
        {

            String prevSearchStatus = null;
            String searchStatus;
            int retryCount = 0;
            while(retryCount++ < 3)
            {
                if(retryCount > 1) _instanceLogger.warn("Trying to download results file again; try number "  + retryCount);
                while (true)
                {
                    try
                    {
                        Thread.sleep(delayAfterSubmitSec*1000);
                    }
                    catch (InterruptedException e) { }

                    searchStatus = getTaskStatus(taskId);
                    if (null == prevSearchStatus || !searchStatus.equals(prevSearchStatus))
                        _instanceLogger.info("Sequest search status: " + searchStatus);
                    prevSearchStatus = searchStatus;
                    if (!searchStatus.toLowerCase().contains("waiting") &&
                        !searchStatus.toLowerCase().contains("searching"))
                    {
                        break;
                    }
                }
                if (!searchStatus.toLowerCase().contains("complete"))
                {
                    _instanceLogger.info("Bad status returned '" + searchStatus + "'.");
                    _instanceLogger.info("Retreiving remote log file.");
                    getLogFile( taskId);
                    _instanceLogger.info("Finished retreiving remote log file.");
                    returnCode = 4;
                }
                else
                {
                    _instanceLogger.info("Retrieving Sequest search result...");
                    if (getResultFile( taskId, resultFile))
                    {
                        _instanceLogger.info("Sequest search result retrieved.");
                        _instanceLogger.info("Retreiving remote log file.");
                        getLogFile( taskId);
                        _instanceLogger.info("Finished retreiving remote log file.");
                        _instanceLogger.info("Cleaning search file from remote sequest server.");
                        clean(taskId);
                        break;
                    }
                    else
                    {
                        _instanceLogger.info("Retreiving remote log file.");
                        getLogFile( taskId);
                        _instanceLogger.info("Finished retreiving remote log file.");
                        returnCode = 5;
                    }
                }
            }
        }
        _instanceLogger.info("Sequest session ended.");

        return returnCode;
    }

    protected boolean submitFile(String databaseDir, String taskId, String seqParamPath, String mzXmlPath,Collection<String> mzXmlCommand)
    {
        errorCode = 0;
        errorString = "";

        if ("".equals(databaseDir) ||
             "".equals(taskId) ||
             "".equals(seqParamPath) ||
             "".equals(mzXmlPath))
            return false;

        int partCount = 4 + mzXmlCommand.size();
        int count = 0;
        Part[] parts = new Part[partCount];

        parts[count++] = new StringPart("taskId",taskId);
        parts[count++] = new StringPart("databaseDir", databaseDir);

        File mzXmlFile = new File(mzXmlPath);
        try
        {
            parts[count++] = new FilePart("mzXML",mzXmlFile);
        }
        catch (FileNotFoundException err)
        {
            _instanceLogger.error("Cannot find the  file '" + mzXmlFile.getPath () + "'.\n");
            return false;
        }

        File seqParamFile = new File(seqParamPath);
        try
        {
            parts[count++] = new FilePart("seqParams",seqParamFile);
        }
        catch (FileNotFoundException err)
        {
            _instanceLogger.error("Cannot find the  file '" + seqParamFile.getPath () + "'.\n");
            return false;
        }

        for(String command :mzXmlCommand)
        {
            parts[count++] = new StringPart(command.substring(1,2),command.substring(2));
        }

        StringBuffer urlSB = new StringBuffer(_url);
        if (!_url.endsWith("/"))
            urlSB.append("/");
        urlSB.append("SequestQueue");
        String sequestRequestURL = urlSB.toString();
        _instanceLogger.info("Search URL ='" + sequestRequestURL);
        PostMethod post = new PostMethod(sequestRequestURL);
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()) );
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
                _instanceLogger.error("Failed to submit Sequest query '" + sequestRequestURL + "' for " +
                        mzXmlFile.getPath() + " with parameters " + seqParamFile.getPath () + " on attempt#" +
                        Integer.toString(attempt+1) + ".\n", err);
                attempt = maxAttempt;
            }
            attempt++;
        }
        // Check that we didn't run out of retries.
        if (statusCode == -1) {
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
                _instanceLogger.info ("Sequest search task status: query upload completed");
            }
            else
            {
                _instanceLogger.info(
                        "Sequest search task status: query upload failed with this error: " + sb.toString());
            }
            in.close();
        }
        catch (IOException err)
        {
            _instanceLogger.error("Failed to get response from Sequest query '" + sequestRequestURL + "' for " +
                    mzXmlFile.getPath() + " with parameters " + seqParamFile.getPath () + " on attempt#" +
                    Integer.toString(attempt+1) + ".\n",err);
        }
        finally
        {
            post.releaseConnection();
        }

        return uploadFinished;
    }

    protected boolean getResultFile (String taskId, String resultFile)
    {
        errorCode = 0;
        errorString = "";
        if ("".equals(taskId) || "".equals(resultFile))
            return false;

        Properties parameters = new Properties();
        parameters.setProperty("cmd", "retrieve");
        parameters.setProperty("fileType","result");
        parameters.setProperty("taskId", taskId);
        InputStream in = getRequestResultStream (parameters);
        if (null == in)
            return false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        boolean ioError = true;
        File outFile = new File(resultFile);
        BufferedWriter writer = null;
        try
        {
            FileOutputStream out = new FileOutputStream(outFile);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            String line;
            boolean isFirstLine = true;
            String lastLine = null;
            while ((line = reader.readLine()) != null)
            {
                if(isFirstLine)isFirstLine = false;
                else writer.newLine();
                if(line.startsWith("</BODY></HTML>"))
                {
                    ioError = false;
                    break;
                }
                writer.write(line);
                lastLine = line;
            }
            if(ioError)
            {
                _instanceLogger.error("getResultFile(result="+resultFile+",taskid="+taskId+")." +
                        " Incomplete download.Expected: </BODY></HTML> Actual: " + lastLine );
            }
        }
        catch (FileNotFoundException e)
        {
            // output file cannot be created!
            ioError = true;
            _instanceLogger.error("getResultFile(result="+resultFile+",taskid="+taskId+")", e);
        }
        catch (IOException e)
        {
            // a read or write error occurred
            ioError = true;
            _instanceLogger.error("getResultFile(result="+resultFile+",taskid="+taskId+")", e);
        }
        finally
        {
            try
            {
                if(reader != null) reader.close();
                if (null != writer) writer.close();
            }
            catch (IOException e)
            { }
        }

        if (ioError)
            return false;

        // let's check that we have the right file
        BufferedReader resultStream = null;
        String firstLine = "";
        String secondLine = "";
        try
        {
            resultStream = new BufferedReader(new InputStreamReader(new FileInputStream(outFile)));
            firstLine = resultStream.readLine();
        }
        catch (FileNotFoundException e)
        {
        }
        catch (IOException e)
        {
        }
        finally
        {
            try { if (null != resultStream) resultStream.close(); } catch (IOException e) {}
        }

        if (!firstLine.startsWith("<HTML>") && secondLine.startsWith("<HEAD><TITLE>HTML-SUMMARY</TITLE></HEAD>"))
        {
            outFile.delete();
            return false;
        }
        else
        {
            return true;
        }
    }

     protected boolean getLogFile (String taskId)
    {
        errorCode = 0;
        errorString = "";
        //sessionID is optional
        if ("".equals(taskId))
            return false;

        Properties parameters = new Properties();
        parameters.setProperty("cmd", "retrieve");
        parameters.setProperty("fileType","log");
        parameters.setProperty("taskId", taskId);

        // Open a stream to the file using the URL.
        InputStream in = getRequestResultStream (parameters);
        if (null == in)
            return false;
        BufferedReader dis =
               new BufferedReader (new InputStreamReader (in));
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
            ioError = true;
            _instanceLogger.error("getLogFile(taskid=" + taskId + ")", e);
        }
        finally
        {
            try { in.close(); } catch (IOException e) { }
        }

        if (ioError)
            return false;
        return true;
    }

    protected String clean(String taskId)
    {
        errorCode = 0;
        errorString = "";

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
            if (results.contains("error") && !"0".equals(results.getProperty("error","-1")))
            {
                // fall thru', return the full HTTP Content as we need the full text for diagnosis
                _instanceLogger.info ("Sequest search task status error: (" + results.getProperty("error","-1") + ") " +
                    results.getProperty("errorstring",""));
            }
            else
                statusString = results.getProperty("running", "");
        }

        return statusString;

    }

    public String getDbNames()
        {
            // retrieve the list of databases from SequestServer
            Properties results = getDbNamesResults("");
            return results.getProperty("HTTPContent", "");
        }


    private Properties getDbNamesResults(String directory)
    {
        // retrieve the list of databases from SequestServer
        Properties parameters = new Properties();
        parameters.setProperty("cmd", "listDatabases");
        parameters.setProperty("dir", directory);
        return request(parameters);
    }

    public String getEnvironmentConf()
    {
        // retrieve the the configuation of SequestServer
        Properties parameters = new Properties();
        parameters.setProperty("cmd", "admin");
        String results = request(parameters).getProperty("HTTPContent", getErrorString());

        if(!results.contains("----DATABASE DIRECTORY----") && getErrorCode()== 0)
        {
            errorCode = 3;
            return "Failed to interact with SequestQueue application: Unexpected content returned.";
        }
        return request(parameters).getProperty("HTTPContent", getErrorString());
    }    

    private String requestURL (Properties parameters)
    {
        StringBuffer requestURLLSB = new StringBuffer(_url);
        if(parameters.size() > 0)
            requestURLLSB.append("/SequestQueue?");
        boolean firstEntry=true;
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
            try {
                requestURLLSB.append(URLEncoder.encode(s, "UTF-8"));
            } catch (UnsupportedEncodingException x) {
                requestURLLSB.append(s);
            }
            String val = parameters.getProperty(s);
                requestURLLSB.append("=");
                try {
                    requestURLLSB.append(URLEncoder.encode(val, "UTF-8"));
                } catch (UnsupportedEncodingException x) {
                    requestURLLSB.append(val);
                }
        }
        return requestURLLSB.toString();
    }

    private Properties request(Properties parameters)
    {
        // connect to the Sequest Server to send request
        // report the results as a property set, i.e. key=value pairs

        Properties results = new Properties();
        String sequestRequestURL = requestURL(parameters);
        _instanceLogger.debug("Submitting URL '" + sequestRequestURL + "'.");
        try
        {
            URL sequestURL = new URL(sequestRequestURL);

            BufferedReader in = new BufferedReader(new InputStreamReader(sequestURL.openStream()));
            String str;
            StringBuffer reply = new StringBuffer();
            while ((str = in.readLine()) != null) {
                reply.append (str);
                reply.append ("\n");
            }
            results.setProperty("HTTPContent", reply.toString());
            in.close();
        }
        catch (MalformedURLException x)
        {
            // If using the class logger, then assume user interface will deliver the error message.
            String msg = "Exception "+x.getClass()+" connect("+ _url + ")=" + sequestRequestURL;
            if (_instanceLogger == _log)
                _instanceLogger.debug(msg);
            else
                _instanceLogger.error(msg);
            errorCode = 1;
            errorString = "Fail to parse Sequest Server URL: " + x.getMessage();
            results.setProperty("error", "1");
            results.setProperty("errorstring", errorString);
        }
        catch (Exception x)
        {
            // If using the class logger, then assume user interface will deliver the error message.
            String msg = "Exception "+x.getClass()+" connect("+_url+"," + ")=" + sequestRequestURL;
            if (_instanceLogger == _log)
                _instanceLogger.debug(msg);
            else
                _instanceLogger.error(msg);
            errorCode = 2;
            errorString = "Failed to interact with SequestQueue application: " + x.getMessage();
            results.setProperty("error", "2");
            results.setProperty("errorstring", errorString);
        }
        return results;
    }

    private InputStream getRequestResultStream (Properties parameters)
    {
        // connect to the Sequest Server to send request
        // return the reply as a stream

        String sequestRequestURL = requestURL(parameters);
        try
        {
            URL sequestURL = new URL(sequestRequestURL);
            return sequestURL.openStream();
        }
        catch (MalformedURLException x)
        {
            _instanceLogger.error("Exception "+x.getClass()+" connect("+_url + ")=" + sequestRequestURL, x);
            errorCode = 1;
        }
        catch (Exception x)
        {
            _instanceLogger.error("Exception "+x.getClass()+" on connect("+_url + "," + sequestRequestURL, x);
            errorCode = 2;
        }
        return null;
    }
}
