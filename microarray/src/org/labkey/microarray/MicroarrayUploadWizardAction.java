/*
 * Copyright (c) 2008 LabKey Corporation
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
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.view.InsertView;
import org.labkey.api.action.LabkeyError;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.FileInputStream;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
@RequiresPermission(ACL.PERM_INSERT)
public class MicroarrayUploadWizardAction extends UploadWizardAction<AssayRunUploadForm>
{
    private Integer _channelCount;
    private String _barcode;

    protected void addSampleInputColumns(ExpProtocol protocol, InsertView insertView)
    {
        insertView.getDataRegion().addDisplayColumn(new SampleChooserDisplayColumn(protocol, _channelCount, _barcode));
    }

    protected InsertView createRunInsertView(AssayRunUploadForm form, boolean reshow, BindException errors)
    {
        Map<PropertyDescriptor, String> allProperties = form.getRunProperties();
        Map<PropertyDescriptor, String> userProperties = new HashMap<PropertyDescriptor, String>();

        // We want to split the run properties into the ones that come from the user directly and the ones that
        // come from the MageML - hide the ones from the MageML so the user doesn't edit the value
        Map<String, String> hiddenElements = new HashMap<String, String>();

        Document document = null;
        try
        {
            // Create a document for the MageML
            DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
            dbfact.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
            DocumentBuilder builder = dbfact.newDocumentBuilder();
            document = builder.parse(new InputSource(new FileInputStream(form.getSelectedDataCollector().createData(form).values().iterator().next())));
        }
        catch (IOException e)
        {
            errors.addError(new ObjectError("main", null, null, "Error parsing file: " + e.toString()));
        }
        catch (SAXException e)
        {
            errors.addError(new ObjectError("main", null, null, "Error parsing file: " + e.toString()));
        }
        catch (ExperimentException e)
        {
            errors.addError(new ObjectError("main", null, null, "Error parsing file: " + e.toString()));
        }
        catch (ParserConfigurationException e)
        {
            errors.addError(new ObjectError("main", null, null, "Error parsing file: " + e.toString()));
        }

        for (Map.Entry<PropertyDescriptor, String> entry : allProperties.entrySet())
        {
            PropertyDescriptor runPD = entry.getKey();
            String expression = runPD.getDescription();
            if (expression != null)
            {
                // We use the description of the property descriptor as the XPath. Far from ideal.
                try
                {
                    String value = evaluateXPath(document, expression);
                    hiddenElements.put(ColumnInfo.propNameFromName(runPD.getName()), value);
                }
                catch (XPathExpressionException e)
                {
                    // User isn't required to use the description as an XPath
                    userProperties.put(runPD, entry.getValue());
                }
            }
            else
            {
                userProperties.put(runPD, entry.getValue());
            }
        }

        InsertView result = createInsertView(ExperimentService.get().getTinfoExperimentRun(),
                "lsid", userProperties, reshow, form.isResetDefaultValues(), RunStepHandler.NAME, form, errors);

        for (Map.Entry<String, String> hiddenElement : hiddenElements.entrySet())
        {
            result.getDataRegion().addHiddenFormField(hiddenElement.getKey(), hiddenElement.getValue());
        }

        try
        {
            ProtocolParameter channelCountParam = form.getProtocol().getProtocolParameters().get(MicroarrayAssayDesigner.CHANNEL_COUNT_PARAMETER_URI);
            String channelCountXPath = channelCountParam == null ? null : channelCountParam.getStringValue();
            String channelCountString = evaluateXPath(document, channelCountXPath);
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
            _barcode = evaluateXPath(document, barcodeXPath);
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
