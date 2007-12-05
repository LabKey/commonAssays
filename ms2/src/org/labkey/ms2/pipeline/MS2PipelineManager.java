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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

/**
 * MS2PipelineManager class
 * <p/>
 * Created: Sep 21, 2005
 *
 * @author bmaclean
 */
public class MS2PipelineManager
{
    private static Logger _log = Logger.getLogger(MS2PipelineProvider.class);
    protected static String _pipelineDBDir = "databases";

    protected static String _pipelineMzXMLExt = ".mzXML";
    protected static String _pipelineThermoRawExt = ".RAW";
    protected static String _pipelineWatersRawExt = ".raw";
    protected static String _pipelineDataAnnotationExt = ".xar.xml";
    protected static String _pipelineSearchExperimentExt = ".search.xar.xml";
    protected static String _pipelineLogExt = ".log";
    protected static String _pipelineStatusExt = ".status";
    protected static String _pipelineWorkExt = ".work";

    private static final String ALLOW_SEQUENCE_DB_UPLOAD_KEY = "allowSequenceDbUpload";
    public static final String SEQUENCE_DB_ROOT_TYPE = "SEQUENCE_DATABASE";

    //todo this the right way
    public static String _allFractionsMzXmlFileBase = "all";

    public static File getMzXMLFile(File dirData, String baseName)
    {
        return new File(dirData, baseName + _pipelineMzXMLExt);
    }

    public static boolean isMzXMLFile(File file)
    {
        return file.getName().endsWith(_pipelineMzXMLExt);
    }

    public static boolean isThermoRawFile(File file)
    {
        return file.getName().endsWith(_pipelineThermoRawExt) && file.isFile();
    }

    public static boolean isWatersRawDir(File file)
    {
        return file.getName().endsWith(_pipelineWatersRawExt) && file.isDirectory();
    }

    public static File getAnnotationFile(File dirData, String baseName)
    {
        File xarDir = new File(dirData, "xars");
        return new File(xarDir, baseName + _pipelineDataAnnotationExt);
    }

    public static File getLegacyAnnotationFile(File dirData, String baseName)
    {
        return new File(dirData, baseName + _pipelineDataAnnotationExt);
    }

    public static File getSearchExperimentFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineSearchExperimentExt);
    }

    public static boolean isSearchExperimentFile(File file)
    {
        return file.getName().toLowerCase().endsWith(_pipelineSearchExperimentExt);
    }

    public static File getStatusFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineStatusExt);
    }

    public static File getLogFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineLogExt);
    }

    public static File getLogFile(URI uriRoot, String logPath)
    {
        return new File(uriRoot.resolve(logPath));
    }

    public static String getDataDescription(File dirData, String baseName, String protocolName)
    {
        String dataName = "";
        if (dirData != null)
        {
            dataName = dirData.getName();
            if ("xml".equals(dataName))
            {
                dirData = dirData.getParentFile();
                if (dirData != null)
                    dataName = dirData.getName();
            }
        }

        StringBuffer description = new StringBuffer(dataName);
        if (baseName != null && !_allFractionsMzXmlFileBase.equals(baseName))
        {
            if (description.length() > 0)
                description.append("/");
            description.append(baseName);
        }
        description.append(" (").append(protocolName).append(")");
        return description.toString();
    }

    public static class UploadFileFilter extends PipelineProvider.FileEntryFilter
    {
        public boolean accept(File file)
        {
            if (MascotSearchTask.isNativeOutputFile(file))
                return true;

            if (TPPTask.isPepXMLFile(file))
            {
                File parent = file.getParentFile();
                String basename = FileUtil.getBaseName(file, 2);
                return !fileExists(TPPTask.getProtXMLFile(parent, basename)) &&
                        !fileExists(TPPTask.getProtXMLIntermediatFile(parent, basename)) &&
                        !fileExists(getSearchExperimentFile(parent, basename));
            }

            return false;
        }
    }

    public static PipelineProvider.FileEntryFilter getUploadFilter()
    {
        return new UploadFileFilter();
    }

    public static class AnalyzeFileFilter extends PipelineProvider.FileEntryFilter
    {
        public boolean accept(File file)
        {
            // Show all mzXML files.
            if (isMzXMLFile(file))
                return true;

            // If no corresponding mzXML file, show raw files.
            if (isThermoRawFile(file) || isWatersRawDir(file))
            {
                String basename = FileUtil.getBaseName(file);
                File dirData = file.getParentFile();
                if (!fileExists(getMzXMLFile(dirData, basename)))
                    return true;
            }

            return false;
        }
    }

    public static PipelineProvider.FileEntryFilter getAnalyzeFilter()
    {
        return new AnalyzeFileFilter();
    }

    public static File createWorkingDirectory(File dirAnalysis, String baseName) throws IOException
    {
        File dirWork = new File(dirAnalysis, baseName + _pipelineWorkExt);
        if (dirWork.exists())
        {
            if (!FileUtil.deleteDirectoryContents(dirWork))
                throw new IOException("Failed to clean up existing working directory " + dirWork);
        }
        else
        {
            if (!dirWork.mkdir())
                throw new IOException("Failed to create working directory " + dirWork);
        }
        return dirWork;
    }

    public static void removeWorkingDirectory(File dirRemove) throws IOException
    {
        if (!dirRemove.delete())
            throw new IOException("Failed to remove working directory " + dirRemove);
    }

    public static void moveWorkToParent(File fileWork) throws IOException
    {
        File fileDest = new File(fileWork.getParentFile().getParentFile(), fileWork.getName());
        moveWorkFile(fileDest, fileWork);
    }

    public static void moveWorkFile(File fileDest, File fileWork) throws IOException
    {
        if (!fileWork.renameTo(fileDest))
            throw new IOException("Failed to move file " + fileWork + " to " + fileDest);
    }

    public static void removeWorkFile(File fileRemove) throws IOException
    {
        if (fileRemove.exists() && !fileRemove.delete())
            throw new IOException("Failed to remove file " + fileRemove);
    }

    public static Map<String, String[]> addSequenceDBNames(File dir, String path, Map<String, String[]> m)
    {
        File[] dbFiles = dir.listFiles(new FileFilter()
            {
                public boolean accept(File f)
                {
                    final String name = f.getName();
                    //added filters for Sequest indexed databases
                    return !(name.startsWith(".") ||
                            name.endsWith(".check") ||
                            name.endsWith(".out") ||
                            name.endsWith(".idx") ||
                            name.endsWith(".dgt") ||
                            name.endsWith(".log"));
                }
            });

        if (dbFiles == null)
            return m;
        Arrays.sort(dbFiles, new Comparator<File>()
        {
            public int compare(File f1, File f2)
            {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        ArrayList<String> listNames = new ArrayList<String>();
        ArrayList<File> listSubdirs = new ArrayList<File>();
        for (File dbFile : dbFiles)
        {
            if (dbFile.isDirectory())
                listSubdirs.add(dbFile);
            else
                listNames.add(dbFile.getName());
        }

        if (listNames.size() > 0)
            m.put(path, listNames.toArray(new String[listNames.size()]));

        for (File subdir : listSubdirs)
        {
             addSequenceDBNames(subdir, path + subdir.getName() + "/", m);
        }

        return m;
    }

    public static void addSequenceDB(Container container, String name, BufferedReader reader) throws IOException, SQLException
    {
        URI uriSequenceRoot = getSequenceDatabaseRoot(container);
        if (uriSequenceRoot == null)
        {
            throw new IllegalArgumentException("Sequence root directory is not set.");
        }
        File fileDB = getSequenceDBFile(uriSequenceRoot, name);
        if (fileDB.exists())
        {
            throw new IllegalArgumentException("The sequence database '" + name +
                    "' already exists. Please choose another name.");
        }

        File dirDB = fileDB.getParentFile();
        if (!dirDB.exists())
            dirDB.mkdirs();

        BufferedWriter writer = null;
        try
        {
            String line;
            writer = new BufferedWriter(new FileWriter(fileDB));
            while ((line = reader.readLine()) != null)
            {
                /* @todo: check FASTA file format, and throw exception, if bad. */
                writer.write(line, 0, line.length());
                writer.write("\n", 0, 1);
            }
        }
        catch (IOException eio)
        {
            _log.error("Error writing sequence database.", eio);
            throw eio;
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException eio)
                {
                    _log.error("Error writing sequence database.", eio);
                    throw eio;
                }
            }
        }
    }

    public static boolean removeSequenceDB(URI uriSequenceRoot, String name)
    {
        File fileDB = getSequenceDBFile(uriSequenceRoot, name);
        return fileDB.delete();
    }

    public static File getSequenceDBFile(URI uriSequenceRoot, String name)
    {
        if (uriSequenceRoot == null)
            throw new IllegalArgumentException("Invalid sequence root directory.");
        File fileRoot = new File(uriSequenceRoot);
        File file = new File(uriSequenceRoot.getPath() + name);
        if (!file.getAbsolutePath().startsWith(fileRoot.getAbsolutePath()))
            throw new IllegalArgumentException("Invalid sequence database '" + name + "'.");

        return file;
    }


    public static URI getSequenceDatabaseRoot(Container container) throws SQLException
    {
        URI dbRoot = PipelineService.get().getPipelineRootSetting(container, SEQUENCE_DB_ROOT_TYPE);
        if (dbRoot == null)
        {
            // return default root
            URI root = PipelineService.get().getPipelineRootSetting(container);
            if (root != null)
                dbRoot = new File(root.getPath(), _pipelineDBDir).toURI();
        }
        return dbRoot;
    }

    public static void setSequenceDatabaseRoot(User user, Container container, URI rootSeq, boolean allowUpload) throws SQLException
    {
        PipelineService service = PipelineService.get();

        // If the new root is just the default, then clear the entry.
        URI root = service.getPipelineRootSetting(container);
        if (rootSeq != null && root != null && rootSeq.equals(new File(root.getPath(), _pipelineDBDir).toURI()))
             rootSeq = null;

        service.setPipelineRoot(user, container, rootSeq, SEQUENCE_DB_ROOT_TYPE);
        if (root != null)
            service.setPipelineProperty(container, ALLOW_SEQUENCE_DB_UPLOAD_KEY, allowUpload ? "true" : "false");
        else
            service.setPipelineProperty(container, ALLOW_SEQUENCE_DB_UPLOAD_KEY, null);
    }

    public static boolean allowSequenceDatabaseUploads(User user, Container container) throws SQLException
    {
        if (!container.hasPermission(user, ACL.PERM_INSERT))
            return false;
        Object obj = PipelineService.get().getPipelineProperty(container, ALLOW_SEQUENCE_DB_UPLOAD_KEY);
        return Boolean.parseBoolean((String) obj);
    }

    public static File getLocalMascotFile(String sequenceRoot, String db, String release)
    {
        return new File(sequenceRoot+File.separator+"mascot"+File.separator+db, release);
    }

    public static File getLocalMascotFileHash(String sequenceRoot, String db, String release)
    {
        return new File(sequenceRoot+File.separator+"mascot"+File.separator+db, release+".hash");
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
                    knownFiles.addAll(Arrays.asList(files));
                checkedDirectories.add(parent);
            }
            return knownFiles.contains(file);
        }
        return file.exists();
    }

    public static Map<File, FileStatus> getAnalysisFileStatus(File dirData, File dirAnalysis, Container c) throws IOException
    {
        Set<File> knownFiles = new HashSet<File>();
        Set<File> checkedDirectories = new HashSet<File>();
        
        String dirDataURL = dirData.toURI().toURL().toString();
        File[] mzXMLFiles = dirData.listFiles(getAnalyzeFilter());

        Map<File, FileStatus> analysisMap = new LinkedHashMap<File, FileStatus>();
        if (mzXMLFiles != null && mzXMLFiles.length > 0)
        {
            Arrays.sort(mzXMLFiles, new Comparator<File>()
            {
                public int compare(File o1, File o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            if (dirAnalysis != null && !NetworkDrive.exists(dirAnalysis))
                dirAnalysis = null;

            boolean all = exists(getLogFile(dirAnalysis, _allFractionsMzXmlFileBase), knownFiles, checkedDirectories);
            boolean allComplete = all && exists(getSearchExperimentFile(dirAnalysis, _allFractionsMzXmlFileBase), knownFiles, checkedDirectories);

            ExpData[] allContainerDatas = null;

            for (File file : mzXMLFiles)
            {
                FileStatus status = FileStatus.UNKNOWN;
                String baseName = FileUtil.getBaseName(file);
                if (dirAnalysis != null)
                {
                    if (allComplete ||
                            (!all && exists(getSearchExperimentFile(dirAnalysis, baseName), knownFiles, checkedDirectories)))
                        status = FileStatus.COMPLETE;
                    else if (exists(getLogFile(dirAnalysis, baseName), knownFiles, checkedDirectories))
                        status = FileStatus.RUNNING;
                }
                if (status == FileStatus.UNKNOWN)
                {
                    if (findAnnotationFile(file, knownFiles, checkedDirectories, baseName) != null)
                    {
                        status = FileStatus.ANNOTATED;
                    }
                }
                if (status == FileStatus.UNKNOWN)
                {
                    try
                    {
                        String fileURL;
                        try
                        {
                            URI localURI = new URI("file", null, "/" + file.getName(), null);
                            // Strip off the leading "file:/" to get just the encoded file name
                            fileURL = dirDataURL + localURI.toString().substring("file:/".length());
                        }
                        catch (URISyntaxException e)
                        {
                            throw new IllegalStateException(e);
                        }
                        if (allContainerDatas == null)
                        {
                            allContainerDatas = ExperimentService.get().getExpData(c);
                        }
                        for (ExpData data : allContainerDatas)
                        {
                            if (fileURL.equals(data.getDataFileUrl()))
                            {
                                if (data.getRun() != null)
                                {
                                    status = FileStatus.ANNOTATED;
                                }
                                break;
                            }
                        }
                    }
                    catch (SQLException e)
                    {
                        throw new RuntimeSQLException(e);
                    }
                }
                analysisMap.put(file, status);
            }
        }
        return analysisMap;
    }

    public static File findAnnotationFile(File file)
    {
        return findAnnotationFile(file, new HashSet<File>(), new HashSet<File>());
    }

    public static File findAnnotationFile(File file, Set<File> knownFiles, Set<File> checkedDirectories)
    {
        return findAnnotationFile(file, knownFiles, checkedDirectories, FileUtil.getBaseName(file));
    }

    private static File findAnnotationFile(File file, Set<File> knownFiles, Set<File> checkedDirectories, String baseName)
    {
        File dirData = file.getParentFile();
        File annotationFile = getAnnotationFile(dirData, baseName);
        if (exists(annotationFile, knownFiles, checkedDirectories))
        {
            return annotationFile;
        }
        annotationFile = getAnnotationFile(dirData, _allFractionsMzXmlFileBase);
        if (exists(annotationFile, knownFiles, checkedDirectories))
        {
            return annotationFile;
        }
        annotationFile =getLegacyAnnotationFile(dirData, baseName);
        if (exists(annotationFile, knownFiles, checkedDirectories))
        {
            return annotationFile;
        }
        annotationFile = getLegacyAnnotationFile(dirData, _allFractionsMzXmlFileBase);
        if (exists(annotationFile, knownFiles, checkedDirectories))
        {
            return annotationFile;
        }
        return null;
    }

    public static File[] getAnalysisFiles(File dirData, File dirAnalysis, FileStatus status, Container c) throws IOException
    {
        Map<File, FileStatus> mzXMLFileStatus = getAnalysisFileStatus(dirData, dirAnalysis, c);
        List<File> fileList = new ArrayList<File>();
        for (File fileMzXML : mzXMLFileStatus.keySet())
        {
            if (status == null || status.equals(mzXMLFileStatus.get(fileMzXML)))
                fileList.add(fileMzXML);
        }
        return fileList.toArray(new File[fileList.size()]);
    }
}
