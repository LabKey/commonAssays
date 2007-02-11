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
package org.labkey.ms2.pipeline;

import org.apache.log4j.Logger;
import org.labkey.api.exp.ExperimentPipelineJob;
import org.labkey.api.exp.FileXarSource;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.PathRelativizer;
import org.labkey.api.util.XMLValidationParser;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.data.Container;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

/**
 * XTandemPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class XTandemPipelineJob extends AbstractMS2SearchPipelineJob
{
    private static Logger _log = Logger.getLogger(XTandemPipelineJob.class);

    private File _fileTandemXML;

    public static String getProviderName(boolean cluster)
    {
        if (cluster)
            return XTandemCPipelineProvider.name;
        else
            return XTandemLocalPipelineProvider.name;
    }

    public XTandemPipelineJob(ViewBackgroundInfo info,
                              String name,
                              URI uriRoot,
                              URI uriSequenceRoot,
                              File filesMzXML[],
                              File fileInputXML,
                              boolean cluster,
                              boolean append)
    {
        super(getProviderName(cluster), info, filesMzXML, name, uriRoot, uriSequenceRoot);

        _fileTandemXML = fileInputXML;
        if (isFractions())
            _baseName = "all";
        else
            _baseName = MS2PipelineManager.getBaseName(filesMzXML[0]);
        _dirAnalysis = _fileTandemXML.getParentFile();
        _filePepXML = MS2PipelineManager.getPepXMLFile(_dirAnalysis, _baseName);
        setLogFile(MS2PipelineManager.getLogFile(_dirAnalysis, _baseName), append);
        if (cluster)
            setStatusFile(MS2PipelineManager.getStatusFile(_dirAnalysis, _baseName));

        if (isFractions())
            header("X! Tandem search for " + _dirMzXML.getName());
        else
            header("X! Tandem search for " + filesMzXML[0].getName());
    }

    public XTandemInputParser getInputParameters()
    {
        BufferedReader inputReader = null;
        StringBuffer xmlBuffer = new StringBuffer();
        try
        {
            inputReader = new BufferedReader(new FileReader(_fileTandemXML));
            String line;
            while ((line = inputReader.readLine()) != null)
                xmlBuffer.append(line).append("\n");
        }
        catch (IOException eio)
        {
            error("Failed to read X!Tandem input xml '" + _fileTandemXML.getPath() + "'.");
            return null;
        }
        finally
        {
            if (inputReader != null)
            {
                try
                {
                    inputReader.close();
                }
                catch (IOException eio)
                {
                }
            }
        }

        XTandemInputParser parser = new XTandemInputParser();
        parser.parse(xmlBuffer.toString());
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
            {
                error("Failed parsing X!Tandem input xml '" + _fileTandemXML.getPath() + "'.\n" +
                        err.getMessage());
            }
            else
            {
                error("Failed parsing X!Tandem input xml '" + _fileTandemXML.getPath() + "'.\n" +
                        "Line " + err.getLine() + ": " + err.getMessage());
            }
            return null;
        }
        return parser;
    }

    public void upload()
    {
        setStatus("LOADING");

        XTandemInputParser parser = getInputParameters();
        if (parser == null)
            return;

        String paramDatabase = parser.getInputParameter("pipeline, database");
        if (paramDatabase == null)
        {
            error("Failed parsing X!Tandem input xml '" + _fileTandemXML.getPath() + "'.\n" +
                    "Missing required input parameter 'pipeline, database'");
            return;
        }
        String[] databases = paramDatabase.split(";");

        boolean isXComet = "comet".equals(parser.getInputParameter("scoring, algorithm"));
        boolean isXpress = parser.getInputParameter("pipeline quantitation, residue label mass") != null;

        File fileExperimentXML;
        try
        {
            fileExperimentXML = writeExperimentXML(databases,
                                                    _dirAnalysis,
                                                    _dirAnalysis,
                                                    _filePepXML,
                                                    isXComet,
                                                    isXpress);
        }
        catch (IOException e)
        {
            error("Failed writing XAR file");
            return;
        }
        catch (SQLException e)
        {
            error("Failed writing XAR file");
            return;
        }

        File fileExperimentXMLFinal = new File(_dirAnalysis, fileExperimentXML.getName());

        FileXarSource source = new FileXarSource(fileExperimentXMLFinal);
        String status = ExperimentPipelineJob.loadExperiment(this, source, false);
        _experimentRowId = source.getExperimentRowId();

        setStatus(status);
    }

    public void search()
    {
        setStatus("SEARCHING");

        if (isFractions())
        {
            error("Fractions not supported by mini-pipeline.");
            return;
        }
        File fileMzXML = _filesMzXML[0];
        File dirWork = new File(_dirAnalysis, _baseName + ".work");
        dirWork.mkdir();

        XTandemInputParser parser = getInputParameters();
        if (parser == null)
            return;

        boolean isXComet = "comet".equals(parser.getInputParameter("scoring, algorithm"));

        String paramDefaults = parser.getInputParameter("list path, default parameters");
        File fileDefaults;
        if (paramDefaults == null)
//wch: mascotdev
            fileDefaults = MS2PipelineManager.getDefaultInputFile(_uriRoot,"X!Tandem");
//END-wch: mascotdev
        else
            fileDefaults = new File(_uriRoot.resolve(paramDefaults));
        parser.setInputParameter("list path, default parameters",
                fileDefaults.getAbsolutePath());

        String paramDatabase = parser.getInputParameter("pipeline, database");
        if (paramDatabase == null)
        {
            error("Failed parsing X!Tandem input xml '" + _fileTandemXML.getPath() + "'.\n" +
                    "Missing required input parameter 'pipeline, database'");
            return;
        }
        String[] databases = paramDatabase.split(";");

        StringBuffer taxonomyBuffer = new StringBuffer();
        taxonomyBuffer.append("<?xml version=\"1.0\"?>\n");
        taxonomyBuffer.append("<bioml label=\"x! taxon-to-file matching list\">\n");
        taxonomyBuffer.append("  <taxon label=\"sequences\">\n");
        for (String database : databases)
        {
            File fileDatabase = MS2PipelineManager.getSequenceDBFile(_uriSequenceRoot, database);
            taxonomyBuffer.append("    <file format=\"peptide\" URL=\"");
            taxonomyBuffer.append(fileDatabase.getAbsolutePath());
            taxonomyBuffer.append("\"/>\n");
        }
        taxonomyBuffer.append("  </taxon>\n");
        taxonomyBuffer.append("</bioml>\n");
        String taxonomyText = taxonomyBuffer.toString();

        BufferedWriter taxonomyWriter = null;
        File fileTaxonomy = new File(dirWork, "taxonomy.xml");
        try
        {
            taxonomyWriter = new BufferedWriter(new FileWriter(fileTaxonomy));
            String[] lines = taxonomyText.split("\n");
            for (String line : lines)
            {
                taxonomyWriter.write(line);
                taxonomyWriter.newLine();
            }
        }
        catch (IOException eio)
        {
            error("Failed to write X!Tandem taxonomy file '" + fileTaxonomy + "'.");
            return;
        }
        finally
        {
            if (taxonomyWriter != null)
            {
                try
                {
                    taxonomyWriter.close();
                }
                catch (IOException eio)
                {
                    error("Failed to write X!Tandem taxonomy file '" + fileTaxonomy + "'.");
                    return;
                }
            }
        }
        parser.setInputParameter("list path, taxonomy information", "taxonomy.xml");
        parser.setInputParameter("protein, taxon", "sequences");
        parser.setInputParameter("spectrum, path", fileMzXML.getAbsolutePath());
        parser.setInputParameter("output, path", "output.xml");
        parser.removeInputParameter("pipeline, database");
        parser.removeInputParameter("pipeline, email address");

        String xml = parser.getXML();

        BufferedWriter inputWriter = null;
        File fileInputXML = new File(dirWork, "input.xml");
        try
        {
            inputWriter = new BufferedWriter(new FileWriter(fileInputXML));
            inputWriter.write(xml);
        }
        catch (IOException eio)
        {
            error("Failed to write X!Tandem input file '" + fileInputXML + "'.");
            return;
        }
        finally
        {
            if (inputWriter != null)
            {
                try
                {
                    inputWriter.close();
                }
                catch (IOException eio)
                {
                    error("Failed to write X!Tandem input file '" + fileInputXML + "'.");
                    return;
                }
            }
        }

        File fileOutputXML = MS2PipelineManager.getTandemOutFile(dirWork, _baseName);
        File filePepXMLRaw = MS2PipelineManager.getPepXMLFile(dirWork, _baseName + "-raw");
        File filePepXML = MS2PipelineManager.getPepXMLFile(dirWork, _baseName);
        File fileProtXML = null;
        String paramQuant = null;
        //handle the case that user may be generating TPP version of file
        File filePepXSL = MS2PipelineManager.getPepXSLIntermediatFile(dirWork, _baseName);
        File filePepSHTML = MS2PipelineManager.getPepSHTMLIntermediatFile(dirWork, _baseName);
        File fileProtXSL = MS2PipelineManager.getProtXSLIntermediatFile(dirWork, _baseName);
        File fileProtSHTML = MS2PipelineManager.getProtSHTMLIntermediatFile(dirWork, _baseName);
//END-wch: non-XML only TPP handling
        int iReturn = 0;
        try
        {
            header("tandem.exe output");

            iReturn = runSubProcess(new ProcessBuilder("tandem.exe",
                    "input.xml"),
                    dirWork);

            File[] outputFiles = dirWork.listFiles(new FileFilter()
            {
                public boolean accept(File f)
                {
                    if (f.isDirectory())
                        return false;
                    String name = f.getName();
                    return name.startsWith("output") && name.endsWith(".xml");
                }
            });

            if (iReturn != 0 || outputFiles.length != 1)
            {
                error("Failed running X!Tandem.");
                return;
            }

            outputFiles[0].renameTo(fileOutputXML);

            setStatus("ANALYZING");
            header("Tandem2XML output");

            // CONSIDER: Try Tandem2XML.exe for back-compat on Linux?
            iReturn = runSubProcess(new ProcessBuilder("Tandem2XML",
                    fileOutputXML.getPath(),
                    filePepXMLRaw.getPath()),
                    dirWork);

            if (iReturn != 0 || !filePepXMLRaw.exists())
            {
                error("Failed running Tandem2XML.");
                return;
            }

            List<String> interactCmd = new ArrayList<String>();
            interactCmd.add("xinteract");

            // Currently prophet analysis is only supported for the 'comet' scoring
            // algorithm.
            String paramScore = parser.getInputParameter("scoring, algorithm");
            if (paramScore == null || paramScore.length() == 0)
                paramScore = "native";

            if (!"native".equals(paramScore) &&
                    !"comet".equals(paramScore) &&
                    !"k-score".equals(paramScore))
            {
                interactCmd.add("-nP"); // no Prophet analysis
            }
            // Currently ProteinProphet relies on perl, so make sure the system has
            // a valid perl version.
            else if (hasValidPerl())
            {
                fileProtXML = MS2PipelineManager.getProtXMLIntermediatFile(dirWork, _baseName);
                interactCmd.add("-Opt");
                interactCmd.add("-nR");
            }
            else
            {
                warn("No valid Perl version found on this machine.  " +
                    "Skipping ProteinProphet analysis.  " +
                    "Ask your system administrator to check the system path.");
            }

            String paramMinProb = parser.getInputParameter("pipeline prophet, min probability");
            if (paramMinProb != null && paramMinProb.length() > 0)
                interactCmd.add("-p" + paramMinProb);

            String quantParam = getQuantitationCmd(parser, fileMzXML.getParentFile());
            if (quantParam != null)
                interactCmd.add(quantParam);          

            interactCmd.add("\"-N" + filePepXML.getName() + "\"");
            interactCmd.add("\"" + filePepXMLRaw.getPath() + "\"");

            header("xinteract output");

            iReturn = runSubProcess(new ProcessBuilder(interactCmd),
                    dirWork);

            if (iReturn != 0 || !filePepXML.exists())
            {
                error("Failed running xinteract.");
                return;
            }
        }
        catch (RunProcessException erp)
        {
            // Handled in runSubProcess
            return;
        }
        catch (InterruptedException ei)
        {
            // Handled in runSubProcess
            return;
        }

        File filePepXMLFinal = _filePepXML;
        File fileOutputXMLFinal = new File(_dirAnalysis, fileOutputXML.getName());

        File fileExperimentXML;
        try
        {
            boolean isXpress = parser.getInputParameter("pipeline quantitation, residue label mass") != null;
            fileExperimentXML = writeExperimentXML(databases,
                                                    dirWork,
                                                    _dirAnalysis,
                                                    filePepXMLFinal,
                                                    isXComet,
                                                    isXpress);
        }
        catch (IOException e)
        {
            error("Failed writing XAR file");
            return;
        }
        catch (SQLException e)
        {
            error("Failed writing XAR file");
            return;
        }

        File fileExperimentXMLFinal = new File(_dirAnalysis, fileExperimentXML.getName());

        if (!filePepXML.renameTo(filePepXMLFinal))
        {
            error("Failed to move " + filePepXML.getAbsolutePath() + " to " + filePepXMLFinal.getAbsolutePath());
            return;
        }
        else if (!fileOutputXML.renameTo(fileOutputXMLFinal))
        {
            error("Failed to move " + fileOutputXML.getAbsolutePath() + " to " + fileOutputXMLFinal.getAbsolutePath());
            return;
        }
        else if (fileProtXML != null &&
                !fileProtXML.renameTo(MS2PipelineManager.getProtXMLFile(_dirAnalysis, _baseName)))
        {
            error("Failed to move " + fileProtXML.getAbsolutePath() + " to " + MS2PipelineManager.getProtXMLFile(_dirAnalysis, _baseName).getAbsolutePath());
            return;
        }
        else if (!fileExperimentXML.renameTo(new File(_dirAnalysis, fileExperimentXML.getName())))
        {
            error("Failed to move " + fileExperimentXML.getAbsolutePath() + " to " + (new File(_dirAnalysis, fileExperimentXML.getName())).getAbsolutePath());
            return;
        }
        else if (!filePepXMLRaw.delete())
        {
            error("Failed to delete " + filePepXMLRaw.getAbsolutePath());
            return;
        }
        else if (!fileInputXML.delete())
        {
            error("Failed to delete " + fileInputXML.getAbsolutePath());
            return;
        }
        else if (!fileTaxonomy.delete())
        {
            error("Failed to delete " + fileTaxonomy.getAbsolutePath());
            return;
        }
//wch: non-XML only TPP handling
        else if (filePepXSL.exists() && !filePepXSL.delete())
        {
            error("Failed to delete " + filePepXSL.getAbsolutePath());
            return;
        }
        else if (filePepSHTML.exists() && !filePepSHTML.delete())
        {
            error("Failed to delete " + filePepSHTML.getAbsolutePath());
            return;
        }
        else if (fileProtXSL.exists() && !fileProtXSL.delete())
        {
            error("Failed to delete " + fileProtXSL.getAbsolutePath());
            return;
        }
        else if (fileProtSHTML.exists() && !fileProtSHTML.delete())
        {
            error("Failed to delete " + fileProtSHTML.getAbsolutePath());
            return;
        }
//END-wch: non-XML only TPP handling
        else if (!dirWork.delete())
        {
            error("Failed to delete " + dirWork.getAbsolutePath());
            return;
        }

        setStatus("LOADING");
        FileXarSource source = new FileXarSource(fileExperimentXMLFinal);
        String status = ExperimentPipelineJob.loadExperiment(this, source, false);
        _experimentRowId = source.getExperimentRowId();

        setStatus(status);
    }

    private File writeExperimentXML(String[] databases,
                                    File workDir,
                                    File analysisDir,
                                    File pepXMLFile,
                                    boolean isXComet,
                                    boolean isXpress) throws IOException, SQLException
    {
        StringBuilder templateResource = new StringBuilder("org/labkey/ms2/templates/MS2Search");
        if (isFractions())
        {
            templateResource.append("Fractions");
        }
        if (isXComet)
        {
            templateResource.append("XComet");
        }
        if (isXpress)
        {
            templateResource.append("Xpress");
        }
        templateResource.append(".xml");

        InputStream in = getClass().getClassLoader().getResourceAsStream(templateResource.toString());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
                sb.append("\n");
            }
        }
        finally
        {
            reader.close();
        }

        StringBuilder databaseSB = new StringBuilder();
        File[] databaseFiles = new File[databases.length];
        for (int i = 0; i < databases.length; i++)
        {
            String database = databases[i];
            databaseFiles[i] = MS2PipelineManager.getSequenceDBFile(_uriSequenceRoot, database);
            databaseSB.append(getStartingInputDataSnippet(databaseFiles[i], analysisDir));
        }

        StringBuilder mzxmlStartingInputsSB = new StringBuilder();
        StringBuilder instanceDetailsSB = new StringBuilder();

        for (File f : _filesMzXML)
        {
            mzxmlStartingInputsSB.append(getStartingInputDataSnippet(f, analysisDir));
            instanceDetailsSB.append(getInstanceDetailsSnippet(f, analysisDir, databaseFiles, _fileTandemXML));
        }

        String mzXMLPaths = getMzXMLPaths(analysisDir);

        Container c = _info.getContainer();
        PipelineService service = PipelineService.get();
        PipelineService.PipeRoot pr = service.findPipelineRoot(c);
        if (pr == null)
            throw new FileNotFoundException("Failed to find a pipeline root for " + c.getPath());
        File containerRoot = new File(pr.getUri(c));

        String searchName = getDescription();

        replaceString(sb, "SEARCH_NAME", searchName);
        replaceString(sb, "PROTEIN_DATABASE_DATALSIDS", getDataLSIDSnippet(databaseFiles, analysisDir, "FASTA"));
        replaceString(sb, "MZXML_DATALSIDS", getDataLSIDSnippet(_filesMzXML, analysisDir, "mzXML"));
        replaceString(sb, "MZXML_STARTING_INPUTS", mzxmlStartingInputsSB.toString());
        replaceString(sb, "MZXML_PATHS", mzXMLPaths);
        replaceString(sb, "TANDEM_XML_FILE_PATH",
                PathRelativizer.relativizePathUnix(analysisDir, _fileTandemXML));
        replaceString(sb, "PROTEIN_DATABASES", databaseSB.toString());
        if (!isFractions())
        {
            File xtanXMLFile = MS2PipelineManager.getTandemOutFile(analysisDir, _baseName);
            replaceString(sb, "XTAN_XML_FILE_PATH",
                    PathRelativizer.relativizePathUnix(analysisDir, xtanXMLFile));
        }
        replaceString(sb, "INSTANCE_DETAILS", instanceDetailsSB.toString());

        String pepXMLFilePath = PathRelativizer.relativizePathUnix(analysisDir, pepXMLFile);
        replaceString(sb, "PEP_XML_FILE_PATH",
                pepXMLFilePath);

        File protXMLFile = MS2PipelineManager.getProtXMLFile(analysisDir, _baseName);
        if (!NetworkDrive.exists(protXMLFile))
        {
            File protXMLIntFile = MS2PipelineManager.getProtXMLIntermediatFile(analysisDir, _baseName);
            if (NetworkDrive.exists(protXMLIntFile))
                protXMLFile = protXMLIntFile;
        }
        replaceString(sb, "PEP_PROT_XML_FILE_PATH",
                PathRelativizer.relativizePathUnix(analysisDir, protXMLFile));

        File uniquifierFile = _fileTandemXML.getParentFile();
        if (!isFractions())
            uniquifierFile = new File(uniquifierFile, _baseName);
        String uniquifier = PathRelativizer.relativizePathUnix(containerRoot, uniquifierFile);

        replaceString(sb, "RUN-UNIQUIFIER", uniquifier);

        File experimentXMLFile = MS2PipelineManager.getSearchExperimentFile(workDir, _baseName);

        FileOutputStream fOut = new FileOutputStream(experimentXMLFile);
        PrintWriter writer = new PrintWriter(fOut);
        try
        {
            writer.write(sb.toString());
        }
        finally
        {
            writer.close();
        }

        return experimentXMLFile;
    }
}