/*
 * Copyright (c) 2008-2015 LabKey Corporation
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
package org.labkey.nab;

import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ApiVersion;
import org.labkey.api.assay.nab.view.GraphSelectedForm;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;

/*
 * User: brittp
 * Date: April 21, 2010
 */

@RequiresPermission(ReadPermission.class)
@ApiVersion(10.2)
public class GetStudyNabGraphURLAction extends ApiAction<GraphSelectedForm>
{
    @Override
    public ApiResponse execute(GraphSelectedForm form, BindException errors) throws Exception
    {
        Map<Pair<Integer, String>, ExpProtocol> readableIds = NabManager.get().getReadableStudyObjectIds(getContainer(), getUser(), form.getId());

        StringBuilder objectIdParam = new StringBuilder();
        String sep = "";
        for (Pair<Integer, String> id : readableIds.keySet())
        {
            objectIdParam.append(sep).append(id.getKey());
            sep = ",";
        }

        ActionURL url = new ActionURL(StudyNabGraphAction.class, getContainer());
        // simply pass the incoming parameters through, except for objectIds, which is subject to modification for
        // security reasons:
        url.addParameters(getPropertyValues());
        url.replaceParameter("id", objectIdParam.toString());

        final Map<String, Object> returnValue = new HashMap<>();
        returnValue.put("url", url.getLocalURIString());
        returnValue.put("objectIds", readableIds.keySet());

        return new ApiSimpleResponse(returnValue);
    }
}