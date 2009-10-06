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

package org.labkey.flow.gateeditor.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Window;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.util.ServiceUtil;
import org.labkey.flow.gateeditor.client.model.*;
import org.labkey.flow.gateeditor.client.ui.GateEditorPanel;
import org.labkey.flow.gateeditor.client.ui.GateEditorListener;


public class GateEditor implements EntryPoint
{
    GateEditorServiceAsync _service;
    private RootPanel _root;
    private GateEditorPanel gateEditorPanel;
    private EditorState editorState;

    public void onModuleLoad()
    {
        final GWT.UncaughtExceptionHandler ueh = GWT.getUncaughtExceptionHandler();
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler()
        {
            public void onUncaughtException(Throwable e)
            {
                GWT.log("AppController: Uncaught exception handled: " + e.getMessage(), e);
                if (ueh != null) {
                  ueh.onUncaughtException(e);
                }
            }
        });

        editorState = new EditorState();
        GWTEditingMode editingMode = GWTEditingMode.valueOf(PropertyUtil.getServerProperty("editingMode"));
        getState().setEditingMode(editingMode);

        GWTWorkspaceOptions workspaceOptions = new GWTWorkspaceOptions();
        workspaceOptions.editingMode = editingMode;
        String strScriptId = PropertyUtil.getServerProperty("scriptId");
        if (strScriptId != null)
        {
            workspaceOptions.scriptId = Integer.parseInt(strScriptId);
        }
        String strRunId = PropertyUtil.getServerProperty("runId");
        if (strRunId != null)
        {
            workspaceOptions.runId = Integer.parseInt(strRunId);
        }

        getState().setSubsetName(PropertyUtil.getServerProperty("subset"));
        init(workspaceOptions);

        _root = RootPanel.get("org.labkey.flow.gateeditor.GateEditor-Root");
        gateEditorPanel = new GateEditorPanel(this);
        _root.add(gateEditorPanel.getWidget());
    }

    public GateEditorServiceAsync getService()
    {
        if (_service == null)
        {
            _service = (GateEditorServiceAsync) GWT.create(GateEditorService.class);
            ServiceUtil.configureEndpoint(_service, "gateEditorService");
        }
        return _service;
    }

    void init(GWTWorkspaceOptions workspaceOptions)
    {
        getService().getWorkspace(workspaceOptions, new GateCallback<GWTWorkspace>()
        {
            public void onSuccess(GWTWorkspace result)
            {
                getState().setWorkspace(result);
            }
        });
    }

    public void addListener(GateEditorListener listener)
    {
        editorState.addListener(listener);
    }

    public void removeListener(GateEditorListener listener)
    {
        editorState.removeListener(listener);
    }

    public EditorState getState()
    {
        return editorState;
    }

    public void save(GWTScript script)
    {
        getService().save(script, new GateCallback<GWTScript>()
        {
            public void onSuccess(GWTScript result)
            {
                getState().setScript(result);
            }
        });
    }

    public void save(GWTWell well, GWTScript script)
    {
        getService().save(well, script, new GateCallback<GWTWell>()
        {
            public void onSuccess(GWTWell well)
            {
                GWTWorkspace workspace = getState().getWorkspace();
                GWTWell[] wells = workspace.getWells();
                for (int i = 0; i < wells.length; i ++)
                {
                    GWTWell wellCompare = wells[i];
                    if (well.getWellId() == wellCompare.getWellId())
                    {
                        wells[i] = well;
                        break;
                    }
                }
                if (getState().getWell() != null && getState().getWell().getWellId() == well.getWellId())
                {
                    getState().setWell(well);
                }
            }
        });
    }
    public RootPanel getRootPanel()
    {
        return _root;
    }
}
