package org.labkey.ms2.reader;

import org.labkey.api.util.NetworkDrive;
import org.labkey.common.tools.SimpleXMLStreamReader;
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

    private String _fileName;
    private InputStream _in;
    private SimpleXMLStreamReader _parser;
    private int _scanCount = 0;
    private SimpleScan _currentScan;

    public SequentialMzxmlIterator(String fileName, int msLevel) throws FileNotFoundException, XMLStreamException
    {
        super(msLevel);
        _fileName = fileName;
        File f = new File(_fileName);
        if (!NetworkDrive.exists(f))
        {
            throw new FileNotFoundException(fileName);
        }
        FileInputStream fIn = new FileInputStream(f);
        _in = new BufferedInputStream(new BufferedInputStream(fIn));
        _parser = new SimpleXMLStreamReader(_in);
        if (!_parser.skipToStart("msRun"))
        {
            throw new XMLStreamException("Did not find a starting msRun element");
        }
        _scanCount = Integer.parseInt(_parser.getAttributeValue(null, "scanCount"));
    }

    public int getScanCount()
    {
        return _scanCount;
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
        while (msLevel != _msLevel)
        {
            try
            {
                if (!findNextScan())
                    return false;

                msLevel = Integer.parseInt(getAttributeValue("msLevel"));
                if (msLevel != _msLevel)
                {
                    continue;
                }

                // Grab the required attributes
                num = Integer.parseInt(getAttributeValue("num"));
                retentionTime = getAttributeValue("retentionTime");

                if (!_parser.skipToStart("peaks"))
                    return false;

                int precision = Integer.parseInt(getAttributeValue("precision"));
                int nextType;
                StringBuilder sb = new StringBuilder();
                while ((nextType = _parser.next()) != XMLStreamConstants.END_ELEMENT || !"peaks".equals(_parser.getLocalName()))
                {
                    if (nextType == XMLStreamConstants.CHARACTERS)
                    {
                        sb.append(_parser.getText());
                    }
                }
                data = Scan.parseRawIntensityData(sb.toString(), precision);
            }
            catch (XMLStreamException e)
            {
                _log.error("Failed to parse file " + _fileName, e);
                return false;
            }
        }
        assert data != null && num != -1 : "Did not find a valid scan";
        _currentScan = new SequentialSimpleScan(num, retentionTime, data);
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
                else if(elementName.equals("index"))
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
        private final String _retentionTime;  // Store as a string... convert to double only if requested
        private final float[][] _data;

        private SequentialSimpleScan(int scan, String retentionTime, float[][] data)
        {
            _scan = scan;
            _retentionTime = retentionTime;
            _data = data;
        }

        public int getScan()
        {
            return _scan;
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
