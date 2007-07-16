package org.labkey.flow.controllers;

import org.apache.struts.upload.FormFile;
import org.apache.log4j.Logger;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ViewForm;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.data.Container;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

public class WorkspaceData
{
    static final private Logger _log = Logger.getLogger(WorkspaceData.class);
    String path;
    FormFile file;
    FlowJoWorkspace object;
    String name;

    public void setPath(String path)
    {
        this.path = path;
        this.name = new File(path).getName();
    }

    public void setFile(FormFile file)
    {
        this.file = file;
        this.name = new File(file.getFileName()).getName();
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

    public FormFile getFile()
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

    public void validate(ViewForm form)
    {
        Container container = form.getContainer();
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
                        form.addActionError("There is no pipeline root in this folder.");
                    }
                }
                catch (Exception e)
                {
                    ExceptionUtil.logExceptionToMothership(form.getRequest(), e);
                    form.addActionError("An error occurred trying to retrieve the pipeline root: " + e);
                }
                if (pipeRoot != null)
                {
                    File file = pipeRoot.resolvePath(path);
                    if (file == null)
                    {
                        form.addActionError("The path '" + path + "' is invalid.");
                    }
                    else
                    {
                        if (!file.exists())
                        {
                            form.addActionError("The file '" + path + "' does not exist.");
                        }
                        else if (file.isDirectory())
                        {
                            form.addActionError("The file '" + path + "' is a directory.");
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
                                    form.addActionError("Error parsing the workspace.  This might be because it is not an " +
                                            "XML document: " + spe);
                                }
                                catch (Exception e)
                                {
                                    ExceptionUtil.logExceptionToMothership(form.getRequest(), e);
                                    form.addActionError("Error parsing workspace: " + e);
                                }
                            }
                            catch (FileNotFoundException fnfe)
                            {
                                _log.error("Error", fnfe);
                                form.addActionError("Unable to access the file '" + path + "'.");
                            }
                        }
                    }
                }

            }
            else
            {
                if (file != null)
                {
                    try
                    {
                        object = FlowJoWorkspace.readWorkspace(file.getInputStream());
                    }
                    catch (SAXParseException spe)
                    {
                        form.addActionError("Error parsing the workspace.  This might be because it is not an " +
                                "XML document: " + spe);
                    }
                    catch (Exception e)
                    {
                        _log.error("Error parsing workspace: " + e);
                        form.addActionError("Error parsing workspace: " + e);
                    }
                }
                else
                {
                    form.addActionError("No workspace file was specified.");
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
}
