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
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.ExperimentException;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.springframework.validation.ObjectError;
import org.springframework.validation.BindException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.io.FileInputStream;
import java.io.IOException;/*
 * User: brittp
 * Date: Jan 19, 2009
 * Time: 2:29:57 PM
 */

public class MicroarrayRunUploadForm extends AssayRunUploadForm<MicroarrayAssayProvider>
{
    private Map<DomainProperty, String> _mageMLProperties;
    private Document _mageML;

    public Document getMageML(BindException errors)
    {
        if (_mageML == null)
        {
            try
            {
                DocumentBuilderFactory dbfact = DocumentBuilderFactory.newInstance();
                dbfact.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
                DocumentBuilder builder = dbfact.newDocumentBuilder();
                _mageML = builder.parse(new InputSource(new FileInputStream(getSelectedDataCollector().createData(this).values().iterator().next())));
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
        return _mageML;
    }

    public Map<DomainProperty, String> getMageMLProperties(BindException errors)
    {
        if (_mageMLProperties == null)
        {
            _mageMLProperties = new HashMap<DomainProperty, String>();
            // Create a document for the MageML
            Document document = getMageML(errors);
            if (document == null)
                return Collections.emptyMap();

            AssayProvider provider = AssayService.get().getProvider(getProtocol());
            Domain domain = provider.getRunInputDomain(getProtocol());
            for (DomainProperty runPD : domain.getProperties())
            {
                String expression = runPD.getDescription();
                if (expression != null)
                {
                    // We use the description of the property descriptor as the XPath. Far from ideal.
                    try
                    {
                        String value = evaluateXPath(document, expression);
                        _mageMLProperties.put(runPD, value);
                    }
                    catch (XPathExpressionException e)
                    {
                        // User isn't required to use the description as an XPath
                    }
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
    public Map<DomainProperty, String> getDefaultValues(Domain domain, BindException errors)
    {
        Map<DomainProperty, String> defaults = super.getDefaultValues(domain, errors);
        if (!isResetDefaultValues() && UploadWizardAction.RunStepHandler.NAME.equals(getUploadStep()))
        {
            for (Map.Entry<DomainProperty, String> entry : getMageMLProperties(errors).entrySet())
                defaults.put(entry.getKey(), entry.getValue());
        }
        return defaults;
    }
}