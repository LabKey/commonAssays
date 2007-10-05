package org.labkey.ms1;

import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.security.User;
import org.labkey.api.util.URLHelper;
import org.labkey.common.tools.SimpleXMLStreamReader;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.sql.SQLException;

/**
 * Imports the Peaks XML file format used by Ceadars-Sinai
 *
 * Created by IntelliJ IDEA.
 * User: DaveS
 * Date: Sep 25, 2007
 * Time: 9:15:46 AM
 */
public class PeaksFileDataHandler extends AbstractExperimentDataHandler
{
    ///////////////////////////////////////////////////////////////////////////
    // Public Interface

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if(null == data || null == dataFile || null == info || null == log || null == context)
            return;

        BufferedInputStream stream = null;
        SimpleXMLStreamReader parser = null;
        try
        {
            if(MS1Manager.get().isAlreadyImported(dataFile,data))
            {
                log.info("The file " + dataFile.getAbsolutePath().replace("\\","/") + " has already been imported for this experiment into this container.");
                return;
            }

            //open a buffered input stream over the file
            log.info("Opening the peaks xml file for import.");
            stream = new BufferedInputStream(new FileInputStream(dataFile));

            //create a simple xml stream parser over the stream
            parser = new SimpleXMLStreamReader(stream);

            //skip to the opening element. warn and return if there isn't one
            if(XMLStreamConstants.START_ELEMENT != parser.nextTag())
            {
                log.warn("The peaks data file contians no elements. Skipping import.");
                return;
            }

            //ensure that the opening element is named "peakdata".
            //note that the current format does not use namespaces, but this might change in the future
            if(!parser.getLocalName().equalsIgnoreCase(ELEM_PEAKDATA))
                throw new XMLStreamException("No <peakdata> element found in peaks file.");

            //create a new import context
            ImportContext icontext = new ImportContext(data.getRowId(), getMzXmlFilePath(data), log, parser,
                                                        MS1Manager.get().getSchema(), info.getUser(), MS1Manager.get());

            assert icontext.isValid();

            //NOTE: we will not use a transaction here because the amount
            //of data in these files could be enormous (hundreds of thousands of detail rows)
            //instead we will use an imported bit on the PeaksFiles table to indicate if
            //the file was successfully imported or not.
            //begin the parse and import
            importPeakData(icontext);
        }
        catch(IOException e)
        {
            throw new ExperimentException(e);
        }
        catch(SQLException e)
        {
            throw new ExperimentException(e);
        }
        catch(XMLStreamException e)
        {
            throw new ExperimentException(e);
        }
        finally
        {
            //close the parser and the underlying stream
            try{if(null != parser) parser.close();} catch(XMLStreamException ignore){}
            try{if(null != stream) stream.close();} catch(IOException ignore){}
        }

    } //importFile()

    protected void importPeakData(ImportContext icontext) throws XMLStreamException, SQLException
    {
        int eventID = 0;
        String elemName;
        icontext.peaksFile = new PeaksFile(icontext.dataFileRowID, icontext.mzXmlFilePath);

        icontext.log.info("Beginning import of peaks xml data.");

        while(icontext.parser.hasNext())
        {
            eventID = icontext.parser.nextTag();
            elemName = icontext.parser.getLocalName();

            //break out when we hit the </peakdata> tag
            if(XMLStreamConstants.END_ELEMENT == eventID && elemName.equalsIgnoreCase(ELEM_PEAKDATA))
                break;

            if(XMLStreamConstants.START_ELEMENT  == eventID)
            {
                if(elemName.equalsIgnoreCase("id"))
                    icontext.peaksFile.setDescription(icontext.parser.getAllText());
                else if(elemName.equalsIgnoreCase(ELEM_SCANS))
                {
                    //Save the peaksFile so we can get its new ID
                    icontext.peaksFile = icontext.manager.save(icontext.peaksFile, icontext.user);
                    assert icontext.peaksFile.getPeaksFileID() >= 0 : "Didn't get new peak file ID from the table layer!";

                    //import the scans
                    importScans(icontext);
                }
            } //if there was another start element
        } //while more to parse

        //set imported to true and save
        icontext.peaksFile.setImported(true);
        icontext.manager.save(icontext.peaksFile, icontext.user);

        icontext.log.info("Finished importing " + icontext.numPeaks + " peaks.");

    } //importPeakData()

    protected void importScans(ImportContext icontext) throws XMLStreamException, SQLException
    {
        int eventID = 0;
        String elemName;

        //<scans> can contain one or more <scan> elements only
        while(icontext.parser.hasNext())
        {
            eventID = icontext.parser.nextTag();
            elemName = icontext.parser.getLocalName();

            //break out when we hit the </scans> tag
            if(XMLStreamConstants.END_ELEMENT == eventID && elemName.equalsIgnoreCase(ELEM_SCANS))
                break;

            if(elemName.equalsIgnoreCase(ELEM_SCAN))
                importScan(icontext);
        }
    } //importScans

    protected void importScan(ImportContext icontext) throws XMLStreamException, SQLException
    {
        String elemName;
        icontext.scan = new Scan(icontext.peaksFile.getPeaksFileID());

        try
        {
            while(icontext.parser.nextTag() != XMLStreamConstants.END_DOCUMENT
                    && !(icontext.parser.isEndElement()
                    && icontext.parser.getLocalName().equalsIgnoreCase(ELEM_SCAN)))
            {
                elemName = icontext.parser.getLocalName();
                if(elemName.equalsIgnoreCase("scanNumber"))
                    icontext.scan.setScan(Integer.parseInt(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("retentionTime"))
                    icontext.scan.setRetentionTime(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("observationDuration"))
                    icontext.scan.setObservedDuration(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase(ELEM_CALIBRATION_PARAMS))
                {
                    icontext.scan = icontext.manager.save(icontext.scan, icontext.user);
                    importCalibrationParams(icontext);
                }
                else if(elemName.equalsIgnoreCase(ELEM_PEAK_FAMILIES))
                {
                    icontext.scan = icontext.manager.save(icontext.scan, icontext.user);
                    importPeakFamilies(icontext);
                }
            } //while within this <scan> element

            //save it in case scan-specific tags followed one of the child collection tags
            //(save will no-op if the object is not dirty)
            icontext.manager.save(icontext.scan, icontext.user);
        }
        catch(NumberFormatException e)
        {
            icontext.log.error("The value " + icontext.parser.getAllText() + " for element " + icontext.parser.getLocalName() + " could not be parsed as a number.");
            throw new XMLStreamException("The value " + icontext.parser.getAllText() + " for element " + icontext.parser.getLocalName() + " could not be parsed as a number: " + e);
        }
    } //importScan()

    protected void importCalibrationParams(ImportContext icontext) throws XMLStreamException, SQLException
    {
        while(icontext.parser.nextTag() != XMLStreamConstants.END_DOCUMENT
                && !(icontext.parser.isEndElement()
                && icontext.parser.getLocalName().equalsIgnoreCase(ELEM_CALIBRATION_PARAMS)))
        {
            importCalibrationParam(icontext);
        }
    } //importCalibrationParams()

    protected void importCalibrationParam(ImportContext icontext) throws XMLStreamException, SQLException
    {
        CalibrationParam cparam = new CalibrationParam(icontext.peaksFile.getPeaksFileID(), icontext.scan.getScan());
        cparam.setName(icontext.parser.getLocalName());
        cparam.setValue(Double.valueOf(icontext.parser.getAllText()));
        icontext.manager.save(cparam, icontext.user);
    } //importCalibrationParam()

    protected void importPeakFamilies(ImportContext icontext) throws XMLStreamException, SQLException
    {
        //the database structure allows a m:m relationship between peaks and peak families
        //and for peak families to group peaks across scans, but the current xml file doesn't
        //allow for this yet. So for now, each peak family read from these files
        //will be associated with its parent scan and all peaks will belong to
        //only their enclosing peak family. This might need to change in the future
        while(icontext.parser.nextTag() != XMLStreamConstants.END_DOCUMENT
                && !(icontext.parser.isEndElement()
                && icontext.parser.getLocalName().equalsIgnoreCase(ELEM_PEAK_FAMILIES)))
        {
            importPeakFamily(icontext);
        }

        icontext.peakFamily = null;
    } //importPeakFamilies()

    protected void importPeakFamily(ImportContext icontext) throws XMLStreamException, SQLException
    {
        try
        {
            String elemName;
            icontext.peakFamily = (null == icontext.scan ?
                    new PeakFamily() :
                    new PeakFamily(icontext.peaksFile.getPeaksFileID(), icontext.scan.getScan()));

            while(icontext.parser.nextTag() != XMLStreamConstants.END_DOCUMENT
                    && !(icontext.parser.isEndElement()
                    && icontext.parser.getLocalName().equalsIgnoreCase(ELEM_PEAK_FAMILY)))
            {
                elemName = icontext.parser.getLocalName();
                if(elemName.equalsIgnoreCase("mz"))
                    icontext.peakFamily.setMz(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("charge"))
                    icontext.peakFamily.setCharge(Byte.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase(ELEM_PEAKS))
                {
                    icontext.peakFamily = icontext.manager.save(icontext.peakFamily, icontext.user);
                    importPeaks(icontext);
                } //peaks collection
            } //while parsing tags

            icontext.manager.save(icontext.peakFamily, icontext.user);
        }
        catch(NumberFormatException e)
        {
            icontext.log.error("The value " + icontext.parser.getAllText() + " for element " + icontext.parser.getLocalName() + " could not be parsed as a number.");
            throw new XMLStreamException("The value " + icontext.parser.getAllText() + " for element " + icontext.parser.getLocalName() + " could not be parsed as a number: " + e);
        }
    } //importPeakFamily()

    protected void importPeaks(ImportContext icontext) throws XMLStreamException, SQLException
    {
        while(icontext.parser.nextTag() != XMLStreamConstants.END_DOCUMENT
                && !(icontext.parser.isEndElement()
                && icontext.parser.getLocalName().equalsIgnoreCase(ELEM_PEAKS)))
        {
            importPeak(icontext);
        }
    } //importPeaks()

    protected void importPeak(ImportContext icontext) throws XMLStreamException, SQLException
    {
        try
        {
            String elemName;
            Peak peak = new Peak(icontext.peaksFile.getPeaksFileID(), icontext.scan.getScan());

            while(icontext.parser.nextTag() != XMLStreamConstants.END_DOCUMENT
                    && !(icontext.parser.isEndElement()
                    && icontext.parser.getLocalName().equalsIgnoreCase(ELEM_PEAK)))
            {
                elemName = icontext.parser.getLocalName();
                if(elemName.equalsIgnoreCase("mz"))
                    peak.setMz(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("frequency"))
                    peak.setFrequency(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("amplitude"))
                    peak.setAmplitude(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("phase"))
                    peak.setPhase(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("decay"))
                    peak.setDecay(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("error"))
                    peak.setError(Double.valueOf(icontext.parser.getAllText()));
                else if(elemName.equalsIgnoreCase("area"))
                    peak.setArea(Double.valueOf(icontext.parser.getAllText()));

            } //while parsing tags

            //insert the new peak
            //and relate it to the parent peak family
            peak = icontext.manager.save(peak, icontext.user);
            icontext.manager.addPeakToFamily(icontext.peakFamily, peak, icontext.user);

            icontext.numPeaks++;
        }
        catch(NumberFormatException e)
        {
            icontext.log.error("The value " + icontext.parser.getAllText() + " for element " + icontext.parser.getLocalName() + " could not be parsed as a number.");
            throw new XMLStreamException("The value " + icontext.parser.getAllText() + " for element " + icontext.parser.getLocalName() + " could not be parsed as a number: " + e);
        }
    } //importPeak()

    /**
     * Returns the master mzXML file path for the data file
     * @param data  Experiment data object
     * @return      Path to the mzXML File
     */
    protected String getMzXmlFilePath(ExpData data)
    {
        //by convention, the mzXML has the same base name as the data file (minus the ".peaks.xml")
        //and is located three directories above the data file
        File dataFile = data.getDataFile();
        String dataFileName = dataFile.getName().substring(0, dataFile.getName().length() - ".peaks.xml".length());
        File mzxmlFile = new File(dataFile.getParentFile().getParentFile().getParentFile(), dataFileName + ".mzXML");
        return mzxmlFile.toURI().toString();
    } //getMzXmlFilePath()

    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
    {
        if(null == data || null == container || null == user)
            return;

        int expDataFileID = data.getRowId();
        try
        {
            MS1Manager.get().deletePeakData(data.getRowId(), user);
        }
        catch(SQLException e)
        {
            throw new ExperimentException(e);
        }
    } //deleteData()


    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        if(null == newData || null == user) //anything else?
            return;

        try
        {
            MS1Manager.get().movePeakData(oldDataRowID, newData.getRowId(), user);
        }
        catch(SQLException e)
        {
            throw new ExperimentException(e);
        }
    } //runMoved()

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        //TODO: This was copied from MSInspectFeaturesDataHandler -- not sure how this needs to be changed
        ViewURLHelper url = new ViewURLHelper(request, "ms1", "showPeaksFile.view", container);
        url.addParameter("dataRowId", Integer.toString(data.getRowId()));
        return url;
    } //getContentURL()

    public Priority getPriority(ExpData data)
    {
        //we handle only *.peaks.xml files
        return (null != data && null != data.getDataFileUrl() && 
                data.getDataFileUrl().endsWith(".peaks.xml")) ? Priority.MEDIUM : null;
    } //Priority()

    ///////////////////////////////////////////////////////////////////////////
    // Protected Constants
    protected static final String ELEM_PEAKDATA = "peakdata";
    protected static final String ELEM_SCANS = "scans";
    protected static final String ELEM_SCAN = "scan";
    protected static final String ELEM_CALIBRATION_PARAMS = "calibrationParameters";
    protected static final String ELEM_PEAK_FAMILIES = "peak_families";
    protected static final String ELEM_PEAK_FAMILY = "peak_family";
    protected static final String ELEM_PEAKS = "peaks";
    protected static final String ELEM_PEAK = "peak";
    
    
    protected static final String TABLE_PEAKSFILES = "PeaksFiles";
    protected static final String TABLE_SCANS = "Scans";
    protected static final String TABLE_CALIBRATION_PARAMS = "CalibrationParams";

    ///////////////////////////////////////////////////////////////////////////
    // Protected Inner Classes
    protected class ImportContext
    {
        public int dataFileRowID = -1;
        public String mzXmlFilePath;
        public Logger log = null;
        public SimpleXMLStreamReader parser = null;

        public DbSchema schema = null;
        public DbScope scope = null;
        public User user = null;
        public MS1Manager manager = null;

        public PeaksFile peaksFile = null;      //the current peaks file
        public Scan scan = null;                //the current scan
        public PeakFamily peakFamily = null;    //the current peak family

        public int numPeaks = 0;                //total number of peaks loaded

        public ImportContext(int dataFileRowID, String mzXmlFilePath, Logger log,
                             SimpleXMLStreamReader parser, DbSchema schema, User user, MS1Manager manager)
        {
            this.dataFileRowID = dataFileRowID;
            this.mzXmlFilePath = mzXmlFilePath;
            this.log = log;
            this.parser = parser;
            this.schema = schema;
            if(null != schema)
                this.scope = schema.getScope();
            this.user = user;
            this.manager = manager;
        }

        public boolean isValid()
        {
            return null != parser && null != log && null != schema &&
                    null != scope && null != user && null != manager && dataFileRowID > 0;
        }
    } //class ImportContext

} //class PeaksFileDataHandler
