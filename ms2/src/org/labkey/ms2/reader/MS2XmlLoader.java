/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.reader.SimpleXMLStreamReader;
import org.labkey.api.util.PossiblyGZIPpedFileInputStreamFactory;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public abstract class MS2XmlLoader extends MS2Loader
{
    protected InputStream _fIn;
    protected SimpleXMLStreamReader _parser;

    protected void init(File f, Logger log) throws FileNotFoundException, XMLStreamException
    {
        super.init(f, log);
        _fIn = new BufferedInputStream(PossiblyGZIPpedFileInputStreamFactory.getStream(f), STREAM_BUFFER_SIZE);
        _parser = new SimpleXMLStreamReader(_fIn);
    }

    public int getCurrentOffset()
    {
        return _parser.getLocation().getCharacterOffset();
    }


    public void close()
    {
        try
        {
            if (null != _parser)
                _parser.close();
        }
        catch (XMLStreamException e)
        {
            _log.error(e);
        }
        try
        {
            if (null != _fIn)
                _fIn.close();
        }
        catch (IOException e)
        {
            _log.error(e);
        }
    }
}
