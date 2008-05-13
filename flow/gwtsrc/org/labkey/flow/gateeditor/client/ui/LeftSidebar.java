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

package org.labkey.flow.gateeditor.client.ui;

import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.flow.gateeditor.client.GateEditor;

public class LeftSidebar extends GateComponent
{
    StackPanel widget;
    PopulationTree populationTree;
    RunList runList;
    WellList wellList;

    public LeftSidebar(GateEditor editor)
    {
        super(editor);
        widget = new StackPanel();
        widget.setWidth("200px");
        runList = new RunList(editor);
        wellList = new WellList(editor);
        populationTree = new PopulationTree(editor);
        widget.add(runList.getWidget(), "Runs");
        widget.add(wellList.getWidget(), "Wells");
        widget.add(populationTree.getWidget(), "Populations");
    }

    public Widget getWidget()
    {
        return widget;
    }
}
