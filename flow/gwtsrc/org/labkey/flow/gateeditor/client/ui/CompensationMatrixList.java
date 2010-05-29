/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
import org.labkey.flow.gateeditor.client.model.GWTCompensationMatrix;
import org.labkey.flow.gateeditor.client.model.GWTRun;

import java.util.Arrays;

public class CompensationMatrixList extends GateComponent
{
    HorizontalPanel widget;
    ListBox listBox;
    GWTCompensationMatrix[] matrices;

    GateEditorListener listener = new GateEditorListener() {

        public void onCompMatrixChanged()
        {
            updateCompensationMatrix();
        }

        public void onRunChanged()
        {
            GWTRun run = getRun();
            setCompMatrixForRun(run);
        }
    };

    ChangeHandler changeHandler = new ChangeHandler()
    {
        public void onChange(ChangeEvent event)
        {
            getEditor().getState().setCompensationMatrix(matrices[listBox.getSelectedIndex()]);
        }
    };

    public CompensationMatrixList(GateEditor editor)
    {
        super(editor);
        widget = new HorizontalPanel();
        widget.add(new Label("Compensation Matrix:"));
        listBox = new ListBox();
        listBox.setEnabled(false);
        listBox.addChangeHandler(changeHandler);
        widget.add(listBox);
        editor.addListener(listener);
        editor.getService().getCompensationMatrices(new ErrorDialogAsyncCallback<GWTCompensationMatrix[]>()
        {
            public void onSuccess(GWTCompensationMatrix[] result)
            {
                setCompensationMatrices(result);
                listBox.setEnabled(true);
            }
        });
    }


    public Widget getWidget()
    {
        return widget;
    }

    public void updateCompensationMatrix()
    {
        GWTCompensationMatrix matrix = getCompensationMatrix();
        int index = matrix == null ? -1 : Arrays.asList(matrices).indexOf(matrix);
        index++; // 0th index is "<No compensation>"
        if (index >= 0 && listBox.getSelectedIndex() != index)
        {
            listBox.setSelectedIndex(index);
        }
    }

    public void setCompensationMatrices(final GWTCompensationMatrix[] comps)
    {
        matrices = comps;
        listBox.clear();
        listBox.addItem("<No Compensation>", "0");
        for (GWTCompensationMatrix comp : comps)
        {
            listBox.addItem(comp.getLabel(), Integer.toString(comp.getCompId()));
        }
        if (comps.length > 0)
        {
            setCompMatrixForRun(getRun());
        }
    }

    public void setCompMatrixForRun(GWTRun run)
    {
        if (matrices == null)
            return;
        if (run != null)
        {
            editor.getService().getRunCompensationMatrix(run.getRunId(), new ErrorDialogAsyncCallback<Integer>()
            {
                public void onSuccess(Integer compId)
                {
                    if (compId != null)
                    {
                        for (GWTCompensationMatrix comp : matrices)
                        {
                            if (comp.getCompId() == compId.intValue())
                            {
                                editor.getState().setCompensationMatrix(comp);
                                break;
                            }
                        }
                    }
                }
            });
        }
        editor.getState().setCompensationMatrix(null);
    }
}
