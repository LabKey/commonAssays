/*
 * Copyright (c) 2005 LabKey Software, LLC
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
package org.labkey.ms2.pipeline;

import org.labkey.api.util.XMLValidationParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * BioMLInputParser class
 * <p/>
 * Created: Nov 17, 2006
 *
 * @author bmaclean
 */
public class BioMLInputParser extends XMLValidationParser
{
    private static String TAG_BIOML = "bioml";
    private static String TAG_NOTE = "note";
    private static String ATTR_LABEL = "label";
    private static String ATTR_TYPE = "type";
    private static String VAL_INPUT = "input";

    /**
     * Override this function to further validate specific parameters.
     */
    public void validateDocument()
    {
        Element el = _doc.getDocumentElement();
        if (!TAG_BIOML.equals(el.getTagName()))
            addError(new Error("Root tag name should be 'bioml'"));
        NodeList notes = el.getChildNodes();
        for (int i = 0; i < notes.getLength(); i++)
        {
            Node child = notes.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element elNote = (Element) child;
            if (!TAG_NOTE.equals(elNote.getNodeName()))
            {
                addError(new Error("Tag '" + elNote.getNodeName() + "' not supported."));
                continue;
            }

            String type = elNote.getAttribute(ATTR_TYPE);
            if (type == null || type.length() == 0 || "description".equals(type))
                continue;

            if (!VAL_INPUT.equals(type))
            {
                addError(new Error("Note type '" + type + "' not supported."));
                continue;
            }
        }
    }

    public String getInputParameter(String name)
    {
        Element el = _doc.getDocumentElement();
        NodeList notes = el.getElementsByTagName("note");
        for (int i = 0; i < notes.getLength(); i++)
        {
            Element elNote = (Element) notes.item(i);
            if (isInputParameterElement(name, elNote))
                return elNote.getTextContent();
        }
        return null;
    }

    public void setInputParameter(String name, String value)
    {
        setInputParameter(name, value, null);
    }

    public void setInputParameter(String name, String value, String before)
    {
        removeInputParameter(name);

        Element el = _doc.getDocumentElement();
        Element elParameter = _doc.createElement(TAG_NOTE);
        elParameter.setAttribute(ATTR_TYPE, VAL_INPUT);
        elParameter.setAttribute(ATTR_LABEL, name);
        elParameter.setTextContent(value);

        Node beforeNode = null;
        if (before != null)
        {
            NodeList notes = el.getElementsByTagName(TAG_NOTE);
            for (int i = 0; i < notes.getLength(); i++)
            {
                Element elNote = (Element) notes.item(i);
                if (isInputParameterElement(name, elNote))
                {
                    beforeNode = elNote;
                    break;
                }
            }
        }

        if (beforeNode == null)
            el.appendChild(elParameter);
        else
            el.insertBefore(elParameter, beforeNode);
    }

    public String removeInputParameter(String name)
    {
        String value = null;
        Element el = _doc.getDocumentElement();
        NodeList notes = el.getElementsByTagName(TAG_NOTE);
        for (int i = 0; i < notes.getLength(); i++)
        {
            Element elNote = (Element) notes.item(i);
            if (isInputParameterElement(name, elNote))
            {
                value = elNote.getTextContent();
                el.removeChild(elNote);
                break;
            }
        }
        return value;
    }

    public String[] getInputParameterNames()
    {
        ArrayList<String> names = new ArrayList<String>();
        Element el = _doc.getDocumentElement();
        NodeList notes = el.getElementsByTagName(TAG_NOTE);
        for (int i = 0; i < notes.getLength(); i++)
        {
            Element elNote = (Element) notes.item(i);
            if (VAL_INPUT.equals(elNote.getAttribute(ATTR_TYPE)))
            {
                names.add(elNote.getAttribute(ATTR_LABEL));
            }
        }
        return names.toArray(new String[names.size()]);
    }

    public Map<String, String> getInputParameters()
    {
        Map<String, String> parameters = new HashMap<String, String>();
        if (_doc != null)
        {
            Element el = _doc.getDocumentElement();
            NodeList notes = el.getElementsByTagName(TAG_NOTE);
            for (int i = 0; i < notes.getLength(); i++)
            {
                Element elNote = (Element) notes.item(i);
                if (VAL_INPUT.equals(elNote.getAttribute(ATTR_TYPE)))
                {
                    parameters.put(elNote.getAttribute(ATTR_LABEL), elNote.getTextContent());
                }
            }
        }

        return parameters;
    }

    private boolean isInputParameterElement(String name, Element elNote)
    {
        String type = elNote.getAttribute(ATTR_TYPE);
        return (VAL_INPUT.equals(type) && name.equals(elNote.getAttribute(ATTR_LABEL)));
    }

    public static void writeFromMap(Map<String, String> props, File fileDest) throws IOException
    {
        BioMLInputParser parser = new BioMLInputParser();
        String xmlEmpty = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<bioml>\n" +
                "</bioml>";
        parser.parse(xmlEmpty);
        String[] keys = props.keySet().toArray(new String[props.size()]);
        Arrays.sort(keys);
        for (String key : keys)
            parser.setInputParameter(key, props.get(key));

        String xml = parser.getXML();

        BufferedWriter inputWriter = null;
        try
        {
            inputWriter = new BufferedWriter(new FileWriter(fileDest));
            inputWriter.write(xml);
        }
        finally
        {
            if (inputWriter != null)
                inputWriter.close();
        }        
    }
}
