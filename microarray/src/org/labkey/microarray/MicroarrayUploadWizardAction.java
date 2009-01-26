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
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.view.InsertView;
import org.labkey.api.action.LabkeyError;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.w3c.dom.Document;
import org.springframework.validation.BindException;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

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
        insertView.getDataRegion().addDisplayColumn(new SampleChooserDisplayColumn(protocol, _channelCount, _barcode));
    }

    protected InsertView createRunInsertView(MicroarrayRunUploadForm form, boolean reshow, BindException errors)
    {
        List<DomainProperty> userProperties = new ArrayList<DomainProperty>();
        Map<DomainProperty, String> mageMLProperties = form.getMageMLProperties(errors);

        for (Map.Entry<DomainProperty, String> entry : form.getRunProperties().entrySet())
        {
            DomainProperty runPD = entry.getKey();
            String mageMLValue = mageMLProperties.get(runPD);
            if (mageMLValue == null)
                userProperties.add(runPD);
        }

        InsertView result = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", userProperties.toArray(new DomainProperty[userProperties.size()]),
                reshow, RunStepHandler.NAME, form, errors);
        
        for (Map.Entry<DomainProperty, String> entry : mageMLProperties.entrySet())
        {
            DomainProperty runPD = entry.getKey();
            result.getDataRegion().addHiddenFormField(getInputName(runPD), entry.getValue());
        }

        Document mageML = form.getMageML(errors);

        try
        {
            ProtocolParameter channelCountParam = form.getProtocol().getProtocolParameters().get(MicroarrayAssayDesigner.CHANNEL_COUNT_PARAMETER_URI);
            String channelCountXPath = channelCountParam == null ? null : channelCountParam.getStringValue();
            String channelCountString = evaluateXPath(mageML, channelCountXPath);
            if (channelCountString != null)
            {
                try
                {
                    _channelCount = new Integer(channelCountString);
                }
                catch (NumberFormatException e)
                {
                    // Continue on, the user can choose the count themselves
                }
            }
        }
        catch (XPathExpressionException e)
        {
            errors.addError(new LabkeyError("Failed to evaluate channel count XPath: " + e.getMessage()));
        }

        try
        {
            ProtocolParameter barcodeParam = form.getProtocol().getProtocolParameters().get(MicroarrayAssayDesigner.BARCODE_PARAMETER_URI);
            String barcodeXPath = barcodeParam == null ? null : barcodeParam.getStringValue();
            _barcode = evaluateXPath(mageML, barcodeXPath);
        }
        catch (XPathExpressionException e)
        {
            errors.addError(new LabkeyError("Failed to evaluate barcode XPath: " + e.getMessage()));
        }
        return result;
    }

    private String evaluateXPath(Document document, String expression) throws XPathExpressionException
    {
        if (expression == null && document != null)
        {
            return null;
        }
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        XPathExpression xPathExpression = xPath.compile(expression);
        return xPathExpression.evaluate(document);
    }
}
