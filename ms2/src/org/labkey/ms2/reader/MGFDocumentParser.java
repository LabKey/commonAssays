/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

    public boolean detect(WebdavResource resource, String contentType, byte[] buf) throws IOException
    {
        return resource.getName().toLowerCase().endsWith(".mgf") || getMediaType().equals(resource.getContentType());
    }
}
