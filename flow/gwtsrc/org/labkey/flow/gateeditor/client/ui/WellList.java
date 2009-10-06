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

import org.labkey.flow.gateeditor.client.GateEditor;
import org.labkey.flow.gateeditor.client.FlowUtil;
import org.labkey.flow.gateeditor.client.model.GWTWell;
import org.labkey.api.gwt.client.ui.WebPartPanel;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.DOM;

import java.util.Map;
import java.util.HashMap;

public class WellList extends GateComponent
{
    WebPartPanel panel;
    VerticalPanel widget;
    ScrollPanel scrollPanel;
    VerticalPanel list;
    Map<Label,GWTWell> labelWellMap = new HashMap<Label,GWTWell>();
    Map<Integer,Label> wellLabelMap = new HashMap<Integer,Label>();
    Label currentLabel;

    GateEditorListener listener = new GateEditorListener()
    {
        public void onWellChanged()
        {
            selectWell(getWell());
        }

        public void onBeforeWorkspaceChanged()
        {
            setLoadingMessage();
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
            GWTWell well = labelWellMap.get(widget);
            editor.getState().setWell(well);
        }
    };

    public WellList(GateEditor editor)
    {
        super(editor);
        widget = new VerticalPanel();
        this.panel = new WebPartPanel("Well List", widget);
        Image spacer = new Image();
        spacer.setUrl(FlowUtil._gif());
        spacer.setHeight("4px");
        spacer.setWidth("160px");
        widget.add(spacer);
        scrollPanel = new ScrollPanel();
        scrollPanel.setWidth("150px");
        scrollPanel.setHeight("400px");
        list = new VerticalPanel();
        setLoadingMessage();
        scrollPanel.add(list);
        widget.add(scrollPanel);
        editor.addListener(listener);
    }

    public Widget getWidget()
    {
        return panel;
    }

    public void setLoadingMessage()
    {
        list.clear();
        list.add(new InlineHTML("<em>Loading wells...</em>"));
    }

    public void setWells(GWTWell[] wells)
    {
        list.clear();
        for (Label label : labelWellMap.keySet())
        {
            label.removeFromParent();
        }
        labelWellMap.clear();
        wellLabelMap.clear();
        currentLabel = null;
        for (GWTWell well : wells)
        {
            Label label = new Label(well.getLabel());
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
            currentLabel = wellLabelMap.get(new Integer(well.getWellId()));
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
