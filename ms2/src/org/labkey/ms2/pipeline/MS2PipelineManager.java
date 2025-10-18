/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.cmd.ConvertTaskId;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.Path;
import org.labkey.api.view.NotFoundException;
import org.labkey.ms2.pipeline.mascot.MascotSearchTask;
import org.labkey.vfs.FileSystemLike;

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
    private static final Logger _log = LogManager.getLogger(MS2PipelineProvider.class);
    private static final String DEFAULT_FASTA_DIR = "databases";

    public static final String SEQUENCE_DB_ROOT_TYPE = "SEQUENCE_DATABASE";

    public static final TaskId MZXML_CONVERTER_TASK_ID = new TaskId(ConvertTaskId.class, "mzxmlConverter");
    
    public static boolean isMzXMLFile(File file)
    {
        return AbstractMS2SearchProtocol.FT_MZXML.isType(file);
    }

    public static class UploadFileFilter extends PipelineProvider.FileEntryFilter
    {
        @Override
        public boolean accept(File file)
        {
            if (MascotSearchTask.isNativeOutputFile(FileSystemLike.wrapFile(file)))
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
        TaskFactory<?> factory = PipelineJobService.get().getTaskFactory(MZXML_CONVERTER_TASK_ID);
        if (factory != null)
            return new PipelineProvider.FileTypesEntryFilter(factory.getInputTypes());

        return new PipelineProvider.FileEntryFilter()
            {
                @Override
                public boolean accept(File f)
                {
                    return isMzXMLFile(f);
                }
            };
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


    public static File getSequenceDatabaseRoot(Container container, boolean includeParentContainers)
    {
        PipeRoot dbRoot = includeParentContainers ? PipelineService.get().findPipelineRoot(container, SEQUENCE_DB_ROOT_TYPE) : PipelineService.get().getPipelineRootSetting(container, SEQUENCE_DB_ROOT_TYPE);
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
                    try
                    {
                        FileUtil.mkdir(file);
                    }
                    catch (IOException e)
                    {
                        throw new NotFoundException("Could not create database sequence root for " + container.getPath());
                    }
                }
                if (NetworkDrive.exists(file))
                {
                    return file;
                }
            }
            throw new NotFoundException("Could not find database sequence root for " + container.getPath());
        }
        return dbRoot.getRootPath();
    }

    public static void setSequenceDatabaseRoot(User user, Container container, URI rootSeq) throws SQLException
    {
        PipelineService service = PipelineService.get();

        // If the new root is just the default, then clear the entry.
        PipeRoot root = service.getPipelineRootSetting(container);
        if (rootSeq != null && root != null && rootSeq.equals(getSequenceDatabaseRoot(root).toURI()))
             rootSeq = null;

        service.setPipelineRoot(user, container, SEQUENCE_DB_ROOT_TYPE, false, rootSeq);
    }

    private static File getSequenceDatabaseRoot(PipeRoot root)
    {
        return root.resolvePath(DEFAULT_FASTA_DIR);
    }

    public static File getLocalMascotFile(File sequenceRoot, String db, String release)
    {
        return FileUtil.appendPath(sequenceRoot, Path.parse("mascot/" + db + "/" + release));
    }

    public static File getLocalMascotFileHash(File sequenceRoot, String db, String release)
    {
        return FileUtil.appendPath(sequenceRoot, Path.parse("mascot/" + db + "/" + release+".hash"));
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
        @Override
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
