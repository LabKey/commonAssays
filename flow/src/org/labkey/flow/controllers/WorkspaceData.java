/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

package org.labkey.flow.controllers;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.IWorkspace;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.persist.AnalysisSerializer;
import org.springframework.validation.Errors;
import org.xml.sax.SAXParseException;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WorkspaceData implements Serializable
{
    static final private Logger _log = Logger.getLogger(WorkspaceData.class);

    String path;
    String name;
    String originalPath;
    IWorkspace _object;
    // UNDONE: Placeholder for when analysis archives (or ACS archives) include FCS files during import.
    boolean _includesFCSFiles;

    public void setPath(String path)
    {
        if (path != null)
        {
            path = PageFlowUtil.decode(path);
            this.path = path;
            this.name = new File(path).getName();
        }
    }

    public String getPath()
    {
        return path;
    }

    public void setOriginalPath(String path)
    {
        if (path != null)
        {
            path = PageFlowUtil.decode(path);
            this.originalPath = path;
        }
    }

    public String getOriginalPath()
    {
        return this.originalPath;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public void setObject(String object) throws Exception
    {
        this._object = (IWorkspace) PageFlowUtil.decodeObject(object);
    }

    public IWorkspace getWorkspaceObject()
    {
        return _object;
    }

    public boolean isIncludesFCSFiles()
    {
        return _includesFCSFiles;
    }

    public void setIncludesFCSFiles(boolean includesFCSFiles)
    {
        _includesFCSFiles = includesFCSFiles;
    }

    public void validate(Container container, Errors errors, HttpServletRequest request)
    {
        try
        {
            validate(container);
        }
        catch (FlowException | WorkspaceValidationException ex)
        {
            errors.reject(null, ex.getMessage());
        }
        catch (Exception ex)
        {
            errors.reject(null, ex.getMessage());
            ExceptionUtil.decorateException(ex, ExceptionUtil.ExceptionInfo.ExtraMessage, "name: " + this.name + ", path: " + this.path, true);
            ExceptionUtil.logExceptionToMothership(request, ex);
        }
    }

    public void validate(Container container) throws WorkspaceValidationException, IOException
    {
        if (_object == null)
        {
            if (path != null)
            {
                PipeRoot pipeRoot;
                try
                {
                    pipeRoot = PipelineService.get().findPipelineRoot(container);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("An error occurred trying to retrieve the pipeline root: " + e, e);
                }

                if (pipeRoot == null)
                {
                    throw new WorkspaceValidationException("There is no pipeline root in this folder.");
                }

                File file = pipeRoot.resolvePath(path);
                if (file == null)
                {
                    throw new WorkspaceValidationException("The path '" + path + "' is invalid.");
                }
                if (!file.exists())
                {
                    throw new WorkspaceValidationException("The file '" + path + "' does not exist.");
                }
                if (!file.canRead())
                {
                    throw new WorkspaceValidationException("The file '" + path + "' is not readable.");
                }

                if (file.getName().endsWith(AnalysisSerializer.STATISTICS_FILENAME))
                {
                    // Set path to parent directory
                    file = file.getParentFile();
                    this.path = pipeRoot.relativePath(file);
                }
                else if (path.endsWith(".zip"))
                {
                    // Extract external analysis zip into pipeline
                    File tempDir = pipeRoot.resolvePath(PipelineService.UNZIP_DIR);
                    if (tempDir.exists() && !FileUtil.deleteDir(tempDir))
                        throw new IOException("Failed to delete temp directory");

                    String originalPath = path;
                    File zipFile = pipeRoot.resolvePath(path);
                    file = AnalysisSerializer.extractArchive(zipFile, tempDir);

                    String workspacePath = pipeRoot.relativePath(file);
                    this.path = workspacePath;
                    this.originalPath = originalPath;
                }

                _object = readWorkspace(file, path);
            }
            else
            {
                throw new WorkspaceValidationException("No workspace file was specified.");
            }
        }
    }

    private static IWorkspace readWorkspace(File file, String path) throws WorkspaceValidationException
    {
        try
        {
            if (file.isDirectory() && new File(file, AnalysisSerializer.STATISTICS_FILENAME).isFile())
            {
                return AnalysisSerializer.readAnalysis(file);
            }
            else
            {
                return Workspace.readWorkspace(file);
            }
        }
        catch (IOException e)
        {
            throw new WorkspaceValidationException("Unable to load analysis for '" + path + "': " + e.getMessage(), e);
        }
    }

    public Map<String, String> getHiddenFields()
    {
        if (path != null)
        {
            Map<String, String> ret = new HashMap<>();
            ret.put("path", path);
            if (originalPath != null)
                ret.put("originalPath", originalPath);
            return ret;
        }
        else
        {
            Map<String, String> ret = new HashMap<>();
            if (_object != null)
            {
                try
                {
                    ret.put("object", PageFlowUtil.encodeObject(_object));
                }
                catch (IOException e)
                {
                    throw UnexpectedException.wrap(e);
                }
                ret.put("name", name);

            }
            return ret;
        }
    }

    public static class WorkspaceValidationException extends Exception
    {
        public WorkspaceValidationException()
        {
            super();
        }

        public WorkspaceValidationException(String message)
        {
            super(message);
        }

        public WorkspaceValidationException(String message, Throwable cause)
        {
            super(message, cause);
        }

        public WorkspaceValidationException(Throwable cause)
        {
            super(cause);
        }
    }
}
