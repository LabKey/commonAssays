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
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;

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
import java.io.FileNotFoundException;

/**
 * User: jeckels
 * Date: Feb 6, 2008
 */
@RequiresPermission(ACL.PERM_INSERT)
public class MicroarrayUploadWizardAction extends UploadWizardAction<AssayRunUploadForm>
{

    private XPathExpression compileXPathExpression(String expression) throws XPathExpressionException
    {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        return xPath.compile(expression);
    }

    protected void addSampleInputColumns(ExpProtocol protocol, InsertView insertView)
    {
        insertView.getDataRegion().addColumn(new SampleChooserDisplayColumn(MicroarrayAssayProvider.MIN_SAMPLE_COUNT, MicroarrayAssayProvider.MAX_SAMPLE_COUNT));
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

        Document input = null;

        Map<String, String> hiddenElements = new HashMap<String, String>();
        AssayDataCollector dataCollector = form.getSelectedDataCollector();

        for (Map.Entry<PropertyDescriptor, String> entry : allProperties.entrySet())
        {
            PropertyDescriptor runPD = entry.getKey();
            String expression = runPD.getDescription();
            if (expression != null)
            {
                try
                {
                    XPathExpression xPathExpression = compileXPathExpression(expression);
                    if (input == null)
                    {
                        DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
                        dbfact.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
                        DocumentBuilder builder = dbfact.newDocumentBuilder();
                        input = builder.parse(new InputSource(new FileInputStream(dataCollector.createData(form).values().iterator().next())));
                    }
                    String value = xPathExpression.evaluate(input);

                    hiddenElements.put(ColumnInfo.propNameFromName(runPD.getName()), value);
                }
                catch (XPathExpressionException e)
                {
                    userProperties.put(runPD, entry.getValue());
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
        return result;
    }
}
