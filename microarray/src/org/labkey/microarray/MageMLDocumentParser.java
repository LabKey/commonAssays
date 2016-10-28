/*
 * Copyright (c) 2010-2016 LabKey Corporation
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
    public boolean detect(WebdavResource resource, String contentType, byte[] buf) throws IOException
    {
        if (MicroarrayModule.MAGE_ML_INPUT_TYPE.getFileType().isType(resource.getName()) ||
            getMediaType().equals(resource.getContentType()))
        {
            return true;
        }
        
        String header = new String(buf, 0, buf.length);
        return header.contains("<MAGE-ML");
    }

    @Override
    protected DefaultHandler createSAXHandler(ContentHandler xhtmlHandler)
    {
        SAXHandler result = new SAXHandler(xhtmlHandler, false, false, true, true, SAXHandler.DEFAULT_MAX_INDEXABLE_SIZE);
        result.addStopElement("DataInternal");
        return result;
    }
}
