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

import org.labkey.common.tools.SimpleXMLStreamReader;
import org.labkey.api.exp.XarContext;

import javax.xml.stream.XMLStreamException;
import java.sql.SQLException;
import java.sql.Types;
import java.io.FileInputStream;
import java.io.IOException;


public abstract class CometImporter extends MS2Importer
{
    protected String gzFileName = null;
    protected FileInputStream _fIn;
    protected SimpleXMLStreamReader parser;
    protected String scan, charge, ionPercent, mass, deltaMass, peptideProphet, peptide, prevAA, trimmedPeptide, nextAA, proteinHits, protein, dtaFileName;

    public CometImporter(XarContext context)
    {
        super(context);
    }


    protected void prepareWrite()
    {
        if (null == peptideProphet)
            peptideProphet = "0";
    }


    protected int setParameters(int n) throws SQLException
    {
        _stmt.setInt(n++, _fractionId);
        _stmt.setInt(n++, Integer.parseInt(scan));
        _stmt.setInt(n++, Integer.parseInt(charge));
        _stmt.setFloat(n++, Float.parseFloat(ionPercent));
        _stmt.setDouble(n++, Double.parseDouble(mass));        // Store as double so mass + deltaMass results returns high mass accuracy precursor
        _stmt.setFloat(n++, Float.parseFloat(deltaMass));
        _stmt.setFloat(n++, Float.parseFloat(peptideProphet));
        _stmt.setNull(n++, Types.REAL); // No PeptideProphet error rate 
        _stmt.setString(n++, peptide);
        _stmt.setString(n++, prevAA);
        _stmt.setString(n++, trimmedPeptide);
        _stmt.setString(n++, nextAA);
        _stmt.setInt(n++, Integer.parseInt(proteinHits));
        _stmt.setString(n++, protein);

        return n;
    }


    protected void write() throws SQLException
    {
        prepareWrite();

        setParameters(1);
        _stmt.execute();

        _scanCount++;

        if (0 == _scanCount % 5000)
            _log.info("uploadRun: Scan " + _scanCount);
    }


    protected void close()
    {
        try
        {
            if (null != parser)
                parser.close();
        }
        catch (XMLStreamException e)
        {
            logError("Error closing parser", e);
        }
        try
        {
            if (null != _fIn)
                _fIn.close();
        }
        catch (IOException e)
        {
            logError("Error closing input stream", e);
        }

        super.close();
    }
}
