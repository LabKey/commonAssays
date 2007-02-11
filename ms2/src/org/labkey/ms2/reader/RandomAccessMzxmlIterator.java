/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
import org.systemsbiology.jrap.MSXMLParser;
import org.systemsbiology.jrap.ScanHeader;
import org.systemsbiology.jrap.Scan;
import org.apache.xmlbeans.GDuration;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;


/**
 * Created by IntelliJ IDEA.
 * User: mbellew
 * Date: Oct 6, 2005
 * Time: 9:03:43 AM
 */
public class RandomAccessMzxmlIterator extends AbstractMzxmlIterator
{
    MSXMLParser _parser = null;
    int _scanCount = 0;
    int _currScan = 0;
    MzxmlSimpleScan _nextSimpleScan = null;
    
    public RandomAccessMzxmlIterator(String fileName, int msLevel)
            throws IOException
    {
        super(msLevel);
        if (!NetworkDrive.exists(new File(fileName)))
            throw new FileNotFoundException(fileName);
        _parser = new MSXMLParser(fileName);
        _scanCount = _parser.getScanCount();
    }

    public RandomAccessMzxmlIterator(String fileName, int msLevel, int startingScan)
            throws IOException
    {
        this(fileName, msLevel);
        _currScan = startingScan - 1;
    }


    static final int BUFFER_SIZE = 128 * 1024;

    public boolean hasNext()
    {
        if (null == _parser)
            return false;

        if (null == _nextSimpleScan && _currScan < _scanCount)
        {
            while (++_currScan <= _scanCount)
            {
                ScanHeader header = _parser.rapHeader(_currScan);
                if (_msLevel == 0 || header.getMsLevel() == _msLevel)
                {
                    _nextSimpleScan = new MzxmlSimpleScan(_currScan, header);
                    break;
                }
            }
            assert (_currScan <= _scanCount) == (_nextSimpleScan != null);
        }
        return _nextSimpleScan != null;
    }


    public SimpleScan next()
    {
        if (null == _nextSimpleScan)
            throw new IllegalStateException();
        MzxmlSimpleScan next = _nextSimpleScan;
        _nextSimpleScan = null;
        return next;
    }


    public void close()
    {
        // apparently _parser does not need to be closed
        _nextSimpleScan = null;
    }


    protected static byte[] realloc(int size, byte[] buf)
    {
        if (null == buf || buf.length < size)
            return new byte[size];
        return buf;
    }


    /**
     * NOTE: GzSimpleSpectrum is actually stateful.  It depends on the state of the
     * TarInputStream tis, so you can't hold onto the object after calling
     * GzSimpleSpectrumInterator.next()
     * <p/>
     * Since it is stateful anyway, we can just return the same GzSimpleSpectrum everytime
     */
    protected class MzxmlSimpleScan implements SimpleScan
    {
        int _scanIndex;
        ScanHeader _scanHeader;
        Scan _scan;

        MzxmlSimpleScan(int scanIndex, ScanHeader scanHeader)
        {
            _scanIndex = scanIndex;
            _scanHeader = scanHeader;
        }

        public int getScan()
        {
            return _scanHeader.getNum();
        }

        public Double getRetentionTime()
        {
            if (_scanHeader.getRetentionTime() == null)
            {
                return null;
            }
            // Convert XML duration into double... we assume times are less than a day
            GDuration ret = new GDuration(_scanHeader.getRetentionTime());
            return ret.getHour() * 60 * 60 + ret.getMinute() * 60 + ret.getSecond() + ret.getFraction().doubleValue();
        }

        public float[][] getData() throws IOException
        {
            if (null == _scan)
                _scan = _parser.rap(_scanIndex);
            return _scan.getMassIntensityList();
        }
    }
}
