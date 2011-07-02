/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PossiblyGZIPpedFileInputStreamFactory;
import org.labkey.api.reader.SimpleXMLStreamReader;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.GDuration;
import org.systemsbiology.jrap.Scan;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import java.io.*;
import java.util.NoSuchElementException;

/**
 * User: jeckels
 * Date: May 8, 2006
 */
public class SequentialMzxmlIterator extends AbstractMzxmlIterator
{
    private static Logger _log = Logger.getLogger(SequentialMzxmlIterator.class);

    private File _file;
    private InputStream _in;
    private SimpleXMLStreamReader _parser;
    private SimpleScan _currentScan;
    private static final int STREAM_BUFFER_SIZE = 128 * 1024;

    public SequentialMzxmlIterator(File file, int msLevel) throws FileNotFoundException, XMLStreamException
    {
        super(msLevel);
        _file = file;
        if (!NetworkDrive.exists(file))
        {
            throw new FileNotFoundException(file.toString());
        }
        _in = new BufferedInputStream(PossiblyGZIPpedFileInputStreamFactory.getStream(file), STREAM_BUFFER_SIZE);
        _parser = new SimpleXMLStreamReader(_in);
        if (!_parser.skipToStart("msRun"))
        {
            throw new XMLStreamException("Did not find a starting msRun element");
        }
    }

    public void close()
    {
        if (_in != null)
        {
            try
            {
                _in.close();
            }
            catch (IOException e)
            {
                _log.error("Failed to close file", e);
            }
            _in = null;
        }
        if (_parser != null)
        {
            try
            {
                _parser.close();
            }
            catch (XMLStreamException e)
            {
                _log.error("Failed to close parser", e);
            }
            _parser = null;
        }
    }

    public boolean hasNext()
    {
        if (_parser == null)
        {
            return false;
        }

        if (_currentScan != null)
        {
            return true;
        }

        int msLevel = -1;
        int num = -1;
        String retentionTime = null;
        float[][] data = null;
        while (!((_msLevelFilter == NO_SCAN_FILTER && msLevel != -1) || msLevel == _msLevelFilter))
        {
            try
            {
                if (!findNextScan())
                    return false;

                msLevel = Integer.parseInt(getAttributeValue("msLevel"));
                if (_msLevelFilter != NO_SCAN_FILTER && msLevel != _msLevelFilter)
                {
                    continue;
                }

                // Grab the required attributes
                num = Integer.parseInt(getAttributeValue("num"));
                retentionTime = getAttributeValue("retentionTime");
                int peaksCount = Integer.parseInt(getAttributeValue("peaksCount"));

                if (!_parser.skipToStart("peaks"))
                    return false;

                int precision = Integer.parseInt(getAttributeValue("precision"));
                String byteOrder = getAttributeValue("byteOrder");
                String compressionType = getAttributeValue("compressionType");
                int nextType;
                StringBuilder sb = new StringBuilder();
                while ((nextType = _parser.next()) != XMLStreamConstants.END_ELEMENT || !"peaks".equals(_parser.getLocalName()))
                {
                    if (nextType == XMLStreamConstants.CHARACTERS)
                    {
                        sb.append(_parser.getText());
                    }
                }
                data = Scan.parseRawIntensityData(sb.toString(), peaksCount, precision, compressionType, byteOrder);
            }
            catch (XMLStreamException e)
            {
                _log.error("Failed to parse file " + _file, e);
                return false;
            }
        }
        assert data != null && num != -1 : "Did not find a valid scan";
        _currentScan = new SequentialSimpleScan(num, retentionTime, data, msLevel);
        return true;
    }

    // Quit as soon as we hit an <index> tag, as that comes after all the <scan> tags
    // that we care about
    private boolean findNextScan() throws XMLStreamException
    {
        while (_parser.hasNext())
        {
            _parser.next();

            if (_parser.isStartElement())
            {
                String elementName = _parser.getLocalName();
                if (elementName.equals("scan"))
                {
                    return true;
                }
                else if (elementName.equals("index"))
                {
                    return false;
                }
            }
        }

        return false;
    }

    private String getAttributeValue(String attributeName)
    {
        return _parser.getAttributeValue(null, attributeName);
    }

    public SimpleScan next()
    {
        if (_currentScan == null && !hasNext())
        {
            throw new NoSuchElementException();
        }
        SimpleScan result = _currentScan;
        _currentScan = null;
        return result;
    }

    private class SequentialSimpleScan implements SimpleScan
    {
        private final int _scan;
        private final int _msLevel;
        private final String _retentionTime;  // Store as a string... convert to double only if requested
        private final float[][] _data;

        private SequentialSimpleScan(int scan, String retentionTime, float[][] data, int msLevel)
        {
            _scan = scan;
            _retentionTime = retentionTime;
            _data = data;
            _msLevel = msLevel;
        }

        public int getScan()
        {
            return _scan;
        }

        public int getMSLevel()
        {
            return _msLevel;
        }

        public Double getRetentionTime()
        {
            if (_retentionTime == null)
            {
                return null;
            }
            // Convert XML duration into double... we assume times are less than a day
            GDuration ret = new GDuration(_retentionTime);
            return ret.getHour() * 60 * 60 + ret.getMinute() * 60 + ret.getSecond() + ret.getFraction().doubleValue();
        }

        public float[][] getData() throws IOException
        {
            return _data;
        }
    }
}
