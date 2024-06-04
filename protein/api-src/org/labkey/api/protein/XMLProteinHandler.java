/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.api.protein;

import org.labkey.api.protein.uniprot.ParseActions;
import org.labkey.api.protein.uniprot.ParseContext;
import org.labkey.api.protein.uniprot.ParserTree;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

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
    /* in this list. The name must be unique within the entire parse tree. Currently,    */
    /* the VALUE part of hashtable is ignored.                                           */
    private String skipMe = null;
    private final XMLProteinLoader _loader;
    public static final String PROGRAM_PREFIX = "XMLProteinLoader";

    public XMLProteinHandler(Connection conn, XMLProteinLoader loader) throws SAXException, IOException
    {
        _parseContext = new ParseContext(conn, loader.isClearExisting());
        _loader = loader;
        _tree = new ParserTree(_loader.getLogger());
        setProgProperties(REProperties.loadREProperties(PROGRAM_PREFIX + "." + loaderPrefix));
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
            _loader.getLogger().warn("One or more requested parser features is unavailable", e);
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

    private Set<String> skipTheseTags = new HashSet<>();

    public Set<String> getSkipTheseTags()
    {
        return skipTheseTags;
    }

    public void setSkipTheseTags(Set<String> skipTheseTags)
    {
        this.skipTheseTags = skipTheseTags;
    }

    /* info about parse state, passed to content handlers */
    private ParseContext _parseContext;

    @Override
    public void endDocument()
    {
        _loader.handleThreadStateChangeRequests();
    }

    private final ParserTree _tree;

    /**
     * Start element.
     */
    @Override
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
            if (p.getFile() == null) p.setFile(_loader.getFile());
            if (p.getComment() == null) p.setComment(_loader.getComment());
            if (p.getCurrentInsertId() == 0)
                p.setCurrentInsertId(_loader.getCurrentInsertId());
            p.beginElement(_parseContext, attrs);
        }
    }

    /**
     * end element
     */
    @Override
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
            if (p.getFile() == null) p.setFile(_loader.getFile());
            if (p.getComment() == null) p.setComment(_loader.getComment());
            p.endElement(_parseContext);
        }
        _tree.pop();
    }

    /**
     * Characters.
     */
    @Override
    public void characters(char ch[], int start, int length)
    {
        _loader.handleThreadStateChangeRequests();
        if (skipMe != null) return;
        if (length == 0) return;
        ParseActions p = _tree.getCurrent();
        if (p != null)
        {
            if (p.getFile() == null) p.setFile(_loader.getFile());
            p.characters(_parseContext, ch, start, length);
        }
    }

    /**
     * Ignorable whitespace.
     */
    @Override
    public void ignorableWhitespace(char ch[], int start, int length)
    {
        _loader.handleThreadStateChangeRequests();
    }

    /**
     * Processing instruction.
     */
    @Override
    public void processingInstruction(String target, String data)
    {
        _loader.handleThreadStateChangeRequests();
        if (skipMe != null) return;
        if (data != null && !data.isEmpty())
        {
            _loader.getLogger().warn("PROCESSING INSTRUCTION: target=" + target + "; data=" + data);
        }
    }

    /**
     * Start document.
     */
    @Override
    public void startDocument()
    {
        _loader.handleThreadStateChangeRequests();
    }

    public void parse(File file) throws IOException, SAXException
    {
        getParser().parse(file.getPath());
    }
}

