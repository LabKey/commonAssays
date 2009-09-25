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
import org.labkey.api.data.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.action.SpringActionController;
import org.labkey.viability.data.MultiValueInputColumn;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.*;

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
        addStepHandler(new SpecimensStepHandler());
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
        return new RunStepHandler() {
            @Override
            protected ModelAndView handleSuccessfulPost(ViabilityAssayRunUploadForm form, BindException errors) throws SQLException, ServletException
            {
                try
                {
                    return getSpecimensView(form, errors);
                }
                catch (ExperimentException e)
                {
                    throw new ServletException(e);
                }
            }
        };
    }


    protected ModelAndView getSpecimensView(ViabilityAssayRunUploadForm form, BindException errors) throws ExperimentException
    {
        List<Map<String, Object>> rows = form.getParsedData();

        String lsidCol = "RowID";
        InsertView view = createInsertView(ViabilitySchema.getTableInfoResults(), lsidCol, new DomainProperty[0], form.isResetDefaultValues(), SpecimensStepHandler.NAME, form, errors);

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
            for (Map<String, Object> row : rows)
            {
                String poolID = (String)row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME);
                assert poolID != null;
                String inputName = getInputName(resultDomainProperty, ViabilityAssayRunUploadForm.INPUT_PREFIX + poolID);
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
                col.setInputLength(9); // XXX: inputLength on PropertyDescriptor isn't saved

                DisplayColumn displayCol;
                if (resultDomainProperty.getName().equals(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME))
                {
                    String[] values = (String[])initialValue;
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

    public class SpecimensStepHandler extends RunStepHandler
    {
        public static final String NAME = "Specimens";

        @Override
        public String getName()
        {
            return NAME;
        }

        @Override
        protected boolean validatePost(ViabilityAssayRunUploadForm form, BindException errors)
        {
            boolean valid = super.validatePost(form, errors);
            try
            {
                List<Map<String, Object>> rows = form.getResultProperties();
                ViabilityAssayDataHandler.validateData(rows, true);
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
            return super.handleSuccessfulPost(form, errors);
        }

        @Override
        public ExpRun saveExperimentRun(ViabilityAssayRunUploadForm form) throws ExperimentException, ValidationException
        {
            ExpRun run = super.saveExperimentRun(form);

            // Find the ExpData that was just inserted as a run output,
            // then update the results with the form posted data.
            List<ExpData> datas = run.getDataOutputs();
            assert datas.size() == 1;
            ExpData data = datas.get(0);
            try
            {
                // XXX: hack hack. It would be nice to insert the form posted values the first time rather than deleting and re-inserting.
                List<Map<String, Object>> posted = form.getResultProperties();
                ViabilityManager.deleteAll(data, form.getContainer());

                AssayProvider provider = AssayService.get().getProvider(form.getProtocol());
                Domain resultDomain = provider.getResultsDomain(form.getProtocol());

                ViabilityAssayDataHandler._insertRowData(data, form.getUser(), form.getContainer(), resultDomain, posted);
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }

            return run;
        }
    }
}
