/*
 * Copyright (c) 2007 LabKey Corporation
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
import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.GateCallback;
import org.labkey.flow.gateeditor.client.model.GWTCompensationMatrix;

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
    };

    ChangeListener changeListener = new ChangeListener() {

        public void onChange(Widget sender)
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
        listBox.addChangeListener(changeListener);
        widget.add(listBox);
        editor.getService().getCompensationMatrices(new GateCallback() {

            public void onSuccess(Object result)
            {
                setCompensationMatrices((GWTCompensationMatrix[]) result);
            }
        });
    }


    public Widget getWidget()
    {
        return widget;
    }

    public void updateCompensationMatrix()
    {
        for (int i = 0; i < matrices.length; i ++)
        {
            if (matrices[i].equals(getCompensationMatrix()))
            {
                listBox.setSelectedIndex(1);
            }
        }
    }

    public void setCompensationMatrices(GWTCompensationMatrix[] comps)
    {
        matrices = comps;
        listBox.clear();
        for (int i = 0; i < comps.length; i++)
        {
            listBox.addItem(comps[i].getLabel(), Integer.toString(comps[i].getCompId()));
        }
        if (comps.length > 0)
        {
            editor.getState().setCompensationMatrix(comps[0]);
        }
    }
}
