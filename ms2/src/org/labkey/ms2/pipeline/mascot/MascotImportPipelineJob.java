/*
 * Copyright (c) 2006-2015 LabKey Corporation
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

package org.labkey.ms2.pipeline.mascot;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PepXMLFileType;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Importer;
import org.labkey.ms2.PeptideImporter;
import org.labkey.ms2.pipeline.MS2ImportPipelineJob;
import org.labkey.ms2.pipeline.TPPTask;
import org.labkey.ms2.reader.MascotDatLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * User: jeckels
 * Date: Mar 9, 2006
 */
public class MascotImportPipelineJob extends MS2ImportPipelineJob
{
    private final File _file;

    private Boolean _useMascot2XML = false;


    public MascotImportPipelineJob(ViewBackgroundInfo info, File file, String description,
                                   MS2Importer.RunInfo runInfo, PipeRoot root, Boolean useMascot2XML)
    {
        super(info, (useMascot2XML ? TPPTask.getPepXMLFile(file.getParentFile(), FileUtil.getBaseName(file)) : file), description, runInfo, root);
        _useMascot2XML = useMascot2XML;
        _file = file;
    }

    private File getSequenceDatabase () throws IOException
    {
        // return the sequence database queried against in this search
        final File dat = new File(_file.getAbsolutePath());

        if (!NetworkDrive.exists(dat))
            throw new FileNotFoundException(_file.getAbsolutePath() + " not found");

        // TODO replace with use of MascotDatLoader
        InputStream datIn = new FileInputStream(dat);
        boolean skipParameter = true;
        String dbValue = null;
        String fastaFileValue = null;
        String line;
        try (BufferedReader datReader = new BufferedReader(new InputStreamReader(datIn, StringUtilsLabKey.DEFAULT_CHARSET));
        )
        {
            while ((line = datReader.readLine()) != null)
            {
                // TODO: check for actual MIME boundary
                if (line.startsWith("Content-Type: "))
                {
                    skipParameter = !line.endsWith("; name=\"parameters\"") && !line.endsWith("; name=\"header\"");
                }
                else if (!skipParameter)
                {
                    if (line.startsWith(MascotDatLoader.DB_PREFIX))
                    {
                        dbValue = line.substring(MascotDatLoader.DB_PREFIX.length());
                    }
                    else if (line.startsWith(MascotDatLoader.FASTAFILE_PREFIX))
                    {
                        fastaFileValue = line.substring(MascotDatLoader.FASTAFILE_PREFIX.length());
                    }
                }

                if (dbValue != null && fastaFileValue != null)
                    break;
            }
        }

        return PeptideImporter.getDatabaseFile(getPipeRoot().getContainer(), dbValue, fastaFileValue);
    }

    public void run()
    {
        if ((_useMascot2XML && !setStatus("TRANSLATING")) || (!_useMascot2XML && !setStatus("INITIALIZING")))
        {
            return;
        }

        String _dirAnalysis = _file.getParent();
        String _baseName = FileUtil.getBaseName(_file);
        File dirWork = new File(_dirAnalysis, _baseName + ".import.work");
        File workFile = new File(dirWork.getAbsolutePath(), _file.getName());

        File fileOutputTGZ = new File(dirWork.getAbsolutePath(), _baseName + ".tgz");
        // .pep.tgz is the faked-up .out and .dta files from Mascot2XML
        File filePepXMLTGZ = new File(_dirAnalysis, _baseName + ".pep.tgz");
        // output filename depends on Mascot2XML version, figure that out post-execution
        File fileOutputXML = null;
        File filePepXML = null;

        boolean completeStatus = false;
        try
        {
            if (!dirWork.exists() && !dirWork.mkdir())
            {
                getLogger().error("Failed create working folder "+dirWork.getAbsolutePath()+".");
                return;
            }

            try
            {
                FileUtils.copyFile(_file, workFile);
            }
            catch (IOException x)
            {
                getLogger().error("Failed to move Mascot result file to working folder as "+workFile.getAbsolutePath(), x);
                return;
            }

            if (_useMascot2XML)
            {
                File fileSequenceDatabase = getSequenceDatabase();

                // let's convert Mascot .dat to .pep.xml
                // via Mascot2XML.exe <input.dat> -D<database> -xml

                String mascot2XMLPath = PipelineJobService.get().getExecutablePath("Mascot2XML", null, "tpp", null, getLogger());

                ProcessBuilder builder = new ProcessBuilder(mascot2XMLPath
                        , workFile.getName()
                        , "-D" + fileSequenceDatabase.getAbsolutePath()
                        , "-xml"
                        //,"-notgz" - we might not have the mzXML file with us
                        , "-desc"
                );
                builder.environment().put("WEBSERVER_ROOT", StringUtils.trimToEmpty(new File(mascot2XMLPath).getParent()));

                runSubProcess(builder,
                        workFile.getParentFile());

                PepXMLFileType pepxft = new PepXMLFileType(true); // "true" == accept .xml as valid extension for older converters
                fileOutputXML = new File(dirWork.getAbsolutePath(), pepxft.getName(dirWork.getAbsolutePath(), _baseName));
                // three possible output names: basename.xml, basename.pep.xml, basename.pep.xml.gz
                String pepXMLFileName = _baseName + "." + pepxft.getDefaultRole() + (fileOutputXML.getName().endsWith(".gz") ? ".gz" : "");
                filePepXML = new File(_dirAnalysis, pepXMLFileName);

                // we let any error fall thru' to super.run() so that
                // MS2Run's status get updated correctly
                if (!fileOutputTGZ.exists())
                {
                    getLogger().error("Failed running Mascot2XML - expected output file " + fileOutputTGZ + " was not created");
                    return;
                }
                if (!fileOutputXML.exists())
                {
                    getLogger().error("Failed running Mascot2XML - expected output file " + fileOutputXML + " was not created");
                    return;
                }

                // let's rename the file to the appropriate extension
                if (!fileOutputTGZ.renameTo(filePepXMLTGZ))
                {
                    getLogger().error("Failed moving " + fileOutputTGZ.getName() + " to " + filePepXMLTGZ.getAbsolutePath());
                    return;
                }
                if (!fileOutputXML.renameTo(filePepXML))
                {
                    getLogger().error("Failed moving " + fileOutputXML.getName() + " to " + filePepXML.getAbsolutePath());
                    return;
                }
            }

            // let's import the .pep.xml or .dat file
            super.run();
            if (getErrors() == 0)
            {

                if (_useMascot2XML && filePepXML != null && !filePepXML.delete())
                {
                    getLogger().error("Failed to delete " + filePepXML.getAbsolutePath());
                    return;
                }
                else if (_useMascot2XML && !filePepXMLTGZ.delete())
                {
                    getLogger().error("Failed to delete " + filePepXMLTGZ.getAbsolutePath());
                    return;
                }
                else if (!workFile.delete())
                {
                    getLogger().error("Failed to delete " + workFile.getAbsolutePath());
                    return;
                }
                else if (!dirWork.delete())
                {
                    getLogger().error("Failed to delete " + dirWork.getAbsolutePath());
                    return;
                }
                else
                {
                    setStatus(TaskStatus.complete);
                }
                completeStatus = true;
            }
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
                if ((filePepXML != null) && filePepXML.exists())
                    filePepXML.renameTo (fileOutputXML);
                setStatus(TaskStatus.error);
            }
            if (workFile.exists())
            {
                workFile.delete();
            }
        }
    }
}
