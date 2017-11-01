/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PepXMLFileType;
import org.labkey.ms2.MS2RunType;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.AbstractMS2SearchTaskFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.MS2SearchJobSupport;
import org.labkey.ms2.pipeline.TPPTask;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <code>MascotSearchTask</code>
 */
public class MascotSearchTask extends AbstractMS2SearchTask<MascotSearchTask.Factory>
{
    private static final String KEY_HASH = "HASH";
    private static final String KEY_FILESIZE = "FILESIZE";
    private static final String KEY_TIMESTAMP = "TIMESTAMP";

    private static final FileType FT_MASCOT_DAT = new FileType(".dat");
    private static final FileType FT_MASCOT_MGF = new FileType(".mgf");
    private static final String MZXML2SEARCH_ACTION_NAME = "MzXML2Search";
    private static final String MASCOT_ACTION_NAME = MS2RunType.Mascot.name();
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

        public boolean isJobComplete(PipelineJob job)
        {
            JobSupport support = (JobSupport) job;
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            // Mascot input (MGF) and Mascot native output
            if (!NetworkDrive.exists(getNativeSpectraFile(dirAnalysis, baseName)) ||
                    !NetworkDrive.exists(getNativeOutputFile(dirAnalysis, baseName)))
                return false;

            String baseNameJoined = support.getJoinedBaseName();

            // Fraction roll-up, completely analyzed sample pepXML, or the raw pepXML exist
            return NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseNameJoined)) ||
                   NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseName)) ||
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

    @NotNull
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            // Make a copy so that we can modify the map
            Map<String, String> params = new HashMap<>(getJob().getParameters());

            RecordedAction mzxml2SearchAction = new RecordedAction(MZXML2SEARCH_ACTION_NAME);
            RecordedAction mascotAction = new RecordedAction(MASCOT_ACTION_NAME);
            RecordedAction mascot2XMLAction = new RecordedAction(MASCOT2XML_ACTION_NAME);

            File fileWorkMGF = _wd.newFile(FT_MASCOT_MGF);
            File fileWorkDAT = _wd.newFile(FT_MASCOT_DAT);

            // Mascot starts with remote sequence file names, so it has to look at the
            // raw parameter, rather than using getJob().getSequenceFiles().
            String paramDatabase = params.get("pipeline, database");
            if (paramDatabase == null)
            {
                throw new IOException("Failed parsing Mascot input xml '" + getJobSupport().getParametersFile() + "'.\n" +
                        "Missing required input parameter 'pipeline, database'");
            }

            params.put("pipeline, user name", "LabKey User"); // BUGBUG: should be "pipeline, username" ?

            File fileWorkInputXML = _wd.newFile("input.xml");
            getJobSupport().createParamParser().writeFromMap(params, fileWorkInputXML);

            File fileMzXML = _factory.findInputFile(getJobSupport());
            File fileMGF = new File(_wd.getDir(), fileWorkMGF.getName());

            // 0. pre-Mascot search: c) translate the mzXML file to mgf for Mascot using MzXML2Search
            File fileWorkSpectra = _wd.inputFile(fileMzXML, true);
            ArrayList<String> argsM2S = new ArrayList<>();
            String ver = TPPTask.getTPPVersion(getJob());
            argsM2S.add(PipelineJobService.get().getExecutablePath("MzXML2Search", null, "tpp", ver, getJob().getLogger()));
            argsM2S.add("-mgf");
            String paramMinParent = params.get(MINIMUM_PARENT_M_H);
            if (paramMinParent != null)
                argsM2S.add("-B" + paramMinParent);
            String paramMaxParent = params.get(MAXIMUM_PARENT_M_H);
            if (paramMaxParent != null)
                argsM2S.add("-T" + paramMaxParent);
            String paramMinPeakIntensity = params.get("spectrum, minimum peak intensity");
            if (paramMinPeakIntensity != null)
                argsM2S.add("-I" + paramMinPeakIntensity);
            String paramMinPeakCount = params.get("spectrum, minimum peak count");
            if (paramMinPeakCount != null)
                argsM2S.add("-P" + paramMinPeakCount);
            argsM2S.add(fileWorkSpectra.getAbsolutePath());

            getJob().runSubProcess(new ProcessBuilder(argsM2S), _wd.getDir());

            //  1. perform Mascot search
            getJob().header("mascot client output");

            MascotClientImpl mascotClient = new MascotClientImpl(getJobSupport().getMascotServer(), getJob().getLogger(),
                getJobSupport().getMascotUserAccount(), getJobSupport().getMascotUserPassword());
            mascotClient.setProxyURL(getJobSupport().getMascotHTTPProxy());
            int iReturn = mascotClient.search(fileWorkInputXML.getAbsolutePath(),
                    fileMGF.getAbsolutePath(), fileWorkDAT.getAbsolutePath());
            if (iReturn != 0)
            {
                throw new IOException("Error code " + iReturn + " " + mascotClient.getErrorString());
            }
            if (!fileWorkDAT.exists())
            {
                throw new IOException("Did not get expected results file from Mascot: " + fileWorkDAT);
            }

            getJob().header("Sequence Database Synchronization output");

            //a. get database and release entry
            String sequenceDB = getSequenceDatabase(fileWorkDAT);
            String sequenceRelease = getDatabaseRelease(fileWorkDAT);
            //b. get release information at Mascot server
            getJob().info("Retrieving database information ("+sequenceRelease+")...");
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

            long nmascotFileSize = smascotFileSize == null ? -1 : Long.parseLong(smascotFileSize);
            long nmascotFileTimestamp= smascotFileTimestamp == null ? -1 : Long.parseLong(smascotFileTimestamp);

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
                if (smascotFileHash == null || !smascotFileHash.equals(hash) ||
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
                mascotClient.downloadDB(localDB.getCanonicalPath(),
                        sequenceDB, sequenceRelease, smascotFileHash, nmascotFileSize, nmascotFileTimestamp);

                getJob().info("Database "+sequenceRelease+" downloaded");
                getJob().info("Saving its checksums...");
                saveLocalMascotFileHash(localDBHash.getCanonicalPath(),
                        smascotFileHash, nmascotFileSize, nmascotFileTimestamp);
                getJob().info("Checksums saved.");
            }

            // 2. translate Mascot result file to pep.xml format
            File fileSequenceDatabase = MS2PipelineManager.getLocalMascotFile(dirSequenceRoot.getPath(), sequenceDB, sequenceRelease);
            String exePath = PipelineJobService.get().getExecutablePath("Mascot2XML", null, "tpp", ver, getJob().getLogger());
            String[] args =
            {
                exePath,
                fileWorkDAT.getName(),
                "-D" + fileSequenceDatabase.getAbsolutePath(),
                "-xml",
                "-notgz",     // don't create the tarball of fake .out and .dta
                "-desc"
                //wch: 2007-05-11
                //     expand the protein id to match X!Tandem output or user who run X! Tandem first
                //     will fail to access protein associated information in mascot run
                //,"-shortid"
            };
            getJob().runSubProcess(new ProcessBuilder(args), _wd.getDir());

            PepXMLFileType pepxft = new PepXMLFileType(true); // "true" == accept .xml as valid extension for older converters
            File fileOutputPepXML = _wd.newFile(pepxft);
            File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(_wd.getDir(),
                    getJobSupport().getBaseName(),
                    getJobSupport().getGZPreference());
            // three possibilities: basename.xml, basename.pep.xml, basename.pep.xml.gz
            if (fileOutputPepXML.getName().endsWith(".gz")&&!fileWorkPepXMLRaw.getName().endsWith(".gz"))
            {
                fileWorkPepXMLRaw = new File(fileWorkPepXMLRaw.getParent(),fileWorkPepXMLRaw.getName()+".gz");
            }
            if (!fileOutputPepXML.renameTo(fileWorkPepXMLRaw))
            {
                throw new IOException("Failed to rename " + fileOutputPepXML + " to " + fileWorkPepXMLRaw);
            }

            try (WorkDirectory.CopyingResource lock = _wd.ensureCopyingLock())
            {
                mzxml2SearchAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(argsM2S, " "));
                mzxml2SearchAction.addInput(fileMzXML, SPECTRA_INPUT_ROLE);
                mzxml2SearchAction.addOutput(fileMGF, "MGF", false);

                for (File file : getJobSupport().getSequenceFiles())
                {
                    mascotAction.addInput(file, FASTA_INPUT_ROLE);
                }
                mascotAction.addInput(fileMGF, "MGF");
                mascotAction.addOutput(_wd.outputFile(fileWorkDAT), "DAT", false);

                mascot2XMLAction.addInput(_wd.outputFile(fileWorkDAT), "DAT");
                mascot2XMLAction.addOutput(_wd.outputFile(fileWorkPepXMLRaw), "RawPepXML", true);
                mascot2XMLAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(args));
            }

            _wd.discardFile(fileWorkMGF);
            _wd.discardFile(fileWorkInputXML);

            return new RecordedActionSet(mzxml2SearchAction, mascotAction, mascot2XMLAction);
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
        boolean skipParameter = true;
        String mimeNameSubString = "; name=\""+mimeName+"\"";
        String tagEqual=tag+"=";
        String value = null;
        try
        {
            datIn = new FileInputStream(dat);

            BufferedReader datReader = new BufferedReader(new InputStreamReader(datIn));

            String line;
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
        catch (FileNotFoundException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            // fail to readLine!
        }
        finally
        {
            if (datIn != null) { try { datIn.close(); } catch (IOException e) {} }
        }
        return value;
    }

    private Map<String,String> readLocalMascotFileHash(String filepath)
    {
        final File hashFile = new File(filepath);

        Map<String,String> returns=new HashMap<>();

        if (hashFile.exists()) {
            InputStream datIn;
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
        OutputStream datOut;
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
