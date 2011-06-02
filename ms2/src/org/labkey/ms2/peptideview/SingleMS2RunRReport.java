/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
package org.labkey.ms2.peptideview;

import org.labkey.api.reports.report.CustomRReport;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Sep 10, 2008
 */
public class SingleMS2RunRReport extends CustomRReport
{
    public static final String TYPE = "MS2.SingleRun.rReport";
    public static final String[] PARAMS = new String[]
    {
        MS2Controller.RunForm.PARAMS.run.toString(),
        MS2Controller.RunForm.PARAMS.grouping.toString(),
        MS2Controller.RunForm.PARAMS.expanded.toString()
    };


    public SingleMS2RunRReport()
    {
        super(PARAMS, TYPE);
    }

    protected QueryView getQueryView(ViewContext context) throws Exception
    {
        ActionURL url = context.getActionURL();
        MS2Run run;
        String runString = url.getParameter(MS2Controller.RunForm.PARAMS.run);
        try
        {
            run = MS2Manager.getRun(Integer.parseInt(runString));
        }
        catch (NumberFormatException e)
        {
            throw new NotFoundException("No such run: " + runString);
        }
        if (run == null || !run.getContainer().equals(context.getContainer()))
        {
            throw new NotFoundException("No such run: " + runString);
        }

        String groupingString = url.getParameter(MS2Controller.RunForm.PARAMS.grouping);
        MS2RunViewType type = MS2RunViewType.getViewType(groupingString);
        AbstractMS2RunView view = type.createView(context, run);
        if (view instanceof AbstractQueryMS2RunView)
        {
            String expandedString = url.getParameter(MS2Controller.RunForm.PARAMS.expanded);
            boolean expanded = "1".equals(expandedString);
            return ((AbstractQueryMS2RunView)view).createGridView(expanded, null, null, false);
        }

        throw new NotFoundException("Unsupported grouping type: " + groupingString);
    }


    protected boolean hasRequiredParams(ViewContext context)
    {
        try
        {
            Integer.parseInt(context.getActionURL().getParameter(MS2Controller.RunForm.PARAMS.run));
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        return MS2RunViewType.getViewType(context.getActionURL().getParameter(MS2Controller.RunForm.PARAMS.grouping)) != null;
    }
}