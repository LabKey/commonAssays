/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.labkey.api.action.LabKeyError;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.study.actions.BulkPropertiesDisplayColumn;
import org.labkey.api.study.assay.BulkPropertiesUploadWizardAction;
import org.labkey.api.study.assay.SampleChooserDisplayColumn;
import org.labkey.api.study.assay.PipelineDataCollector;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.RedirectException;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.pipeline.PipelineUrls;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: phussey
 * Date: Sep 17, 2007
 * Time: 12:30:55 AM
 */
@RequiresPermission(InsertPermission.class)
public class MassSpecMetadataUploadAction extends BulkPropertiesUploadWizardAction<MassSpecMetadataAssayForm, MassSpecMetadataAssayProvider>
{
    public MassSpecMetadataUploadAction()
    {
        super(MassSpecMetadataAssayForm.class);
        addStepHandler(new DeleteAssaysStepHandler());
    }

    @RequiresPermission(DeletePermission.class)
    public class DeleteAssaysStepHandler extends StepHandler<MassSpecMetadataAssayForm>
    {
        public static final String NAME = "DELETEASSAYS";

        @Override
        public ModelAndView handleStep(MassSpecMetadataAssayForm form, BindException errors)
        {
            Container c = form.getContainer();

            List<Map<String,File>> allFiles = PipelineDataCollector.getFileQueue(form);

            for (Map<String, File> fileSet : allFiles)
            {
                for (File file : fileSet.values())
                {
                    ExpRun run = ExperimentService.get().getCreatingRun(file, c);
                    if (run != null)
                    {
                        run.delete(form.getUser());
                    }
                }
            }

            return null;
        }

        public String getName()
        {
            return NAME;
        }
    }

    @Override
    protected InsertView createRunInsertView(MassSpecMetadataAssayForm form, boolean reshow, BindException errors) throws ExperimentException
    {
        InsertView parent = super.createRunInsertView(form, reshow, errors);
        parent.getDataRegion().addHiddenFormField(FractionsDisplayColumn.FRACTIONS_FIELD_NAME, Boolean.toString(form.isFractions()));

        if (form.isFractions())
        {
            try
            {
                ExpSampleSet sampleSet = form.getProvider().getFractionSampleSet(form);
                ArrayList<File> files = new ArrayList<>(form.getAllFiles());
                MsFractionPropertyHelper helper = new MsFractionPropertyHelper(sampleSet, files, getContainer());
                helper.addSampleColumns(parent, form.getUser());
            }
            catch (ExperimentException e)
            {
                errors.addError(new LabKeyError(e));
            }
        }
        return parent;
    }

    @Override
    protected boolean showBatchStep(MassSpecMetadataAssayForm form, Domain batchDomain) throws ServletException
    {
        if (PipelineDataCollector.getFileQueue(form).size() == 0)
        {
            throw new RedirectException(PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(form.getContainer(), null));
        }

        // Show the batch step if we have batch properties or might be a fractions search
        return super.showBatchStep(form, batchDomain) ||
            PipelineDataCollector.getFileQueue(form).size() > 1;
    }

    @Override
    protected InsertView createBatchInsertView(MassSpecMetadataAssayForm form, boolean reshow, BindException errors) throws ExperimentException
    {
        InsertView result = super.createBatchInsertView(form, reshow, errors);
        ActionURL templateURL = new ActionURL(MassSpecBulkPropertiesTemplateAction.class, getContainer());
        templateURL.addParameter("rowId", form.getProtocol().getRowId());
        form.setTemplateURL(templateURL);
        result.getDataRegion().addDisplayColumn(new FractionsDisplayColumn(form));
        result.getDataRegion().addDisplayColumn(new BulkPropertiesDisplayColumn(form));
        return result;
    }

    @Override
    protected void addSampleInputColumns(MassSpecMetadataAssayForm form, InsertView insertView)
    {
        super.addSampleInputColumns(form, insertView);
        insertView.getDataRegion().addDisplayColumn(new SampleChooserDisplayColumn(1, 2, Collections.emptyList(), 1));
    }

    @Override
    protected boolean shouldShowDataCollectorUI(MassSpecMetadataAssayForm newRunForm)
    {
        return true;
    }
}
