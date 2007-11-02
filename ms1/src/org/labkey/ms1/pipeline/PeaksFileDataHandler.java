package org.labkey.ms1.pipeline;

import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.util.URLHelper;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.pipeline.PeaksFileImporter;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
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
    public static final String PEAKS_FILE_EXTENSION = ".peaks.xml";

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if(null == data || null == dataFile || null == info || null == log || null == context)
            return;

        try
        {
            if(MS1Manager.get().isAlreadyImported(dataFile,data))
            {
                log.info("The file " + dataFile.toURI() + " has already been imported for this experiment into this container.");
                return;
            }

            //because peak files can be huge, we do not load them under a transaction, which would escallate to a table lock
            //however, this also means that if the server dies during an import, we could have half-imported data
            //sitting in the database for this same experiment data file id.
            //this method will mark those for deletion (see PurgeTask)
            //note that this assumes the pipeline will never allow two user to load the same file at the same time
            MS1Manager.get().deleteFailedImports(data.getRowId(), MS1Manager.FILETYPE_PEAKS);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            SAXParser parser = factory.newSAXParser();
            PeaksFileImporter importer = new PeaksFileImporter(data, getMzXmlFilePath(data), info.getUser(), log);
            
            parser.parse(dataFile, importer);
        }
        catch(IOException e)
        {
            throw new ExperimentException(e);
        }
        catch(ParserConfigurationException e)
        {
            throw new ExperimentException(e);
        }
        catch(SAXException e)
        {
            throw new ExperimentException(e);
        }
        catch(SQLException e)
        {
            throw new ExperimentException(MS1Manager.get().getAllErrors(e));
        }

    } //importFile()

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
            MS1Manager.get().deletePeakData(data.getRowId());
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
            MS1Manager.get().moveFileData(oldDataRowID, newData.getRowId());
        }
        catch(SQLException e)
        {
            throw new ExperimentException(e);
        }
    } //runMoved()

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        ViewURLHelper url = new ViewURLHelper(request, "ms1", "showPeaks.view", container);
        url.addParameter("dataRowId", Integer.toString(data.getRowId()));
        return url;
    } //getContentURL()

    public Priority getPriority(ExpData data)
    {
        //we handle only *.peaks.xml files
        return (null != data && null != data.getDataFileUrl() && 
                data.getDataFileUrl().endsWith(PEAKS_FILE_EXTENSION)) ? Priority.MEDIUM : null;
    } //Priority()

} //class PeaksFileDataHandler
