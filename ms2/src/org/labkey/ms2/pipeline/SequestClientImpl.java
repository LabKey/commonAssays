package org.labkey.ms2.pipeline;

import org.apache.log4j.Logger;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpClient;
import org.labkey.api.ms2.SearchClient;

import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.io.*;

/**
 * User: billnelson@uky.edu
 * Date: Dec 13, 2006
 * Time: 4:35:42 PM
 */
public class SequestClientImpl implements SearchClient {
    private static Logger _log = Logger.getLogger(SequestClientImpl.class);
    private Logger _instanceLogger = null;
    private String _url;
    private String _userAccount = "";
    private String _userPassword = "";
    private String _proxyURL = "";
    private int errorCode = 0;
    private String errorString = "";

    private static volatile int _lastWorkingSet = 0;
    private static volatile String _lastWorkingUrl = "";
    private static volatile String _lastProvidedUrl = "";
    private static volatile String _lastProvidedUserAccount = "";
    private static volatile String _lastProvidedUserPassword = "";
    private static volatile String _lastProvidedProxy = "";

    public SequestClientImpl(String url, Logger instanceLogger)
    {
        _url = url;
        _instanceLogger = instanceLogger;
        _userAccount = "";
        _userPassword = "";
        errorCode = 0;
        errorString = "";
    }

    public int getErrorCode ()
    {
        return errorCode;
    }

    public String getErrorString ()
    {
        return errorString;
    }

    public boolean setProxyURL (String proxyURL)
    {
        return false;
    }

    public boolean requireAuthentication ()
    {
        //GET /cgi/login.pl?display=nothing&onerrdisplay=nothing&action=issecuritydisabled
        errorCode = 0;
        errorString = "";
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "login.pl");
        parameters.setProperty("display", "nothing");
        parameters.setProperty("onerrdisplay", "nothing");
        parameters.setProperty("action", "issecuritydisabled");
        Properties results = request (parameters, true);
        return (results.getProperty("error","0").equals("0"));
    }

    public String testConnectivity (boolean useAuthentication)
    {
        // to test and report connectivity problem
        errorCode = 0;
        errorString = "";
        String sessionId = startSession();
        if (0 == errorCode)
        {
            // no error, terminate session
            endSession (sessionId);
            return "";
        }
        else if (!useAuthentication && -3 == errorCode)
        {
            return "";
        }
        else
        {
            return (("".equals(errorString)) ? "Fail to contact Sequest server at " + _url : errorString);
        }
    }

    public void findWorkableSettings (boolean useAuthentication)
    {
        errorCode = 0;
        errorString = "";

        if (_lastWorkingSet>0)
        {
            // TODO: check that we can re-use the workable setting
            if (_lastProvidedUrl.equals(_url)
                && _lastProvidedProxy.equals(_proxyURL))
            {
                if (!useAuthentication)
                {
                    _url = _lastWorkingUrl;
                    return;
                }
                else if (2 == _lastWorkingSet
                    && _lastProvidedUserAccount.equals(_userAccount)
                    && _lastProvidedUserPassword.equals(_userPassword))
                {
                    _url = _lastWorkingUrl;
                    return;
                }
            }

            _lastWorkingSet = 0;
        }

        // we have to figure out which is the workable settings from what are given
        _lastWorkingUrl = "";
        _lastProvidedUserAccount = "";
        _lastProvidedUserPassword = "";
        _lastProvidedProxy = "";

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

            StringBuffer alternativeLink = new StringBuffer();
            alternativeLink = new StringBuffer("http://");
            alternativeLink.append(url.getHost());
            if (80 != url.getPort() && -1 != url.getPort())
            {
                alternativeLink.append(url.getPort());
            }
            String alternativeLinkPrefix = alternativeLink.toString();
            String alternativeUrl = "/sequest/";
            if (!alternativeUrl.equals(url.getPath()))
                possibleURLs.add(alternativeLinkPrefix + alternativeUrl);

            for (String testUrl : possibleURLs)
            {
                _url = testUrl;
                String sessionId = startSessionInternal();
                int attemptStatus = getErrorCode();
                String attemptMessage = getErrorString();
                if (!"".equals(sessionId))
                {
                    endSession(sessionId);
                }

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
                        _lastProvidedUserAccount = _userAccount;
                        _lastProvidedUserPassword = _userPassword;
                        _lastProvidedProxy = _proxyURL;

                        break;
                    }
                    else
                    {
                        errorCode = attemptStatus;
                        errorString = "Sequest server responded on " + testUrl + " with \"" + attemptMessage + "\"";

                        _lastWorkingSet = 1;
                        _lastWorkingUrl = testUrl;
                        _lastProvidedUrl = originalUrl;
                        _lastProvidedProxy = _proxyURL;

                        if (!useAuthentication) break;
                    }
                }
            }
            if (_lastWorkingSet>0)
                _url = _lastWorkingUrl;
        }
        catch (MalformedURLException x)
        {
            _log.error("connect("+_url+","+_userAccount+","+_userPassword+","+_proxyURL+")", x);
            //Fail to parse Sequest Server URL
            errorCode = 1;
            errorString = "Fail to parse Sequest Server URL";
        }
    }

    public String startSession ()
    {
        findWorkableSettings(false);

        if (0 == errorCode)
            return startSessionInternal();
        else
            return "";
    }

    private String startSessionInternal ()
    {
        Properties results;

        errorCode = 0;
        errorString = "";
        if ("".equals(_userAccount) && "".equals(_userPassword))
        {
            //anoymous session
            Properties parameters = new Properties();
            results = request (parameters, true);
        }
        else
        {
            //GET /cgi/login.pl?display=nothing&onerrdisplay=nothing&action=login&username=<userid>&password=<password>
            Properties parameters = new Properties();
            parameters.setProperty("cgi", "login.pl");
            parameters.setProperty("display", "nothing");
            parameters.setProperty("onerrdisplay", "nothing");
            parameters.setProperty("action", "login");
            parameters.setProperty("username", _userAccount);
            parameters.setProperty("password", _userPassword);
            results = request (parameters, true);
        }
        if ("0".equals(results.getProperty("error","0")))
            return results.getProperty("sessionID", "");
        else
        {
            if (results.containsKey("error"))
                errorCode = Integer.parseInt(results.getProperty("error", "0"));
            return "";
        }
    }

    public void endSession (String sessionID)
    {
        errorCode = 0;
        errorString = "";

        if ("".equals(sessionID))
            return;

        //GET /cgi/login.pl?display=nothing&onerrdisplay=nothing&action=logout&sessionID=<sessionid>
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "login.pl");
        parameters.setProperty("display", "nothing");
        parameters.setProperty("onerrdisplay", "nothing");
        parameters.setProperty("action", "logout");
        parameters.setProperty("sessionID", sessionID);
        request (parameters, true);
        // we basically ignore the failure to log out
    }

    protected String getTaskID (String sessionID)
    {
        Properties results = new Properties();

        errorCode = 0;
        errorString = "";
        Date date = new Date();
        String taskId = Long.toString(date.getTime());
        return taskId;
    }

    protected String getTaskStatus (String sessionID, String taskId)
    {
        errorCode = 0;
        errorString = "";

        if ("".equals(taskId))
            return "";

        Properties parameters = new Properties();
        parameters.setProperty("cgi", "SequestQueue");
        parameters.setProperty("cmd", "status");
        parameters.setProperty("taskId", taskId);
        if (!"".equals(sessionID))
            parameters.setProperty("sessionID", sessionID);
        Properties results = request (parameters, false);
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
                if (null != _instanceLogger)
                    _instanceLogger.info ("Sequest search task status error: (" + results.getProperty("error","-1") + ") " +
                        results.getProperty("errorstring",""));
            }
            else
                statusString = results.getProperty("running", "");
        }

        return statusString;
    }

    public Map<String, String[]> getSequenceDBNames()
    {
        errorCode = 0;
        errorString = "";

        findWorkableSettings (false);

        Properties results;
        if (0 == errorCode || -3 == errorCode)
            results = getParametersResults();
        else
            results = new Properties();

        List<String> dbNames = new ArrayList<String>();
        String dbsString = results.getProperty("HTTPContent", "");
        String[] contentLines = dbsString.split("\n");
        boolean sectionDB = false;
        for (String contentLine : contentLines)
        {
                    if (!"".equals(contentLine))
                    {
                        dbNames.add(contentLine);
                    }
        }
        Map<String, String[]> result = new LinkedHashMap<String, String[]>();
        if (dbNames.size() > 0)
        {
            String[] dbsList;
            dbsList = dbNames.toArray(new String[dbNames.size()]);
            result.put("", dbsList);
        }
        return result;
    }

    public int search (String sequestParamFile, String mzXmlFile, String resultFile, Collection<String> mzXmlCommand)
    {
        errorCode = 0;
        errorString = "";

        if (null!=_instanceLogger) _instanceLogger.info("Creating Sequest session...");
        String sequestSessionId =  startSession();
        if (0 != getErrorCode())
        {
            if (null!=_instanceLogger) _instanceLogger.info("Fail to start Sequest session");
            return 2;
        }

        int returnCode = 0;
        final int maxRetry = 3;
        int attempt = 0;
        final int delayAfterSubmitSec = 30;
        final int delayBetweenRetrySec = 3 * 60;
        final int delayBetweenResultRetrievalSec = 10;
        while (attempt < maxRetry)
        {
            attempt++;

            // get a TaskID to submit the job
            if (null!=_instanceLogger) _instanceLogger.info("Creating Sequest search task...");
            String taskId = getTaskID(sequestSessionId);
            if ("".equals(taskId))
            {
                if (null!=_instanceLogger) _instanceLogger.info("Fail to create Sequest search task id.");
                returnCode = 5;
                break;
            }

            // submit job to sequest server
            if (null!=_instanceLogger) _instanceLogger.info("Submitting search to Sequest server (taskId=" + taskId + ").");
            if (!submitFile(sequestSessionId, taskId, sequestParamFile, mzXmlFile, mzXmlCommand))
            {
                if (null!=_instanceLogger) _instanceLogger.info("Failed to submit search to Sequest server.");
                returnCode = 3;
                break;
            }

            String prevSearchStatus = null;
            String searchStatus;
            while (true)
            {
                try
                {
                    Thread.sleep(delayAfterSubmitSec*1000);
                }
                catch (InterruptedException e) { }

                searchStatus = getTaskStatus (sequestSessionId, taskId);
                if (null!=_instanceLogger)
                {
                    if (null == prevSearchStatus || !searchStatus.equals(prevSearchStatus))
                        _instanceLogger.info("Sequest search status: " + searchStatus);
                    prevSearchStatus = searchStatus;
                }
                if (searchStatus.toLowerCase().contains("complete") ||
                    searchStatus.toLowerCase().contains("error"))
                {
                    break;
                }
            }

            if (!searchStatus.toLowerCase().contains("complete"))
            {
                if (searchStatus.toLowerCase().contains("error=51"))
                {
                    if (null!=_instanceLogger) _instanceLogger.info("Retrying " + delayBetweenRetrySec + " seconds later...");
                    try
                    {
                        Thread.sleep(delayBetweenRetrySec*1000);
                    }
                    catch (InterruptedException e) { }
                    continue;
                }
                else
                {
                    returnCode = 3;
                    break;
                }
            }

            try
            {
                Thread.sleep(delayBetweenResultRetrievalSec*1000);
            }
            catch (InterruptedException e) { }

            if (null!=_instanceLogger) _instanceLogger.info("Retrieving Sequest search result...");
            if (getResultFile(sequestSessionId, taskId, resultFile))
            {
                if (null!=_instanceLogger) _instanceLogger.info("Sequest search result retrieved.");
                clean(sequestSessionId, taskId);
            }
            else
            {
                returnCode = 3;
            }
            break;
        }

        // let's terminate the session
        endSession(sequestSessionId);
        if (null!=_instanceLogger) _instanceLogger.info("Sequest session ended.");

        return returnCode;
    }

    protected boolean submitFile (String sessionID, String taskId, String seqParamPath, String mzXmlPath,Collection<String> mzXmlCommand)
    {
        errorCode = 0;
        errorString = "";

        //sessionID is optional
        if (/*"".equals(sessionID) ||*/ "".equals(taskId) ||
             "".equals(seqParamPath) || "".equals(mzXmlPath))
            return false;

        /**
         * TODO: Add a conditional sessionId param
         */
        int partCount = 3 + mzXmlCommand.size();
        int count = 0;
        Part[] parts = new Part[partCount];
        String formFieldKey = "taskId";
        String formFieldValue = taskId;

        parts[count++] = new StringPart(formFieldKey,formFieldValue);

        File mzXmlFile = new File(mzXmlPath);
        try
        {
            parts[count++] = new FilePart("mzXML",mzXmlFile);
        }
        catch (FileNotFoundException err)
        {
            _log.error("Cannot find the  file '" + mzXmlFile.getPath () + "'.\n");
            return false;
        }

        File seqParamFile = new File(seqParamPath);
        try
        {
            parts[count++] = new FilePart("seqParams",seqParamFile);
        }
        catch (FileNotFoundException err)
        {
            _log.error("Cannot find the  file '" + seqParamFile.getPath () + "'.\n");
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
                _log.error("Failed to submit Sequest query '" + sequestRequestURL + "' for " +
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
                if (null != _instanceLogger) _instanceLogger.info ("Sequest search task status: query upload completed");
            }
            else
            {
                if (null != _instanceLogger) _instanceLogger.info(
                        "Sequest search task status: query upload failed with this error: " + sb.toString());
            }
            in.close();
        }
        catch (IOException err)
        {
            _log.error("Failed to get response from Sequest query '" + sequestRequestURL + "' for " +
                    mzXmlFile.getPath() + " with parameters " + seqParamFile.getPath () + " on attempt#" +
                    Integer.toString(attempt+1) + ".\n",err);
        }
        finally
        {
            post.releaseConnection();
        }

        return uploadFinished;
    }

    protected boolean getResultFile (String sessionID, String taskId, String resultFile)
    {
        errorCode = 0;
        errorString = "";
        //sessionID is optional
        if (/*"".equals(sessionID) ||*/ "".equals(taskId) || "".equals(resultFile))
            return false;

        //GET /cgi/client.pl?result_file_mime&task_id=<taskid>&sessionID=<sessionid>
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "SequestQueue");
        parameters.setProperty("cmd", "retrieve");
        parameters.setProperty("taskId", taskId);
        if (!"".equals(sessionID))
            parameters.setProperty("sessionID", sessionID);
        InputStream in = getRequestResultStream (parameters);
        if (null == in)
            return false;

        boolean ioError = false;
        File outFile = new File(resultFile);
        OutputStream out = null;
        try
        {
            // TODO: wch - write to log on the retsult retrieval progress
            //       we do not know the real size, as it is chunked stream
            out = new FileOutputStream(outFile);
            byte[] buffer = new byte [4096]; // use 4-KB fragment
            int readLen;
            while ((readLen=in.read(buffer))>0)
            {
                out.write(buffer,0,readLen);
            }
        }
        catch (FileNotFoundException e)
        {
            // output file cannot be created!
            ioError = true;
            _log.error("getResultFile(result="+resultFile+",session="+sessionID+",taskid="+taskId+")", e);
        }
        catch (IOException e)
        {
            // a read or write error occurred
            ioError = true;
            _log.error("getResultFile(result="+resultFile+",session="+sessionID+",taskid="+taskId+")", e);
        }
        finally
        {
            try { in.close(); } catch (IOException e) { }
            if (null != out)
            {
                try { out.close(); } catch (IOException e) { }
            }
        }

        if (ioError)
            return false;

        // let's check that we have the right file
        BufferedReader resultStream = null;
        String firstLine = "";
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

        if (!firstLine.startsWith("<HTML>"))
        {
            outFile.delete();
            return false;
        }
        else
        {
            return true;
        }
    }

    protected String clean(String sessionID, String taskId)
    {
        errorCode = 0;
        errorString = "";

       if ("".equals(taskId))
            return "";

        Properties parameters = new Properties();
        parameters.setProperty("cgi", "SequestQueue");
        parameters.setProperty("cmd", "clean");
        parameters.setProperty("taskId", taskId);
        if (!"".equals(sessionID))
            parameters.setProperty("sessionID", sessionID);
        Properties results = request(parameters, false);
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
                if (null != _instanceLogger)
                    _instanceLogger.info ("Sequest search task status error: (" + results.getProperty("error","-1") + ") " +
                        results.getProperty("errorstring",""));
            }
            else
                statusString = results.getProperty("running", "");
        }

        return statusString;

    }

    public String getParameters()
    {
        // retrieve the list of databases from SequestServer
        Properties results = getParametersResults();
        return results.getProperty("HTTPContent", "");
    }

    private Properties getParametersResults()
    {
        // retrieve the list of databases from SequestServer
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "SequestQueue");
        parameters.setProperty("cmd", "listDatabases");
        return request (parameters, false);
    }

    private String requestURL (Properties parameters)
    {
        StringBuffer requestURLLSB = new StringBuffer(_url);
        if (!_url.endsWith("/"))
        {
            requestURLLSB.append("/");
        }
        requestURLLSB.append(parameters.getProperty("cgi",""));
        requestURLLSB.append("?");
        boolean firstEntry=true;
        for (Enumeration e = parameters.propertyNames(); e.hasMoreElements();)
        {
            String s = (String) e.nextElement();
            if (!"cgi".equalsIgnoreCase(s))
            {
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
                if (!"".equals(val))
                {
                    requestURLLSB.append("=");
                    try {
                        requestURLLSB.append(URLEncoder.encode(val, "UTF-8"));
                    } catch (UnsupportedEncodingException x) {
                        requestURLLSB.append(val);
                    }
                }
            }
        }

        return requestURLLSB.toString();
    }

    private Properties request(Properties parameters, boolean parse)
    {
        // connect to the Sequest Server to send request
        // report the results as a property set, i.e. key=value pairs

        Properties results = new Properties();
        String sequestRequestURL = requestURL(parameters);
        try
        {
            URL sequestURL = new URL(sequestRequestURL);
            if (parse)
            {
                InputStream in = new BufferedInputStream(sequestURL.openStream());
                results.load(in);
                in.close();
                errorString = results.getProperty("errorstring", "");
            }
            else
            {
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
        }
        catch (MalformedURLException x)
        {
            String password = parameters.getProperty("password","");
            if (password.length() >0)
                sequestRequestURL = sequestRequestURL.replace(password, "***");
            _log.warn("Exception "+x.getClass()+" connect("+_url+","+parameters.getProperty("username","<null>")+","
                    +(parameters.getProperty("password","").length()>0 ? "***" : "")
                    +","+_proxyURL+")="+sequestRequestURL);
            errorCode = 1;
            errorString = "Fail to parse Sequest Server URL";
            results.setProperty("error", "1");
            results.setProperty("errorstring", errorString);
        }
        catch (Exception x)
        {
            String password = parameters.getProperty("password","");
            if (password.length() >0)
                sequestRequestURL = sequestRequestURL.replace(password, "***");
            _log.warn("Exception "+x.getClass()+" connect("+_url+","+parameters.getProperty("username","<null>")+","
                    +(password.length()>0 ? "***" : "")
                    +","+_proxyURL+")="+sequestRequestURL);
            errorCode = 2;
            errorString = "Fail to interact with Sequest Server";
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
            String password = parameters.getProperty("password","");
            if (password.length() >0)
                sequestRequestURL = sequestRequestURL.replace(password, "***");
            _log.warn("Exception "+x.getClass()+" connect("+_url+","+parameters.getProperty("username","<null>")+","
                    +(parameters.getProperty("password","").length()>0 ? "***" : "")
                    +","+_proxyURL+")="+sequestRequestURL, x);
            //Fail to parse Sequest Server URL
            errorCode = 1;
        }
        catch (Exception x)
        {
            String password = parameters.getProperty("password","");
            if (password.length() >0)
                sequestRequestURL = sequestRequestURL.replace(password, "***");
            _log.warn("Exception "+x.getClass()+" on connect("+_url+","+parameters.getProperty("username","<null>")+","
                    +(parameters.getProperty("password","").length()>0 ? "***" : "")
                    +","+_proxyURL+")="+sequestRequestURL, x);
            //Fail to interact with Sequest Server
            errorCode = 2;
        }

        return null;
    }

}
