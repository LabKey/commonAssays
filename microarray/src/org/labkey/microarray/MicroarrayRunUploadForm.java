/*
 * Copyright (c) 2009-2013 LabKey Corporation
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

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.actions.BulkPropertiesUploadForm;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.SampleChooserDisplayColumn;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
/*
 * User: brittp
 * Date: Jan 19, 2009
 * Time: 2:29:57 PM
 */

public class MicroarrayRunUploadForm extends BulkPropertiesUploadForm<MicroarrayAssayProvider>
{
    private Map<DomainProperty, String> _mageMLProperties;
    private Document _mageML;
    private boolean _loadAttempted;

    public Document getMageML(File f) throws ExperimentException
    {
        try
        {
            DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
            dbfact.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbfact.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            DocumentBuilder builder = dbfact.newDocumentBuilder();
            return builder.parse(new InputSource(new FileInputStream(f)));
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
        catch (SAXException e)
        {
            throw new ExperimentException("Error parsing " + f.getName(), e);
        }
        catch (ParserConfigurationException e)
        {
            throw new ExperimentException(e);
        }
    }

    public Document getCurrentMageML() throws ExperimentException
    {
        if (!_loadAttempted)
        {
            _loadAttempted = true;
            _mageML = getMageML(getUploadedData().get(AssayDataCollector.PRIMARY_FILE));
        }
        return _mageML;
    }

    public Map<DomainProperty, String> getMageMLProperties() throws ExperimentException
    {
        if (_mageMLProperties == null)
        {
            _mageMLProperties = new HashMap<>();
            // Create a document for the MageML
            Document document = getCurrentMageML();
            if (document == null)
                return Collections.emptyMap();

            MicroarrayAssayProvider provider = getProvider();
            Map<DomainProperty, XPathExpression> xpathProperties = provider.getXpathExpressions(getProtocol());
            for (Map.Entry<DomainProperty, XPathExpression> entry : xpathProperties.entrySet())
            {
                try
                {
                    String value = entry.getValue().evaluate(document);
                    _mageMLProperties.put(entry.getKey(), value);
                }
                catch (XPathExpressionException e)
                {
                    // Don't add to the map, force the user to enter the value
                }
            }
        }
        return _mageMLProperties;
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

    @Override
    public Map<DomainProperty, Object> getDefaultValues(Domain domain) throws ExperimentException
    {
        Map<DomainProperty, Object> defaults = super.getDefaultValues(domain);
        if (!isResetDefaultValues() && UploadWizardAction.RunStepHandler.NAME.equals(getUploadStep()))
        {
            for (Map.Entry<DomainProperty, String> entry : getMageMLProperties().entrySet())
                defaults.put(entry.getKey(), entry.getValue());
        }
        return defaults;
    }

    @Override
    public void clearUploadedData()
    {
        super.clearUploadedData();
        _mageML = null;
        _mageMLProperties = null;
        _loadAttempted = false;
    }

    @Override
    public Map<DomainProperty, String> getRunProperties() throws ExperimentException
    {
        if (_runProperties == null)
        {
            super.getRunProperties();
            _runProperties.putAll(getMageMLProperties());
        }
        return _runProperties;
    }

    @Override
    public String getHelpPopupHTML()
    {
        return "<p>You may use a set of TSV (tab-separated values) to specify run metadata.</p>" +
                "<p>The barcode column in the TSV is matched with the barcode value in the MageML file. " +
                "The sample name columns, configured in the assay design, will be used to look for matching samples by " +
                "name in all visible sample sets.</p>" +
                "<p>Any additional run level properties may be specified as separate columns.</p>";
    }

    public Map<String, Object> getBulkProperties() throws ExperimentException
    {
        String barcode = getBarcode(getCurrentMageML());
        if (barcode == null || "".equals(barcode))
        {
            throw new ExperimentException("Could not find a barcode value in " + getUploadedData().get(AssayDataCollector.PRIMARY_FILE));
        }
        return getProperties(barcode);
    }

    public int getSampleCount(Document mageML) throws ExperimentException
    {
        if (isBulkUploadAttempted())
        {
            Integer result = getChannelCount(mageML);
            if (result == null)
            {
                throw new ExperimentException("Unable to find the channel count");
            }
            return result.intValue();
        }
        else
        {
            return SampleChooserDisplayColumn.getSampleCount(getRequest(), MicroarrayAssayProvider.MAX_SAMPLE_COUNT);
        }
    }

    public ExpMaterial getSample(int index) throws ExperimentException
    {
        if (isBulkUploadAttempted())
        {
            String name = getSampleName(index);
            if (name != null)
            {
                return resolveSample(name);
            }
            throw new ExperimentException("No sample name was specified for sample " + (index + 1));
        }
        else
        {
            return SampleChooserDisplayColumn.getMaterial(index, getContainer(), getRequest());
        }
    }

    public String getSampleName(int index) throws ExperimentException
    {
        if (isBulkUploadAttempted())
        {
            ProtocolParameter param = null;
            if (index == 0)
            {
                param = getProtocol().getProtocolParameters().get(MicroarrayAssayDesigner.CY3_SAMPLE_NAME_COLUMN_PARAMETER_URI);
            }
            else if (index == 1)
            {
                param = getProtocol().getProtocolParameters().get(MicroarrayAssayDesigner.CY5_SAMPLE_NAME_COLUMN_PARAMETER_URI);
            }

            String columnName;
            if (param == null)
            {
                columnName = "sample" + (index + 1);
            }
            else
            {
                columnName = param.getStringValue();
            }
            if (!getBulkProperties().containsKey(columnName))
            {
                throw new ExperimentException("Could not find a '" + columnName + "' column for sample information.");
            }
            Object result = getBulkProperties().get(columnName);
            if (result == null)
            {
                throw new ExperimentException("No sample information specified for '" + columnName + "'.");
            }
            return result.toString();
        }
        else
        {
            return SampleChooserDisplayColumn.getSampleName(index, getRequest());
        }
    }

    public String getBarcode(Document mageML) throws ExperimentException
    {
        try
        {
            ProtocolParameter barcodeParam = getProtocol().getProtocolParameters().get(MicroarrayAssayDesigner.BARCODE_PARAMETER_URI);
            String barcodeXPath = barcodeParam == null ? null : barcodeParam.getStringValue();
            return evaluateXPath(mageML, barcodeXPath);
        }
        catch (XPathExpressionException e)
        {
            throw new ExperimentException("Failed to evaluate barcode XPath", e);
        }
    }

    public Integer getChannelCount(Document mageML) throws ExperimentException
    {
        try
        {
            ProtocolParameter channelCountParam = getProtocol().getProtocolParameters().get(MicroarrayAssayDesigner.CHANNEL_COUNT_PARAMETER_URI);
            String channelCountXPath = channelCountParam == null ? null : channelCountParam.getStringValue();
            if (mageML != null)
            {
                String channelCountString = evaluateXPath(mageML, channelCountXPath);
                if (channelCountString != null)
                {
                    try
                    {
                        return new Integer(channelCountString);
                    }
                    catch (NumberFormatException e)
                    {
                        // Continue on, the user can choose the count themselves
                    }
                }
            }
        }
        catch (XPathExpressionException e)
        {
            throw new ExperimentException("Failed to evaluate channel count XPath", e);
        }
        return null;
    }

    @Override
    protected String getIdentifierColumnName()
    {
        return "barcode";
    }
}
