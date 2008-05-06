package org.labkey.flow.controllers;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXParseException;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WorkspaceData
{
    static final private Logger _log = Logger.getLogger(WorkspaceData.class);
    String path;
    MultipartFile file;
    FlowJoWorkspace object;
    String name;

    public void setPath(String path)
    {
        this.path = path;
        this.name = new File(path).getName();
    }

    public void setFile(MultipartFile file)
    {
        this.file = file;
        this.name = file.getOriginalFilename();
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
        this.object = (FlowJoWorkspace) PageFlowUtil.decodeObject(object);
    }

    public MultipartFile getFile()
    {
        return file;
    }

    public FlowJoWorkspace getWorkspaceObject()
    {
        return object;
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
        catch (RuntimeException ex)
        {
            errors.reject(null, ex.getMessage());
            ExceptionUtil.logExceptionToMothership(request, ex);
        }
    }

    public void validate(Container container) throws WorkspaceValidationException
    {
        if (object == null)
        {
            if (path != null)
            {
                PipeRoot pipeRoot = null;
                try
                {
                    pipeRoot = PipelineService.get().findPipelineRoot(container);
                    if (pipeRoot == null)
                    {
                        throw new WorkspaceValidationException("There is no pipeline root in this folder.");
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException("An error occurred trying to retrieve the pipeline root: " + e, e);
                }
                if (pipeRoot != null)
                {
                    File file = pipeRoot.resolvePath(path);
                    if (file == null)
                    {
                        throw new WorkspaceValidationException("The path '" + path + "' is invalid.");
                    }
                    else
                    {
                        if (!file.exists())
                        {
                            throw new WorkspaceValidationException("The file '" + path + "' does not exist.");
                        }
                        else if (file.isDirectory())
                        {
                            throw new WorkspaceValidationException("The file '" + path + "' is a directory.");
                        }
                        else
                        {
                            try
                            {
                                FileInputStream is = new FileInputStream(file);
                                try
                                {
                                    object = FlowJoWorkspace.readWorkspace(is);
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
                            catch (FileNotFoundException fnfe)
                            {
                                _log.error("Error", fnfe);
                                throw new WorkspaceValidationException("Unable to access the file '" + path + "'.");
                            }
                        }
                    }
                }

            }
            else
            {
                if (file != null && !file.isEmpty())
                {
                    try
                    {
                        object = FlowJoWorkspace.readWorkspace(file.getInputStream());
                    }
                    catch (SAXParseException spe)
                    {
                        throw new WorkspaceValidationException("Error parsing the workspace.  This might be because it is not an " +
                                "XML document: " + spe);
                    }
                    catch (Exception e)
                    {
                        _log.error("Error parsing workspace: " + e);
                        throw new WorkspaceValidationException("Error parsing workspace: " + e);
                    }
                }
                else
                {
                    throw new WorkspaceValidationException("No workspace file was specified.");
                }
            }
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
            Map<String, String> ret = new HashMap();
            if (object != null)
            {
                try
                {
                    ret.put("object", PageFlowUtil.encodeObject(object));
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
