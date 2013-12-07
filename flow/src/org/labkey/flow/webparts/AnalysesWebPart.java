/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.flow.webparts;

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.view.FlowQueryView;

public class AnalysesWebPart extends FlowQueryView
{
    static public final WebPartFactory FACTORY = new SimpleWebPartFactory("Flow Analyses", AnalysesWebPart.class);

    public AnalysesWebPart(ViewContext context, Portal.WebPart wp)
    {
        super(new FlowSchema(context.getUser(), context.getContainer()), null, null);
        FlowQuerySettings settings = (FlowQuerySettings) getSchema().getSettings(wp, context);
        settings.setAllowChooseQuery(false);
        settings.setAllowChooseView(false);
        settings.setQueryName(FlowTableType.Analyses.toString());
        setSettings(settings);
        
        setTitle("Flow Analyses");
        setShowExportButtons(false);
        setShowPagination(false);
        // Workaround for "Issue 18903: can't change query on showRuns.view" -- for now, just don't display the [details] link which includes the returnURL parameter.
        setShowDetailsColumn(false);
        setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);

        if (getViewContext().hasPermission(InsertPermission.class))
        {
            FlowScript[] scripts = FlowScript.getScripts(getContainer());
            FlowScript analysisScript = null;
            for (FlowScript script : scripts)
            {
                if (script.hasStep(FlowProtocolStep.analysis))
                {
                    analysisScript = script;
                    break;
                }
            }
            if (analysisScript != null)
            {
                ActionButton btnAnalyze = new ActionButton("Choose runs to analyze", analysisScript.urlFor(AnalysisScriptController.ChooseRunsToAnalyzeAction.class));
                bar.add(btnAnalyze);
            }

            ActionURL createRunGroupURL = PageFlowUtil.urlProvider(ExperimentUrls.class).getCreateRunGroupURL(getContainer(), getReturnURL(), false);
            ActionButton createExperiment = new ActionButton(createRunGroupURL, "Create Analysis Folder");
            createExperiment.setActionType(ActionButton.Action.LINK);
            createExperiment.setDisplayPermission(InsertPermission.class);
            bar.add(createExperiment);
        }
    }

    @Override
    public ActionButton createDeleteButton()
    {
        // Use default delete button, but without showing the confirmation text -- DeleteSelectedExperimentsAction will show a confirmation page.
        ActionButton button = super.createDeleteButton();
        if (button != null)
        {
            button.setRequiresSelection(true);
        }
        return button;
    }
}
