package org.labkey.ms2.reader;

import org.labkey.api.search.AbstractDocumentParser;
import org.labkey.api.webdav.WebdavResource;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * User: jeckels
 * Date: Mar 6, 2011
 */
public class MGFDocumentParser extends AbstractDocumentParser
{
    @Override
    protected void parseContent(InputStream stream, ContentHandler handler) throws IOException, SAXException
    {
        // Intentionally no-op as the content isn't very interesting for full text search
    }

    public String getMediaType()
    {
        return "application/mgf";
    }

    public boolean detect(WebdavResource resource, byte[] buf) throws IOException
    {
        return resource.getName().toLowerCase().endsWith(".mgf") || getMediaType().equals(resource.getContentType());
    }
}
