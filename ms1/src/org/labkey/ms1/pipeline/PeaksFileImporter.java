/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

package org.labkey.ms1.pipeline;

import org.apache.log4j.Logger;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.Table;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.security.User;
import org.labkey.ms1.MS1Manager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * The SAX handler for .peaks.xml files.
 * <p>
 * This class is stateful, so create one per parse.
 *
 * User: Dave
 * Date: Oct 8, 2007
 * Time: 5:07:47 PM
 */
public class PeaksFileImporter extends DefaultHandler
{
    public PeaksFileImporter(ExpData expData, String mzXmlUrl, User user, Logger log, DbScope.Transaction transaction)
    {
        _log = log;
        _expData = expData;
        _mzXmlUrl = mzXmlUrl;
        _user = user;
        _transaction = transaction;
    }

    public void startDocument() throws SAXException
    {
        _log.info("Starting to parse and import peaks file " + _expData.getFile().toURI());
        _msStart = System.currentTimeMillis();

        //parsing state initialization
        clear();
    }

    public void endDocument() throws SAXException
    {
        //set imported flag on file to true
        try
        {
            HashMap<String,Object> map = new HashMap<>();
            map.put("Imported", Boolean.TRUE);
            Table.update(_user, MS1Manager.get().getTable(MS1Manager.TABLE_FILES), map, _idFile);
        }
        catch(RuntimeSQLException e)
        {
            throw new SAXException(MS1Manager.get().getAllErrors(e.getSQLException()));
        }

        _log.info("Finished importing " + _numScans + " scans, " + _numPeakFamilies + " peak families, and "
                    + _numPeaks + " peaks into the database in "
                    + ((System.currentTimeMillis() - _msStart) / 1000) + " seconds.");

        //cleanup
        clear();
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        try
        {
            if(qName.equalsIgnoreCase("peakdata"))
                _idFile = onFile(attributes);
            else if(qName.equalsIgnoreCase("software"))
                _idSoftware = onSoftware(attributes, _idFile);
            else if(qName.equalsIgnoreCase("parameter"))
                onParameter(attributes, _idSoftware);
            else if(qName.equalsIgnoreCase("scan"))
                _idScan = onScan(attributes, _idFile);
            else if(qName.equalsIgnoreCase("calibrationParameters"))
                onCalibrations(attributes, _idScan);
            else if(qName.equalsIgnoreCase("peakFamily"))
                _idPeakFamily = onPeakFamily(attributes, _idScan);
            else if(qName.equalsIgnoreCase("peak"))
                onPeak(attributes, _idScan, _idPeakFamily);
        }
        catch(SQLException e)
        {
            throw new SAXException(MS1Manager.get().getAllErrors(e));
        }
    }

    protected Integer onFile(Attributes attrs) throws SQLException
    {
        //build up a map from our initialization parameters
        Map<String,Object> map = newMap("FileId");
        map.put("ExpDataFileId", Integer.valueOf(_expData.getRowId()));
        map.put("Type", Integer.valueOf(MS1Manager.FILETYPE_PEAKS));
        map.put("MzXmlUrl", _mzXmlUrl);

        //save to the database
        map = Table.insert(_user, MS1Manager.get().getTable(MS1Manager.TABLE_FILES), map);

        //return new ID
        return (Integer)map.get("FileId");
    }

    protected Integer onSoftware(Attributes attrs, Integer idFile) throws SQLException
    {
        Map<String,Object> map = newMap("SoftwareId", "FileId", idFile);
        map.put("Name", attrs.getValue("name"));
        map.put("Version", attrs.getValue("version"));
        map.put("Author", attrs.getValue("source"));

        map = Table.insert(_user, MS1Manager.get().getTable(MS1Manager.TABLE_SOFTWARE), map);
        return (Integer)map.get("SoftwareId");
    }

    protected void onParameter(Attributes attrs, Integer idSoftware) throws SQLException, SAXException
    {
        HashMap<String,Object> map = newMap("SoftwareId", idSoftware);
        map.put("Name", attrs.getValue("name"));
        map.put("Value", getAttrAsDouble(attrs, "value"));

        Table.insert(_user, MS1Manager.get().getTable(MS1Manager.TABLE_SOFTWARE_PARAMS), map);
    }

    protected Integer onScan(Attributes attrs, Integer idFile) throws SQLException, SAXException
    {
        Map<String, Object> map = newMap("ScanId", "FileId", idFile);
        map.put("Scan", getAttrAsInteger(attrs, "scanNumber"));
        map.put("RetentionTime", getAttrAsDouble(attrs, "retentionTime"));
        map.put("ObservationDuration", getAttrAsDouble(attrs, "observationDuration"));

        map = Table.insert(_user, MS1Manager.get().getTable(MS1Manager.TABLE_SCANS), map);
        ++_numScans;
        return (Integer)map.get("ScanId");
    }

    protected void onCalibrations(Attributes attrs, Integer idScan) throws SQLException, SAXException
    {
        //each attribute is a spearate calibration parameter for the given scan
        //where the attribute name is the calibration parameter name, and value is value
        Map<String, Object> map = newMap("ScanId", idScan);

        for(int idx = 0; idx < attrs.getLength(); ++idx)
        {
            map.put("Name", attrs.getQName(idx));
            map.put("Value", getAttrAsDouble(attrs, attrs.getQName(idx)));
            map = Table.insert(_user, MS1Manager.get().getTable(MS1Manager.TABLE_CALIBRATION_PARAMS), map);
        }
    }

    protected Integer onPeakFamily(Attributes attrs, Integer idScan) throws SQLException, SAXException
    {
        Map<String, Object> map = newMap("PeakFamilyId", "ScanId", idScan);
        map.put("MzMono", getAttrAsDouble(attrs, "mzMonoisotopic"));
        map.put("Charge", getAttrAsInteger(attrs, "charge"));
        map = Table.insert(_user, MS1Manager.get().getTable(MS1Manager.TABLE_PEAK_FAMILIES), map);
        ++_numPeakFamilies;
        return (Integer)map.get("PeakFamilyId");
    }

    protected void onPeak(Attributes attrs, Integer idScan, Integer idPeakFamily) throws SQLException, SAXException
    {
        //since the database is setup for a m:m relationship between peaks
        //and peak families, we need to insert the peak and then also
        //insert a row into the join table (PeaksToFamilies)
        Map<String,Object> map = newMap("PeakId", "ScanId", idScan);

        map.put("MZ", getAttrAsDouble(attrs, "mz"));
        map.put("Intensity", getAttrAsDouble(attrs, "intensity"));
        map.put("Area", getAttrAsDouble(attrs, "area"));
        map.put("Error", getAttrAsDouble(attrs, "error"));
        map.put("Frequency", getAttrAsDouble(attrs, "frequency"));
        map.put("Phase", getAttrAsDouble(attrs, "phase"));
        map.put("Decay", getAttrAsDouble(attrs, "decay"));

        map = Table.insert(_user, MS1Manager.get().getTable(MS1Manager.TABLE_PEAKS), map);

        HashMap<String,Object> mapP2F = new HashMap<>();
        mapP2F.put("PeakFamilyId", idPeakFamily);
        mapP2F.put("PeakId", map.get("PeakId"));
        Table.insert(_user, MS1Manager.get().getTable(MS1Manager.TABLE_PEAKS_TO_FAMILIES), mapP2F);

        ++_numPeaks;

        if(_numPeaks % 1000 == 0)
        {
            _transaction.commitAndKeepConnection();
        }

        if((_numPeaks % 5000) == 0)
            _log.info("Imported " + _numPeaks + " peaks so far....");
    }

    protected Double getAttrAsDouble(Attributes attrs, String qName) throws SAXException
    {
        String sVal = attrs.getValue(qName);
        if(null == sVal)
            return null;

        try
        {
            return Double.parseDouble(sVal);
        }
        catch(NumberFormatException e)
        {
            throw new SAXException("The value '" + attrs.getValue(qName) +
                                    "' in the attribtue named '" + qName +
                                    "' could not be interpreted as a number for the following reason: "
                                    + e);
        }
    }

    protected Integer getAttrAsInteger(Attributes attrs, String qName) throws SAXException
    {
        String sVal = attrs.getValue(qName);
        if(null == sVal)
            return null;

        try
        {
            return Integer.parseInt(sVal);
        }
        catch(NumberFormatException e)
        {
            throw new SAXException("The value '" + attrs.getValue(qName) +
                                    "' in the attribtue named '" + qName +
                                    "' could not be interpreted as a number for the following reason: "
                                    + e);
        }
    }

    protected HashMap<String,Object> newMap(String pkName)
    {
        return newMap(pkName, null, null);
    }

    protected HashMap<String,Object> newMap(String fkName, Integer fkValue)
    {
        return newMap(null, fkName, fkValue);
    }

    protected HashMap<String,Object> newMap(String pkName, String fkName, Integer fkValue)
    {
        HashMap<String,Object> map = new HashMap<>();
        if(null != pkName)
            map.put(pkName, null);
        if(null != fkName)
            map.put(fkName, fkValue);
        return map;
    }

    protected void clear()
    {
        _numPeakFamilies = 0;
        _numScans = 0;
        _numPeaks = 0;
        _idFile = 0;
        _idSoftware = 0;
        _idScan = 0;
        _idPeakFamily = 0;
    }


    protected Logger _log;              //the log file
    protected String _mzXmlUrl;         //URI to the mzXML file
    protected ExpData _expData;         //the experiment data file id
    protected User _user;               //the current user
    private final DbScope.Transaction _transaction;
    protected long _msStart;            //milliseconds at start of doc

    protected int _numPeakFamilies;     //the number of peak families imported
    protected int _numScans;            //the number of scans imported
    protected int _numPeaks;            //the number of peaks imported

    protected Integer _idFile;          //id for newly-inserted file
    protected Integer _idSoftware;
    protected Integer _idScan;
    protected Integer _idPeakFamily;

} //class PeaksFileImporter
