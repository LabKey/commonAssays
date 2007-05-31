package org.labkey.flow.gateeditor.client.ui;

import com.google.gwt.user.client.ui.*;
import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.GateCallback;
import org.labkey.flow.gateeditor.client.model.GWTRun;
import org.labkey.flow.gateeditor.client.model.GWTWorkspaceOptions;
import org.labkey.flow.gateeditor.client.model.GWTWorkspace;

public class RunList extends GateComponent
{
    HorizontalPanel widget;
    ListBox listBox;
    GateCallback currentRequest;
    GateEditorListener listener = new GateEditorListener()
    {
        public void onRunChanged()
        {
            GWTRun run = getRun();
            GWTRun[] runs = getRuns();
            for (int i = 0; i < runs.length; i ++)
            {
                if (runs[i].equals(run))
                {
                    listBox.setSelectedIndex(i);
                }
            }
            GWTWorkspaceOptions options = new GWTWorkspaceOptions();
            options.runId = run.getRunId();
            options.scriptId = getScript().getScriptId();
            options.editingMode = getEditor().getState().getEditingMode();
            currentRequest = new GateCallback()
            {
                public void onSuccess(Object result)
                {
                    if (currentRequest == this)
                    {
                        editor.getState().setWorkspace((GWTWorkspace) result);
                    }
                }
            };

            editor.getService().getWorkspace(options, currentRequest);
        }

        public void onRunsChanged()
        {
            setRuns(getRuns());
        }
    };
    ChangeListener changeListener = new ChangeListener()
    {
        public void onChange(Widget sender)
        {
            editor.getState().setRun(getRuns()[listBox.getSelectedIndex()]);
        }
    };

    public RunList(GateEditor editor)
    {
        super(editor);
        widget = new HorizontalPanel();
        widget.add(new Label("Run"));
        listBox = new ListBox();
        listBox.addChangeListener(changeListener);
        widget.add(listBox);
        editor.addListener(listener);
        editor.getService().getRuns(new GateCallback() {

            public void onSuccess(Object result)
            {
                setRuns((GWTRun[]) result);
            }
        });

    }

    public Widget getWidget()
    {
        return widget;
    }

    public void setRuns(GWTRun[] runs)
    {
        GWTRun run = getRun();
        listBox.clear();
        for (int i = 0; i < runs.length; i++)
        {
            listBox.addItem(runs[i].getName(), Integer.toString(runs[i].getRunId()));
        }

        if (runs.length > 0)
        {
            if (run != null)
            {
                for (int i = 0; i < runs.length; i ++)
                {
                    if (runs[i].equals(run))
                    {
                        listBox.setSelectedIndex(i);
                        return;
                    }
                }
            }
            getEditor().getState().setRun(runs[0]);
        }
    }
}
