package org.labkey.flow.analysis.model;

import org.apache.log4j.Logger;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.util.SymbolTable;
import org.w3c.dom.*;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * User: kevink
 * Date: 2/8/12
 */
public class WorkspaceParser
{
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

    final static HashMap<String,Short> elements = new HashMap<String, Short>(100);
    static
    {
        for (String s : knownElements)
            elements.put(s, defaultFilter);
        for (String s : rejectElements)
            elements.put(s, LSParserFilter.FILTER_REJECT);
        for (String s : parsedElements)
            elements.put(s, LSParserFilter.FILTER_ACCEPT);
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
        Set<String> rejected = new HashSet<String>();

        public short startElement(Element element)
        {
            Short s = elements.get(element.getNodeName());
            short filter = null == s ? defaultFilter : s.shortValue();
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
