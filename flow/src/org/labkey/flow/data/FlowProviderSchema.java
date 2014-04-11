package org.labkey.flow.data;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProviderSchema;

/**
 * User: kevink
 * Date: 4/10/14
 */
public class FlowProviderSchema extends AssayProviderSchema
{
    public FlowProviderSchema(User user, Container container, FlowAssayProvider provider, Container targetStudy)
    {
        super(user, container, provider, targetStudy);
        // Issue 19812: Flow Assay appears in schema browser
        _hidden = true;
    }
}
