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

import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.PipelineDataCollector;
import org.labkey.api.study.assay.SampleChooserDisplayColumn;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.api.*;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.view.InsertView;
import org.labkey.api.action.LabkeyError;
import org.labkey.api.query.ValidationException;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.w3c.dom.Document;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import java.util.*;
import java.io.File;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
@RequiresPermission(ACL.PERM_INSERT)
public class MicroarrayUploadWizardAction extends UploadWizardAction<MicroarrayRunUploadForm, MicroarrayAssayProvider>
{
    private Integer _channelCount;
    private String _barcode;

    public MicroarrayUploadWizardAction()
    {
        super(MicroarrayRunUploadForm.class);
    }

    protected void addSampleInputColumns(ExpProtocol protocol, InsertView insertView)
    {
        int maxCount = _channelCount == null ? MicroarrayAssayProvider.MAX_SAMPLE_COUNT : _channelCount.intValue();
        int minCount = _channelCount == null ? MicroarrayAssayProvider.MIN_SAMPLE_COUNT : _channelCount.intValue();

        String[] barcodeFieldNames = { "Barcode" };
        ProtocolParameter barcodeFieldNamesParam = _protocol.getProtocolParameters().get(MicroarrayAssayDesigner.BARCODE_FIELD_NAMES_PARAMETER_URI);
        if (barcodeFieldNamesParam != null && barcodeFieldNamesParam.getStringValue() != null)
        {
            barcodeFieldNames = barcodeFieldNamesParam.getStringValue().split(",");
        }

        List<ExpMaterial> matchingMaterials = new ArrayList<ExpMaterial>();
        if (barcodeFieldNames != null && _barcode != null)
        {
            // Look through all the sample sets that are visible from this folder to check for samples where
            // the barcode matches
            for (ExpSampleSet sampleSet : ExperimentService.get().getSampleSets(getContainer(), getViewContext().getUser(), true))
            {
                ExpMaterial[] materials = sampleSet.getSamples();
                Domain domain = sampleSet.getType();
                DomainProperty[] properties = domain == null ? new DomainProperty[0] : domain.getProperties();
                // Check all of the possible barcode field names
                for (String barcodeFieldName : barcodeFieldNames)
                {
                    barcodeFieldName = barcodeFieldName.trim();
                    for (DomainProperty prop : properties)
                    {
                        // Look for fields with matching names
                        if (barcodeFieldName.equalsIgnoreCase(prop.getName()) || barcodeFieldName.equalsIgnoreCase(prop.getLabel()))
                        {
                            for (ExpMaterial material : materials)
                            {
                                // If the names match, check if the material has the desired barcode value
                                Object propObj = material.getProperty(prop);
                                if (propObj != null && _barcode.equals(propObj.toString()))
                                {
                                    // Add it to the list of matching materials
                                    matchingMaterials.add(material);
                                }
                            }
                        }
                    }
                }
            }
        }

        insertView.getDataRegion().addDisplayColumn(new SampleChooserDisplayColumn(minCount, maxCount, matchingMaterials));
    }

    protected InsertView createBatchInsertView(MicroarrayRunUploadForm form, boolean reshow, BindException errors)
    {
        InsertView result = super.createBatchInsertView(form, reshow, errors);
        result.getDataRegion().addDisplayColumn(new MicroarrayBulkPropertiesDisplayColumn(form));
        return result;
    }

    protected InsertView createRunInsertView(MicroarrayRunUploadForm form, boolean errorReshow, BindException errors)
    {
        List<DomainProperty> userProperties = new ArrayList<DomainProperty>();
        Map<DomainProperty, String> mageMLProperties;
        try
        {
            mageMLProperties = form.getMageMLProperties();
        }
        catch (ExperimentException e)
        {
            errors.addError(new LabkeyError("Unable to get properties from MageML file:" + e.getMessage()));
            mageMLProperties = new HashMap<DomainProperty, String>();
        }

        for (Map.Entry<DomainProperty, String> entry : form.getRunProperties().entrySet())
        {
            DomainProperty runPD = entry.getKey();
            String mageMLValue = mageMLProperties.get(runPD);
            if (mageMLValue == null)
                userProperties.add(runPD);
        }

        InsertView result = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", userProperties.toArray(new DomainProperty[userProperties.size()]),
                errorReshow, RunStepHandler.NAME, form, errors);
        
        for (Map.Entry<DomainProperty, String> entry : mageMLProperties.entrySet())
        {
            DomainProperty runPD = entry.getKey();
            result.getDataRegion().addHiddenFormField(getInputName(runPD), entry.getValue());
        }

        try
        {
            Document mageML = form.getCurrentMageML();

            if (mageML != null)
            {
                _channelCount = form.getChannelCount(mageML);
                _barcode = form.getBarcode(mageML);
            }
        }
        catch (ExperimentException e)
        {
            errors.addError(new LabkeyError("Unable to get barcode and channel count from MageML file:" + e.getMessage()));
        }
        return result;
    }

    @Override
    protected boolean showBatchStep(MicroarrayRunUploadForm runForm, Domain uploadDomain)
    {
        return true;
    }

    @Override
    protected StepHandler<MicroarrayRunUploadForm> getBatchStepHandler()
    {
        return new MicroarrayBatchStepHandler();
    }

    private class MicroarrayBatchStepHandler extends BatchStepHandler
    {
        @Override
        public ModelAndView handleStep(MicroarrayRunUploadForm form, BindException errors) throws ServletException
        {
            if (form.isBulkUploadAttempted())
            {
                BindException batchErrors = new BindException(form, "form");
                // Collect the errors in a separate list because if otherwise we fail, the superclass will add them a
                // second time during its reshow logic
                if (validatePostedProperties(form.getBatchProperties(), batchErrors))
                {
                    List<ExpRun> runs = insertRuns(form, errors);
                    if (batchErrors.getErrorCount() == 0 && errors.getErrorCount() == 0 && !runs.isEmpty())
                    {
                        return afterRunCreation(form, runs.get(0), errors);
                    }
                }
            }

            return super.handleStep(form, errors);
        }

        private List<ExpRun> insertRuns(MicroarrayRunUploadForm form, BindException errors)
        {
            try
            {
                AssayDataCollector collector = form.getSelectedDataCollector();
                RunStepHandler handler = getRunStepHandler();
                List<ExpRun> runs = new ArrayList<ExpRun>();

                // Hold on to a copy of the original file list so that we can reset the selection state if one of them fails
                List<Map<String, File>> allFiles =
                        new ArrayList<Map<String, File>>(PipelineDataCollector.getFileCollection(getViewContext().getRequest().getSession(true), getContainer(), form.getProtocol()));
                boolean success = false;
                ExperimentService.get().beginTransaction();
                try
                {
                    boolean hasMoreRuns;
                    do
                    {
                        hasMoreRuns = collector.allowAdditionalUpload(form);
                        form.getUploadedData();
                        form.checkBulkProperties();
                        validatePostedProperties(form.getRunProperties(), errors);
                        if (errors.getErrorCount() > 0)
                        {
                            return Collections.emptyList();
                        }
                        runs.add(handler.saveExperimentRun(form));
                        form.clearUploadedData();
                    }
                    while (hasMoreRuns);
                    success = true;
                    ExperimentService.get().commitTransaction();
                    return runs;
                }
                finally
                {
                    if (!success)
                    {
                        // Something went wrong, restore the full list of files
                        PipelineDataCollector.setFileCollection(getViewContext().getRequest().getSession(true), getContainer(), form.getProtocol(), allFiles);
                    }
                    ExperimentService.get().closeTransaction();
                }
            }
            catch (ExperimentException e)
            {
                errors.addError(new LabkeyError(e));
            }
            catch (ValidationException e)
            {
                errors.addError(new LabkeyError(e));
            }
            return Collections.emptyList();
        }
    }
}
