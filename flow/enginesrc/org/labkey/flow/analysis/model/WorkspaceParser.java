/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
package org.labkey.flow.analysis.model;

import org.apache.log4j.Logger;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XNIException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * User: kevink
 * Date: 2/8/12
 */
public class WorkspaceParser
{
    protected static final String GATING_1_5_NS = "http://www.isac-net.org/std/Gating-ML/v1.5/gating";
    protected static final String TRANSFORMATIONS_1_5_NS = "http://www.isac-net.org/std/Gating-ML/v1.5/transformations";
    protected static final String DATATYPES_1_5_NS = "http://www.isac-net.org/std/Gating-ML/v1.5/datatypes";
    protected static final String COMPENSATION_1_5_NS = "http://www.isac-net.org/std/Gating-ML/v1.5/compensation";

    // FlowJo v10.0.0 through v10.0.5 uses these incorrect namespaces
    protected static final String FJ_GATING_1_5_NS = "http://flowcyt.sourceforge.net/std/Gating-ML/v1.5/gating";
    protected static final String FJ_TRANSFORMATIONS_1_5_NS = "http://flowcyt.sourceforge.net/std/Gating-ML/v1.5/transformations";
    protected static final String FJ_DATATYPES_1_5_NS = "http://flowcyt.sourceforge.net/std/Gating-ML/v1.5/datatypes";
    protected static final String FJ_COMPENSATION_1_5_NS = "http://flowcyt.sourceforge.net/std/Gating-ML/v1.5/compensation";

    protected static final String GATING_2_0_NS = "http://www.isac-net.org/std/Gating-ML/v2.0/gating";
    protected static final String TRANSFORMATIONS_2_0_NS = "http://www.isac-net.org/std/Gating-ML/v2.0/transformations";
    protected static final String DATATYPES_2_0_NS = "http://www.isac-net.org/std/Gating-ML/v2.0/datatypes";
    protected static final String COMPENSATION_2_0_NS = "http://www.isac-net.org/std/Gating-ML/v2.0/datatypes";

    protected static Map<String, String> GATINGML_1_5_PREFIX_MAP;
    static
    {
        GATINGML_1_5_PREFIX_MAP = new HashMap<>();
        GATINGML_1_5_PREFIX_MAP.put("gating", GATING_1_5_NS);
        GATINGML_1_5_PREFIX_MAP.put("transforms", TRANSFORMATIONS_1_5_NS);
        GATINGML_1_5_PREFIX_MAP.put("data-type", DATATYPES_1_5_NS);
        GATINGML_1_5_PREFIX_MAP.put("comp", COMPENSATION_1_5_NS);
    }

    protected static Set<String> GATINGML_1_5_NAMESPACES;
    static
    {
        GATINGML_1_5_NAMESPACES = new HashSet<>();
        GATINGML_1_5_NAMESPACES.add(GATING_1_5_NS);
        GATINGML_1_5_NAMESPACES.add(TRANSFORMATIONS_1_5_NS);
        GATINGML_1_5_NAMESPACES.add(DATATYPES_1_5_NS);
        GATINGML_1_5_NAMESPACES.add(COMPENSATION_1_5_NS);
    }

    // Map from incorrect to correct namespace URI
    protected static Map<String, String> FJ_GATINGML_1_5_NAMEPSACE_FIXUP;
    static
    {
        FJ_GATINGML_1_5_NAMEPSACE_FIXUP = new HashMap<>();
        FJ_GATINGML_1_5_NAMEPSACE_FIXUP.put(FJ_GATING_1_5_NS, GATING_1_5_NS);
        FJ_GATINGML_1_5_NAMEPSACE_FIXUP.put(FJ_TRANSFORMATIONS_1_5_NS, TRANSFORMATIONS_1_5_NS);
        FJ_GATINGML_1_5_NAMEPSACE_FIXUP.put(FJ_DATATYPES_1_5_NS, DATATYPES_1_5_NS);
        FJ_GATINGML_1_5_NAMEPSACE_FIXUP.put(FJ_COMPENSATION_1_5_NS, COMPENSATION_1_5_NS);
    }

    protected static Map<String, String> GATINGML_2_0_PREFIX_MAP;
    static
    {
        GATINGML_2_0_PREFIX_MAP = new HashMap<>();
        GATINGML_2_0_PREFIX_MAP.put("gating", GATING_2_0_NS);
        GATINGML_2_0_PREFIX_MAP.put("transforms", TRANSFORMATIONS_2_0_NS);
        GATINGML_2_0_PREFIX_MAP.put("data-type", DATATYPES_2_0_NS);
        GATINGML_2_0_PREFIX_MAP.put("comp", COMPENSATION_2_0_NS);
    }

    protected static Set<String> GATINGML_2_0_NAMESPACES;
    static
    {
        GATINGML_2_0_NAMESPACES = new HashSet<>();
        GATINGML_2_0_NAMESPACES.add(GATING_2_0_NS);
        GATINGML_2_0_NAMESPACES.add(TRANSFORMATIONS_2_0_NS);
        GATINGML_2_0_NAMESPACES.add(DATATYPES_2_0_NS);
        GATINGML_2_0_NAMESPACES.add(COMPENSATION_2_0_NS);
    }

    static public boolean isFlowJoWorkspace(File file)
    {
        if (file.isDirectory())
            return false;
        if (file.getName().endsWith(".wsp") || file.getName().endsWith(".WSP"))
            return true;
        if (!file.getName().endsWith(".xml"))
            return false;
        WorkspaceRecognizer recognizer = new WorkspaceRecognizer();
        try
        {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

            parser.parse(file, recognizer);
        }
        catch (Exception e)
        {
            // suppress
        }
        return recognizer.isWorkspace();
    }


    /** For debugging. */
    static public Document parseXml(InputStream stream) throws Exception
    {
        DOMParser p = FJDOMParser.create();
        p.parse(new InputSource(stream));
        return p.getDocument();
    }


    static private class WorkspaceRecognizer extends DefaultHandler
    {
        boolean _isWorkspace = false;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            _isWorkspace = "Workspace".equals(qName);
            throw new SAXException("Stop parsing");
        }
        boolean isWorkspace()
        {
            return _isWorkspace;
        }
    }


    final static String[] parsedElements =
            {
                    "AndNode",
                    "AutoCompensationScripts",
                    "Axis",
                    "BooleanGate",
                    "CalibrationTables",
                    "Channel",
                    "ChannelValue",
                    "CompensationMatrices",
                    "CompensationMatrix",
                    "Dependent",
                    "Dependents",
                    "Ellipse",
                    "EllipseGate",
                    "FCSHeader",
                    "Gate",
                    "GatePaths",
                    "Group",
                    "GroupAnalyses",
                    "GroupNode",
                    "Groups",
                    "Keyword",
                    "Keywords",
                    "MatchingCriteria",
                    "NotNode",
                    "OrNode",
                    "Parameter",
                    "ParameterDefinition",
                    "Point",
                    "PolyRect",
                    "Polygon",
                    "PolygonGate",
                    "Population",
                    "Range",
                    "RangeGate",
                    "RectangleGate",
                    "Sample",
                    "SampleAnalyses",
                    "SampleID",
                    "SampleList",
                    "SampleNode",
                    "SampleRef",
                    "SampleRefs",
                    "Samples",
                    "Script",
                    "Statistic",
                    "String",
                    "StringArray",
                    "Subpopulations",
                    "Table",
                    // FlowJo 10.0.6 adds <Transformations> under <Sample> node and contains parameter transforms (transforms:linear, transforms:biex)
                    "Transformations",
                    "ValidateCompensation",
                    "Vertex",
                    "Workspace",
                    "and",
                    "ellipse",
                    "focus",
                    "interval",
                    "not",
                    "or",
                    "point",
                    "polygon"
            };


    final static String[] rejectElements =
            {
                    "Layout",
                    "LayoutEditor",
                    "LayoutGraph",
                    "OverlayGraphs",
                    "TableEditor"
            };


    final static String[] knownElements =
            {
                    "Annotation",
                    "AnnotationTextTraits",
                    "AutoCompensationScripts",
                    "Axis",
                    "AxisLabelText",
                    "AxisText",
                    "BooleanGate",
                    "CalibrationTables",
                    "Channel",
                    "ChannelValue",
                    "Column",
                    "Columns",
                    "CompensationMatrices",
                    "CompensationMatrix",
                    "Contents",
                    "Criteria",
                    "CriteriaFormula",
                    "Criterion",
                    "DT_32BitKeepAsLinear",
                    "DataSet",
                    "EventLimit",
                    "GatePaths",
                    "GateText",
                    "Graph",
                    "GraphInformation", // Replaces "Graph" in FlowJo v9.7.2
                    "Group",
                    "GroupNode",
                    "Groups",
                    "Keyword",
                    "Keywords",
                    "Layer",
                    "Layout",
                    "LayoutEditor",
                    "LayoutGraph",
                    "Legend",
                    "LegendTextTraits",
                    "OverlayGraphs",
                    "PCPlotBlueControl",
                    "PCPlotGreenControl",
                    "PCPlotRedControl",
                    "Parameter",
                    "ParameterNames",
                    "PolyChromaticPlot",
                    "PolyRect",
                    "Polygon",
                    "PolygonGate",
                    "Population",
                    "Preferences",
                    "Sample",
                    "SampleID",
                    "SampleList",
                    "SampleNode",
                    "SampleRef",
                    "SampleRefs",
                    "Samples",
                    "SampleSortCriteria",
                    "SciBook",
                    "StainChannelList",
                    "StainCriterion",
                    "String",
                    "StringArray",
                    "Table",
                    "TableEditor",
                    "Text",
                    "TextTraits",
                    "Vertex",
                    "WindowPosition",
                    "Workspace",
                    "graphList",
                    "subsetList"
            };


    static final short defaultFilter = LSParserFilter.FILTER_SKIP;

    final static HashMap<String,Short> noNamespaceElements = new HashMap<>(100);

    static
    {
        for (String s : knownElements)
            noNamespaceElements.put(s, defaultFilter);
        for (String s : rejectElements)
            noNamespaceElements.put(s, LSParserFilter.FILTER_REJECT);
        for (String s : parsedElements)
            noNamespaceElements.put(s, LSParserFilter.FILTER_ACCEPT);
    }

    static class FJErrorHandler implements ErrorHandler
    {
        public void warning(SAXParseException exception) throws SAXException
        {
            // ignore
        }

        public void error(SAXParseException exception) throws SAXException
        {
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException
        {
            String msg = exception.getLocalizedMessage();
            if (msg != null)
            {
                // ignore malformed XML in <OverlayGraphs> element
                if (msg.contains("OverlayGraphs") && (msg.contains("xParameter") || msg.contains("yParameter")))
                    return;
            }
            throw exception;
        }
    }


    static class FJParseFilter implements LSParserFilter
    {
        SymbolTable fSymbolTable = new SymbolTable();
        Set<String> rejected = new HashSet<>();

        public short startElement(Element element)
        {
            String localName = element.getLocalName();
            String nsURI = element.getNamespaceURI();

            short filter = defaultFilter;
            if (noNamespaceElements.containsKey(localName))
                filter = noNamespaceElements.get(localName);
            else if (nsURI != null && (GATINGML_1_5_NAMESPACES.contains(nsURI) || FJ_GATINGML_1_5_NAMEPSACE_FIXUP.containsKey(nsURI) || GATINGML_2_0_NAMESPACES.contains(nsURI)))
                filter = FILTER_ACCEPT;

//            if (filter != FILTER_ACCEPT && rejected.add(element.getNodeName())) System.err.println((filter == FILTER_SKIP ? "SKIPPED:  " : "REJECTED: ") + element.getNodeName());
            return filter;
        }

        public short acceptNode(Node node)
        {
            if (node instanceof Text)
            {
                String data = ((Text)node).getData();
                if (data.length() < 10 && data.trim().length() == 0)
                    return FILTER_REJECT;
                else
                    return FILTER_ACCEPT;
            }
            if (node instanceof Element)
            {
                int len = node.getAttributes().getLength();
                for (int i=0 ; i<len ; i++)
                {
                    Attr a = (Attr)node.getAttributes().item(i);
                    a.setValue(fSymbolTable.addSymbol(a.getValue()));
                }
            }
            return FILTER_ACCEPT;
        }

        public int getWhatToShow()
        {
            return NodeFilter.SHOW_ALL;
        }
    }

/*
    static class FJSymbolTable extends SymbolTable
    {
        int sizeIn = 0;
        int sizeOut = 0;

        @Override
        public String addSymbol(String symbol)
        {
            assert (sizeIn += symbol.length()) > -1;
            assert (sizeOut += (containsSymbol(symbol) ? 0 : symbol.length())) > -1;
            return super.addSymbol(symbol);
        }

        @Override
        public String addSymbol(char[] buffer, int offset, int length)
        {
            assert (sizeIn += length) > -1;
            assert (sizeOut += (containsSymbol(buffer, offset, length) ? 0 : length)) > -1;
            return super.addSymbol(buffer, offset, length);
        }
    }
*/

    static class FJDOMParser extends DOMParser
    {
        SymbolTable fSymbolTable;

        static FJDOMParser create()
        {
            SymbolTable fj = new SymbolTable();
            return new FJDOMParser(fj);
        }

        FJDOMParser(SymbolTable st)
        {
            super(st);
            fSymbolTable = st;
            fSkippedElemStack = new Stack();
            fDOMFilter = new FJParseFilter();
            try
            {
                setFeature(Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE, false);
                setFeature(DEFER_NODE_EXPANSION, false);
                setFeature(INCLUDE_IGNORABLE_WHITESPACE, false);
                setFeature(NAMESPACES, true);
                setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE, true);
                setErrorHandler(new FJErrorHandler());
            }
            catch (SAXNotSupportedException x)
            {
                throw new RuntimeException(x);
            }
            catch (SAXNotRecognizedException x)
            {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException
        {
            // FlowJo v7.5.5 didn't add Gating-ML namepsace declarations.
            // I'm not sure if this is the preferred way to inject the prefixes, but it seems to work.
            // Adding the namespace declaration attributes in startElement() is too late.
            for (Map.Entry<String, String> entry : GATINGML_1_5_PREFIX_MAP.entrySet())
            {
                namespaceContext.declarePrefix(entry.getKey(), entry.getValue());
            }
            super.startDocument(locator, encoding, namespaceContext, augs);
        }

        @Override
        protected Attr createAttrNode(QName attrQName)
        {
            // FlowJo v10.0.0-v10.0.5 used incorrect Gating-ML namespace declarations.
            // Replace the incorrect namespace and replace with the proper namespaces.
            if (FJ_GATINGML_1_5_NAMEPSACE_FIXUP.containsKey(attrQName.uri))
                attrQName.uri = FJ_GATINGML_1_5_NAMEPSACE_FIXUP.get(attrQName.uri);
            return super.createAttrNode(attrQName);
        }

        @Override
        protected Element createElementNode(QName qName)
        {
            // FlowJo v10.0.0-v10.0.5 used incorrect Gating-ML namespace declarations.
            // Replace the incorrect namespace and replace with the proper namespaces.
            if (FJ_GATINGML_1_5_NAMEPSACE_FIXUP.containsKey(qName.uri))
                qName.uri = FJ_GATINGML_1_5_NAMEPSACE_FIXUP.get(qName.uri);
            return super.createElementNode(qName);
        }

        @Override
        public void parse(InputSource inputSource) throws SAXException, IOException
        {
            try
            {
                super.parse(inputSource);
            }
            catch (RuntimeException x)
            {
                Logger.getLogger(FlowJoWorkspace.class).error("Unexpected error", x);
                throw x;
            }
        }
    }

}
