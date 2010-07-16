package org.labkey.ms2.reader;

import org.labkey.api.search.AbstractXMLDocumentParser;
import org.labkey.api.webdav.WebdavResource;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

/**
 * User: jeckels
 * Date: Jul 15, 2010
 */
public class MzMLDocumentParser extends AbstractXMLDocumentParser
{
    public String getMediaType()
    {
        return "application/mzml";
    }

    public boolean detect(WebdavResource resource, byte[] buf) throws IOException
    {
        if (resource.getName().endsWith(".mzML") || getMediaType().equals(resource.getContentType()))
        {
            return true;
        }

        String header = new String(buf, 0, buf.length);
        return header.indexOf("<mzML") != -1;
    }

    @Override
    protected DefaultHandler createSAXHandler(ContentHandler xhtmlHandler)
    {
        SAXHandler result = new SAXHandler(xhtmlHandler, true, true, true, true);
        result.addStopElement("spectrum");
        return result;
    }

}