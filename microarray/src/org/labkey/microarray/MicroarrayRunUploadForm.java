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
package org.labkey.microarray;

import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.util.CaseInsensitiveHashMap;
import org.labkey.api.util.UnexpectedException;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.sampleset.client.SampleChooser;
import org.labkey.microarray.sampleset.client.SampleInfo;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.labkey.common.tools.TabLoader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import java.util.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;/*
 * User: brittp
 * Date: Jan 19, 2009
 * Time: 2:29:57 PM
 */

public class MicroarrayRunUploadForm extends AssayRunUploadForm<MicroarrayAssayProvider>
{
    private Map<DomainProperty, String> _mageMLProperties;
    private Document _mageML;
    private boolean _loadAttempted;
    private List<Map<String, Object>> _bulkProperties;

    public Document getMageML(File f) throws ExperimentException
    {
        try
        {
            DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
            dbfact.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
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
            _mageML = getMageML(getUploadedData().values().iterator().next());
        }
        return _mageML;
    }

    public Map<DomainProperty, String> getMageMLProperties() throws ExperimentException
    {
        if (_mageMLProperties == null)
        {
            _mageMLProperties = new HashMap<DomainProperty, String>();
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
    public Map<DomainProperty, String> getRunProperties()
    {
        if (_runProperties == null)
        {
            _runProperties = super.getRunProperties();
            if (isBulkUploadAttempted())
            {
                try
                {
                    Map<String, Object> values = getBulkProperties();
                    for (DomainProperty prop : _runProperties.keySet())
                    {
                        Object value = values.get(prop.getName());
                        if (value == null)
                        {
                            value = values.get(prop.getLabel());
                        }
                        _runProperties.put(prop, value == null ? null : value.toString());
                    }
                    _runProperties.putAll(getMageMLProperties());
                }
                catch (ExperimentException e)
                {
                    throw new UnexpectedException(e);
                }
            }
        }
        return _runProperties;
    }

    @Override
    public void clearUploadedData()
    {
        super.clearUploadedData();
        _mageML = null;
        _mageMLProperties = null;
        _loadAttempted = false;
    }

    public boolean isBulkUploadAttempted()
    {
        return "on".equals(getRequest().getParameter(MicroarrayBulkPropertiesDisplayColumn.ENABLED_FIELD_NAME));
    }

    private Map<String, Object> getBulkProperties() throws ExperimentException
    {
        if (_bulkProperties == null)
        {
            String tsv = getRawBulkProperties();
            try
            {
                TabLoader loader = new TabLoader(tsv, true);
                List<Map<String, Object>> maps = loader.load();
                _bulkProperties = new ArrayList<Map<String, Object>>(maps.size());
                for (Map<String, Object> map : maps)
                {
                    _bulkProperties.add(new CaseInsensitiveHashMap<Object>(map));
                }
            }
            catch (IOException e)
            {
                throw new UnexpectedException(e);
            }
        }
        String barcode = getBarcode(getCurrentMageML());
        for (Map<String, Object> props : _bulkProperties)
        {
            if (barcode.equals(props.get("barcode")))
            {
                return props;
            }
        }
        return Collections.emptyMap();
    }

    public String getRawBulkProperties()
    {
        return getRequest().getParameter(MicroarrayBulkPropertiesDisplayColumn.PROPERTIES_FIELD_NAME);
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
            String countString = getRequest().getParameter(SampleChooser.SAMPLE_COUNT_ELEMENT_NAME);
            if (countString != null)
            {
                try
                {
                    return Integer.parseInt(countString);
                }
                catch (NumberFormatException e)
                {
                    return MicroarrayAssayProvider.MAX_SAMPLE_COUNT;
                }
            }
            else
            {
                return MicroarrayAssayProvider.MAX_SAMPLE_COUNT;
            }
        }
    }

    public String getSampleLSID(int index) throws ExperimentException
    {
        if (isBulkUploadAttempted())
        {
            String name = getSampleName(index);
            if (name != null)
            {
                ExpMaterial material = ExperimentService.get().getExpMaterialByName(name, getContainer(), getUser());
                if (material != null)
                {
                    return material.getLSID();
                }
            }
            return null;
        }
        else
        {
            return getRequest().getParameter(SampleInfo.getLsidFormElementID(index));
        }
    }

    public String getSampleName(int index) throws ExperimentException
    {
        if (isBulkUploadAttempted())
        {
            Object result = getBulkProperties().get("sample" + index);
            if (result == null)
            {
                // Try some other column names values
                if (index == 0)
                {
                    result = getBulkProperties().get("ProbeID_Cy3");
                }
                else if (index == 1)
                {
                    result = getBulkProperties().get("ProbeID_Cy5");
                }
            }
            return result == null ? null : result.toString();
        }
        else
        {
            return getRequest().getParameter(SampleInfo.getNameFormElementID(index));
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
}
