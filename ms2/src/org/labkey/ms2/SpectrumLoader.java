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

package org.labkey.ms2;

import org.apache.log4j.Logger;
import org.labkey.ms2.reader.*;
import org.labkey.api.util.NetworkDrive;
import org.labkey.common.util.Pair;
import org.labkey.api.data.DbSchema;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Set;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * User: arauch
 * Date: Oct 26, 2005
 * Time: 3:37:30 PM
 */
public class SpectrumLoader
{
    private static Logger _systemLog = Logger.getLogger(SpectrumLoader.class);
    private static final int SQL_BATCH_SIZE = 100;

    private Logger _log = null;
    private InputStream _gzFileStream = null;
    private MS2Importer.MS2Progress _progress = null;
    private Set _scans = null;
    private int _fractionId;
    private SimpleScanIterator _scanIterator;
    private String _fileName = null;
    private boolean _uploadRetentionTime;


    protected SpectrumLoader(String gzFileName, String dtaFileNamePrefix, String mzXmlFileName, Set scans, MS2Importer.MS2Progress progress, int fractionId, Logger log, boolean uploadRetentionTime)
    {
        _scans = scans;
        _progress = progress;
        _fractionId = fractionId;
        _log = log;
        _uploadRetentionTime = uploadRetentionTime;

        if (null == scans)
            return;

        try
        {
            // Try to open the gz file first... if that fails, try to open the mzXML file
            File gz = new File(gzFileName);

            if (NetworkDrive.exists(gz))
                _gzFileStream = new FileInputStream(gz);
        }
        catch (FileNotFoundException e)
        {
            _log.error(e);
            _systemLog.error(e);
        }

        try
        {
            if (null != _gzFileStream)
            {
                _fileName = gzFileName;
                _scanIterator = new TarIterator(_gzFileStream, dtaFileNamePrefix);
            }
            else
            {
                if (null == mzXmlFileName)
                    _log.warn(gzFileName + " could not be opened and no mzXML file name was specified.  Spectra will not be loaded.");
                else
                {
                    _fileName = mzXmlFileName;
                    _scanIterator = new SequentialMzxmlIterator(mzXmlFileName, 2);
                }
            }
        }
        catch (IOException x)
        {
            if (null != _scanIterator)
                _scanIterator.close();
            _scanIterator = null;
        }
        catch (XMLStreamException x)
        {
            if (null != _scanIterator)
                _scanIterator.close();
            _scanIterator = null;
        }
    }


    // Iterates the spectra writes them to the ms2.SpectraData table using the same Fraction & Row as the peptide table
    protected void upload()
    {
        long start = System.currentTimeMillis();

        if (null == _scans || _scans.isEmpty())
        {
            _log.warn("Spectrum upload abandoned: no scans were loaded from pep.xml file.");
            return;
        }

        if (null == _scanIterator)
        {
            _log.warn("Spectrum upload abandoned: could not open spectrum source.");
            return;
        }

        _log.info("Starting spectrum upload from " + _fileName);

        DbSchema schema = MS2Manager.getSchema();
        Connection conn = null;
        PreparedStatement gzStmt = null;
        PreparedStatement retentionStmt = null;

        try
        {
            conn = schema.getScope().getConnection();
            conn.setAutoCommit(false);
            gzStmt = conn.prepareStatement("INSERT INTO " + MS2Manager.getTableInfoSpectraData() + " (Fraction, Scan, Spectrum) VALUES (?, ?, ?)");
            gzStmt.setInt(1, _fractionId);

            if (_uploadRetentionTime)
            {
                retentionStmt = conn.prepareStatement("UPDATE " + MS2Manager.getTableInfoPeptidesData() + " SET RetentionTime = ? WHERE Scan = ? AND Fraction = ?");
                retentionStmt.setInt(3, _fractionId);
            }

            int file = 0;

            while (_scanIterator.hasNext())
            {
                SimpleScan spectrum = _scanIterator.next();
                int scan = spectrum.getScan();

                // Load spectrum only if we loaded the corresponding scan in the HTML or XML file.
                // Since we want to store spectrum only once per scan, remove it from the set so
                // we store it once even if this spectrum shows up again (e.g., multiple DTA files
                // for a single scan but different charge)
                if (_scans.contains(scan))
                {
                    _scans.remove(scan);

                    float[][] data = spectrum.getData();
                    byte[] copyBytes = floatArraysToByteArray(data[0], data[1]);

                    gzStmt.setInt(2, scan);
                    gzStmt.setBytes(3, copyBytes);
                    gzStmt.addBatch();

                    if (_uploadRetentionTime)
                    {
                        Double retentionTime = spectrum.getRetentionTime();
                        if (retentionTime != null)
                        {
                            retentionStmt.setDouble(1, retentionTime.doubleValue());
                            retentionStmt.setInt(2, scan);
                            retentionStmt.addBatch();
                        }
                    }

                    file++;

                    if (0 == file % SQL_BATCH_SIZE)
                    {
                        gzStmt.executeBatch();

                        if (_uploadRetentionTime)
                            retentionStmt.executeBatch();

                        conn.commit();
                    }

                    // TODO: Eliminate this check once we eliminate COMET HTML
                    if (null != _progress)
                    {
                        _progress.addSpectrum();
                    }
                }
            }
        }
        catch (IOException e)
        {
            _log.error(e);
            _systemLog.error(e);
        }
        catch (SQLException e)
        {
            _log.error(e);
            _systemLog.error(e);
        }
        finally
        {
            try
            {
                if (null != gzStmt)
                {
                    gzStmt.executeBatch();
                    gzStmt.close();
                }
            }
            catch (SQLException e)
            {
                _log.error(e);
                _systemLog.error(e);
            }

            try
            {
                if (null != retentionStmt)
                {
                    retentionStmt.executeBatch();
                    retentionStmt.close();
                }
            }
            catch (SQLException e)
            {
                _log.error(e);
                _systemLog.error(e);
            }

            try
            {
                conn.commit();
            }
            catch (SQLException e)
            {
                _log.error(e);
                _systemLog.error(e);
            }

            try
            {
                conn.setAutoCommit(true);
            }
            catch (SQLException e)
            {
                _log.error(e);
                _systemLog.error(e);
            }


            try
            {
                if (null != conn)
                {
                    schema.getScope().releaseConnection(conn);
                }
                close();
            }
            catch (SQLException e)
            {
                _log.error(e);
                _systemLog.error(e);
            }
            if (null != _scanIterator)
                _scanIterator.close();
            _scanIterator = null;
            close();
        }

        MS2Importer.logElapsedTime(_log, start, "upload spectra");
    }


    public void close()
    {
        if (null != _gzFileStream)
        {
            try
            {
                _gzFileStream.close();
            }
            catch (IOException x)
            {
                _systemLog.error(x);
            }
        }
        _gzFileStream = null;
    }


    protected void finalize() throws Throwable
    {
        super.finalize();

        close();
    }

    public File getFile()
    {
        if (_fileName == null)
        {
            return null;
        }
        return new File(_fileName);
    }


    public static Pair<float[], float[]> byteArrayToFloatArrays(byte[] source)
    {
        if (null == source)
            source = new byte[0];

        ByteBuffer bb = ByteBuffer.wrap(source);

        int plotCount = (bb.capacity() / 8);

        // Intel native is LITTLE_ENDIAN -- UNDONE: Make this an app-wide constant?
        bb.order(ByteOrder.LITTLE_ENDIAN);

        float[] x = new float[plotCount];
        float[] y = new float[plotCount];

        if (plotCount > 0)
        {
            for (int i = 0; i < plotCount; i++)
            {
                x[i] = bb.getFloat();
                y[i] = bb.getFloat();
            }
        }

        return new Pair<float[], float[]>(x, y);
    }

    public static byte[] floatArraysToByteArray(float[] x, float[] y)
    {
        int floatCount = x.length;
        ByteBuffer bb = ByteBuffer.allocate(floatCount * 4 * 2);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < floatCount; i++)
        {
            bb.putFloat(x[i]);
            bb.putFloat(y[i]);
        }

        return bb.array();
    }
}
