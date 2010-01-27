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

package org.labkey.flow.query;

import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.TableEditHelper;
import org.labkey.api.query.QueryUpdateForm;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.Permission;
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

    public boolean hasPermission(User user, Class<? extends Permission> perm)
    {
        return DeletePermission.class.isAssignableFrom(perm) && _schema.getContainer().hasPermission(user, perm);
    }
}
