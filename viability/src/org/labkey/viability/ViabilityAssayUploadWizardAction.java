/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnGroup;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.PropertyValidationError;
import org.labkey.api.query.ValidationError;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.actions.StudyPickerColumn;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.PreviouslyUploadedDataCollector;
import org.labkey.api.util.GUID;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.VBox;
import org.labkey.viability.data.MultiValueInputColumn;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * User: kevink
 * Date: Sep 19, 2009
 */
@RequiresPermission(ReadPermission.class)
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
            protected boolean validatePost(ViabilityAssayRunUploadForm form, BindException errors) throws ExperimentException
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
            protected ModelAndView handleSuccessfulPost(ViabilityAssayRunUploadForm form, BindException errors) throws ExperimentException
            {
                return getResultsView(form, false, errors);
            }
        };
    }


    protected ModelAndView getResultsView(ViabilityAssayRunUploadForm form, boolean errorReshow, BindException errors) throws ExperimentException
    {
        InsertView view = _getResultsView(form, errorReshow, errors);
        String formRef = view.getDataRegion().getJavascriptFormReference();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("<script type=\"text/javascript\">\n");
        sb.append("LABKEY.requiresScript(['internal/jQuery', 'viability/CheckRunUploadForm.js'],function(){\n");
        sb.append(formRef).append(".onsubmit = function(){return checkRunUploadForm(").append(formRef).append(", jQuery);};\n");
        sb.append("});\n");
        sb.append("</script>\n");

        VBox vbox = new VBox();
        vbox.addView(new HtmlView("<style type='text/css'>input { font-family: monospace; }</style>"));
        vbox.addView(view);
        vbox.addView(new HtmlView(sb.toString()));
        return vbox;
    }

    @Override
    protected DataRegion createDataRegionForInsert(TableInfo baseTable, String lsidCol, List<? extends DomainProperty> domainProperties, Map<String, String> columnNameToPropertyName)
    {
        // The base implementation adds ColumnInfos to the DataRegion, but we
        // do this manually in _getResultsView() for the Results table.
        if (baseTable.getName().equals(ViabilitySchema.Tables.Results.name()))
        {
            DataRegion rgn = new DataRegion();
            rgn.setTable(baseTable);
            return rgn;
        }

        return super.createDataRegionForInsert(baseTable, lsidCol, domainProperties, columnNameToPropertyName);
    }

    @Override
    protected InsertView createInsertView(TableInfo baseTable, String lsidCol, List<? extends DomainProperty> properties, boolean errorReshow, String uploadStepName, ViabilityAssayRunUploadForm form, BindException errors)
    {
        InsertView view = super.createInsertView(baseTable, lsidCol, properties, errorReshow, uploadStepName, form, errors);
        if (form.isDelete())
            view.getDataRegion().addHiddenFormField("delete", "" + form.isDelete());
        return view;
    }

    protected InsertView _getResultsView(ViabilityAssayRunUploadForm form, boolean errorReshow, BindException errors) throws ExperimentException
    {
        List<Map<String, Object>> rows = errorReshow ? form.getResultProperties(errors) : form.getParsedResultData();
        Map<String, Map<String, Object>> reRunResults = form.getReRunResults();

        Domain resultDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, ExpProtocol.ASSAY_DOMAIN_DATA);
        List<? extends DomainProperty> resultDomainProperties = resultDomain.getProperties();
        String lsidCol = "RowID";
        InsertView view = createInsertView(ViabilitySchema.getTableInfoResults(), lsidCol, resultDomainProperties, errorReshow, ResultsStepHandler.NAME, form, errors);

        boolean firstPass = true;
        List<String> poolIDs = new ArrayList<>(rows.size());
        for (DomainProperty resultDomainProperty : resultDomainProperties)
        {
            String propertyName = resultDomainProperty.getName();
            ViabilityAssayProvider.ResultDomainProperty rdp = ViabilityAssayProvider.RESULT_DOMAIN_PROPERTIES.get(propertyName);
            if (rdp != null && rdp.hideInUploadWizard)
                continue;

            boolean editable = rdp == null || rdp.editableInUploadWizard;
            // UNDONE: Issue 15513: add flag on defaultable values to not copy previous run value when performing a re-run
            boolean copyReRunValue = editable && !resultDomainProperty.getName().equalsIgnoreCase("Unreliable");
            boolean copyable = editable;

            List<DisplayColumn> columns = new ArrayList<>(rows.size());
            int rowIndex = 0;
            for (ListIterator<Map<String, Object>> iter = rows.listIterator(); iter.hasNext(); rowIndex++)
            {
                Map<String, Object> row = iter.next();
                String poolID = (String) row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME);
                if (poolID == null)
                    throw new ExperimentException("Row " + rowIndex + " missing required property " + ViabilityAssayProvider.POOL_ID_PROPERTY_NAME);
                if (firstPass)
                {
                    poolIDs.add(poolID);
                }

                String inputName = getInputName(resultDomainProperty, ViabilityAssayRunUploadForm.INPUT_PREFIX + poolID + "_" + rowIndex);

                // first, get the property's default value set in the assay design
                Object initialValue = view.getInitialValues().get(propertyName);

                // second, get the value from the parsed file
                if (!errorReshow && row.containsKey(propertyName))
                    initialValue = row.get(propertyName);

                // third, if the field is editable get the property's default value from a previous run
                String lowerPoolID = poolID.replaceAll(" ", "").toLowerCase();
                Map<String, Object> reRun = reRunResults.get(lowerPoolID);
                if (copyReRunValue && reRun != null && reRun.containsKey(propertyName))
                    initialValue = reRun.get(propertyName);

                // finally, get the value as entered by the user in the case of errorReshow
                if (errorReshow && row.containsKey(propertyName))
                    initialValue = row.get(propertyName);

                if (initialValue != null)
                    view.setInitialValue(inputName, initialValue);

                ColumnInfo col = resultDomainProperty.getPropertyDescriptor().createColumnInfo(view.getDataRegion().getTable(), lsidCol, form.getUser(), form.getContainer());
                col.setUserEditable(editable);
                col.setName(inputName);

                 // XXX: inputLength on PropertyDescriptor isn't saved
                col.setInputLength(rdp != null ? rdp.inputLength : 9);

                DisplayColumn displayCol;
                if (propertyName.equals(AbstractAssayProvider.TARGET_STUDY_PROPERTY_NAME))
                {
                    displayCol = new StudyPickerColumn(col, inputName);
                }
                else if (propertyName.equals(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME))
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
            view.getDataRegion().addGroup(new DisplayColumnGroup(columns, propertyName, copyable));
            firstPass = false;
        }

        view.getDataRegion().setHorizontalGroups(false);
        view.getDataRegion().setGroupHeadings(poolIDs);
        //view.getDataRegion().setShadeAlternatingRows(true);

        addHiddenBatchProperties(form, view);
        addHiddenRunProperties(form, view);

        for (String poolID : poolIDs)
            view.getDataRegion().addHiddenFormField("poolIds", poolID);
        view.getDataRegion().addHiddenFormField("name", form.getName());
        view.getDataRegion().addHiddenFormField("comments", form.getComments());

        ParticipantVisitResolverType resolverType = getSelectedParticipantVisitResolverType(form.getProvider(), form);
        if (resolverType != null)
            resolverType.addHiddenFormFields(form, view);

        PreviouslyUploadedDataCollector collector = new PreviouslyUploadedDataCollector(form.getUploadedData(), PreviouslyUploadedDataCollector.Type.PassThrough);
        collector.addHiddenFormFields(view, form);

        ButtonBar bbar = new ButtonBar();
        bbar.setStyle(ButtonBar.Style.separateButtons);
        addFinishButtons(form, view, bbar);
        addResetButton(form, view, bbar);

        ActionButton cancelButton = new ActionButton("Cancel", getSummaryLink(_protocol));
        bbar.add(cancelButton);

        view.getDataRegion().setButtonBar(bbar, DataRegion.MODE_INSERT);

        return view;
    }

    protected void addHiddenRunProperties(ViabilityAssayRunUploadForm form, InsertView insertView) throws ExperimentException
    {
        Map<DomainProperty, String> runProperties = new HashMap<>();
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
        public ModelAndView handleStep(ViabilityAssayRunUploadForm form, BindException errors) throws ServletException, SQLException, ExperimentException
        {
            if (getCompletedUploadAttemptIDs().contains(form.getUploadAttemptID()))
            {
                throw new RedirectException(getViewContext().getActionURL());
            }

            if (!form.isResetDefaultValues() && validatePost(form, errors))
                return handleSuccessfulPost(form, errors);
            else
                return getResultsView(form, !form.isResetDefaultValues(), errors);
        }

        @Override
        protected boolean validatePost(ViabilityAssayRunUploadForm form, BindException errors) throws ExperimentException
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

        private ExpExperiment createExperiment(ViabilityAssayRunUploadForm form)
        {
            ExpProtocol protocol = form.getProtocol();
            ViabilityAssayProvider provider = form.getProvider();

            ExpExperiment exp = ExperimentService.get().createExpExperiment(form.getContainer(), GUID.makeGUID());
            exp.save(form.getUser());
            exp.setName(protocol.getName() + "-" + exp.getRowId());
            exp.setComments("Re-importing any " + provider.getName() + " run in this run group will place the new run in this same run group.");
            exp.save(form.getUser());
            return exp;
        }

        private ExpExperiment findExperiment(ExpRun run)
        {
            ExpProtocol protocol = run.getProtocol();
            String prefix = protocol.getName() + "-";
            for (ExpExperiment exp : run.getExperiments())
            {
                if (exp.getName().startsWith(prefix) && exp.getBatchProtocol() == null)
                    return exp;
            }

            return null;
        }

        @Override
        protected ModelAndView handleSuccessfulPost(ViabilityAssayRunUploadForm form, BindException errors) throws ExperimentException
        {
            try (DbScope.Transaction transaction = ExperimentService.get().ensureTransaction())
            {
                ExpRun run = saveExperimentRun(form);

                ExpExperiment experiment = null;
                if (form.getReRunId() != null)
                {
                    ExpRun reRun = ExperimentService.get().getExpRun(form.getReRunId());
                    if (reRun != null)
                    {
                        experiment = findExperiment(reRun);

                        if (form.isDelete())
                        {
                            reRun.delete(getUser());
                        }
                        else if (experiment == null)
                        {
                            experiment = createExperiment(form);
                            experiment.addRuns(form.getUser(), reRun);
                        }
                    }
                }

                if (experiment == null)
                    experiment = createExperiment(form);
                experiment.addRuns(form.getUser(), run);

                transaction.commit();

                return afterRunCreation(form, run, errors);
            }
            catch (ValidationException e)
            {
                for (ValidationError error : e.getErrors())
                {
                    if (error instanceof PropertyValidationError)
                        errors.addError(new FieldError("AssayUploadForm", ((PropertyValidationError)error).getProperty(), null, false,
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

    @Override
    protected boolean shouldShowDataCollectorUI(ViabilityAssayRunUploadForm newRunForm)
    {
        return true;
    }
}
