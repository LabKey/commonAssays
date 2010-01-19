/*
 * Copyright (c) 2005-2010 LabKey Corporation
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
import proteowizard.pwiz.RAMPAdapter.Scan;
import proteowizard.pwiz.RAMPAdapter.pwiz_RAMPAdapter;
import proteowizard.pwiz.RAMPAdapter.vectord;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;


/**
 * Created by IntelliJ IDEA.
 * User: bpratt
 * Date: March 5, 2009
 * Time: 9:03:43 AM
 */
public class RandomAccessPwizMSDataIterator extends AbstractMzxmlIterator
{
    private static Logger _log = Logger.getLogger(RandomAccessPwizMSDataIterator.class);

    pwiz_RAMPAdapter _parser = null;
    long _maxScan = 0;
    int _currScan = 0;
    MzxmlSimpleScan _nextSimpleScan = null;

    static boolean _isAvailable;
    static boolean _triedLoadLib;

    // use this to investigate availability of DLL that
    // implements the pwiz interface
    public static boolean isAvailable()
             throws IOException
    {
        if (!_triedLoadLib)
        {
            _triedLoadLib = true;
            /*
            a more thorough implementation of mzML and mzXML.gz handling is forthcoming
            just back this out for now - bpratt
            try {
                System.loadLibrary("pwiz_swigbindings");
                _isAvailable = true;
                _log.info("Successfully loaded pwiz_swigbindings lib");
            } catch (UnsatisfiedLinkError e) {
                throw new IOException ("pwiz_swigbindings lib not found, falling back to older mzXML reader code (no mzML support)" + e);
            } catch (Exception e) {
                throw new IOException ("pwiz_swigbindings lib not loaded, falling back to older mzXML reader code (no mzML support)" + e);
            }
            */
        }
        return _isAvailable;
    }

    public RandomAccessPwizMSDataIterator(String fileName, int msLevel)
            throws IOException
    {
        super(msLevel);
        if (!NetworkDrive.exists(new File(fileName)))
            throw new FileNotFoundException(fileName);
        if (isAvailable())
        {
            _parser = new pwiz_RAMPAdapter(fileName);
            _maxScan = _parser.scanCount();
        }
    }

    public RandomAccessPwizMSDataIterator(String fileName, int msLevel, int startingScan)
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
                long ind0 = _parser.index(_currScan); // get 0-based index, returns _maxScan on mapping failure
                if (ind0 != _maxScan)
                {
                    _nextSimpleScan = new MzxmlSimpleScan(ind0);
                    if ((_msLevel == 0 || _nextSimpleScan._scan.getMsLevel() == _msLevel))
                    {
                        break;
                    }
                    _nextSimpleScan = null;
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
        if (_parser != null)
        {
            _parser.delete();
            _parser = null;
        }
        _nextSimpleScan = null;
    }


    protected static byte[] realloc(int size, byte[] buf)
    {
        if (null == buf || buf.length < size)
            return new byte[size];
        return buf;
    }

    protected class MzxmlSimpleScan implements SimpleScan
    {
        long _scanIndex;   // 0-based index into scan table (as opposed to scan number
        Scan _scan;

        MzxmlSimpleScan(long scanIndex)
        {
            _scanIndex = scanIndex;
            _parser.getScanHeader(scanIndex, _scan);
        }

        public int getScan()
        {
            return _scan.getSeqNum();
        }

        public Double getRetentionTime()
        {
            return _scan.getRetentionTime();
        }

        public float[][] getData() throws IOException
        {
            float[][] result = _scan.getMassIntensityList();  // casts mass/intens pairs to floats if they were read as doubles
            if (result == null)
            {
                vectord peaks = new vectord();
                peaks.reserve(2*(1+_scan.getPeaksCount()));
                _parser.getScanPeaks(_scanIndex, peaks);
                if (peaks.isEmpty())
                {
                    throw new IOException("No spectra available for scan " + _scan.getAcquisitionNum() + ", most likely there was an exception parsing. Check the server logs");
                }
                else
                {
                    _scan.setMassIntensityList(peaks);
                    result = _scan.getMassIntensityList();
                }
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
                RandomAccessPwizMSDataIterator mzxml2 = new RandomAccessPwizMSDataIterator(mzxml2Fname, 1);
                RandomAccessPwizMSDataIterator mzxml3 = new RandomAccessPwizMSDataIterator(mzxml3Fname, 1);
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