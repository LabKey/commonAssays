/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.settings.AppProps;
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
    int _maxScan = 0;
    int _currScan = 0;
    MzxmlSimpleScan _nextSimpleScan = null;
    
    public RandomAccessMzxmlIterator(String fileName, int msLevel)
            throws IOException
    {
        super(msLevel);
        if (!NetworkDrive.exists(new File(fileName)))
            throw new FileNotFoundException(fileName);
        _parser = new MSXMLParser(fileName);
        _maxScan = _parser.getMaxScanNumber();
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

        if (null == _nextSimpleScan && _currScan < _maxScan)
        {
            while (++_currScan <= _maxScan)
            {
                ScanHeader header = _parser.rapHeader(_currScan);
                if (header != null && (_msLevel == 0 || header.getMsLevel() == _msLevel))
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
            float[][] result = _scan.getMassIntensityList();  // casts mass/intens pairs to floats if they were read as doubles
            if (result == null)
            {
                throw new IOException("No spectra available for scan " + _scan.getNum() + ", most likely there was an exception parsing. Check the server logs");
            }
            return result;
        }
    }

    //JUnit TestCase
    public static class TestCase extends junit.framework.TestCase
    {

        TestCase(String name)
        {
            super(name);
        }

        public static Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTest(new TestCase("testMzxml"));
            return suite;
        }

        public void testMzxml()
        {
            String projectRoot = AppProps.getInstance().getProjectRoot();
            if (projectRoot == null || projectRoot.equals("")) projectRoot = "C:/Labkey";
            String mzxml2Fname = projectRoot + "/sampledata/mzxml/test_nocompression.mzXML";
            String mzxml3Fname = projectRoot + "/sampledata/mzxml/test_zlibcompression.mzXML";
            try
            {
                RandomAccessMzxmlIterator mzxml2 = new RandomAccessMzxmlIterator(mzxml2Fname, 1);
                RandomAccessMzxmlIterator mzxml3 = new RandomAccessMzxmlIterator(mzxml3Fname, 1);
                while (mzxml2.hasNext() && mzxml3.hasNext())
                {
                    SimpleScan scan2 = mzxml2.next();
                    SimpleScan scan3 = mzxml3.next();
                    assertEquals(scan2.getScan(),scan3.getScan());
                    float [][]data2 = scan2.getData();
                    float [][]data3 = scan3.getData();
                    assertEquals(data2[0].length,data3[1].length);
                    assertEquals(data2[1].length,data3[0].length);
                    for (int i=0;i < data2[0].length; i++)
                    {
                        assertEquals(data2[0][i],data3[0][i]);
                        assertEquals(data2[1][i],data3[1][i]);
                    }
                }
                assertEquals("files should have same scan counts",mzxml2.hasNext(), mzxml3.hasNext());
            }
            catch (IOException e)
            {
                fail(e.toString());
            }
            finally {
            }
        }
    }
}
