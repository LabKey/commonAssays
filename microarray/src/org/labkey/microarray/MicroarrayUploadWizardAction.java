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

package org.labkey.microarray;

import org.apache.log4j.Logger;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.actions.BulkPropertiesDisplayColumn;
import org.labkey.api.study.assay.BulkPropertiesUploadWizardAction;
import org.labkey.api.study.assay.SampleChooserDisplayColumn;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.RedirectException;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.springframework.validation.BindException;
import org.w3c.dom.Document;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
@RequiresPermission(InsertPermission.class)
public class MicroarrayUploadWizardAction extends BulkPropertiesUploadWizardAction<MicroarrayRunUploadForm, MicroarrayAssayProvider>
{
    private static final Logger LOG = Logger.getLogger(MicroarrayUploadWizardAction.class);
    private Integer _channelCount;
    private String _barcode;

    public MicroarrayUploadWizardAction()
    {
        super(MicroarrayRunUploadForm.class);
    }

    protected void addSampleInputColumns(MicroarrayRunUploadForm form, InsertView insertView)
    {
        int maxCount = _channelCount == null ? MicroarrayAssayProvider.MAX_SAMPLE_COUNT : _channelCount.intValue();
        int minCount = _channelCount == null ? MicroarrayAssayProvider.MIN_SAMPLE_COUNT : _channelCount.intValue();

        String[] barcodeFieldNames = { "Barcode" };
        ProtocolParameter barcodeFieldNamesParam = _protocol.getProtocolParameters().get(MicroarrayAssayDesigner.BARCODE_FIELD_NAMES_PARAMETER_URI);
        if (barcodeFieldNamesParam != null && barcodeFieldNamesParam.getStringValue() != null)
        {
            barcodeFieldNames = barcodeFieldNamesParam.getStringValue().split(",");
        }

        List<ExpMaterial> matchingMaterials = new ArrayList<>();
        if (barcodeFieldNames != null && _barcode != null)
        {
            // Look through all the sample sets that are visible from this folder to check for samples where
            // the barcode matches
            for (ExpSampleSet sampleSet : ExperimentService.get().getSampleSets(getContainer(), getUser(), true))
            {
                List<? extends ExpMaterial> materials = sampleSet.getSamples();
                Domain domain = sampleSet.getType();
                List<? extends DomainProperty> properties = domain == null ? Collections.emptyList() : domain.getProperties();
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

    @Override
    protected boolean showBatchStep(MicroarrayRunUploadForm form, Domain uploadDomain) throws ServletException
    {
        try
        {
            if (form.getUploadedData().isEmpty())
            {
                throw new RedirectException(PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(form.getContainer(), null));
            }
        }
        catch (ExperimentException e)
        {
            // No data was present
            throw new RedirectException(PageFlowUtil.urlProvider(PipelineUrls.class).urlBrowse(form.getContainer(), null));
        }

        return super.showBatchStep(form, uploadDomain);
    }

    protected InsertView createBatchInsertView(MicroarrayRunUploadForm form, boolean reshow, BindException errors) throws ExperimentException
    {
        InsertView result = super.createBatchInsertView(form, reshow, errors);
        ActionURL templateURL = new ActionURL(MicroarrayBulkPropertiesTemplateAction.class, getContainer());
        templateURL.addParameter("rowId", form.getProtocol().getRowId());
        form.setTemplateURL(templateURL);
        result.getDataRegion().addDisplayColumn(new BulkPropertiesDisplayColumn(form));
        return result;
    }

    protected InsertView createRunInsertView(MicroarrayRunUploadForm form, boolean errorReshow, BindException errors)
    {
        List<DomainProperty> userProperties = new ArrayList<>();
        Map<DomainProperty, String> mageMLProperties;
        try
        {
            mageMLProperties = form.getMageMLProperties();
        }
        catch (ExperimentException e)
        {
            errors.addError(new LabKeyError("Unable to get properties from MageML file: " + e.getMessage()));
            mageMLProperties = new HashMap<>();
        }

        try
        {
            for (Map.Entry<DomainProperty, String> entry : form.getRunProperties().entrySet())
            {
                DomainProperty runPD = entry.getKey();
                String mageMLValue = mageMLProperties.get(runPD);
                if (mageMLValue == null)
                    userProperties.add(runPD);
            }
        }
        catch (ExperimentException e)
        {
            LOG.warn(e);
        }

        InsertView result = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", userProperties,
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
            errors.addError(new LabKeyError("Unable to get barcode and channel count from MageML file:" + e.getMessage()));
        }
        return result;
    }

    @Override
    protected boolean shouldShowDataCollectorUI(MicroarrayRunUploadForm newRunForm)
    {
        // Always expect input files, regardless of whether we're configured to import spot-level data
        return true;
    }
}
