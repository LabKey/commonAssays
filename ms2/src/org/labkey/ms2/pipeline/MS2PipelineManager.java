/*
 * Copyright (c) 2005-2010 LabKey Corporation
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
import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.cmd.ConvertTaskId;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.mascot.MascotSearchTask;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

/**
 * MS2PipelineManager class
 * <p/>
 * This whole class should probably go away, and be moved onto
 * <code>AbstractMS2SearchPipelineJob</code> or somewhere similar.
 * <p/>
 * Created: Sep 21, 2005
 *
 * @author bmaclean
 */
public class MS2PipelineManager
{
    private static Logger _log = Logger.getLogger(MS2PipelineProvider.class);
    private static final String DEFAULT_FASTA_DIR = "databases";

    protected static String _pipelineDataAnnotationExt = ".xar.xml";

    private static final String ALLOW_SEQUENCE_DB_UPLOAD_KEY = "allowSequenceDbUpload";
    public static final String SEQUENCE_DB_ROOT_TYPE = "SEQUENCE_DATABASE";

    public static final TaskId MZXML_CONVERTER_TASK_ID = new TaskId(ConvertTaskId.class, "mzxmlConverter");
    
    //todo this the right way
    public static String _allFractionsMzXmlFileBase = "all";

    public static boolean isMzXMLFile(File file)
    {
        return AbstractMS2SearchProtocol.FT_MZXML.isType(file);
    }

    public static File getAnnotationFile(File dirData)
    {
        return getAnnotationFile(dirData, _allFractionsMzXmlFileBase);
    }

    public static File getAnnotationFile(File dirData, String baseName)
    {
        File xarDir = new File(dirData, "xars");
        return new File(xarDir, baseName + _pipelineDataAnnotationExt);
    }

    public static File getLegacyAnnotationFile(File dirData)
    {
        return getLegacyAnnotationFile(dirData, _allFractionsMzXmlFileBase);
    }

    public static File getLegacyAnnotationFile(File dirData, String baseName)
    {
        return new File(dirData, baseName + _pipelineDataAnnotationExt);
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
                String basename = TPPTask.FT_PEP_XML.getBaseName(file);
                return !fileExists(TPPTask.getProtXMLFile(parent, basename)) &&
                        !fileExists(AbstractMS2SearchProtocol.FT_SEARCH_XAR.newFile(parent, basename));
            }

            return false;
        }
    }

    public static PipelineProvider.FileEntryFilter getUploadFilter()
    {
        return new UploadFileFilter();
    }

    public static PipelineProvider.FileEntryFilter getAnalyzeFilter()
    {
        TaskFactory factory = PipelineJobService.get().getTaskFactory(MZXML_CONVERTER_TASK_ID);
        if (factory != null)
            return new PipelineProvider.FileTypesEntryFilter(factory.getInputTypes());

        return new PipelineProvider.FileEntryFilter()
            {
                public boolean accept(File f)
                {
                    return isMzXMLFile(f);
                }
            };
    }

    public static List<String> getSequenceDirList(File dir, String path)
    {
        File[] dbFiles = dir.listFiles(new SequenceDbFileFilter());

        if (dbFiles == null)
            return null;

        ArrayList<String> dirList = new ArrayList<String>();

        for (File dbFile : dbFiles)
        {
            if (dbFile.isDirectory())
                dirList.add(path + dbFile.getName() + "/");
            else
                dirList.add(dbFile.getName());
        }

        return dirList;
    }

    public static List<String> addSequenceDbPaths(File dir, String path, List<String> m)
    {
        File[] dbFiles = dir.listFiles(new SequenceDbFileFilter());

        if (dbFiles == null)
            return null;
        ArrayList<File> listSubdirs = new ArrayList<File>();
        int fileCount  = 0;
        for (File dbFile : dbFiles)
        {
            if (dbFile.isDirectory())
            {
                listSubdirs.add(dbFile);
                m.add(path + dbFile.getName() + "/");
            }
            else
            {
              fileCount++;
            }
        }
        if(fileCount == 0)
        {
            m.remove(path);
        }
        for (File subdir : listSubdirs)
        {
             addSequenceDbPaths(subdir, path + subdir.getName() + "/", m);
        }
        return m;
    }

    public static void addSequenceDB(Container container, String name, BufferedReader reader) throws IOException, SQLException
    {
        File sequenceRoot = getSequenceDatabaseRoot(container);
        if (sequenceRoot == null)
        {
            throw new IllegalArgumentException("Sequence root directory is not set.");
        }
        File fileDB = getSequenceDBFile(sequenceRoot, name);
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

    public static File getSequenceDBFile(File fileRoot, String name)
    {
        if (fileRoot == null)
            throw new IllegalArgumentException("Invalid sequence root directory.");
        File file = new File(fileRoot, name);
        if (!file.getAbsolutePath().startsWith(fileRoot.getAbsolutePath()))
            throw new IllegalArgumentException("Invalid sequence database '" + name + "'.");

        return file;
    }


    public static File getSequenceDatabaseRoot(Container container)
    {
        PipeRoot dbRoot = PipelineService.get().getPipelineRootSetting(container, SEQUENCE_DB_ROOT_TYPE);
        if (dbRoot == null)
        {
            // return default root
            PipeRoot root = PipelineService.get().getPipelineRootSetting(container);
            if (root != null)
            {
                File file = getSequenceDatabaseRoot(root);
                if (!NetworkDrive.exists(file) && NetworkDrive.exists(file.getParentFile()))
                {
                    // Try to create it if it doesn't exist
                    file.mkdir();
                }
                if (NetworkDrive.exists(file))
                {
                    return file;
                }
            }
            return null;
        }
        return dbRoot.getRootPath();
    }

    public static void setSequenceDatabaseRoot(User user, Container container, URI rootSeq, boolean allowUpload) throws SQLException
    {
        PipelineService service = PipelineService.get();

        // If the new root is just the default, then clear the entry.
        PipeRoot root = service.getPipelineRootSetting(container);
        if (rootSeq != null && root != null && rootSeq.equals(getSequenceDatabaseRoot(root).toURI()))
             rootSeq = null;

        service.setPipelineRoot(user, container, rootSeq, SEQUENCE_DB_ROOT_TYPE, null, false);
        if (root != null)
            service.setPipelineProperty(container, ALLOW_SEQUENCE_DB_UPLOAD_KEY, allowUpload ? "true" : "false");
        else
            service.setPipelineProperty(container, ALLOW_SEQUENCE_DB_UPLOAD_KEY, null);
    }

    private static File getSequenceDatabaseRoot(PipeRoot root)
    {
        return root.resolvePath(DEFAULT_FASTA_DIR);
    }

    public static boolean allowSequenceDatabaseUploads(User user, Container container) throws SQLException
    {
        if (!container.hasPermission(user, InsertPermission.class))
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
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });

            if (dirAnalysis != null && !NetworkDrive.exists(dirAnalysis))
                dirAnalysis = null;

            String baseNameDataSet = AbstractFileAnalysisProtocol.getDataSetBaseName(dirData);
            File fileDataSetLog = PipelineJob.FT_LOG.newFile(dirAnalysis, baseNameDataSet);
            boolean all = exists(fileDataSetLog, knownFiles, checkedDirectories);
            boolean allComplete = all;
            if (allComplete)
            {
                File fileDataSetXar = AbstractMS2SearchProtocol.FT_SEARCH_XAR.newFile(dirAnalysis, baseNameDataSet);
                allComplete = exists(fileDataSetXar, knownFiles, checkedDirectories);
            }

            ExpData[] allContainerDatas = null;

            for (File file : mzXMLFiles)
            {
                FileStatus status = FileStatus.UNKNOWN;
                String baseName = FileUtil.getBaseName(file);
                if (dirAnalysis != null)
                {
                    if (allComplete ||
                            (!all && exists(AbstractMS2SearchProtocol.FT_SEARCH_XAR.newFile(dirAnalysis, baseName), knownFiles, checkedDirectories)))
                        status = FileStatus.COMPLETE;
                    else if (exists(PipelineJob.FT_LOG.newFile(dirAnalysis, baseName), knownFiles, checkedDirectories))
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
        annotationFile = getAnnotationFile(dirData);
        if (exists(annotationFile, knownFiles, checkedDirectories))
        {
            return annotationFile;
        }
        annotationFile =getLegacyAnnotationFile(dirData, baseName);
        if (exists(annotationFile, knownFiles, checkedDirectories))
        {
            return annotationFile;
        }
        annotationFile = getLegacyAnnotationFile(dirData);
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

    private static class SequenceDbFileFilter implements FileFilter
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
                    name.endsWith(".log") ||
                    name.endsWith(".hdr") ||
                    name.endsWith(".hash"));
        }
    }
}
