/*
 * Copyright (c) 2010-2017 LabKey Corporation
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

import org.apache.xmlbeans.GDuration;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.massSpecDataFileType;
import org.systemsbiology.jrap.MSXMLParser;
import org.systemsbiology.jrap.Scan;
import org.systemsbiology.jrap.ScanHeader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * User: mbellew
 * Date: Oct 6, 2005
 * Time: 9:03:43 AM
 */

// formerly know as RandomAccessMzxmlIteror, this has been subclassed to
// admit optional use of pwiz for mzml and mzxml.gz
public class RandomAccessJrapMzxmlIterator extends RandomAccessMzxmlIterator
{
    MSXMLParser _parser = null;
    int _maxScan = 0;
    int _currScan = 0;
    MzxmlSimpleScan _nextSimpleScan = null;

    public RandomAccessJrapMzxmlIterator(File file, int msLevel)
            throws IOException
    {
        super(msLevel);
        if (!NetworkDrive.exists(file))
            throw new FileNotFoundException(file.toString());
        _parser = new MSXMLParser(file);
        _maxScan = _parser.getMaxScanNumber();
    }

    public RandomAccessJrapMzxmlIterator(File file, int msLevel, int startingScan)
            throws IOException
    {
        this(file, msLevel);
        _currScan = startingScan - 1;
    }


    public boolean hasNext()
    {
        if (null == _parser)
            return false;

        if (null == _nextSimpleScan && _currScan < _maxScan)
        {
            while (++_currScan <= _maxScan)
            {
                ScanHeader header = _parser.rapHeader(_currScan);
                if (header != null && (_msLevelFilter == NO_SCAN_FILTER || header.getMsLevel() == _msLevelFilter))
                {
                    _nextSimpleScan = new MzxmlSimpleScan(_currScan, header);
                    break;
                }
            }
            assert (_currScan <= _maxScan) == (_nextSimpleScan != null);
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

        public int getMSLevel()
        {
            return _scanHeader.getMsLevel();
        }

        public float[][] getData() throws IOException
        {
            if (null == _scan)
                _scan = _parser.rap(_scanIndex);
            float[][] result = _scan.getMassIntensityList();  // casts mass/intens pairs to floats if they were read as doubles
            if (result == null)
            {
                throw new IOException("No spectra available for scan " + _scan.getNum() + ", most likely there was an exception parsing. Check the server logs");
            }
            return result;
        }
    }

    //JUnit TestCase
    public static class TestCase extends Assert
    {
        @Test
        public void testMzxml() throws IOException
        {
            massSpecDataFileType FT_MZXML = new massSpecDataFileType();
            File mzxml2File = JunitUtil.getSampleData(null, "mzxml/test_nocompression.mzXML");
            File mzxml3File = JunitUtil.getSampleData(null, "mzxml/test_zlibcompression.mzXML");
            assertTrue(FT_MZXML.isType(mzxml2File));
            assertTrue(FT_MZXML.isType(mzxml3File));
            try
            {
                RandomAccessMzxmlIterator mzxml2 = new RandomAccessJrapMzxmlIterator(mzxml2File, 1);
                RandomAccessMzxmlIterator mzxml3 = new RandomAccessJrapMzxmlIterator(mzxml3File, 1);
                compare_mzxml(this,mzxml2, mzxml3);
            }
            catch (IOException e)
            {
                fail(e.toString());
            }
        }
    }
}
