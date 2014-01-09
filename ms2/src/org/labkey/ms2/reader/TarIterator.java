/*
 * Copyright (c) 2005-2014 LabKey Corporation
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.log4j.Logger;
import org.labkey.api.arrays.FloatArray;
import org.labkey.ms2.FloatParser;
import org.labkey.ms2.MS2Importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * User: mbellew
 * Date: Oct 6, 2005
 * Time: 9:03:43 AM
 * <p/>
 * NOTE: The SimpleScan objects returned by this class are not usable
 * after subsequent calls to hasNext()
 */
public class TarIterator implements SimpleScanIterator
{
    private static final Logger _log = Logger.getLogger(TarIterator.class);
    private static final int STREAM_BUFFER_SIZE = 128 * 1024;

    private String _dtaFileNamePrefix = null;
    private InputStream _is;
    private GZIPInputStream _gzInputStream;
    private TarArchiveInputStream _tis;
    private TarArchiveEntry _te;
    private boolean _checkNext = true;   // set to true after next()
    private boolean _hasNext = false;    // set by hasNext()
    private byte[] _spectrumData = new byte[BUFFER_SIZE];


    public TarIterator(File gzFile, String dtaFileNamePrefix) throws java.io.IOException
    {
        boolean success = false;
        try
        {
            _dtaFileNamePrefix = dtaFileNamePrefix;
            _is = new BufferedInputStream(new FileInputStream(gzFile), STREAM_BUFFER_SIZE);
            _gzInputStream = new GZIPInputStream(_is);
            _tis = new TarArchiveInputStream(_gzInputStream);
            success = true;
        }
        finally
        {
            if (!success)
                close();
        }
    }


    private static final int BUFFER_SIZE = 128 * 1024;

    public void remove()
    {
        throw new UnsupportedOperationException();
    }


    public boolean hasNext()
    {
        if (null == _tis)
            return false;

        if (_checkNext)
        {
            // advance to next tarEntry
            try
            {
                while (null != (_te = _tis.getNextTarEntry()))
                {
                    String fileName = _te.getName();
                    if (fileName.endsWith(".dta") && fileName.startsWith(_dtaFileNamePrefix))
                        break;
                }
            }
            catch (IOException x)
            {
                throw new RuntimeException(x);
            }

            _checkNext = false;
            _hasNext = null != _te;
        }

        return _hasNext;
    }


    public SimpleScan next()
    {
        if (_checkNext || !_hasNext)
            throw new IllegalStateException();
        _checkNext = true;

        return new GzSimpleSpectrum();
    }


    public void close()
    {
        if (null != _tis)
        {
            try
            {
                _tis.close();
            }
            catch (IOException x)
            {
                _log.error(x);
            }
            _tis = null;
        }

        if (null != _gzInputStream)
        {
            try
            {
                _gzInputStream.close();
            }
            catch (IOException x)
            {
                _log.error(x);
            }
            _gzInputStream = null;
        }

        if (null != _is)
        {
            try
            {
                _is.close();
            }
            catch (IOException x)
            {
                _log.error(x);
            }
            _is = null;
        }
    }

    protected void finalize() throws Throwable
    {
        super.finalize();

        assert null == _is && null == _gzInputStream && null == _tis;
    }


    private static byte[] realloc(int size, byte[] buf)
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
     * Since it is stateful anyway, we can just return the same GzSimpleSpectrum every time
     */
    class GzSimpleSpectrum implements SimpleScan
    {
        GzSimpleSpectrum()
        {
        }


        public int getScan()
        {
            String fileName = _te.getName();
            int scanStart = MS2Importer.nthLastIndexOf(fileName, ".", 4) + 1;
            int scanEnd = fileName.indexOf(".", scanStart + 1);
            return Integer.parseInt(fileName.substring(scanStart, scanEnd));
        }


        // DTA doesn't convey retention time
        public Double getRetentionTime()
        {
            return null;
        }

        public int getMSLevel()
        {
            return 2;
        }

        public float[][] getData() throws IOException
        {
            int length = _tis.available();

            _spectrumData = realloc(length, _spectrumData);
            length = _tis.read(_spectrumData);

            FloatParser floatParser = new FloatParser(_spectrumData, 0, length);

            if (floatParser.hasNext()) floatParser.nextFloat();  // skip precursor mass
            if (floatParser.hasNext()) floatParser.nextFloat();  // skip ion count

            FloatArray xArray = new FloatArray();
            FloatArray yArray = new FloatArray();

            while (true)
            {
                if (!floatParser.hasNext()) break;
                float x = (float) floatParser.nextFloat();

                if (!floatParser.hasNext()) break;
                float y = (float) floatParser.nextFloat();

                xArray.add(x);
                yArray.add(y);
            }

            return new float[][]{xArray.toArray(null), yArray.toArray(null)};
        }
    }
}
