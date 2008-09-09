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
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.view.InsertView;
import org.labkey.api.view.GWTView;
import org.labkey.microarray.sampleset.client.SampleChooser;
import org.labkey.microarray.sampleset.client.SampleInfo;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
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
import java.io.Writer;
import java.io.FileInputStream;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
@RequiresPermission(ACL.PERM_INSERT)
public class MicroarrayUploadWizardAction extends UploadWizardAction<AssayRunUploadForm>
{
    private static final String CHANNEL_COUNT_XPATH = "/MAGE-ML/BioAssay_package/BioAssay_assnlist/MeasuredBioAssay/FeatureExtraction_assn/FeatureExtraction/ProtocolApplications_assnlist/ProtocolApplication/SoftwareApplications_assnlist/SoftwareApplication/ParameterValues_assnlist/ParameterValue[ParameterType_assnref/Parameter_ref/@identifier='Agilent.BRS:Parameter:Scan_NumChannels']/@value";
    private static final String CHANNEL_COUNT_FORM_ELEMENT_NAME = "__channelCount";

    protected void addSampleInputColumns(ExpProtocol protocol, InsertView insertView)
    {
        String channelCountString = insertView.getDataRegion().getHiddenFormFieldValue(CHANNEL_COUNT_FORM_ELEMENT_NAME);
        int minSamples = MicroarrayAssayProvider.MIN_SAMPLE_COUNT;
        int maxSamples = MicroarrayAssayProvider.MAX_SAMPLE_COUNT;
        if (channelCountString != null)
        {
            minSamples = maxSamples = Integer.parseInt(channelCountString);
        }

        insertView.getDataRegion().addDisplayColumn(new SampleChooserDisplayColumn(minSamples, maxSamples));
    }

    private class SampleChooserDisplayColumn extends SimpleDisplayColumn
    {
        private final int _minCount;
        private final int _maxCount;

        public SampleChooserDisplayColumn(int minCount, int maxCount)
        {
            _minCount = minCount;
            _maxCount = maxCount;
            setCaption("Samples");
        }

        public boolean isEditable()
        {
            return true;
        }

        public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
        {
            Map<String, String> props = new HashMap<String, String>();

            out.write("<input type=\"hidden\" name=\"" + SampleChooser.SAMPLE_COUNT_ELEMENT_NAME + "\" id=\"" + SampleChooser.SAMPLE_COUNT_ELEMENT_NAME + "\"/>\n");

            for (int i = 0; i < _maxCount; i++)
            {
                String lsidID = SampleInfo.getLsidFormElementID(i);
                String nameID = SampleInfo.getNameFormElementID(i);
                out.write("<input type=\"hidden\" name=\"" + lsidID + "\" id=\"" + lsidID + "\"/>\n");
                out.write("<input type=\"hidden\" name=\"" + nameID + "\" id=\"" + nameID + "\"/>\n");
            }

            props.put(SampleChooser.PROP_NAME_MAX_SAMPLE_COUNT, Integer.toString(_maxCount));
            props.put(SampleChooser.PROP_NAME_MIN_SAMPLE_COUNT, Integer.toString(_minCount));
            ExpSampleSet sampleSet = ExperimentService.get().lookupActiveSampleSet(ctx.getContainer());
            if (sampleSet != null)
            {
                props.put(SampleChooser.PROP_NAME_DEFAULT_SAMPLE_SET_LSID, sampleSet.getLSID());
                props.put(SampleChooser.PROP_NAME_DEFAULT_SAMPLE_SET_NAME, sampleSet.getName());
                props.put(SampleChooser.PROP_NAME_DEFAULT_SAMPLE_ROW_ID, Integer.toString(sampleSet.getRowId()));
            }
            GWTView view = new GWTView(SampleChooser.class, props);
            try
            {
                view.render(ctx.getRequest(), ctx.getViewContext().getResponse());
            }
            catch (Exception e)
            {
                throw (IOException)new IOException().initCause(e);
            }
        }
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
            if (expression != null && document != null)
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
            String channelCountString = evaluateXPath(document, CHANNEL_COUNT_XPATH);
            if (channelCountString != null)
            {
                try
                {
                    int channelCount = Integer.parseInt(channelCountString);
                    result.getDataRegion().addHiddenFormField(CHANNEL_COUNT_FORM_ELEMENT_NAME, Integer.toString(channelCount));
                }
                catch (NumberFormatException e)
                {
                    // Continue on, the user can choose the count themselves
                }
            }
        }
        catch (XPathExpressionException e)
        {
            throw new RuntimeException("Invalid Channel Count XPath", e);
        }
        return result;
    }

    private String evaluateXPath(Document document, String expression) throws XPathExpressionException
    {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        XPathExpression xPathExpression = xPath.compile(expression);
        return xPathExpression.evaluate(document);
    }
}
