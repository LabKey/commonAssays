/*
 * Copyright (c) 2007-2012 LabKey Corporation
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
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.model.Workspace;
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
    Workspace _object;

    public void setPath(String path)
    {
        if (path != null)
        {
            path = PageFlowUtil.decode(path);
            this.path = path;
            this.name = new File(path).getName();
        }
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
        this._object = (Workspace) PageFlowUtil.decodeObject(object);
    }

    public Workspace getWorkspaceObject()
    {
        return _object;
    }

    public String getPath()
    {
        return path;
    }

    public void validate(Container container, Errors errors, HttpServletRequest request)
    {
        try
        {
            validate(container);
        }
        catch (WorkspaceValidationException wve)
        {
            errors.reject(null, wve.getMessage());
        }
        catch (Exception ex)
        {
            errors.reject(null, ex.getMessage());
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
                _object = readWorkspace(file, path);
            }
            else
            {
                throw new WorkspaceValidationException("No workspace file was specified.");
            }
        }
    }

    private static Workspace readWorkspace(File file, String path) throws WorkspaceValidationException
    {
        if (file == null)
        {
            throw new WorkspaceValidationException("The path '" + path + "' is invalid.");
        }
        if (!file.exists())
        {
            throw new WorkspaceValidationException("The file '" + path + "' does not exist.");
        }
        if (file.isDirectory())
        {
            throw new WorkspaceValidationException("The file '" + path + "' is a directory.");
        }
        if (!file.canRead())
        {
            throw new WorkspaceValidationException("The file '" + path + "' is not readable.");
        }

        try
        {
            FileInputStream is = new FileInputStream(file);
            return readWorkspace(is);
        }
        catch (FileNotFoundException fnfe)
        {
            _log.error("Error", fnfe);
            throw new WorkspaceValidationException("Unable to access the file '" + path + "'.");
        }
    }

    private static Workspace readWorkspace(InputStream is) throws WorkspaceValidationException
    {
        try
        {
            return Workspace.readWorkspace(is);
        }
        catch (SAXParseException spe)
        {
            throw new WorkspaceValidationException("Error parsing the workspace.  This might be because it is not an " +
                    "XML document: " + spe);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error parsing workspace: " + e, e);
        }
    }

    public Map<String, String> getHiddenFields()
    {
        if (path != null)
        {
            return Collections.singletonMap("path", path);
        }
        else
        {
            Map<String, String> ret = new HashMap<String, String>();
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
