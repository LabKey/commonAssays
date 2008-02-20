package org.labkey.flow.query;

import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.TableEditHelper;
import org.labkey.api.query.QueryUpdateForm;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;

import java.util.Set;

public class RunEditHelper extends TableEditHelper
{
    FlowSchema _schema;
    public RunEditHelper(FlowSchema schema)
    {
        _schema = schema;
    }

    public ActionURL delete(User user, ActionURL srcURL, QueryUpdateForm form) throws Exception
    {
        Set<String> pks = DataRegionSelection.getSelected(form.getViewContext(), true);
        ExperimentService.get().deleteExperimentRunsByRowIds(_schema.getContainer(), user, PageFlowUtil.toInts(pks));
        return srcURL;
    }

    public boolean hasPermission(User user, int perm)
    {
        if ((perm & ~ACL.PERM_DELETE) != 0)
        {
            return false;
        }
        return _schema.getContainer().hasPermission(user, perm);
    }
}
