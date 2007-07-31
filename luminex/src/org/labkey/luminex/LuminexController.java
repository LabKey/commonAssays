package org.labkey.luminex;

import org.labkey.api.action.SpringActionController;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.exp.Protocol;
import org.labkey.api.view.InsertView;

/**
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class LuminexController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(LuminexController.class);

    static
    {
        _resolver.addAction(LuminexUploadWizardAction.class);
    }

    public LuminexController()
    {
        super();
        setActionResolver(_resolver.getInstance(this));
    }

    @RequiresPermission(ACL.PERM_READ)
    public class LuminexUploadWizardAction extends UploadWizardAction
    {
        protected void addSampleInputColumns(Protocol protocol, InsertView insertView)
        {
            // Don't add any columns - they're part of the uploaded spreadsheet
        }
    }


}
