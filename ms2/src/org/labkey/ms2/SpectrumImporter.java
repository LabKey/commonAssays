/*
 * Copyright (c) 2005-2013 LabKey Corporation
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
import org.labkey.api.data.DbSchema;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.Pair;
import org.labkey.ms2.reader.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * User: arauch
 * Date: Oct 26, 2005
 * Time: 3:37:30 PM
 */
public class SpectrumImporter
{
    private static Logger _systemLog = Logger.getLogger(SpectrumImporter.class);
    private static final int SQL_BATCH_SIZE = 100;

    private Logger _log = null;
    private MS2Importer.MS2Progress _progress = null;
    private Set _scans = null;
    private int _fractionId;
    private SimpleScanIterator _scanIterator;
    private File _file = null;
    private boolean _shouldImportSpectra;
    private boolean _shouldImportRetentionTime;


    protected SpectrumImporter(String gzFileName, String dtaFileNamePrefix, File mzXmlFile, Set scans, MS2Importer.MS2Progress progress, int fractionId, Logger log, boolean shouldImportSpectra, boolean shouldImportRetentionTime)
    {
        _scans = scans;
        _progress = progress;
        _fractionId = fractionId;
        _log = log;
        _shouldImportRetentionTime = shouldImportRetentionTime;
        _shouldImportSpectra = shouldImportSpectra;

        if (null == scans)
            return;

        try
        {
            // Try to access the gz file first... if that fails, try to open the mzXML file
            File gz = new File(gzFileName);

            if (NetworkDrive.exists(gz))
            {
                _file = gz;
                _scanIterator = new TarIterator(gz, dtaFileNamePrefix);
            }
            else
            {
                if (null == mzXmlFile)
                    _log.warn("Spectra were not imported: " + gzFileName + " could not be opened and no mzXML file name was specified.");
                else
                {
                    _file = mzXmlFile;
                    // prefer ProteoWizard's RAMPAdapter interface
                    // (with JNI bindings via SWIG) as it's actively 
                    // maintained, handles mzML and mzXML, and gzipped
                    // files natively
                    _scanIterator = AbstractMzxmlIterator.createParser(_file, 2);
                }
            }
        }
        catch (IOException x)
        {
            _log.warn("Spectra were not imported: " + x.toString());  // Note: x.getMessage() has just the file name
        }
        catch (XMLStreamException x)
        {
            throw new RuntimeException(x);
        }        
    }

    protected void upload()
    {
        try
        {
            if (shouldImportSpectra())
                importSpectra();
        }
        finally
        {
            close();
        }
    }


    private boolean shouldImportSpectra()
    {
        if (!_shouldImportSpectra)
            _log.info("Spectra were not imported: pep.xml file included \"pipeline, import spectra = no\" or \"pipeline, load spectra = no\" setting.");
        else if (null == _scans || _scans.isEmpty())
            _log.warn("Spectra were not imported: no scans were imported from pep.xml file.");
        else if (null == _scanIterator)
            _log.warn("Spectra were not imported: could not open spectrum source.");
        else
            return true;

        return false;
    }


    // Iterates the spectra and writes them to the ms2.SpectraData table using the same Fraction & Row as the peptide table
    private void importSpectra()
    {
        _progress.getCumulativeTimer().setCurrentTask(MS2Importer.Tasks.ImportSpectra, "from " + _file);

        DbSchema schema = MS2Manager.getSchema();
        Connection conn = null;
        PreparedStatement spectraStmt = null;
        PreparedStatement retentionStmt = null;

        try
        {
            conn = schema.getScope().getConnection();
            conn.setAutoCommit(false);
            spectraStmt = conn.prepareStatement("INSERT INTO " + MS2Manager.getTableInfoSpectraData() + " (Fraction, Scan, Spectrum) VALUES (?, ?, ?)");
            spectraStmt.setInt(1, _fractionId);

            if (_shouldImportRetentionTime)
            {
                retentionStmt = conn.prepareStatement("UPDATE " + MS2Manager.getTableInfoPeptidesData() + " SET RetentionTime = ? WHERE Scan = ? AND Fraction = ?");
                retentionStmt.setInt(3, _fractionId);
            }

            int file = 0;

            while (_scanIterator.hasNext())
            {
                SimpleScan spectrum = _scanIterator.next();
                int scan = spectrum.getScan();

                // Import spectrum only if we imported the corresponding scan in the XML file.
                // Since we want to store spectrum only once per scan, remove it from the set so
                // it's not duplicated if this spectrum shows up again (e.g., multiple DTA files
                // for a single scan but different charge)
                if (_scans.contains(scan))
                {
                    _scans.remove(scan);

                    float[][] data = spectrum.getData();
                    byte[] copyBytes = floatArraysToByteArray(data[0], data[1]);

                    spectraStmt.setInt(2, scan);
                    spectraStmt.setBytes(3, copyBytes);
                    spectraStmt.addBatch();

                    if (_shouldImportRetentionTime)
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
                        spectraStmt.executeBatch();

                        if (_shouldImportRetentionTime)
                            retentionStmt.executeBatch();

                        conn.commit();
                    }

                    _progress.addSpectrum();
                }
            }

            if (!_scans.isEmpty())
            {
                _log.warn("Could not find spectra for " + _scans.size() + " scans.");
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
                if (null != spectraStmt)
                {
                    spectraStmt.executeBatch();
                    spectraStmt.close();
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

            if (null != conn)
            {
                schema.getScope().releaseConnection(conn);
            }
        }
    }


    private void close()
    {
        if (null != _scanIterator)
            _scanIterator.close();

        _scanIterator = null;
    }


    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();

        assert null == _scanIterator;
    }


    public File getFile()
    {
        return _file;
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

        return new Pair<>(x, y);
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
