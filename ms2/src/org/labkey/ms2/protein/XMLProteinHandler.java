package org.labkey.ms2.protein;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.labkey.ms2.protein.tools.REProperties;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.io.IOException;
import java.sql.Connection;

/**
 * User: brittp
 * Date: Dec 23, 2005
 * Time: 4:03:59 PM
 */
public class XMLProteinHandler extends DefaultHandler
{
    /**
     * Namespaces feature id (http://xml.org/sax/features/namespaces).
     */
    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
    /**
     * Namespace prefixes feature id (http://xml.org/sax/features/namespace-prefixes).
     */
    protected static final String NAMESPACE_PREFIXES_FEATURE_ID = "http://xml.org/sax/features/namespace-prefixes";
    /**
     * Validation feature id (http://xml.org/sax/features/validation).
     */
    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";
    /**
     * Schema validation feature id (http://apache.org/xml/features/validation/schema).
     */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";
    /**
     * Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking).
     */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";
    /**
     * Dynamic validation feature id (http://apache.org/xml/features/validation/dynamic).
     */
    protected static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";

    // default settings
    /**
     * Default parser class.
     */
    protected static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";
    /**
     * Default namespaces support (true).
     */
    protected static final boolean DEFAULT_NAMESPACES = true;
    /**
     * Default namespace prefixes (true).
     */
    protected static final boolean DEFAULT_NAMESPACE_PREFIXES = true;
    /**
     * Default validation support (true).
     */
    protected static final boolean DEFAULT_VALIDATION = true;
    /**
     * Default Schema validation support (false).
     */
    protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;
    /**
     * Default Schema full checking support (false).
     */
    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;
    /**
     * Default dynamic validation support (false).
     */
    protected static final boolean DEFAULT_DYNAMIC_VALIDATION = false;

    /* when there is a tag and all its children that we don't want to process, we put it */
    /* in this list.  The name must be unique within the entire parse tree.  Currently   */
    /* the VALUE part of hashtable is ignored.                                           */
    private String skipMe = null;
    private Connection _conn;
    private XMLProteinLoader _loader;
    public static final String PROGRAM_PREFIX = "XMLProteinLoader";
    private static Logger _log = Logger.getLogger(XMLProteinHandler.class);

    public XMLProteinHandler(Connection conn, XMLProteinLoader loader) throws SAXException, IOException
    {
        _conn = conn;
        _loader = loader;
        setProgProperties(REProperties.loadREProperties(PROGRAM_PREFIX + "." + loaderPrefix));
        setParseTables(new Hashtable<String, ParseActions>());
        setLoaderPackage(
                this.getClass().getPackage().getName() + "." + getLoaderPrefix() + "."
        );
        // insert list of elements to skip
        setSkipTheseTags(
                getProgProperties().REGetValues(
                        PROGRAM_PREFIX + "\\." + getLoaderPrefix() + "\\.skipTag\\..+"
                )
        );

        // create parser
        setParser(XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME));

        try
        {
            getParser().setFeature(NAMESPACES_FEATURE_ID, DEFAULT_NAMESPACES);
            getParser().setFeature(NAMESPACE_PREFIXES_FEATURE_ID, DEFAULT_NAMESPACE_PREFIXES);
            getParser().setFeature(VALIDATION_FEATURE_ID, DEFAULT_VALIDATION);
            getParser().setFeature(SCHEMA_VALIDATION_FEATURE_ID, DEFAULT_SCHEMA_VALIDATION);
            getParser().setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, DEFAULT_SCHEMA_FULL_CHECKING);
            getParser().setFeature(DYNAMIC_VALIDATION_FEATURE_ID, DEFAULT_DYNAMIC_VALIDATION);
        }
        catch (Exception e)
        {
            parseWarning("One or more requested parser features is unavailable: " + e);
        }

        // parse file
        getParser().setContentHandler(this);
        getParser().setErrorHandler(this);
    }

    private REProperties progProperties;

    public REProperties getProgProperties()
    {
        return progProperties;
    }

    public void setProgProperties(REProperties progProperties)
    {
        this.progProperties = progProperties;
    }

    private String loaderPrefix = "uniprot";

    public String getLoaderPrefix()
    {
        return loaderPrefix;
    }

    public void setLoaderPrefix(String loaderPrefix)
    {
        this.loaderPrefix = loaderPrefix;
    }

    private String loaderPackage;

    public String getLoaderPackage()
    {
        return loaderPackage;
    }

    public void setLoaderPackage(String loaderPackage)
    {
        this.loaderPackage = loaderPackage;
    }

    protected XMLReader parser;

    public XMLReader getParser()
    {
        return parser;
    }

    public void setParser(XMLReader parser)
    {
        this.parser = parser;
    }

    public String getSkipMe()
    {
        return skipMe;
    }

    public void setSkipMe(String skipMe)
    {
        this.skipMe = skipMe;
    }

    private Set<String> skipTheseTags = new HashSet<String>();

    public Set<String> getSkipTheseTags()
    {
        return skipTheseTags;
    }

    public void setSkipTheseTags(Set<String> skipTheseTags)
    {
        this.skipTheseTags = skipTheseTags;
    }

    /* info about parse state, passed to content handlers */
    private Hashtable<String,ParseActions> parseTables;

    public Hashtable getParseTables()
    {
        return parseTables;
    }

    public void setParseTables(Hashtable<String, ParseActions> parseTables)
    {
        this.parseTables = parseTables;
    }

    public void endDocument() throws SAXException
    {
        _loader.handleThreadStateChangeRequests();
    }

    private ParserTree _tree = new ParserTree(_log);

    /**
     * Start element.
     */
    public void startElement(String uri, String local, String qname, Attributes attrs) throws SAXException
    {
        _loader.handleThreadStateChangeRequests();
        if (skipMe != null) return;
        if (skipTheseTags.contains(local))
        {
            skipMe = local;
            return;
        }
        ParseActions p = _tree.push(local);
        if (p != null)
        {
            if (p.getWhatImParsing() == null) p.setWhatImParsing(_loader.getParseFName());
            if (p.getComment() == null) p.setComment(_loader.getComment());
            if (p.getCurrentInsertId() == 0 && _loader.getRecoveryId() != 0)
                p.setCurrentInsertId(_loader.getRecoveryId());
            if (!p.beginElement(_conn, parseTables, attrs))
                parseWarning("Can't parse beginElement for " + _tree.getCurrentDescription());
        }
    }

    /**
     * end element
     */
    public void endElement(String uri, String local, String qname) throws SAXException
    {
        _loader.handleThreadStateChangeRequests();
        if (skipMe != null)
        {
            if (skipMe.equals(local))
            {
                skipMe = null;
            }
            return;
        }
        ParseActions p = _tree.getCurrent();
        if (p != null)
        {
            if (p.getWhatImParsing() == null) p.setWhatImParsing(_loader.getParseFName());
            if (p.getComment() == null) p.setComment(_loader.getComment());
            if (!p.endElement(_conn, parseTables))
                parseWarning("Can't parse endElement for " + _tree.getCurrentDescription());
        }
        _tree.pop();
    }

    /**
     * Characters.
     */
    public void characters(char ch[], int start, int length) throws SAXException
    {
        _loader.handleThreadStateChangeRequests();
        if (skipMe != null) return;
        if (length == 0) return;
        ParseActions p = _tree.getCurrent();
        if (p != null)
        {
            if (p.getWhatImParsing() == null) p.setWhatImParsing(_loader.getParseFName());
            if (!p.characters(_conn, parseTables, ch, start, length))
                parseWarning("Can't parse characters for " + _tree.getCurrentDescription());
        }
    }

    /**
     * Ignorable whitespace.
     */
    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException
    {
        _loader.handleThreadStateChangeRequests();
    }

    /**
     * Processing instruction.
     */
    public void processingInstruction(String target, String data) throws SAXException
    {
        _loader.handleThreadStateChangeRequests();
        if (skipMe != null) return;
        if (data != null && data.length() > 0)
        {
            parseWarning("PROCESSING INSTRUCTION: target=" + target + "; data=" + data);
        }
    }

    /**
     * Start document.
     */
    public void startDocument() throws SAXException
    {
        _loader.handleThreadStateChangeRequests();
    }

    public void parse(String file) throws IOException, SAXException
    {
        getParser().parse(file);
    }

    public void parse(InputSource is) throws IOException, SAXException
    {
        getParser().parse(is);
    }

    //
    // Error handlers.
    //
    public static void parseWarning(String s)
    {
        _log.warn(s);
    }

    public static void parseError(String s)
    {
        _log.error(s);
    }
}

