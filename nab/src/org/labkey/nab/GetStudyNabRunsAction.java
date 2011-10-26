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
package org.labkey.nab;

import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiVersion;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.ActionNames;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.springframework.validation.BindException;

import java.util.*;

/*
 * User: brittp
 * Date: March 4, 2010
 * Time: 5:13:30 PM
 */

@RequiresPermissionClass(ReadPermission.class)
@ApiVersion(10.1)
public class GetStudyNabRunsAction extends ApiAction<GetStudyNabRunsAction.GetStudyNabRunsForm>
{
    public static class GetStudyNabRunsForm extends GetNabRunsBaseForm
    {
        private int[] _objectIds;

        public int[] getObjectIds()
        {
            return _objectIds;
        }

        public void setObjectIds(int[] objectIds)
        {
            _objectIds = objectIds;
        }
    }

    @Override
    public ApiResponse execute(GetStudyNabRunsForm form, BindException errors) throws Exception
    {
        final Map<String, Object> properties = new HashMap<String, Object>();
        List<NabRunPropertyMap> runList = new ArrayList<NabRunPropertyMap>();
        properties.put("runs", runList);

        for (ExpRun run : getRuns(form, errors))
        {
            AssayProvider provider = AssayService.get().getProvider(run.getProtocol());
            if (!(provider instanceof NabAssayProvider))
                throw new IllegalStateException("Non-NAb run found.");
            NabDataHandler dataHandler = ((NabAssayProvider) provider).getDataHandler();

            runList.add(new NabRunPropertyMap(dataHandler.getAssayResults(run, form.getViewContext().getUser()),
                    form.isIncludeStats(), form.isIncludeWells(), form.isCalculateNeut(), form.isIncludeFitParameters()));
        }

        if (errors.hasErrors())
            return null;
        else
        {
            return new ApiResponse()
            {
                public Map<String, Object> getProperties()
                {
                    return properties;
                }
            };
        }
    }

    protected Collection<ExpRun> getRuns(GetStudyNabRunsForm form, BindException errors)
    {
        Map<Integer, ExpProtocol> readableObjectIds = NabManager.get().getReadableStudyObjectIds(getViewContext().getContainer(),
                getViewContext().getUser(), form.getObjectIds());

        Collection<ExpRun> runs = new ArrayList<ExpRun>();

        for (Integer objectId : readableObjectIds.keySet())
        {
            // Note that we intentionally do NOT filter or check container.  If the user has access to the NAb
            // data copied to the study (verified above), they can get the raw data via the APIs.  This is
            // consistent with the role-based implementation which allows viewing the NAb details view for copied-
            // to-study data even if the Nab details view data is in a folder the user cannot read.
            ExpRun run = NabManager.get().getNAbRunByObjectId(objectId.intValue());
            if (run != null)
                runs.add(run);
        }
        return runs;
    }
}