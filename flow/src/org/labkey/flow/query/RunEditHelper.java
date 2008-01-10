package org.labkey.flow.query;

import org.labkey.api.exp.api.TableEditHelper;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.security.ACL;
import org.labkey.api.view.ActionURL;
import org.labkey.api.query.QueryUpdateForm;
import org.labkey.api.data.DataRegion;
import org.labkey.api.util.PageFlowUtil;

public class RunEditHelper extends TableEditHelper
{
    FlowSchema _schema;
    public RunEditHelper(FlowSchema schema)
    {
        _schema = schema;
    }

    public ActionURL delete(User user, ActionURL srcURL, QueryUpdateForm form) throws Exception
    {
        String[] pks = form.getRequest().getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
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
