/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

package org.labkey.ms2.metadata;

import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Module;
import org.labkey.ms2.MS2Run;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;


public class MassSpecMetadataController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(
            MassSpecMetadataController.class,
            MassSpecMetadataUploadAction.class,
            MassSpecBulkPropertiesTemplateAction.class);


    public MassSpecMetadataController()
    {
        super();
        setActionResolver(_resolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            return getContainer().getStartURL(getUser());
        }
    }

    public static class SearchLinkForm
    {
        private int _runId;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class SearchLinkAction extends SimpleViewAction<SearchLinkForm>
    {
        private ExpRun _run;

        public ModelAndView getView(SearchLinkForm form, BindException errors) throws Exception
        {
            _run = ExperimentService.get().getExpRun(form.getRunId());
            if (_run == null)
            {
                throw new NotFoundException("Could not find run " + form.getRunId());
            }
            if (!_run.getContainer().hasPermission(getUser(), ReadPermission.class))
            {
                throw new UnauthorizedException();
            }

            QueryView result = ExperimentService.get().createExperimentRunWebPart(getViewContext(), MS2Module.SEARCH_RUN_TYPE);
            ExpRunTable table = (ExpRunTable) result.getTable();

            SQLFragment searchSQL = MassSpecMetadataProtocolSchema.getSearchRunSQL(getContainer(), table.getContainerFilter(), Integer.toString(form.getRunId()), "DISTINCT(er.RowId)");
            // Figure out how many search matches there are
            Integer[] searchRunIds = new SqlSelector(ExperimentService.get().getSchema(), searchSQL).getArray(Integer.class);

            if (searchRunIds.length == 0)
            {
                throw new NotFoundException("No search runs found");
            }

            if (searchRunIds.length == 1)
            {
                // If there's only one match, jump to it directly
                ExpRun searchRun = ExperimentService.get().getExpRun(searchRunIds[0].intValue());
                if (searchRun == null)
                {
                    throw new IllegalStateException("Could not find search run with rowId " + searchRunIds[0] + " after its rowId was returned from the database");
                }
                MS2Run ms2Run = MS2Manager.getRunByExperimentRunLSID(searchRun.getLSID());
                if (ms2Run == null)
                {
                    throw new IllegalStateException("Could not find MS2Run for experiment run LSID " + searchRun.getLSID());
                }
                throw new RedirectException(MS2Controller.MS2UrlsImpl.get().getShowRunUrl(getUser(), ms2Run));
            }

            // Otherwise show
            SQLFragment conditionSQL = new SQLFragment("RowId IN (");
            conditionSQL.append(searchSQL);
            conditionSQL.append(")");
            table.addCondition(conditionSQL);
            result.setTitle("MS2 Search Runs");

            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ExpProtocol protocol = _run.getProtocol();
            root.addChild("Assay List", PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
            root.addChild(protocol.getName(), PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), protocol));
            root.addChild("Searches for " + _run.getName());
            return root;
        }
    }
}
