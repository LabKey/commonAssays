package org.labkey.microarray;

import org.labkey.api.search.AbstractXMLDocumentParser;
import org.labkey.api.webdav.WebdavResource;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

/**
 * Only index a subset of the MAGE-ML content. Specifically, just XML attribute values,
 * ignoring the TSV inside the data cube.
 * User: jeckels
 * Date: Jul 14, 2010
 */
public class MageMLDocumentParser extends AbstractXMLDocumentParser
{
    @Override
    public String getMediaType()
    {
        return "application/mageML";
    }

    @Override
    public boolean detect(WebdavResource resource, byte[] buf) throws IOException
    {
        if (MicroarrayModule.MAGE_ML_INPUT_TYPE.getFileType().isType(resource.getName()) ||
            getMediaType().equals(resource.getContentType()))
        {
            return true;
        }
        
        String header = new String(buf, 0, buf.length);
        return header.indexOf("<MAGE-ML") != -1;
    }

    @Override
    protected DefaultHandler createSAXHandler(ContentHandler xhtmlHandler)
    {
        SAXHandler result = new SAXHandler(xhtmlHandler, false, false, true, true);
        result.addStopElement("DataInternal");
        return result;
    }
}
