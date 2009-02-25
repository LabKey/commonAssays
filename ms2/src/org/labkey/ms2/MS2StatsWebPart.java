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

package org.labkey.ms2;

import org.labkey.api.data.ContainerManager;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ActionURL;

import java.util.Map;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class MS2StatsWebPart extends JspView<MS2StatsWebPart.StatsBean>
{
    public MS2StatsWebPart()
    {
        super("/org/labkey/ms2/stats.jsp", new StatsBean());
        setTitle("MS2 Statistics");

        if (!getViewContext().getUser().isGuest())
            setTitleHref(new ActionURL(MS2Controller.ExportHistoryAction.class, ContainerManager.getRoot()));
    }


    public static class StatsBean
    {
        public String runs;
        public String peptides;

        private StatsBean()
        {
            Map<String, String> stats = MS2Manager.getBasicStats();
            runs = stats.get("Runs");
            peptides = stats.get("Peptides");
        }
    }
}
