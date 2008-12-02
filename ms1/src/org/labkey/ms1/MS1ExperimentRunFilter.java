/*
 * Copyright (c) 2008 LabKey Corporation
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
package org.labkey.ms1;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.ExperimentRunFilter;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.ContainerFilter;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms1.query.MS1Schema;

/**
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 28, 2008
 * Time: 2:24:01 PM
 */
public class MS1ExperimentRunFilter extends ExperimentRunFilter
{
    public MS1ExperimentRunFilter()
    {
        super(MS1Module.PROTOCOL_MS1, MS1Schema.SCHEMA_NAME, MS1Schema.TABLE_FEATURE_RUNS);
    }

    @Override
    public void populateButtonBar(ViewContext context, ButtonBar bar, DataView view, ContainerFilter containerFilter)
    {
        ActionURL compareUrl = new ActionURL(MS1Controller.CompareRunsSetupAction.class, context.getContainer());
        bar.add(new ActionButton(MS1Controller.createVerifySelectedScript(view, compareUrl),
                "Compare", DataRegion.MODE_ALL, ActionButton.Action.LINK));
    }

    public Priority getPriority(ExpProtocol protocol)
    {
        if ("MS1.msInspectFeatureFindingAnalysis".equals(new Lsid(protocol.getLSID()).getObjectId()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
