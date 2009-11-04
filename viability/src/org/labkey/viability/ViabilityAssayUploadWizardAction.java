/*
 * Copyright (c) 2009 LabKey Corporation
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

package org.labkey.viability;

import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.*;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.VBox;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.data.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.query.ValidationError;
import org.labkey.api.query.PropertyValidationError;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.qc.TransformResult;
import org.labkey.api.qc.DefaultTransformResult;
import org.labkey.viability.data.MultiValueInputColumn;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.*;
import java.io.File;

/**
 * User: kevink
 * Date: Sep 19, 2009
 */
@RequiresPermissionClass(ReadPermission.class)
public class ViabilityAssayUploadWizardAction extends UploadWizardAction<ViabilityAssayRunUploadForm, ViabilityAssayProvider>
{
    public ViabilityAssayUploadWizardAction()
    {
        super(ViabilityAssayRunUploadForm.class);
        addStepHandler(new ResultsStepHandler());
    }

    @Override
    protected void addRunActionButtons(ViabilityAssayRunUploadForm newRunForm, InsertView insertView, ButtonBar bbar)
    {
        addNextButton(bbar);
        addResetButton(newRunForm, insertView, bbar);
    }

    @Override
    protected RunStepHandler getRunStepHandler()
    {
        return new RunStepHandler()
        {
            @Override
            protected boolean validatePost(ViabilityAssayRunUploadForm form, BindException errors)
            {
                if (!super.validatePost(form, errors))
                    return false;

                try
                {
                    form.getParsedResultData();
                }
                catch (ExperimentException e)
                {
                    errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
                    return false;
                }

                return true;
            }

            @Override
            protected ModelAndView handleSuccessfulPost(ViabilityAssayRunUploadForm form, BindException errors) throws SQLException, ServletException
            {
                InsertView view = getResultsView(form, false, errors);

                StringBuilder sb = new StringBuilder();
                sb.append("\n");
                sb.append("<script type='text/javascript'>\n");
                sb.append("LABKEY.requiresScript('viability/CheckRunUploadForm.js');\n");
                sb.append("</script>\n");
                sb.append("<script type='text/javascript'>\n");
                String formRef = view.getDataRegion().getJavascriptFormReference(false);
                sb.append(formRef).append(".onsubmit = function () { return checkRunUploadForm(").append(formRef).append("); }\n");
                sb.append("</script>\n");

                VBox vbox = new VBox();
                vbox.addView(new HtmlView("<style type='text/css'>input { font-family: monospace; }</style>"));
                vbox.addView(view);
                vbox.addView(new HtmlView(sb.toString()));
                return vbox;
            }
        };
    }


    protected InsertView getResultsView(ViabilityAssayRunUploadForm form, boolean errorReshow, BindException errors) throws ServletException
    {
        try
        {
            return _getResultsView(form, errorReshow, errors);
        }
        catch (ExperimentException e)
        {
            throw new ServletException(e);
        }
    }

    protected InsertView _getResultsView(ViabilityAssayRunUploadForm form, boolean errorReshow, BindException errors) throws ExperimentException
    {
        List<Map<String, Object>> rows = errorReshow ? form.getResultProperties(errors) : form.getParsedResultData();

        String lsidCol = "RowID";
        InsertView view = createInsertView(ViabilitySchema.getTableInfoResults(), lsidCol, new DomainProperty[0], errorReshow, ResultsStepHandler.NAME, form, errors);

        Domain resultDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, ExpProtocol.ASSAY_DOMAIN_DATA);
        DomainProperty[] resultDomainProperties = resultDomain.getProperties();

        boolean firstPass = true;
        List<String> poolIDs = new ArrayList<String>(rows.size());
        for (DomainProperty resultDomainProperty : resultDomainProperties)
        {
            ViabilityAssayProvider.ResultDomainProperty rdp = ViabilityAssayProvider.RESULT_DOMAIN_PROPERTIES.get(resultDomainProperty.getName());
            if (rdp != null && rdp.hideInUploadWizard)
                continue;

            boolean editable = rdp == null || rdp.editableInUploadWizard;
            boolean copyable = editable;

            List<DisplayColumn> columns = new ArrayList<DisplayColumn>(rows.size());
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++)
            {
                Map<String, Object> row = rows.get(rowIndex);
                String poolID = (String) row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME);
                assert poolID != null;
                String inputName = getInputName(resultDomainProperty, ViabilityAssayRunUploadForm.INPUT_PREFIX + poolID + "_" + rowIndex);
                if (firstPass)
                {
                    poolIDs.add(poolID);
                }
                Object initialValue = row.get(resultDomainProperty.getName());
                if (initialValue != null)
                    view.setInitialValue(inputName, initialValue);

                ColumnInfo col = resultDomainProperty.getPropertyDescriptor().createColumnInfo(view.getDataRegion().getTable(), lsidCol, form.getUser());
                col.setUserEditable(editable);
                col.setName(inputName);

                 // XXX: inputLength on PropertyDescriptor isn't saved
                col.setInputLength(rdp != null ? rdp.inputLength : 9);

                DisplayColumn displayCol;
                if (resultDomainProperty.getName().equals(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME))
                {
                    List<String> values = (List<String>) initialValue;
                    displayCol = new MultiValueInputColumn(col, values);
                    copyable = false;
                }
                else
                {
                    displayCol = col.getRenderer();
                }
                columns.add(displayCol);
            }
            view.getDataRegion().addGroup(new DisplayColumnGroup(columns, resultDomainProperty.getName(), copyable));
            firstPass = false;
        }

        view.getDataRegion().setHorizontalGroups(false);
        view.getDataRegion().setGroupHeadings(poolIDs);
        view.getDataRegion().setShadeAlternatingRows(true);

        addHiddenBatchProperties(form, view);
        addHiddenRunProperties(form, view);

        for (String poolID : poolIDs)
            view.getDataRegion().addHiddenFormField("poolIds", poolID);
        view.getDataRegion().addHiddenFormField("name", form.getName());
        view.getDataRegion().addHiddenFormField("comments", form.getComments());

        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(form.getProvider(), form);
        if (resolverType != null)
            resolverType.addHiddenFormFields(form, view);

        PreviouslyUploadedDataCollector collector = new PreviouslyUploadedDataCollector(form.getUploadedData());
        collector.addHiddenFormFields(view, form);

        ButtonBar bbar = new ButtonBar();
        addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        return view;
    }

    protected void addHiddenRunProperties(ViabilityAssayRunUploadForm form, InsertView insertView) throws ExperimentException
    {
        Map<DomainProperty, String> runProperties = new HashMap<DomainProperty, String>();
        Map<DomainProperty, Object> runData = form.getParsedRunData();
        for (Map.Entry<DomainProperty, Object> entry : runData.entrySet())
        {
            String value = String.valueOf(entry.getValue());
            runProperties.put(entry.getKey(), value);
        }

        // Any manually entered values on first step of upload wizard
        // takes precedence over run property values in the file.
        for (Map.Entry<DomainProperty, String> entry : form.getRunProperties().entrySet())
        {
            if (!StringUtils.isEmpty(entry.getValue()))
                runProperties.put(entry.getKey(), entry.getValue());
        }

        addHiddenProperties(runProperties, insertView);
    }

    public class ResultsStepHandler extends RunStepHandler
    {
        public static final String NAME = "Results";

        @Override
        public String getName()
        {
            return NAME;
        }

        @Override
        public ModelAndView handleStep(ViabilityAssayRunUploadForm form, BindException errors) throws ServletException, SQLException
        {
            if (getCompletedUploadAttemptIDs().contains(form.getUploadAttemptID()))
            {
                HttpView.throwRedirect(getViewContext().getActionURL());
            }

            if (!form.isResetDefaultValues() && validatePost(form, errors))
                return handleSuccessfulPost(form, errors);
            else
                return getResultsView(form, !form.isResetDefaultValues(), errors);
        }

        @Override
        protected boolean validatePost(ViabilityAssayRunUploadForm form, BindException errors)
        {
            boolean valid = super.validatePost(form, errors);
            try
            {
                List<Map<String, Object>> rows = form.getResultProperties(errors);
                if (errors.hasErrors())
                    return false;
                ViabilityAssayDataHandler.validateData(rows, false);
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
                valid = false;
            }
            return valid;
        }

        @Override
        protected ModelAndView handleSuccessfulPost(ViabilityAssayRunUploadForm form, BindException errors) throws SQLException, ServletException
        {
            try
            {
                ExpRun run = saveExperimentRun(form);
                return afterRunCreation(form, run, errors);
            }
            catch (ValidationException e)
            {
                for (ValidationError error : e.getErrors())
                {
                    if (error instanceof PropertyValidationError)
                        errors.addError(new FieldError("AssayUploadForm", ((PropertyValidationError)error).getPropety(), null, false,
                                new String[]{SpringActionController.ERROR_MSG}, new Object[0], error.getMessage()));
                    else
                        errors.reject(SpringActionController.ERROR_MSG, error.getMessage());
                }
                return getResultsView(form, true, errors);
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
                return getResultsView(form, true, errors);
            }
        }

    }
}
