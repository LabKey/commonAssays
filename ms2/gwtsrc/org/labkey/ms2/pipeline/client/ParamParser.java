/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

package org.labkey.ms2.pipeline.client;

import com.google.gwt.xml.client.*;
import com.google.gwt.xml.client.impl.DOMParseException;
import com.google.gwt.user.client.ui.HasText;

import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Apr 21, 2008
 */

/**
 * <code>ParamParser</code>
 */
public class ParamParser
{
    public      static String TAG_BIOML     = "bioml";
    public      static String TAG_NOTE      = "note";
    public      static String ATTR_LABEL    = "label";
    public      static String ATTR_TYPE     = "type";
    public      static String VAL_INPUT     = "input";
    protected   static String XML_HEADER    = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                "<bioml>\n";
    protected   static String XML_FOOTER    = "</bioml>";

    StringBuffer error;
    Document xmlDoc;
    HasText xmlWrapper;

    public ParamParser(HasText xml)
    {
        xmlWrapper = xml;
    }

    private void appendError(String s)
    {
        if(this.error == null)
            this.error = new StringBuffer();
        if(error.length() > 0) error.append("\n");
        error.append(s);
    }

    public String toXml()throws SearchFormException
    {
        NodeList nodes = getAllNodes();
        if(nodes == null || nodes.getLength() == 0 ) return "";
        StringBuffer sb = new StringBuffer();
        sb.append(XML_HEADER);
        int nodeCount = nodes.getLength();
        for(int i = 0; i < nodeCount; i++)
        {
            Node node = nodes.item(i);
            short nodeType = node.getNodeType();
            if(nodeType == Node.ELEMENT_NODE)
            {
                sb.append(element2String(node));
            }
            else if(nodeType == Node.COMMENT_NODE)
            {
                sb.append(comment2String(node));
            }
        }
        sb.append(XML_FOOTER);
        return sb.toString();
    }

    private String element2String(Node node)
    {
        if(node == null)return "";
        StringBuffer sb = new StringBuffer();
        Element el = (Element)node;
        Text text = (Text)el.getFirstChild();

        sb.append("    <note label=\"");
        sb.append(el.getAttribute("label"));
        sb.append("\" type=\"input\">");
        if(text == null)
        {
            sb.append("");
        }
        else
        {
            // The node value isn't XML encoded, so be sure to escape any special characters when building up the XML
            sb.append(encode(text.getNodeValue()));
        }
        sb.append("</note>\n");
        return sb.toString();
    }

    private String comment2String(Node node)
    {
        StringBuffer sb = new StringBuffer();
        Comment com = (Comment)node;
        sb.append("    <!-- ");
        sb.append(com.getData());
        sb.append(" -->\n");
        return sb.toString();
    }

    public String validate()
    {
        try
        {
            refresh();
        }
        catch(SearchFormException e)
        {
            return e.getMessage();
        }
        Element el;
        try
        {
            el = getDocumentElement();
        }
        catch(SearchFormException e)
        {
            return "The input XML has syntax errors: " + e.getMessage();
        }
        if(!el.getTagName().equals(TAG_BIOML))
            return "Root tag name should be '" + TAG_BIOML + "'.";
        NodeList notes = el.getChildNodes();
        ArrayList foundList = new ArrayList();
        for(int i = 0; i < notes.getLength(); i++)
        {
            Node child = notes.item(i);
            if(child.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element elNote = (Element)child;
            if(!elNote.getNodeName().equals(TAG_NOTE))
                return "Tag '" + elNote.getNodeName() + "' is not supported.";
            String type = elNote.getAttribute(ATTR_TYPE);
            if(type == null || type.length() == 0 || type.equals("description"))
                continue;
            if(!type.equals(VAL_INPUT))
            {
                return "Note type '" + type + "' is not supported";
            }
            String label = elNote.getAttribute(ATTR_LABEL);
            if(foundList.contains(label))
            {
                return "The \"" + label + "\" label appears more than once in the input XML.";
            }
            foundList.add(label);
        }
        return "";
    }

    public void removeInputParameter(String name)
    {
        if(name == null || name.equals("")) return;
        NodeList notes = getNoteElements();
        if(notes == null) return;
        for(int i = 0; i < notes.getLength(); i++)
        {
            Element elNote = (Element)notes.item(i);
            if(isInputParameterElement(name, elNote))
                removeNode(elNote);
        }
    }

    private void removeNode(Node node)
    {
        if(node == null) return;
        Element el;
        try
        {
            el = getDocumentElement();
        }
        catch(SearchFormException e)
        {
            return;
        }
        el.removeChild(node);
    }

    public void setInputParameter(String name, String value) throws SearchFormException
    {
        if(name == null) throw new SearchFormException("Parameter name is null.");
        if (value != null)
        {
            value = value.trim();
        }
        removeInputParameter(name);
        Element ip = getDocument().createElement(TAG_NOTE);
        ip.setAttribute(ATTR_TYPE, VAL_INPUT);
        ip.setAttribute(ATTR_LABEL,name);
        ip.appendChild(getDocument().createTextNode(value));
        Element de = getDocumentElement();
        if(de == null) return;
        de.appendChild(ip);
    }

    /** Simple XML encoding of a string */
    private String encode(String value)
    {
        if(value == null) value = "";
        value = value.replace("&", "&amp;");
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");
        value = value.replace("\"", "&quot;");
        return value;
    }

    public Map<String, String> getParameters()
    {
        NodeList notes = getNoteElements();
        if (notes == null)
        {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<String, String>();
        for(int i = 0; i < notes.getLength(); i++)
        {
            Element elNote = (Element)notes.item(i);
            String value = getTrimmedTextValue(elNote.getFirstChild());
            result.put(elNote.getAttribute(ATTR_LABEL), value);
        }
        return result;
    }

    public String getInputParameter(String name)
    {
        if(name == null) return "";
        NodeList notes = getNoteElements();
        if(notes == null) return null;
        for(int i = 0; i < notes.getLength(); i++)
        {
            Element elNote = (Element)notes.item(i);
            if(isInputParameterElement(name, elNote))
            {
                return getTrimmedTextValue(elNote.getFirstChild());
            }
        }
        return "";
    }

    private String getTrimmedTextValue(Node node)
    {
        if (node == null)
        {
            return "";
        }
        String result = node.getNodeValue();
        return result == null ? "" : result.trim();
    }

    private boolean isInputParameterElement(String name, Element elNote)
    {
        String type = elNote.getAttribute(ATTR_TYPE);
        return VAL_INPUT.equals(type) && name.equals(elNote.getAttribute(ATTR_LABEL));
    }

    private NodeList getNoteElements()
    {
        Element el;
        try
        {
            el = getDocumentElement();
        }
        catch(SearchFormException e)
        {
            appendError(e.getMessage());
            return null;
        }
        if(el == null) return null;
        return el.getElementsByTagName(TAG_NOTE);

    }

    private NodeList getAllNodes() throws SearchFormException
    {
        Element el = getDocumentElement();
        if(el == null) return null;
        return el.getChildNodes();
    }

    private Element getDocumentElement() throws SearchFormException
    {
        Document xmlDoc = getDocument();
        if(xmlDoc == null) return null;
        return xmlDoc.getDocumentElement();
    }

    private void refresh() throws SearchFormException
    {
        try
        {
            xmlDoc =  XMLParser.parse(xmlWrapper.getText());
        }
        catch(DOMParseException e)
        {
            throw new SearchFormException("Invalid XML. Please check your input.");
        }
    }

    public void writeXml() throws SearchFormException
    {
        xmlWrapper.setText(toXml());
    }

    private Document getDocument() throws SearchFormException
    {
        if( xmlDoc == null)
        {
            try
            {
                xmlDoc =  XMLParser.parse(xmlWrapper.getText());
            }
            catch(DOMParseException e)
            {
                throw new SearchFormException(e);
            }
        }
        return xmlDoc;
    }
}
