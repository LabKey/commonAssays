/*
 * Copyright (c) 2005-2017 LabKey Corporation
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

import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.massSpecDataFileType;
import proteowizard.pwiz.RAMPAdapter.Scan;
import proteowizard.pwiz.RAMPAdapter.pwiz_RAMPAdapter;
import proteowizard.pwiz.RAMPAdapter.vectord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * User: bpratt
 * Date: March 5, 2009
 * Time: 9:03:43 AM
 */
public class RandomAccessPwizMSDataIterator extends RandomAccessMzxmlIterator
{
    pwiz_RAMPAdapter _parser = null;
    long _maxScan = 0;
    int _currScan = 0;
    MzxmlSimpleScan _nextSimpleScan = null;

    public RandomAccessPwizMSDataIterator(File file, int msLevelFilter)
            throws IOException
    {
        super(msLevelFilter);
        if (!NetworkDrive.exists(file))
            throw new FileNotFoundException(file.toString());
        if (massSpecDataFileType.isMZmlAvailable())  // try loadlib
        {
            _parser = new pwiz_RAMPAdapter(file.toString());
            _maxScan = _parser.scanCount();
        }
        else
        {
            throw new IOException("no mzML support");
        }
    }

    public RandomAccessPwizMSDataIterator(File file, int msLevel, int startingScan)
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
                long ind0 = _parser.index(_currScan); // get 0-based index, returns _maxScan on mapping failure
                if (ind0 != _maxScan)
                {
                    _nextSimpleScan = new MzxmlSimpleScan(ind0);
                    if ((_msLevelFilter == NO_SCAN_FILTER || _nextSimpleScan._scan.hdr.getMsLevel() == _msLevelFilter))
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
            _scan = new Scan(_parser, scanIndex);
        }

        public int getScan()
        {
            return _scan.hdr.getSeqNum();
        }

        public int getMSLevel()
        {
            return _scan.hdr.getMsLevel();
        }

        public Double getRetentionTime()
        {
            return _scan.hdr.getRetentionTime();
        }

        public float[][] getData() throws IOException
        {
            float[][] result = _scan.getMassIntensityList();  // casts mass/intens pairs to floats if they were read as doubles
            if (result == null)
            {
                vectord peaks = new vectord();
                peaks.reserve(2*(1+_scan.hdr.getPeaksCount()));
                _parser.getScanPeaks(_scanIndex, peaks);
                if (_scan.hdr.getPeaksCount()>0 && peaks.isEmpty())
                {
                    throw new IOException("No spectra available for scan " + _scan.hdr.getAcquisitionNum() + ", most likely there was an exception parsing. Check the server logs");
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
    public static class TestCase extends Assert
    {
        @Test
        public void testPwizMSData() throws java.io.IOException
        {
            massSpecDataFileType FT_MZXML = new massSpecDataFileType();
            File mzxml2File = JunitUtil.getSampleData(null, "mzxml/test_nocompression.mzXML");
            File mzxml3File = JunitUtil.getSampleData(null, "mzxml/test_zlibcompression.mzXML");
            File mzxml4File = JunitUtil.getSampleData(null, "mzxml/test_gzipcompression.mzXML.gz");
            if (massSpecDataFileType.isMZmlAvailable())
            {
                assertTrue(FT_MZXML.isType(mzxml2File));
                assertTrue(FT_MZXML.isType(mzxml3File));
                assertTrue(FT_MZXML.isType(mzxml4File));
                compare_pwiz(mzxml2File, mzxml3File);
                compare_pwiz(mzxml2File, mzxml4File);
                // and verify that JRAP matches for simple mzXML
                try
                {
                    // test mslevel filtering while we're at it
                    RandomAccessMzxmlIterator mzxml2 = new RandomAccessPwizMSDataIterator(mzxml2File, 1);
                    RandomAccessMzxmlIterator mzxml3 = new RandomAccessJrapMzxmlIterator(mzxml2File, 1);
                    RandomAccessMzxmlIterator.compare_mzxml(this, mzxml2, mzxml3);
                }
                catch (IOException e)
                {
                    fail(e.toString());
                }
            }
        }

        private void compare_pwiz(File mzxml2File, File mzxml3File)
        {
            try
            {
                RandomAccessMzxmlIterator mzxml2 = new RandomAccessPwizMSDataIterator(mzxml2File, 0);
                RandomAccessMzxmlIterator mzxml3 = new RandomAccessPwizMSDataIterator(mzxml3File, 0);
                RandomAccessMzxmlIterator.compare_mzxml(this,mzxml2, mzxml3);
            }
            catch (IOException e)
            {
                fail(e.toString());
            }
        }
    }
}