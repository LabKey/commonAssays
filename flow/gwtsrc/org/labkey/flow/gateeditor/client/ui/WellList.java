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

import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.GateCallback;
import org.labkey.flow.gateeditor.client.FlowUtil;
import org.labkey.flow.gateeditor.client.model.GWTRun;
import org.labkey.flow.gateeditor.client.model.GWTWell;
import org.labkey.flow.gateeditor.client.model.GWTWorkspace;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.DOM;

import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

public class WellList extends GateComponent
{
    VerticalPanel basePanel;
    ScrollPanel scrollPanel;
    VerticalPanel list;
    Map labelWellMap = new HashMap();
    Map wellLabelMap = new HashMap();
    Label currentLabel;

    GateEditorListener listener = new GateEditorListener()
    {
        public void onWellChanged()
        {
            selectWell(getWell());
        }

        public void onWorkspaceChanged()
        {
            setWells(getWorkspace().getWells());
        }
    };
    ClickListener clickListener = new ClickListener()
    {
        public void onClick(Widget widget)
        {
            GWTWell well = (GWTWell) labelWellMap.get(widget);
            editor.getState().setWell(well);
        }
    };

    public WellList(GateEditor editor)
    {
        super(editor);
        basePanel = new VerticalPanel();
        basePanel.setStyleName("normal");
        Image spacer = new Image();
        spacer.setUrl(FlowUtil._gif());
        spacer.setHeight("1px");
        spacer.setWidth("150px");
        scrollPanel = new ScrollPanel();
        scrollPanel.setWidth("150px");
        scrollPanel.setHeight("400px");
        list = new VerticalPanel();
        scrollPanel.add(list);
        basePanel.add(scrollPanel);
        editor.addListener(listener);
    }

    public Widget getWidget()
    {
        return basePanel;
    }

    public void setWells(GWTWell[] wells)
    {
        for (Iterator it = labelWellMap.keySet().iterator(); it.hasNext();)
        {
            Label label = (Label) it.next();
            label.removeFromParent();
        }
        labelWellMap.clear();
        wellLabelMap.clear();
        currentLabel = null;
        for (int i = 0; i < wells.length; i ++)
        {
            GWTWell well = wells[i];
            Label label = new Label(well.getLabel());
            label.setStyleName("normal");
            DOM.setStyleAttribute(label.getElement(), "cursor", "default");
            if (well.getScript() != null)
            {
                DOM.setStyleAttribute(label.getElement(), "fontWeight", "bold");
            }
            labelWellMap.put(label, well);
            wellLabelMap.put(new Integer(well.getWellId()), label);
            label.addClickListener(clickListener);
            list.add(label);
        }
        selectWell(getWell());
    }

    public void selectWell(GWTWell well)
    {
        if (currentLabel != null)
        {
            DOM.setStyleAttribute(currentLabel.getElement(), "backgroundColor", "white");
            currentLabel = null;
        }
        if (well != null)
        {
            currentLabel = (Label) wellLabelMap.get(new Integer(well.getWellId()));
            if (currentLabel != null)
            {
                DOM.setStyleAttribute(currentLabel.getElement(), "backgroundColor", "#FFDF8C");
                if (well.getScript() != null)
                {
                    DOM.setStyleAttribute(currentLabel.getElement(), "fontWeight", "bold");
                }
                else
                {
                    DOM.setStyleAttribute(currentLabel.getElement(), "fontWeight", "normal");
                }
            }
        }
    }
}
