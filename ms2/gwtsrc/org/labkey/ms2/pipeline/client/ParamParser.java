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
    protected   static String SEQUENCE_DB   = "pipeline, database";
    protected   static String TAXONOMY      = "protein, taxon";
    protected   static String ENZYME        = "protein, cleavage site";
    protected   static String STATIC_MOD    = "residue, modification mass";
    protected   static String DYNAMIC_MOD   = "residue, potential modification mass";

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

    public String getErrors()
    {
        if(error == null) return "";
        return error.toString();
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
            sb.append(text.getNodeValue());
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

    private void removeInputParameter(String name)
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
//        try
//        {
//            xmlWrapper.setText(toXml());
//        }
//        catch(SearchFormException e)
//        {
//            //sholdn't happen so throw runtime
//            throw new DOMParseException(e.getMessage());
//        }
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

    public void setSequenceDb(String db) throws SearchFormException
    {
        setInputParameter(SEQUENCE_DB, db);
    }

    public void setTaxonomy(String tax) throws SearchFormException
    {
        setInputParameter(TAXONOMY, tax);
    }

    public void setEnzyme(String enz) throws SearchFormException
    {
        setInputParameter(ENZYME, enz);
    }

    public void setStaticMods(Map<String, String> mods) throws SearchFormException
    {
        if(mods.size() == 0)
        {
            removeStaticMods();
            return;
        }
        StringBuffer valuesString = new StringBuffer();
        for(String mod : mods.values())
        {
            if(valuesString.length() > 0)
                valuesString.append(",");
            valuesString.append(mod);
        }
        setInputParameter(STATIC_MOD, valuesString.toString());
    }

    public void setDynamicMods(Map<String, String> mods) throws SearchFormException
    {
        if(mods.size() == 0)
        {
            removeDynamicMods();
            return;
        }
        StringBuffer valuesString = new StringBuffer();
        for(String mod : mods.values())
        {
            if(valuesString.length() > 0)
                valuesString.append(",");
            valuesString.append(mod);
        }
        setInputParameter(DYNAMIC_MOD, valuesString.toString());
    }

    public String getSequenceDb()
    {
        return getInputParameter(SEQUENCE_DB);
    }

    public String getTaxonomy()
    {
        return getInputParameter(TAXONOMY);
    }

    public String getEnzyme()
    {
        return getInputParameter(ENZYME);
    }

    public String getStaticMods()
    {
        return getInputParameter(STATIC_MOD);
    }

    public String getDynamicMods()
    {
        return getInputParameter(DYNAMIC_MOD);
    }

    public void removeSequenceDb()
    {
        removeInputParameter(SEQUENCE_DB);
    }

    public void removeTaxonomy()
    {
        removeInputParameter(TAXONOMY);
    }

    public void removeEnzyme()
    {
        removeInputParameter(ENZYME);
    }

    public void removeStaticMods()
    {
        removeInputParameter(STATIC_MOD);
    }

    public void removeDynamicMods()
    {
        removeInputParameter(DYNAMIC_MOD);
    }

    public void setInputParameter(String name, String value) throws SearchFormException
    {
        if(name == null) throw new SearchFormException("Parameter name is null.");
        if(value == null) value = "";
        removeInputParameter(name);
        Element ip = getDocument().createElement(TAG_NOTE);
        ip.setAttribute(ATTR_TYPE, VAL_INPUT);
        ip.setAttribute(ATTR_LABEL,name);
        ip.appendChild(getDocument().createTextNode(value));
        Element de = getDocumentElement();
        if(de == null) return;
        Node before = de.getFirstChild();
        de.insertBefore(ip,before);
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
                Node n = elNote.getFirstChild();
                if(n == null) return "";
                return n.getNodeValue();
            }
        }
        return "";
    }

    private boolean isInputParameterElement(String name, Element elNote)
    {
        String type = elNote.getAttribute(ATTR_TYPE);
        return (type.equals(VAL_INPUT) && name.equals(elNote.getAttribute(ATTR_LABEL)));
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
