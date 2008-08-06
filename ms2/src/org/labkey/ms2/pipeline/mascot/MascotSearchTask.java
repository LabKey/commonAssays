/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
package org.labkey.ms2.pipeline.mascot;

import org.labkey.api.pipeline.*;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.*;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 * <code>MascotSearchTask</code>
 */
public class MascotSearchTask extends PipelineJob.Task<MascotSearchTask.Factory>
{
    private static final String KEY_HASH = "HASH";
    private static final String KEY_FILESIZE = "FILESIZE";
    private static final String KEY_TIMESTAMP = "TIMESTAMP";

    private static final FileType FT_MASCOT_DAT = new FileType(".dat");
    private static final FileType FT_MASCOT_MGF = new FileType(".mgf");
    private static final String MZXML2SEARCH_ACTION_NAME = "MzXML2Search";
    private static final String MASCOT_ACTION_NAME = "Mascot";
    private static final String MASCOT2XML_ACTION_NAME = "Mascot2XML";

    public static File getNativeSpectraFile(File dirAnalysis, String baseName)
    {
        return FT_MASCOT_MGF.newFile(dirAnalysis, baseName);
    }

    public static File getNativeOutputFile(File dirAnalysis, String baseName)
    {
        return FT_MASCOT_DAT.newFile(dirAnalysis, baseName);
    }

    public static boolean isNativeOutputFile(File file)
    {
        return FT_MASCOT_DAT.isType(file);
    }

    /**
     * Interface for support required from the PipelineJob to run this task,
     * beyond the base PipelineJob methods.
     */
    public interface JobSupport extends MS2SearchJobSupport
    {
        /**
         * Returns Mascot server name.
         */
        String getMascotServer();

        /**
         * Returns HTTP proxy for Mascot server.
         */
        String getMascotHTTPProxy();

        /**
         * Returns user name for Mascot server connection.
         */
        String getMascotUserAccount();

        /**
         * Return password for Mascot server connection.
         */
        String getMascotUserPassword();

        /**
         * Set the sequence DB info returned from Mascot search.
         */
        void setMascotSequenceDB(String sequenceDB);

        /**
         * Set the sequence release info return from the Mascot search.
         */
        void setMascotSequenceRelease(String sequenceRelease);
    }

    public static class Factory extends AbstractMS2SearchTaskFactory<Factory>
    {
        public Factory()
        {
            super(MascotSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new MascotSearchTask(this, job);
        }

        public String getGroupParameterName()
        {
            return "mascot";
        }

        public boolean isJobComplete(PipelineJob job) throws IOException, SQLException
        {
            JobSupport support = (JobSupport) job;
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            // Mascot input (MGF) and Mascot native output
            if (!NetworkDrive.exists(getNativeSpectraFile(dirAnalysis, baseName)) ||
                    !NetworkDrive.exists(getNativeOutputFile(dirAnalysis, baseName)))
                return false;

            // Either raw converted pepXML from DAT, or completely analyzed pepXML
            return NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseName)) ||
                   NetworkDrive.exists(AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis, baseName));
        }

        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(MZXML2SEARCH_ACTION_NAME, MASCOT_ACTION_NAME, MASCOT2XML_ACTION_NAME);
        }
    }

    protected MascotSearchTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public JobSupport getJobSupport()
    {
        return getJob().getJobSupport(JobSupport.class);
    }

    public List<RecordedAction> run() throws PipelineJobException
    {
        try
        {
            Map<String, String> params = getJob().getParameters();

            WorkDirFactory factory = PipelineJobService.get().getWorkDirFactory();
            WorkDirectory wd = factory.createWorkDirectory(getJob().getJobGUID(), getJobSupport(), getJob().getLogger());

            RecordedAction mzxml2SearchAction = new RecordedAction(MZXML2SEARCH_ACTION_NAME);
            RecordedAction mascotAction = new RecordedAction(MASCOT_ACTION_NAME);
            RecordedAction mascot2XMLAction = new RecordedAction(MASCOT2XML_ACTION_NAME);

            File fileWorkMGF = wd.newFile(FT_MASCOT_MGF);
            File fileWorkDAT = wd.newFile(FT_MASCOT_DAT);
            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(wd.getDir(),
                    getJobSupport().getBaseName());

            // Mascot starts with remote sequence file names, so it has to look at the
            // raw parameter, rather than using getJobSupport().getSequenceFiles().
            String paramDatabase = params.get("pipeline, database");
            if (paramDatabase == null)
            {
                throw new IOException("Failed parsing Mascot input xml '" + getJobSupport().getParametersFile() + "'.\n" +
                        "Missing required input parameter 'pipeline, database'");
            }
            String[] databases = paramDatabase.split(";");
            if (databases.length > 1)
            {
                getJob().error("Mascot does not support multiple databases searching. ("+paramDatabase+")");
                return Collections.emptyList();
            }

            params.put("pipeline, user name", "LabKey User");

            File fileWorkInputXML = wd.newFile("input.xml");
            getJobSupport().createParamParser().writeFromMap(params, fileWorkInputXML);

            File fileMGF = new File(getJobSupport().getSearchSpectraFile().getParentFile(), fileWorkMGF.getName());

            // 0. pre-Mascot search: c) translate the mzXML file to mgf for Mascot (msxml2other)
            File fileWorkSpectra = wd.inputFile(getJobSupport().getSearchSpectraFile(), false);
            ArrayList<String> argsM2S = new ArrayList<String>();
            String ver = getJob().getParameters().get("pipeline, tpp version");
            argsM2S.add(PipelineJobService.get().getExecutablePath("MzXML2Search", "tpp", ver));
            argsM2S.add("-mgf");
            String paramMinParent = params.get("spectrum, minimum parent m+h");
            if (paramMinParent != null)
                argsM2S.add("-B" + paramMinParent);
            String paramMaxParent = params.get("spectrum, maximum parent m+h");
            if (paramMaxParent != null)
                argsM2S.add("-T" + paramMaxParent);
            argsM2S.add(fileWorkSpectra.getAbsolutePath());

            getJob().runSubProcess(new ProcessBuilder(argsM2S), wd.getDir());

            //  1. perform Mascot search
            getJob().header("mascot client output");

            MascotClientImpl mascotClient = new MascotClientImpl(getJobSupport().getMascotServer(), getJob().getLogger(),
                getJobSupport().getMascotUserAccount(), getJobSupport().getMascotUserPassword());
            mascotClient.setProxyURL(getJobSupport().getMascotHTTPProxy());
            int iReturn = mascotClient.search(fileWorkInputXML.getAbsolutePath(),
                    fileMGF.getAbsolutePath(), fileWorkDAT.getAbsolutePath());
            if (iReturn != 0)
            {
                throw new IOException("Error code " + mascotClient.getErrorString());
            }
            if (!fileWorkDAT.exists())
            {
                throw new IOException("Did not get excepted results file from Mascot: " + fileWorkDAT);
            }

            getJob().header("Sequence Database Synchronization output");

            //a. get database and release entry
            String sequenceDB = getSequenceDatabase(fileWorkDAT);
            String sequenceRelease = getDatabaseRelease(fileWorkDAT);
            //b. get release information at Mascot server
            getJob().info("Retreiving database information ("+sequenceRelease+")...");
            Map<String,String> returns = mascotClient.getDBInfo(sequenceDB, sequenceRelease);
            String status = returns.get("STATUS");
            if (null == status || !"OK".equals(status))
            {
                getJob().error("Failed to get database from Mascot server.");
                String exceptionMessage=returns.get("exceptionmessage");
                String exceptionClass=returns.get("exceptionclass");
                if (null!=exceptionMessage)
                {
                    exceptionMessage=exceptionMessage.toLowerCase();
                    exceptionClass=exceptionClass.toLowerCase();

                    if (exceptionMessage.contains("http response code: 500"))
                        throw new IOException("labkeydbmgmt.pl does not seem to be functioning on Mascot server.  " +
                                "Please ask your administrator to verify.");
                    else if (exceptionClass.contains("java.io.filenotfoundexception"))
                        throw new IOException("labkeydbmgmt.pl may not have been installed on Mascot server " +
                                "(<mascot directory>/cgi).  Please ask your administrator to install it.");
                    else
                        throw new IOException("Message: " + returns.get("exceptionmessage"));
                }
            }

            String smascotFileHash=returns.get("HASH");
            String smascotFileSize=returns.get("FILESIZE");
            String smascotFileTimestamp=returns.get("TIMESTAMP");

            getJob().info("Database "+sequenceRelease+", hash="+smascotFileHash+", size="+smascotFileSize+", timestamp="+smascotFileTimestamp);

            long nmascotFileSize=Long.parseLong(smascotFileSize);
            long nmascotFileTimestamp=Long.parseLong(smascotFileTimestamp);

            File dirSequenceRoot = getJobSupport().getSequenceRootDirectory();
            File localDB = MS2PipelineManager.getLocalMascotFile(dirSequenceRoot.getPath(), sequenceDB, sequenceRelease);
            File localDBHash = MS2PipelineManager.getLocalMascotFileHash(dirSequenceRoot.getPath(), sequenceDB, sequenceRelease);
            File localDBParent = localDB.getParentFile();
            localDBParent.mkdirs();
            long filesize=0;
            long timestamp=0;
            String hash="";
            boolean toDownloadDB = false;
            if (!localDB.exists())
            {
                //c. if local copy does not exist, download DB and cache checking hashes
                // use the default hashes
                getJob().info("Local database "+sequenceRelease+" does not exist, downloading from Mascot server");
                toDownloadDB = true;
            }
            else
            {
                //c. if local copy exists & cached checking hashes do not match, download new DB and cache new hashes
                // let's get the hashes
                Map<String,String> hashes=readLocalMascotFileHash(localDBHash.getCanonicalPath());
                if (null!=hashes.get("HASH"))
                {
                    hash=hashes.get("HASH");
                }
                if (null!=hashes.get("FILESIZE"))
                {
                    String value=hashes.get("FILESIZE");
                    filesize=Long.parseLong(value);
                }
                if (null!=hashes.get("TIMESTAMP"))
                {
                    String value=hashes.get("TIMESTAMP");
                    timestamp=Long.parseLong(value);
                }
                if (!smascotFileHash.equals(hash) ||
                    nmascotFileSize!=filesize || nmascotFileTimestamp!=timestamp)
                {
                    getJob().info("Local database "+sequenceRelease+" is different (hash="+
                            hash+", size="+filesize+", timestamp="+timestamp+"), downloading from Mascot server");
                    toDownloadDB = true;
                }
                else
                {
                    getJob().info("Local copy of database "+sequenceRelease+" exists, skipping download.");
                }
            }

            if (toDownloadDB)
            {
                getJob().info("Starting download of database "+sequenceRelease+"...");
                iReturn = mascotClient.downloadDB(localDB.getCanonicalPath(),
                        sequenceDB, sequenceRelease, smascotFileHash, nmascotFileSize, nmascotFileTimestamp);
                if (iReturn != 0)
                    throw new IOException("Failed to download " + sequenceDB + " from Mascot server");
                else
                {
                    getJob().info("Database "+sequenceRelease+" downloaded");
                    getJob().info("Saving its checksums...");
                    saveLocalMascotFileHash(localDBHash.getCanonicalPath(),
                            smascotFileHash, nmascotFileSize, nmascotFileTimestamp);
                    getJob().info("Checksums saved.");
                }
            }

            // 2. translate Mascot result file to pep.xml format
            File fileSequenceDatabase = MS2PipelineManager.getLocalMascotFile(dirSequenceRoot.getPath(), sequenceDB, sequenceRelease);

            String exePath = PipelineJobService.get().getExecutablePath("Mascot2XML", "tpp", ver);
            String[] args =
            {
                exePath,
                fileWorkDAT.getName(),
                "-D" + fileSequenceDatabase.getAbsolutePath(),
                "-xml",
                "-notgz",
                "-desc"
                //wch: 2007-05-11
                //     expand the protein id to match X!Tandem output or user who run X! Tandem first
                //     will fail to access protein associated information in mascot run
                //,"-shortid"
            };
            getJob().runSubProcess(new ProcessBuilder(args), wd.getDir());
            
            File fileOutputPepXML = wd.newFile(new FileType(".xml"));
            if (!fileOutputPepXML.renameTo(fileWorkPepXMLRaw))
                throw new IOException("Failed to rename " + fileOutputPepXML + " to " + fileWorkPepXMLRaw);

            WorkDirectory.CopyingResource lock = null;
            try
            {
                lock = wd.ensureCopyingLock();

                mzxml2SearchAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(argsM2S, " "));
                mzxml2SearchAction.addInput(getJobSupport().getSearchSpectraFile(), "mzXML");
                mzxml2SearchAction.addOutput(fileMGF, "MGF", false);

                mascotAction.addInput(fileWorkInputXML, "SearchConfig");
                for (File file : getJobSupport().getSequenceFiles())
                {
                    mascotAction.addInput(file, "FASTA");
                }
                mascotAction.addInput(fileMGF, "MGF");
                mascotAction.addOutput(wd.outputFile(fileWorkDAT), "DAT", false);

                mascot2XMLAction.addInput(wd.outputFile(fileWorkDAT), "DAT");
                mascot2XMLAction.addOutput(wd.outputFile(fileWorkPepXMLRaw), "RawPepXML", true);
                mascot2XMLAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(args));
            }
            finally
            {
                if (lock != null) { lock.release(); }
            }


            wd.discardFile(fileWorkInputXML);
            wd.remove();

            mzxml2SearchAction.addInput(getJobSupport().getSearchSpectraFile(), "mzXML");
            return Arrays.asList(mzxml2SearchAction, mascotAction, mascot2XMLAction);
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
    }

    private String getSequenceDatabase(File datFile) throws IOException
    {
        return getMascotResultEntity(datFile, "parameters", "DB");
    }

    private String getDatabaseRelease(File datFile) throws IOException
    {
        return getMascotResultEntity(datFile, "header", "release");
    }

    private String getMascotResultEntity(File datFile, String mimeName, String tag) throws FileNotFoundException
    {
        // return the sequence database queried against in this search
        final File dat = new File(datFile.getAbsolutePath());

        if (!NetworkDrive.exists(dat))
            throw new FileNotFoundException(datFile.getAbsolutePath() + " not found");

        InputStream datIn = null;
        try
        {
            datIn = new FileInputStream(dat);
        }
        catch (FileNotFoundException e)
        {
            throw e;
        }
        BufferedReader datReader = new BufferedReader(new InputStreamReader(datIn));
        boolean skipParameter = true;
        String mimeNameSubString = "; name=\""+mimeName+"\"";
        String tagEqual=tag+"=";
        String value = null;
        String line = null;
        try
        {
            while (null != (line = datReader.readLine()))
            {
                // TODO: check for actual MIME boundary
                if (line.startsWith("Content-Type: "))
                {
                    skipParameter = !line.endsWith(mimeNameSubString);
                }
                else
                {
                    if (!skipParameter && line.startsWith(tagEqual))
                    {
                        value = line.substring(tagEqual.length());
                        break;
                    }
                }
            }
        }
        catch (IOException e)
        {
            // fail to readLine!
        }
        finally
        {
            try
            {
                datReader.close();
            }
            catch (IOException e)
            {
            }
        }
        return value;
    }

    private Map<String,String> readLocalMascotFileHash(String filepath)
    {
        final File hashFile = new File(filepath);

        Map<String,String> returns=new HashMap<String,String>();

        if (hashFile.exists()) {
            InputStream datIn = null;
            try
            {
                datIn = new FileInputStream(hashFile);
                InputStream in = new BufferedInputStream(datIn);

                Properties results=new Properties();
                try
                {
                    results.load(in);
                }
                catch (IOException e)
                {
                    getJob().warn("Fail to load database information " + filepath);
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

                for(Map.Entry<Object,Object> entry: results.entrySet()) {
                    returns.put((String)entry.getKey(),(String)entry.getValue());
                }
            }
            catch (FileNotFoundException e)
            {
                //do nothing
            }
        }

        return returns;
    }

    private boolean saveLocalMascotFileHash(String filepath, String hash, long filesize, long timestamp)
    {
        Properties hashes = new Properties();
        hashes.put(KEY_HASH, hash);
        StringBuffer sb;
        sb=new StringBuffer();
        sb.append(filesize);
        hashes.put(KEY_FILESIZE, sb.toString());
        sb=new StringBuffer();
        sb.append(timestamp);
        hashes.put(KEY_TIMESTAMP, sb.toString());

        final File hashFile = new File(filepath);
        OutputStream datOut = null;
        try
        {
            datOut = new FileOutputStream(hashFile);
        }
        catch (FileNotFoundException e)
        {
            getJob().warn("Fail to open database information " + filepath);
            return false;
        }
        boolean status = false;
        try
        {
            hashes.store(datOut, "");
            status = true;
        }
        catch (IOException e)
        {
            getJob().warn("Fail to save database information " + filepath);
        }
        finally
        {
            try
            {
                datOut.close();
            }
            catch (IOException e)
            {
                getJob().warn("Fail to close database information " + filepath);
            }
        }
        return status;
    }
}
