/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.pipeline;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.util.XMLValidationParser;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.ms2.MascotClient;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;


/**
 * Client to make Mascot-specific request
 */

public class MascotClientImpl implements MascotClient
{
    private static Logger _log = Logger.getLogger(MascotClientImpl.class);
    private Logger _instanceLogger = null;
    //private static AppProps _appProps = AppProps.getInstance();
    //_appProps.getMascotServer(), _appProps.getMascotHTTPProxy ()
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

    public MascotClientImpl(String url, Logger instanceLogger)
    {
        _url = url;
        _instanceLogger = instanceLogger;
        _userAccount = "";
        _userPassword = "";
        errorCode = 0;
        errorString = "";
    }

    public MascotClientImpl(String url, Logger instanceLogger, String userAccount, String userPassword)
    {
        _url = url;
        _instanceLogger = instanceLogger;
        _userAccount = (null == userAccount) ? "" : userAccount;
        _userPassword = (null == userPassword) ? "" : userPassword;
        errorCode = 0;
        errorString="";
    }

    public void setUserAccount (String userAccount)
    {
        _userAccount = (null == userAccount) ? "" : userAccount;
    }

    public void setUserPassword (String userPassword)
    {
        _userPassword = (null == userPassword) ? "" : userPassword;
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
        // let' works on the proxy server setup
        boolean succeeded = false;
        if (null == proxyURL || "".equals(proxyURL))
        {
            proxyURL = "";
            Properties systemProperties = System.getProperties();
            systemProperties.setProperty("http.proxyHost","");
            systemProperties.setProperty("http.proxyPort","80");
            succeeded = true;
        }
        else
        {
            try
            {
                URL url = new URL(proxyURL);
                Properties systemProperties = System.getProperties();
                systemProperties.setProperty("http.proxyHost",url.getHost());
                systemProperties.setProperty("http.proxyPort",Integer.toString(url.getPort()));
                succeeded = true;
            }
            catch (MalformedURLException x)
            {
                _log.error("request(proxyURL="+proxyURL+")", x);
            }
        }
        if (succeeded)
            _proxyURL = proxyURL;
        return succeeded;
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
            // Mascot security enabled, but it means Mascot responded
            return "";
        }
        else
        {
            return (("".equals(errorString)) ? "Fail to contact Mascot server at " + _url : errorString);
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

            //http://mascot.server.org/mascot/cgi-bin/login.pl
            //http://mascot.server.org/cgi/login.pl
            //http://mascot.server.org/
            //mascot.server.org

            List<String> possibleURLs = new ArrayList<String>();
            // user provided a http://host/path, we shall test this first
            if (!"".equals(url.getPath()))
                possibleURLs.add(_url);

            StringBuffer alternativeLink;
            alternativeLink = new StringBuffer("http://");
            alternativeLink.append(url.getHost());
            if (80 != url.getPort() && -1 != url.getPort()) {
                alternativeLink.append(":").append(url.getPort());
            }
            String alternativeLinkPrefix = alternativeLink.toString();
            String alternativeUrl = "/mascot/cgi/";
            if (!alternativeUrl.equals(url.getPath()))
                possibleURLs.add(alternativeLinkPrefix + alternativeUrl);
            alternativeUrl = "/cgi/";
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
                            errorString = "Test passed ONLY when mascot server is set to " + alternativeLink.toString();

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
                        errorString = "Mascot server responded on " + testUrl + " with \"" + attemptMessage + "\"";

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
            //Fail to parse Mascot Server URL
            errorCode = 1;
            errorString = "Fail to parse Mascot Server URL";
        }
    }

    public String startSession ()
    {
        findWorkableSettings (true);

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
            //GET /cgi/login.pl?display=nothing&onerrdisplay=nothing&action=issecuritydisabled
            Properties parameters = new Properties();
            parameters.setProperty("cgi", "login.pl");
            parameters.setProperty("display", "nothing");
            parameters.setProperty("onerrdisplay", "nothing");
            parameters.setProperty("action", "issecuritydisabled");
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

    public String getMascotErrorMessage(int mascotErrorCode) {
        if (mascotErrorCode < 0) {
            return "Non-Mascot error code";
        }

        findWorkableSettings(false);

        //GET /cgi/ms-geterror.exe?<errorcode>
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "ms-geterror.exe");
        parameters.setProperty(Integer.toString(mascotErrorCode), "");
        Properties results = request(parameters, false);
        String mascotErrorString = results.getProperty("HTTPContent", "");
        if (0 == errorCode) {
            return mascotErrorString;
        } else {
            return "Sorry, unable to get Mascot error string for code " + Integer.toString(mascotErrorCode);
        }
    }

    public String getMascotVersion() {

        findWorkableSettings(false);

        //GET /cgi/client.pl?version
        // try to find out about the platform that Mascot Server is running on
        String mascotRequestURL;
        {
            StringBuffer urlSB = new StringBuffer(_url);
            if (!_url.endsWith("/"))
                urlSB.append("/");
            urlSB.append("client.pl?version");
            mascotRequestURL = urlSB.toString();
        }

        GetMethod get=new GetMethod(mascotRequestURL);
        HttpClient client = new HttpClient();
        String result="Sorry, unable to get Mascot version";
        try
        {
            int statusCode = client.executeMethod(get);
            if (statusCode == -1) {
                result=result+" "+get.getResponseBodyAsString();
            } else {
                result=get.getResponseBodyAsString()
                        +" - "+get.getResponseHeader("Server");
            }
        } catch (IOException e)
        {
            _log.warn("Failed to get Mascot server information via '" + mascotRequestURL + "'", e);
        } finally {
            get.releaseConnection();
        }

        result=result.replaceAll("[\r\n]"," ");
        return result;
    }

    public boolean isMimeResultSupported() {

        findWorkableSettings(false);

        //GET /cgi/client.pl?version
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "client.pl");
        parameters.setProperty("result_file_mime", "");
        parameters.setProperty("task_id", "0");
        Properties results = request(parameters, false);
        String result = results.getProperty("HTTPContent", "");
        return (!result.contains("Invalid keyword argument"));
    }

    protected Properties getTaskID (String sessionID)
    {
        Properties results = new Properties();

        errorCode = 0;
        errorString = "";
        //sessionID is optional
        /*if ("".equals(sessionID))
            return results;*/

        //GET /cgi/client.pl?create_task_id&sessionID=<sessionid>
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "client.pl");
        parameters.setProperty("create_task_id", "");
        if (!"".equals(sessionID))
            parameters.setProperty("sessionID", sessionID);
        results = request (parameters, true);
        if (! "0".equals(results.getProperty("error","0")))
            results.clear();
        // if the call succeeded, we have keys={'actionstring', 'taskID'}
        return results;
    }

    protected Logger getLogger() {
        Logger logInstance = _log;
        if (null != _instanceLogger)
            logInstance = _instanceLogger;
        return logInstance;
    }

    protected String getTaskStatus (String sessionID, String taskID)
    {
        errorCode = 0;
        errorString = "";

        //sessionID is optional
        if (/*"".equals(sessionID) ||*/ "".equals(taskID))
            return "";

        //GET /cgi/client.pl?status&sessionID=<sessionid>&task_id=<taskid>
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "client.pl");
        parameters.setProperty("status", "");
        parameters.setProperty("task_id", taskID);
        if (!"".equals(sessionID))
            parameters.setProperty("sessionID", sessionID);
        Properties results = request (parameters, false);
        String statusString = results.getProperty("HTTPContent", "");
        if (statusString.contains("=")) {
            /*Logger tlogInstance = getLogger();
            if (null != tlogInstance) {
                tlogInstance.info ("Full Mascot response: (" + results.getProperty("HTTPContent","") + ")");
            }*/
            String[] contentLines = statusString.split("\n");
            for (String contentLine : contentLines) {
                if (contentLine.contains("=")) {
                    String[] parts = contentLine.split("=");
                    if (2 == parts.length)
                        if (!"".equals(parts[0]))
                            results.put(parts[0], parts[1]);
                }
            }
            if (results.containsKey("error")) {
                String errorValue = results.getProperty("error", "-1");
                if (!"0".equals(errorValue)) {
                    // fall thru', return the full HTTP Content as we need the full text for diagnosis
                    Logger logInstance = getLogger();
                    if (null != logInstance) {
                        logInstance.info("Mascot search task status error: (" + results.getProperty("error", "-1") + ") " +
                                results.getProperty("errorstring", ""));
                        if ("-1".equals(errorValue)) {
                            logInstance.info("Full Mascot response: (" + results.getProperty("HTTPContent", "") + ")");
                        } else {
                            String mascotErrorMessage = getMascotErrorMessage(Integer.parseInt(errorValue));
                            logInstance.info("Mascot message: (" + mascotErrorMessage + ")");
                        }
                    }
                }
            } else
                statusString = results.getProperty("running", "");
            results.remove("HTTPContent");
        } else {
            //TODO: wch - do we want to dump this, how frequent will this be?
            String lcStatus = statusString.toLowerCase();
            if (!lcStatus.startsWith("complete\n") && !lcStatus.startsWith("complete\r\n")) {
                Logger logInstance = getLogger();
                if (null != logInstance) {
                    logInstance.info("Mascot response: (" + results.getProperty("HTTPContent", "") + ")");
                }
            }
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
            if (contentLine.startsWith("[") && contentLine.endsWith("]"))
                sectionDB = "[DB]".equals(contentLine);
            else
            {
                if (sectionDB)
                    if (!"".equals(contentLine))
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

    public int search (String paramFile, String queryFile, String resultFile)
    {
        errorCode = 0;
        errorString = "";

        String version=getMascotVersion();
        if (null != _instanceLogger) _instanceLogger.info(version);
        if (!isMimeResultSupported()) {
            String msg1="Your mascot installation does not have support for MIME result download via client.pl!";
            if (null != _instanceLogger) _instanceLogger.warn(msg1);
            _log.warn(msg1);
            String msg2="Result retrieval may fail. See " +
                    (new HelpTopic("configMascot", HelpTopic.Area.INSTALL)).getHelpTopicLink() +" for more info.";
            if (null != _instanceLogger) _instanceLogger.warn(msg2);
            _log.warn(msg2);
        }

        // check if security is enabled
        // let's acquire a session id if we do not have one or security is enabled
        if (null!=_instanceLogger) _instanceLogger.info("Creating Mascot session...");
        String mascotSessionId = startSession();
        if (0 != getErrorCode())
        {
            if (null!=_instanceLogger) _instanceLogger.info("Fail to start Mascot session");
            return 2;
        } else {
            if (null != _instanceLogger) _instanceLogger.info("Mascot session#"+mascotSessionId+" started.");
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
            if (null!=_instanceLogger) _instanceLogger.info("Creating Mascot search task...");
            Properties taskProperties = getTaskID (mascotSessionId);
            String actionString = taskProperties.getProperty("actionstring", "");
            String taskID = taskProperties.getProperty("taskID", "");
            if ("".equals(actionString) || "".equals(taskID))
            {
                if (null!=_instanceLogger) _instanceLogger.info("Fail to create Mascot search task id.");
                returnCode = 5;
                break;
            } else {
                if (null != _instanceLogger) _instanceLogger.info("Mascot search task#"+taskID+" created with '"+actionString+"'.");
            }

            // submit job to mascot server
            if (null!=_instanceLogger) _instanceLogger.info("Submitting search to Mascot server...");
            if (!submitFile (mascotSessionId, taskID, actionString, paramFile, queryFile))
            {
                if (null!=_instanceLogger) _instanceLogger.info("Fail to submit search to Mascot server.");
                returnCode = 3;
                break;
            } else {
                if (null != _instanceLogger) _instanceLogger.info("Search submitted.");
            }

            if (null != _instanceLogger) {
                _instanceLogger.info("Mascot search status verbose reporting on");
            }
            final int delayBetweenSameStatus = 2 * 60;
            int secSinceSameStatus = 0;
            String prevSearchStatus = null;
            String searchStatus;
            final int maxNegativeErrorTry = 3;
            int numOfNegativeError = 0;
            while (true) {
                try {
                    Thread.sleep(delayAfterSubmitSec * 1000);
                }
                catch (InterruptedException e) {
                }

                searchStatus = getTaskStatus(mascotSessionId, taskID);
                if (null != _instanceLogger) {
                    secSinceSameStatus += delayAfterSubmitSec;
                    if (null == prevSearchStatus || !searchStatus.equals(prevSearchStatus)
                            || secSinceSameStatus >= delayBetweenSameStatus) {
                        _instanceLogger.info("Mascot search status: " + searchStatus);
                        secSinceSameStatus = 0;
                    }
                    prevSearchStatus = searchStatus;
                }
                if (searchStatus.toLowerCase().contains("error=-")) {
                    numOfNegativeError++;
                    if (numOfNegativeError>=maxNegativeErrorTry) {
                        break;
                    }
                    if (null != _instanceLogger) _instanceLogger.info(searchStatus+", will retry..");
                }
                else if (searchStatus.toLowerCase().contains("complete") ||
                        searchStatus.toLowerCase().contains("error=")) {
                    break;
                }
            }

            if (!searchStatus.toLowerCase().contains("complete")) {
                if (searchStatus.toLowerCase().contains("error=51")) {
                    if (null != _instanceLogger)
                        _instanceLogger.info("Retrying " + delayBetweenRetrySec + " seconds later...");
                    try {
                        Thread.sleep(delayBetweenRetrySec * 1000);
                    }
                    catch (InterruptedException e) {
                    }
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

            if (null!=_instanceLogger) _instanceLogger.info("Retrieving Mascot search result...");
            if (getResultFile (mascotSessionId, taskID, resultFile))
            {
                if (null!=_instanceLogger) _instanceLogger.info("Mascot search result retrieved.");
            }
            else
            {
                returnCode = 3;
            }

            break;
        }

        // let's terminate the session
        endSession(mascotSessionId);
        if (null!=_instanceLogger) _instanceLogger.info("Mascot session ended.");

        return returnCode;
    }

    private MascotInputParser getInputParameters(File parametersFile)
    {
        BufferedReader inputReader = null;
        StringBuffer xmlBuffer = new StringBuffer();
        try
        {
            inputReader = new BufferedReader(new FileReader(parametersFile));
            String line;
            while ((line = inputReader.readLine()) != null)
                xmlBuffer.append(line).append("\n");
        }
        catch (IOException eio)
        {
            _log.error("Failed to read Mascot input xml '" + parametersFile.getPath() + "'.");
            return null;
        }
        finally
        {
            if (inputReader != null)
            {
                try
                {
                    inputReader.close();
                }
                catch (IOException eio)
                {
                }
            }
        }

        MascotInputParser parser = new MascotInputParser();
        parser.parse(xmlBuffer.toString());
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
                _log.error("Failed parsing Mascot input xml '" + parametersFile.getPath() + "'.\n" +
                        err.getMessage());
            else
                _log.error("Failed parsing Mascot input xml '" + parametersFile.getPath() + "'.\n" +
                        "Line " + err.getLine() + ": " + err.getMessage());
            return null;
        }
        return parser;
    }

    protected boolean submitFile (String sessionID, String taskID,
        String actionString, String paramFile, String analysisFile)
    {
        errorCode = 0;
        errorString = "";

        //sessionID is optional
        if (/*"".equals(sessionID) ||*/ "".equals(taskID) ||
            "".equals(actionString) || "".equals(paramFile) || "".equals(analysisFile))
            return false;

        File queryParamFile = new File(paramFile);
        MascotInputParser parser = getInputParameters(queryParamFile);
        if (null == parser)
            return false;

        String [][] submitFields = {
                {"charge", "mascot, peptide_charge", "search, charge"},
                {"cle", "mascot, enzyme", "search, cle"},
                {"com", "mascot, comment", "search, com"},
                {"db", "pipeline, database", "search, db"},
                {"errortolerant", "mascot, error_tolerant", "default, errortolerant"},
                {"format", "spectrum, path type", "search, format"},
                {"formver", "mascot, form version", "default, formver"},
                {"icat", "mascot, icat", "search, icat"},
                {"instrument", "mascot, instrument", "search, instrument"},
                {"intermediate", "mascot, intermediate", "default, intermediate"},
                {"it_mods", "mascot, variable modifications", "search, it_mods"},
                {"mods", "mascot, fixed modifications", "search, mods"},
                {"overview", "mascot, overview", "search, overview"},
                {"pfa", "scoring, maximum missed cleavage sites", "search, pfa"},
                {"precursor", "mascot, precursor", "search, precursor"},
                {"report", "mascot, report top results", "search, report"},
                {"reptype", "mascot, report type", "default, reptype"},
                {"search", "mascot, search type", "default, search"},
                {"seg", "mascot, protein mass", "search, seg"},
                {"taxonomy", "protein, taxon", "search, taxonomy"},
                {"tolu", "spectrum, parent monoisotopic mass error units", "search, tolu"},
                {"useremail", "pipeline, email address", "search, usermail"},
                {"username", "pipeline, user name", "search, username"},
                {"iatol", "mascot, iatol", "default, iatol"},
                {"iastol", "mascot, iastol", "default, iastol"},
                {"ia2tol", "mascot, ia2tol", "default, ia2tol"},
                {"ibtol", "mascot, ibtol", "default, ibtol"},
                {"ibstol", "mascot, ibstol", "default, ibstol"},
                {"ib2tol", "mascot, ib2tol", "default, ib2tol"},
                {"iytol", "mascot, iytol", "default, iytol"},
                {"iystol", "mascot, iystol", "default, iystol"},
                {"iy2tol", "mascot, iy2tol", "default, iy2tol"},
                {"peak", "mascot, peak", "default, peak"},
                {"ltol", "mascot, ltol", "default, ltol"},
                {"showallmods", "mascot, showallmods", "default, showallmods"}
        };
        Part [] parts = new Part[submitFields.length+4+1];
        int i;
        for (i=0; i<submitFields.length; i++)
        {
            int j;
            String [] keys = submitFields[i];
            String formFieldKey = keys[0].toUpperCase();
            String formFieldValue = null;
            for (j=1; j<keys.length; j++)
            {
                formFieldValue = parser.getInputParameter(keys[j]);
                if (null != formFieldValue)
                    break;
            }
            parts[i] = new StringPart(formFieldKey,(null==formFieldValue)?"":formFieldValue);
        }

        //{"tol",
        //   max{"spectrum, parent monoisotopic mass error plus",
        //     "spectrum, parent monoisotopic mass error minus"}, or
        //  "search, tol"}
        String formFieldKey = "TOL";
        String formFieldValue = null;
        String formFieldValue1 = parser.getInputParameter("spectrum, parent monoisotopic mass error plus");
        String formFieldValue2 = parser.getInputParameter("spectrum, parent monoisotopic mass error minus");
        if (formFieldValue1 != null || formFieldValue2 != null)
        {
            if (formFieldValue1 != null && formFieldValue2 != null)
            {
                float float1 = Float.valueOf(formFieldValue1);
                float float2 = Float.valueOf(formFieldValue2);
                formFieldValue = (float1 > float2) ? formFieldValue1 : formFieldValue2;
            }
            else if (formFieldValue1 != null)
                formFieldValue = formFieldValue1;
            else
                formFieldValue = formFieldValue2;
        }
        else
            formFieldValue = parser.getInputParameter("search, tol");
        parts[i] = new StringPart(formFieldKey,(null==formFieldValue)?"":formFieldValue);
        i++;

        formFieldKey = "MASS";
        formFieldValue = parser.getInputParameter("spectrum, fragment mass type");
        if (formFieldValue == null)
            formFieldValue = parser.getInputParameter("search, mass");
        parts[i] = new StringPart(formFieldKey,(null==formFieldValue)?"":formFieldValue);
        i++;

        boolean isMonoisoptopicMass = "monoisotopic".equalsIgnoreCase(formFieldValue);
        formFieldKey = "ITOL";
        formFieldValue = parser.getInputParameter(isMonoisoptopicMass ? "spectrum, fragment monoisotopic mass error" : "spectrum, fragment mass error");
        if (formFieldValue == null)
        {
            formFieldValue = parser.getInputParameter("search, itol");
        }
        parts[i] = new StringPart(formFieldKey,(null==formFieldValue)?"":formFieldValue);
        i++;
        formFieldKey = "ITOLU";
        formFieldValue = parser.getInputParameter(isMonoisoptopicMass ? "spectrum, fragment monoisotopic mass error units" : "spectrum, fragment mass error units");
        if (formFieldValue == null)
            formFieldValue = parser.getInputParameter("search, itolu");
        parts[i] = new StringPart(formFieldKey,(null==formFieldValue)?"":formFieldValue);
        i++;

        /*String [] submitFields = {
            "default, intermediate", "default, formver",
            "default, search", "default, iatol", "default, iastol",
            "default, ia2tol", "default, ibtol", "default, ibstol",
            "default, ib2tol", "default, iytol", "default, iystol",
            "default, iy2tol", "default, peak", "default, ltol",
            "default, reptype", "default, errortolerant",
            "default, showallmods", "search, username",
            "search, usermail", "search, com", "search, db",
            "search, taxonomy", "search, cle", "search, pfa",
            "search, mods", "search, it_mods", "search, seg",
            "search, icat", "search, tol", "search, tolu",
            "search, itol", "search, itolu", "search, charge",
            "search, mass", "search, format", "search, precursor",
            "search, instrument", "search, overview", "search, report"
        };

        Part [] parts = new Part[submitFields.length+1];
        int i;
        for (i=0; i<submitFields.length; i++)
        {
            String val = parser.getInputParameter(submitFields[i]);
            String[] keyParts = submitFields[i].split(", ");
            parts[i] = new StringPart(keyParts[1].toUpperCase(),(null==val)?"":val);
        }
        */

        File queryFile = new File(analysisFile);
        if (null != _instanceLogger) _instanceLogger.info("Submitting query file, size="+queryFile.length());
        try {
            parts[i] = new FilePart("FILE", queryFile);
        }
        catch (FileNotFoundException err)
        {
            _log.error("Cannot file Mascot query file '" + queryParamFile.getPath () + "'.\n");
            return false;
        }

        String mascotRequestURL;
        {
            StringBuffer urlSB = new StringBuffer(_url);
            if (!_url.endsWith("/"))
                urlSB.append("/");
            urlSB.append(actionString);
            urlSB.append("?");
            urlSB.append("1");
            urlSB.append("+--taskID+");
            urlSB.append(taskID);
            if (!"".equals(sessionID))
            {
                urlSB.append("+--sessionID+");
                urlSB.append(sessionID);
            }
            mascotRequestURL = urlSB.toString();
        }

        PostMethod post = new PostMethod(mascotRequestURL);
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
                // TODO: wch - we should extend StringPart and FilePart
                //       so that we may write to log on the amount of data transmitted
                statusCode = client.executeMethod(post);
            }
            catch (IOException err)
            {
                _log.error("Failed to submit Mascot query '" + mascotRequestURL + "' for " +
                        queryFile.getPath() + " with parameters " + queryParamFile.getPath () + " on attempt#" +
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
            // check for "Finished uploading search details..."
            final String endOfUploadMarker = "Finished uploading search details...";
            BufferedReader in = new BufferedReader(new InputStreamReader(post.getResponseBodyAsStream()));
            String str;
            StringBuffer reply = new StringBuffer();
            while ((str = in.readLine()) != null) {
                if (str.contains(endOfUploadMarker))
                {
                    uploadFinished = true;
                    if (null != _instanceLogger)
                        _instanceLogger.info("Mascot search task status: query upload completed");
                    //WCH: we cannot break for Mascot on Windows platform
                    //break;
                }
            }
            in.close();
        }
        catch (IOException err)
        {
            _log.error("Failed to get response from Mascot query '" + mascotRequestURL + "' for " +
                    queryFile.getPath() + " with parameters " + queryParamFile.getPath () + " on attempt#" +
                    Integer.toString(attempt+1) + ".\n",err);
        }
        finally
        {
            post.releaseConnection();
        }

        return uploadFinished;
    }

    protected boolean getResultFile (String sessionID, String taskID, String resultFile)
    {
        errorCode = 0;
        errorString = "";
        //sessionID is optional
        if (/*"".equals(sessionID) ||*/ "".equals(taskID) || "".equals(resultFile))
            return false;

        //GET /cgi/client.pl?result_file_mime&task_id=<taskid>&sessionID=<sessionid>
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "client.pl");
        parameters.setProperty("result_file_mime", "");
        parameters.setProperty("task_id", taskID);
        if (!"".equals(sessionID))
            parameters.setProperty("sessionID", sessionID);
        InputStream in = getRequestResultStream (parameters);
        if (null == in)
            return false;

        long lByteRead=0;
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
            while ((readLen = in.read(buffer)) > 0) {
                lByteRead += readLen;
                out.write(buffer, 0, readLen);
            }
        }
        catch (FileNotFoundException e)
        {
            // output file cannot be created!
            ioError = true;
            _log.error("getResultFile(result="+resultFile+",session="+sessionID+",taskid="+taskID+")", e);
        }
        catch (IOException e)
        {
            // a read or write error occurred
            ioError = true;
            _log.error("getResultFile(result="+resultFile+",session="+sessionID+",taskid="+taskID+")", e);
        }
        finally
        {
            try { in.close(); } catch (IOException e) { }
            if (null != out)
            {
                try { out.close(); } catch (IOException e) { }
            }
        }

        if (null != _instanceLogger)
            _instanceLogger.info("Downloaded "+lByteRead+" bytes of result file.");

        if (ioError)
            return false;

        // let's check that we have the right file
        BufferedReader resultStream = null;
        final int maxLines=20;
        List<String> contentLines = new ArrayList<String>();
        String firstLine = "";
        try
        {
            resultStream = new BufferedReader(new InputStreamReader(new FileInputStream(outFile)));
            for (int index=0; index<maxLines; index++) {
                firstLine = resultStream.readLine();
                if (null != firstLine) {
                    contentLines.add(firstLine);
                }
            }
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

        firstLine=contentLines.get(0);
        if (!firstLine.startsWith("MIME-Version:")) {
            if (null != _instanceLogger) {
                _instanceLogger.info("First line of Mascot result file does not start with 'MIME-Version:'... will remove file");
                _instanceLogger.info("First "+contentLines.size()+" line(s)\n"+ StringUtils.join(contentLines.iterator(),"\n"));
            }
            outFile.delete();
            return false;
        }
        else
        {
            return true;
        }
    }

    public String getParameters()
    {
        // retrieve the list of databases from MascotServer
        Properties results = getParametersResults();
        return results.getProperty("HTTPContent", "");
    }

    private Properties getParametersResults()
    {
        // retrieve the list of databases from MascotServer
        Properties parameters = new Properties();
        parameters.setProperty("cgi", "get_params.pl");
        return request (parameters, false);
    }

    private String requestURL (Properties parameters)
    {
        StringBuffer requestURLLSB = new StringBuffer(_url);
        if (!_url.endsWith("/"))
        {
            requestURLLSB.append("/");
        }
        requestURLLSB.append(parameters.getProperty("cgi","login.pl"));
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
        // connect to the Mascot Server to send request
        // report the results as a property set, i.e. key=value pairs

        Properties results = new Properties();
        String mascotRequestURL = requestURL(parameters);
        try
        {
            URL mascotURL = new URL(mascotRequestURL);
            if (parse)
            {
                InputStream in = new BufferedInputStream(mascotURL.openStream());
                results.load(in);
                in.close();
                errorString = results.getProperty("errorstring", ""); 
            }
            else
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(mascotURL.openStream()));
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
                mascotRequestURL = mascotRequestURL.replace(password, "***");
            _log.warn("Exception "+x.getClass()+" connect("+_url+","+parameters.getProperty("username","<null>")+","
                    +(parameters.getProperty("password","").length()>0 ? "***" : "")
                    +","+_proxyURL+")="+mascotRequestURL);
            errorCode = 1;
            errorString = "Fail to parse Mascot Server URL";
            results.setProperty("error", "1");
            results.setProperty("errorstring", errorString);
        }
        catch (Exception x)
        {
            String password = parameters.getProperty("password","");
            if (password.length() >0)
                mascotRequestURL = mascotRequestURL.replace(password, "***");
            _log.warn("Exception "+x.getClass()+" connect("+_url+","+parameters.getProperty("username","<null>")+","
                    +(password.length()>0 ? "***" : "")
                    +","+_proxyURL+")="+mascotRequestURL);
            errorCode = 2;
            errorString = "Fail to interact with Mascot Server";
            results.setProperty("error", "2");
            results.setProperty("errorstring", errorString);
        }

        return results;
    }

    private InputStream getRequestResultStream (Properties parameters)
    {
        // connect to the Mascot Server to send request
        // return the reply as a stream

        String mascotRequestURL = requestURL(parameters);
        try
        {
            URL mascotURL = new URL(mascotRequestURL);
            return mascotURL.openStream();
        }
        catch (MalformedURLException x)
        {
            String password = parameters.getProperty("password","");
            if (password.length() >0)
                mascotRequestURL = mascotRequestURL.replace(password, "***");
            _log.warn("Exception "+x.getClass()+" connect("+_url+","+parameters.getProperty("username","<null>")+","
                    +(parameters.getProperty("password","").length()>0 ? "***" : "")
                    +","+_proxyURL+")="+mascotRequestURL, x);
            //Fail to parse Mascot Server URL
            errorCode = 1;
        }
        catch (Exception x)
        {
            String password = parameters.getProperty("password","");
            if (password.length() >0)
                mascotRequestURL = mascotRequestURL.replace(password, "***");
            _log.warn("Exception "+x.getClass()+" on connect("+_url+","+parameters.getProperty("username","<null>")+","
                    +(parameters.getProperty("password","").length()>0 ? "***" : "")
                    +","+_proxyURL+")="+mascotRequestURL, x);
            //Fail to interact with Mascot Server
            errorCode = 2;
        }

        return null;
    }

}

