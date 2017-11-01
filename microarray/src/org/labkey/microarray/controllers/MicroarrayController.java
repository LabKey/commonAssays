/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

package org.labkey.microarray.controllers;

import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.permissions.DesignAssayPermission;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.GWTView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.WebPartView;
import org.labkey.microarray.MicroarrayBulkPropertiesTemplateAction;
import org.labkey.microarray.MicroarrayRunType;
import org.labkey.microarray.MicroarrayUploadWizardAction;
import org.labkey.microarray.PendingMageMLFilesView;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.labkey.microarray.pipeline.GeneDataPipelineProvider;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MicroarrayController extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(
            MicroarrayController.class,
            MicroarrayBulkPropertiesTemplateAction.class,
            MicroarrayUploadWizardAction.class);

    public MicroarrayController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(DesignAssayPermission.class)
    public class DesignerAction extends org.labkey.api.study.actions.DesignerAction
    {
        protected ModelAndView createGWTView(Map<String, String> properties)
        {
            setHelpTopic(new HelpTopic("microarrayProperties"));
            return new GWTView(MicroarrayAssayDesigner.class, properties);
        }
    }

    public static ActionURL getRunsURL(Container c)
    {
        return new ActionURL(ShowRunsAction.class, c);
    }

    public static ActionURL getPendingMageMLFilesURL(Container c)
    {
        return new ActionURL(ShowPendingMageMLFilesAction.class, c);
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowRunsAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            QueryView result = ExperimentService.get().createExperimentRunWebPart(getViewContext(), MicroarrayRunType.INSTANCE);
            result.setShowExportButtons(true);
            result.setFrame(WebPartView.FrameType.NONE);
            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Microarray Runs");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowPendingMageMLFilesAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new PendingMageMLFilesView(getViewContext());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Pending MageML Files");
        }
    }

    /**
     * Basic approach:
     * 1. The user logs in to LabKey Server and selects the files they want to analyze, and clicks on a new "Launch in Analyst" button.
     * 2. LabKey Server will then package up the files as a ZIP or just copy them into a new directory. In either case, the file(s) will end up on a network share that LabKey Server can write to, and Analyst can read from.
     * 3. LabKey Server will then cause the user's browser to navigate to a URL of the general form that you described. It will include a GET parameter that gives the path to the ZIP file or directory.
     * 4. The Analyst plugin takes over from here.
     */
    @RequiresPermission(ReadPermission.class)
    public class GeneDataAnalysisAction extends RedirectAction<PipelinePathForm>
    {
        private URLHelper _successURL;

        @Override
        public URLHelper getSuccessURL(PipelinePathForm pipelinePathForm)
        {
            return _successURL;
        }

        @Override
        public boolean doAction(PipelinePathForm form, BindException errors) throws Exception
        {
            String baseURL = GeneDataPipelineProvider.getGeneDataBaseURL();
            File root = GeneDataPipelineProvider.getGeneDataFileRoot();

            if (baseURL == null || root == null)
            {
                throw new NotFoundException("GeneData URL or file root not configured");
            }

            if (getUser().isGuest())
            {
                throw new UnauthorizedException();
            }

            String simpleDirName = getUser().getDisplayName(getUser()) + DateUtil.formatDateTime(new Date(), "yyyy-MM-dd-HH-mm");
            File analysisDir = new File(root, simpleDirName);
            int suffix = 1;
            while (analysisDir.exists())
            {
                analysisDir = new File(root, simpleDirName + "-" + (suffix++));
            }
            analysisDir.mkdir();

            // Copy over all of the files
            List<File> files = form.getValidatedFiles(getContainer());
            for (File selectedFile : files)
            {
                FileUtil.copyFile(selectedFile, new File(analysisDir, selectedFile.getName()));
            }

            _successURL = new URLHelper(baseURL + analysisDir.getAbsolutePath());
            return true;
        }
    }


    @RequiresPermission(ReadPermission.class)
    public class MatrixQueryAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new JspView<>("/org/labkey/microarray/view/ExpressionMatrixQuery.jsp");
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Microarray Expression Data Comparison");
        }
    }
}
