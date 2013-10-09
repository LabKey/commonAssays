/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.MenuButton;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.query.MS2Schema;

/**
 * User: jeckels
 * Date: Nov 7, 2006
 */
public class MS2SearchExperimentRunType extends ExperimentRunType
{
    private Priority _priority;
    private String[] _protocolPrefixes;

    public MS2SearchExperimentRunType(String name, String tableName, Priority priority, String... protocolPrefixes)
    {
        super(name, MS2Schema.SCHEMA_NAME, tableName);
        _priority = priority;
        _protocolPrefixes = protocolPrefixes;
    }

    public String[] getProtocolPrefixes()
    {
        return _protocolPrefixes;
    }

    @Override
    public void populateButtonBar(ViewContext context, ButtonBar bar, DataView view, ContainerFilter containerFilter)
    {
        MenuButton compareMenu = MS2Controller.createCompareMenu(context.getContainer(), view, true);
        compareMenu.setRequiresSelection(true);
        bar.add(compareMenu);

        ActionURL url = new ActionURL(MS2Controller.PickExportRunsView.class, context.getContainer());
        url.addParameter("experimentRunIds", "true");
        ActionButton exportRuns = new ActionButton(url, "MS2 Export");
        exportRuns.setActionType(ActionButton.Action.POST);
        exportRuns.setRequiresSelection(true);
        exportRuns.setDisplayPermission(ReadPermission.class);
        bar.add(exportRuns);
    }

    public Priority getPriority(ExpProtocol protocol)
    {
        Lsid lsid = new Lsid(protocol.getLSID());
        String objectId = lsid.getObjectId();
        for (String protocolPrefix : _protocolPrefixes)
        {
            if (objectId.startsWith(protocolPrefix))
            {
                return _priority;
            }
        }
        return null;
    }
}
