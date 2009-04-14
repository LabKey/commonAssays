/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.ms2.xarassay;

import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.SampleChooserDisplayColumn;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.NotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;

/**
 * User: phussey
 * Date: Sep 17, 2007
 * Time: 12:30:55 AM
 */
@RequiresPermission(ACL.PERM_INSERT)
public class XarAssayUploadAction extends UploadWizardAction<XarAssayForm, XarAssayProvider>
{
    public XarAssayUploadAction()
    {
        super(XarAssayForm.class);
        addStepHandler(new DeleteAssaysStepHandler());

    }

    @Override
    public ModelAndView getView(XarAssayForm assayRunUploadForm, BindException errors) throws Exception
    {
        return super.getView(assayRunUploadForm, errors);
    }


    @RequiresPermission(ACL.PERM_DELETE)
    public class DeleteAssaysStepHandler extends StepHandler<XarAssayForm>
    {
        public static final String NAME = "DELETEASSAYS";

        @Override
        public ModelAndView handleStep(XarAssayForm form, BindException errors) throws ServletException, SQLException
        {
            try
            {
                Container c = form.getContainer();

                PipelineService service = PipelineService.get();
                PipeRoot pr = service.findPipelineRoot(c);
                if (pr == null || !URIUtil.exists(pr.getUri()))
                    throw new NotFoundException("No pipeline root has been configured for this folder");

                URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
                if (uriData == null)
                    throw new NotFoundException("Could not find file " + form.getPath());

                File[] mzXMLFiles = new File(uriData).listFiles(new XarAssayProvider.AnalyzeFileFilter());
                for (File mzXMLFile : mzXMLFiles)
                {
                    ExpRun run = ExperimentService.get().getCreatingRun(mzXMLFile, c);
                    if (run != null)
                    {
                        ExperimentService.get().deleteExperimentRunsByRowIds(c, form.getUser(), run.getRowId());
                    }
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            ActionURL helper = form.getProvider().getUploadWizardURL(getContainer(), _protocol);
            helper.replaceParameter("path", form.getPath());
            helper.replaceParameter("providerName", form.getProviderName());
            HttpView.redirect(helper);

            return null;
        }

        public String getName()
        {
            return NAME;

        }
    }

    @Override
    protected InsertView createRunInsertView(XarAssayForm form, boolean reshow, BindException errors)
    {
        InsertView parent = super.createRunInsertView(form, reshow, errors);

        AssayProvider provider = getProvider(form);
        try
        {
            if (provider instanceof MsFractionAssayProvider)
            {
                MsFractionPropertyHelper helper = ((MsFractionAssayProvider)getProvider(form)).createSamplePropertyHelper(form, form.getProtocol(),null);
                helper.addSampleColumns(parent, form.getUser());
            }
            return parent;
        }
        catch (ExperimentException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void addSampleInputColumns(ExpProtocol protocol, InsertView insertView)
    {
        super.addSampleInputColumns(protocol, insertView);
        insertView.getDataRegion().addDisplayColumn(new SampleChooserDisplayColumn(1, 2, Collections.<ExpMaterial>emptyList()));
    }
}
