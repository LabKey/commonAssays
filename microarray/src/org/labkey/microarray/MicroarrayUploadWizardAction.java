package org.labkey.microarray;

import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AssayDataCollector;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
@RequiresPermission(ACL.PERM_INSERT)
public class MicroarrayUploadWizardAction extends UploadWizardAction<AssayRunUploadForm>
{

    protected Map<String, String> getDefaultValues(String suffix, AssayRunUploadForm form) throws ExperimentException
    {
        Map<String, String> result = super.getDefaultValues(suffix, form);

        AssayDataCollector dataCollector = form.getSelectedDataCollector();

        if (RunStepHandler.NAME.equals(suffix))
        {
            // Make a copy so we can modify it
            result = new HashMap<String, String>(result);

            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();

            Document input = null;

            try
            {
                PropertyDescriptor[] runPDs = getProvider(form).getRunPropertyColumns(form.getProtocol());
                StringBuilder errors = new StringBuilder();
                for (PropertyDescriptor runPD : runPDs)
                {
                    String expression = runPD.getDescription();
                    if (expression != null)
                    {
                        try
                        {
                            XPathExpression xPathExpression = xPath.compile(expression);
                            if (input == null)
                            {
                                DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
                                dbfact.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
                                DocumentBuilder builder = dbfact.newDocumentBuilder();
                                input = builder.parse(new InputSource(new FileInputStream(dataCollector.createData(form).values().iterator().next())));
                            }
                            String value = xPathExpression.evaluate(input);
                            result.put(ColumnInfo.propNameFromName(runPD.getName()), value);
                        }
                        catch (XPathExpressionException e)
                        {
                            errors.append("Error parsing XPath expression '").append(expression).append("'. ");
                            if (e.getMessage() != null)
                            {
                                errors.append(e.getMessage());
                            }
                        }
                    }
                }

                if (errors.length() > 0)
                {
                    throw new ExperimentException(errors.toString());
                }

//                XPathExpression xPathDescriptionProducer = xPath.compile(
//                        "/MAGE-ML/Descriptions_assnlist/Description/Annotations_assnlist/OntologyEntry[@category='Producer']/@value");
//                XPathExpression xPathDescriptionVersion = xPath.compile(
//                        "/MAGE-ML/Descriptions_assnlist/Description/Annotations_assnlist/OntologyEntry[@category='Version']/@value");
//                XPathExpression xPathProtocol = xPath.compile(
//                        "/MAGE-ML/BioAssay_package/BioAssay_assnlist/MeasuredBioAssay/FeatureExtraction_assn/FeatureExtraction/ProtocolApplications_assnlist/ProtocolApplication/SoftwareApplications_assnlist/SoftwareApplication/ParameterValues_assnlist/ParameterValue[ParameterType_assnref/Parameter_ref/@identifier='Agilent.BRS:Parameter:Protocol_Name']/@value");
//                XPathExpression xPathGrid = xPath.compile(
//                        "/MAGE-ML/BioAssay_package/BioAssay_assnlist/MeasuredBioAssay/FeatureExtraction_assn/FeatureExtraction/ProtocolApplications_assnlist/ProtocolApplication/SoftwareApplications_assnlist/SoftwareApplication/ParameterValues_assnlist/ParameterValue[ParameterType_assnref/Parameter_ref/@identifier='Agilent.BRS:Parameter:Grid_Name']/@value");
//                XPathExpression xPathBarcode = xPath.compile(
//                        "/MAGE-ML/BioAssay_package/BioAssay_assnlist/MeasuredBioAssay/FeatureExtraction_assn/FeatureExtraction/ProtocolApplications_assnlist/ProtocolApplication/SoftwareApplications_assnlist/SoftwareApplication/ParameterValues_assnlist/ParameterValue[ParameterType_assnref/Parameter_ref/@identifier='Agilent.BRS:Parameter:FeatureExtractor_Barcode']/@value");

//                String descriptionProducer = xPathDescriptionProducer.evaluate(input);
//                String descriptionVersion = xPathDescriptionVersion.evaluate(input);
//                String description = descriptionProducer + " Version " + descriptionVersion;
//                String protocol = xPathProtocol.evaluate(input);
//                String grid = xPathGrid.evaluate(input);
//                String barcode = xPathBarcode.evaluate(input);
            }
            catch (IOException e)
            {
                throw new ExperimentException("Error parsing MAGE output", e);
            }
            catch (SAXException e)
            {
                throw new ExperimentException("Error parsing MAGE output", e);
            }
            catch (ParserConfigurationException e)
            {
                throw new ExperimentException("Error parsing MAGE output", e);
            }
        }

        return result;
    }

    protected void addSampleInputColumns(ExpProtocol protocol, InsertView insertView)
    {
        insertView.getDataRegion().addColumn(new SampleChooserDisplayColumn(MicroarrayAssayProvider.SAMPLE_COUNT));
    }

    private class SampleChooserDisplayColumn extends SimpleDisplayColumn
    {
        private final int _count;

        public SampleChooserDisplayColumn(int count)
        {
            _count = count;
            setCaption("Samples");
        }

        public boolean isEditable()
        {
            return true;
        }

        public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
        {
            Map<String, String> props = new HashMap<String, String>();

            for (int i = 0; i < _count; i++)
            {
                String lsidID = SampleInfo.getLsidFormElementID(i);
                String nameID = SampleInfo.getNameFormElementID(i);
                out.write("<input type=\"hidden\" name=\"" + lsidID + "\" id=\"" + lsidID + "\"/>\n");
                out.write("<input type=\"hidden\" name=\"" + nameID + "\" id=\"" + nameID + "\"/>\n");
            }

            props.put(SampleChooser.PROP_NAME_SAMPLE_COUNT, Integer.toString(_count));
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
}
