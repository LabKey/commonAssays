package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Importer;
import org.labkey.api.util.NetworkDrive;

import java.io.*;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Mar 9, 2006
 */
public class MascotImportPipelineJob extends MS2ImportPipelineJob
{
    private final File _file;

    public MascotImportPipelineJob(ViewBackgroundInfo info, File file, String description,
                                MS2Importer.RunInfo runInfo, boolean appendLog) throws SQLException
    {
        super(info,
            MS2PipelineManager.getPepXMLFile(file.getParentFile(), MS2PipelineManager.getBasename(file))
            , description, runInfo, appendLog);
        _file = file;
    }

    private String getSequenceDatabase () throws IOException
    {
        // return the sequence database queried against in this search
        final File dat = new File(_file.getAbsolutePath());

        if (!NetworkDrive.exists(dat))
            throw new FileNotFoundException(_file.getAbsolutePath() + " not found");

        InputStream datIn = new FileInputStream(dat);
        BufferedReader datReader = new BufferedReader(new InputStreamReader(datIn));
        boolean skipParameter = true;
        String sequenceDatabaseTag = "DB=";
        String sequenceDatabase = null;
        while (true)
        {
            String line = datReader.readLine();
            if (null == line) break;

            // TODO: check for actual MIME boundary
            if (line.startsWith("Content-Type: "))
            {
                skipParameter = !line.endsWith("; name=\"parameters\"");
            }
            else
            {
                if (!skipParameter && line.startsWith(sequenceDatabaseTag))
                {
                    sequenceDatabase = line.substring(sequenceDatabaseTag.length());
                    break;
                }
            }
        }
        return sequenceDatabase;
    }

    public void run()
    {
        setStatus("TRANSLATING");

        String _dirAnalysis = _file.getParent();
        String _baseName = MS2PipelineManager.getBasename(_file);
        File dirWork = new File(_dirAnalysis, _baseName + ".import.work");
        File workFile = new File(dirWork.getAbsolutePath(), _file.getName());

        File fileOutputTGZ = new File(dirWork.getAbsolutePath(), _baseName + ".tgz");
        File fileOutputXML = new File(dirWork.getAbsolutePath(), _baseName + ".xml");
        File filePepXMLTGZ = new File(_dirAnalysis, _baseName + ".pep.tgz");
        File filePepXML = new File(_dirAnalysis, _baseName + ".pep.xml");

        boolean completeStatus = false;
        try
        {
            String sequenceDB = getSequenceDatabase ();
            File fileSequenceDatabase = new File(
                    MS2PipelineManager.getSequenceDatabaseRoot(this.getContainer()).getPath(), sequenceDB);

            if (!dirWork.exists() && !dirWork.mkdir())
            {
                getLogger().error("Failed create working folder "+dirWork.getAbsolutePath()+".");
                return;
            }

            try
            {
                MS2PipelineManager.copyFile (_file, workFile);
            }
            catch (IOException x)
            {
                getLogger().error("Failed to move Mascot result file to working folder as "+workFile.getAbsolutePath(), x);
                return;
            }

            // let's convert Mascot .dat to .pep.xml
            // via Mascot2XML.exe <input.dat> -D<database> -xml
            header("Mascot2XML output");
            int iReturn = runSubProcess(new ProcessBuilder("Mascot2XML"
                    ,workFile.getName()
                    ,"-D" + fileSequenceDatabase.getAbsolutePath()
                    ,"-xml"
                    //,"-notgz" - we might not have the mzXML file with us
                    ,"-desc"
                    ,"-shortid"
                    ),
                    workFile.getParentFile());

            // we let any error fall thru' to super.run() so that
            // MS2Run's status get updated correctly
            if (iReturn != 0 || !fileOutputTGZ.exists() || !fileOutputXML.exists())
            {
                getLogger().error("Failed running Mascot2XML.");
            }
            else
            {
                // let's rename the file to the appropriate extension
                if (!fileOutputTGZ.renameTo (filePepXMLTGZ))
                {
                    getLogger().error("Failed move "+fileOutputTGZ.getName()+" to "+filePepXMLTGZ.getAbsolutePath());
                }
                if (!fileOutputXML.renameTo (filePepXML))
                {
                    getLogger().error("Failed move "+fileOutputXML.getName()+" to "+filePepXML.getAbsolutePath());
                }
            }

            // let's import the .pep.xml file
            super.run ();

            if (!filePepXML.delete())
            {
                getLogger().error("Failed to delete " + filePepXML.getAbsolutePath());
                return;
            }
            else if (!filePepXMLTGZ.delete())
            {
                getLogger().error("Failed to delete " + filePepXMLTGZ.getAbsolutePath());
                return;
            }
            else if (!workFile.delete())
            {
                getLogger().error("Failed to delete "+workFile.getAbsolutePath());
                return;
            }
            else if (!dirWork.delete())
            {
                getLogger().error("Failed to delete " + dirWork.getAbsolutePath());
                return;
            }
            else
            {
                setStatus(PipelineJob.COMPLETE_STATUS);
            }
            completeStatus = true;
        }
        catch (Exception e)
        {
            getLogger().error("MS2 import failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                // if not successful run, we move the temporary file
                // back to the working folder
                if (filePepXMLTGZ.exists())
                    filePepXMLTGZ.renameTo (fileOutputTGZ);
                if (filePepXML.exists())
                    filePepXML.renameTo (fileOutputXML);
                setStatus(PipelineJob.ERROR_STATUS);
            }
            if (workFile.exists())
            {
                workFile.delete();
            }
        }
    }
}
