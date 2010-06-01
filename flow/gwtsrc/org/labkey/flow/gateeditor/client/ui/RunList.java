/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.ui;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import org.labkey.api.gwt.client.util.ErrorDialogAsyncCallback;
import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.model.*;

import java.util.Arrays;

public class RunList extends GateComponent
{
    HorizontalPanel widget;
    ListBox listBox;
    ErrorDialogAsyncCallback<GWTWorkspace> currentRequest;
    GWTRun[] runs;
    GateEditorListener listener = new GateEditorListener()
    {
        public void onScriptChanged()
        {
            updateWorkspace();
        }

        public void updateWorkspace()
        {
            GWTRun run = getRun();
            GWTScript script = getScript();
            if (run == null || script == null)
            {
                return;
            }
            GWTWorkspaceOptions options = new GWTWorkspaceOptions();
            options.runId = run.getRunId();
            options.scriptId = script.getScriptId();
            options.editingMode = getEditor().getState().getEditingMode();
            GWTWorkspace cur = editor.getState().getWorkspace();
            if (cur != null)
            {
                if (cur.getRun() != null && cur.getRun().getRunId() == run.getRunId() &&
                        cur.getScript() != null && cur.getScript().getScriptId() == script.getScriptId())
                {
                    return;
                }
            }
            editor.getState().fireBeforeWorkspaceChanged();
            currentRequest = new ErrorDialogAsyncCallback<GWTWorkspace>()
            {
                public void onSuccess(GWTWorkspace result)
                {
                    if (currentRequest == this)
                    {
                        editor.getState().setWorkspace(result);
                    }
                }
            };

            editor.getService().getWorkspace(options, currentRequest);

        }

        public void onRunChanged()
        {
            GWTRun run = getRun();
            GWTRun[] runs = getRuns();
            int index = Arrays.asList(runs).indexOf(run);
            if (index >= 0)
            {
                listBox.setSelectedIndex(index);
            }
            updateWorkspace();
        }

    };
    ChangeHandler changeHandler = new ChangeHandler()
    {
        public void onChange(ChangeEvent event)
        {
            editor.getState().setRun(getRuns()[listBox.getSelectedIndex()]);
        }
    };

    private GWTRun[] getRuns()
    {
        return runs;
    }

    public RunList(GateEditor editor)
    {
        super(editor);
        widget = new HorizontalPanel();
        widget.add(new Label("Run"));
        listBox = new ListBox();
        listBox.setEnabled(false);
        listBox.addChangeHandler(changeHandler);
        widget.add(listBox);
        editor.addListener(listener);
        editor.getService().getRuns(new ErrorDialogAsyncCallback<GWTRun[]>()
        {
            public void onSuccess(GWTRun[] result)
            {
                runs = result;
                updateRuns();
                listBox.setEnabled(true);
            }
        });

    }

    public Widget getWidget()
    {
        return widget;
    }

    public void updateRuns()
    {
        GWTRun run = getRun();
        listBox.clear();
        for (GWTRun run1 : runs)
        {
            listBox.addItem(run1.getName(), Integer.toString(run1.getRunId()));
        }

        if (runs.length > 0 && run != null)
        {
            int index = Arrays.asList(runs).indexOf(run);
            if (index >= 0)
            {
                listBox.setSelectedIndex(index);
                return;
            }
        }
    }
}
