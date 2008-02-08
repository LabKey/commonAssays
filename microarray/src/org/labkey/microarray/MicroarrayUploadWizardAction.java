package org.labkey.microarray;

import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.data.ColumnInfo;
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

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
@RequiresPermission(ACL.PERM_INSERT)
public class MicroarrayUploadWizardAction extends UploadWizardAction<AssayRunUploadForm>
{

    protected Map<String, String> getDefaultValues(String suffix, AssayRunUploadForm form)
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
                for (PropertyDescriptor runPD : runPDs)
                {
                    if (runPD.getDescription() != null)
                    {
                        try
                        {
                            XPathExpression expression = xPath.compile(runPD.getDescription());
                            if (input == null)
                            {
                                DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
                                dbfact.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
                                DocumentBuilder builder = dbfact.newDocumentBuilder();
                                input = builder.parse(new InputSource(new FileInputStream(dataCollector.createData(form).values().iterator().next())));
                            }
                            String value = expression.evaluate(input);
                            result.put(ColumnInfo.propNameFromName(runPD.getName()), value);
                        }
                        catch (XPathExpressionException e)
                        {
                            // TODO - show error to user here?
                        }
                    }
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
                throw new RuntimeException("Error parsing MAGE output", e);
            }
            catch (SAXException e)
            {
                throw new RuntimeException("Error parsing MAGE output", e);
            }
            catch (ParserConfigurationException e)
            {
                throw new RuntimeException("Error parsing MAGE output", e);
            }
            catch (ExperimentException e)
            {
                throw new RuntimeException(e);
            }
        }

        return result;
    }
}
