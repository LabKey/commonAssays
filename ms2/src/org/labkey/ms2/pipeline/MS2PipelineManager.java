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
import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.cmd.ConvertTaskId;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.mascot.MascotSearchTask;

import java.io.*;
import java.net.URI;
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

    private static final String ALLOW_SEQUENCE_DB_UPLOAD_KEY = "allowSequenceDbUpload";
    public static final String SEQUENCE_DB_ROOT_TYPE = "SEQUENCE_DATABASE";

    public static final TaskId MZXML_CONVERTER_TASK_ID = new TaskId(ConvertTaskId.class, "mzxmlConverter");
    
    public static boolean isMzXMLFile(File file)
    {
        return AbstractMS2SearchProtocol.FT_MZXML.isType(file);
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

        service.setPipelineRoot(user, container, SEQUENCE_DB_ROOT_TYPE, null, false, rootSeq);
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
