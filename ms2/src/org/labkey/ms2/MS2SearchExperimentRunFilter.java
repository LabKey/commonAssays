package org.labkey.ms2;

import org.labkey.api.exp.ExperimentRunFilter;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.api.data.*;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.security.ACL;

/**
 * User: jeckels
 * Date: Nov 7, 2006
 */
public class MS2SearchExperimentRunFilter extends ExperimentRunFilter
{
    public MS2SearchExperimentRunFilter(String name, String tableName)
    {
        super(name, MS2Schema.SCHEMA_NAME, tableName);
    }

    private ActionButton createButton(ViewContext context, String actionName, String description, ActionButton.Action method)
    {
        ActionURL url = context.getActionURL().clone();
        url.deleteParameters();
        url.setPageFlow("MS2");
        url.setAction(actionName + ".view");
        ActionButton button = new ActionButton(url.getLocalURIString() + "ExperimentRunIds=true", description, ACL.PERM_READ, method);
        button.setDisplayModes(DataRegion.MODE_GRID);
        return button;
    }

    public void populateButtonBar(ViewContext context, ButtonBar bar, DataView view)
    {
        MenuButton compareMenu = MS2Controller.createCompareMenu(context.getContainer(), view, true);

        bar.add(compareMenu);

        ActionButton exportRuns = new ActionButton("button", "MS2 Export");
        ActionURL url = context.getActionURL().clone();
        url.deleteParameters();
        url.setPageFlow("MS2");
        url.setAction("pickExportRunsView.view");
        exportRuns.setScript("return verifySelected(this.form, \"" + url.getLocalURIString() + "experimentRunIds=true\", \"post\", \"runs\")");
        exportRuns.setActionType(ActionButton.Action.GET);
        exportRuns.setDisplayPermission(ACL.PERM_READ);
        bar.add(exportRuns);

        bar.add(createButton(context, "showHierarchy", "Show Hierarchy", ActionButton.Action.LINK));
    }
}
