/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.action.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.study.actions.ProtocolIdForm;
import org.labkey.api.study.permissions.DesignAssayPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.*;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.labkey.microarray.pipeline.FeatureExtractionPipelineJob;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.*;

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

    public static class DesignerForm extends ProtocolIdForm
    {
        public boolean isCopy()
        {
            return _copy;
        }

        public void setCopy(boolean copy)
        {
            _copy = copy;
        }

        private boolean _copy;
    }
    
    @RequiresPermissionClass(DesignAssayPermission.class)
    public class DesignerAction extends org.labkey.api.study.actions.DesignerAction
    {
        protected ModelAndView createGWTView(Map<String, String> properties)
        {
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

    @RequiresPermissionClass(ReadPermission.class)
    public class ShowRunsAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            QueryView result = ExperimentService.get().createExperimentRunWebPart(getViewContext(), MicroarrayRunType.INSTANCE, true);
            result.setShowExportButtons(true);
            result.setFrame(WebPartView.FrameType.NONE);
            return result;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Microarray Runs");
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
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

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new HtmlView("Test");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    public static class ExtractionForm
    {
        private String _path;
        private int _protocolId;
        private String _protocolName;
        private String _extractionEngine = "Agilent";

        public String getPath()
        {
            return _path;
        }

        public void setPath(String path)
        {
            _path = path;
        }

        public int getProtocol()
        {
            return _protocolId;
        }

        public void setProtocol(int protocol)
        {
            _protocolId = protocol;
        }

        public ExpProtocol lookupProtocol()
        {
            return ExperimentService.get().getExpProtocol(_protocolId);
        }

        public String getExtractionEngine()
        {
            return _extractionEngine;
        }

        public void setExtractionhEngine(String extractionEngine)
        {
            _extractionEngine = extractionEngine;
        }

        public String getProtocolName()
        {
            return _protocolName;
        }

        public void setProtocolName(String protocolName)
        {
            _protocolName = protocolName;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ImportImageFilesAction extends SimpleViewAction<ExtractionForm>
    {
        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }

        public ModelAndView getView(ExtractionForm form, BindException errors) throws Exception
        {
            Container c = getContainer();
            PipelineService service = PipelineService.get();

            PipeRoot pr = service.findPipelineRoot(c);
            if (pr == null || !URIUtil.exists(pr.getUri()))
                throw new NotFoundException("No pipeline root configured for this folder");

            URI uriData = URIUtil.resolve(pr.getUri(), form.getPath());
            if (uriData == null)
            {
                HttpView.throwNotFound();
            }

            try
            {
                PipelineJob job = new FeatureExtractionPipelineJob(getViewBackgroundInfo(), form.getProtocolName(), uriData, form.getExtractionEngine());
                PipelineService.get().queueJob(job);

                HttpView.throwRedirect(PageFlowUtil.urlProvider(ProjectUrls.class).getStartURL(c));
            }
            catch (FileNotFoundException e)
            {
                //TODO - need to buid an error page to display the pipeline errors
                throw new ExperimentException("Import image process failed", e);
            }
            return null;
        }
    }
}
