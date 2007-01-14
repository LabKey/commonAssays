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
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.ArrayList;

/**
 * BioMLInputParser class
 * <p/>
 * Created: Nov 17, 2006
 *
 * @author bmaclean
 */
public class BioMLInputParser extends XMLValidationParser
{
    /**
     * Override this function to further validate specific parameters.
     */
    public void validateDocument()
    {
        Element el = _doc.getDocumentElement();
        if (!el.getTagName().equals("bioml"))
            addError(new Error("Root tag name should be 'bioml'"));
        NodeList notes = el.getChildNodes();
        for (int i = 0; i < notes.getLength(); i++)
        {
            Node child = notes.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element elNote = (Element) child;
            if (!"note".equals(elNote.getNodeName()))
            {
                addError(new Error("Tag '" + elNote.getNodeName() + "' not supported."));
                continue;
            }

            String type = elNote.getAttribute("type");
            if (type == null || type.length() == 0 || "description".equals(type))
                continue;

            if (!"input".equals(type))
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
        Element elParameter = _doc.createElement("note");
        elParameter.setAttribute("type", "input");
        elParameter.setAttribute("label", name);
        elParameter.setTextContent(value);

        Node beforeNode = null;
        if (before != null)
        {
            NodeList notes = el.getElementsByTagName("note");
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
        NodeList notes = el.getElementsByTagName("note");
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

    public String[] getInputParameterNames ()
    {
        ArrayList<String> names = new ArrayList<String>();
        Element el = _doc.getDocumentElement();
        NodeList notes = el.getElementsByTagName("note");
        for (int i = 0; i < notes.getLength(); i++)
        {
            Element elNote = (Element) notes.item(i);
            if ("input".equals(elNote.getAttribute("type")))
            {
                names.add (elNote.getAttribute("label"));
            }
        }
        return names.toArray(new String[names.size()]);
    }

    private boolean isInputParameterElement(String name, Element elNote)
    {
        String type = elNote.getAttribute("type");
        return ("input".equals(type) && name.equals(elNote.getAttribute("label")));
    }
}
