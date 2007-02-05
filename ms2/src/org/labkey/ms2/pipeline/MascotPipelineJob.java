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

import org.apache.commons.lang.StringUtils;
import org.labkey.api.exp.ExperimentPipelineJob;
import org.labkey.api.exp.FileXarSource;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.PathRelativizer;
import org.labkey.api.util.XMLValidationParser;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.AppProps;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.data.Container;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

/**
 * MascotPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class MascotPipelineJob extends AbstractMS2SearchPipelineJob
{
    private File _fileMascotXML;

    public static String getProviderName(boolean cluster)
    {
        if (cluster)
            return MascotCPipelineProvider.name;
        else
            return MascotLocalPipelineProvider.name;
    }

    public MascotPipelineJob(ViewBackgroundInfo info,
                             String name,
                             URI uriRoot,
                             URI uriSequenceRoot,
                             File filesMzXML[],
                             File fileInputXML,
                             boolean cluster,
                             boolean append)
    {
        super(getProviderName(cluster), info, filesMzXML, name, uriRoot, uriSequenceRoot);

        _fileMascotXML = fileInputXML;
        if (isFractions())
            _baseName = "all";
        else
            _baseName = MS2PipelineManager.getBasename(filesMzXML[0]);
        _dirAnalysis = _fileMascotXML.getParentFile();
        _filePepXML = MS2PipelineManager.getPepXMLFile(_dirAnalysis, _baseName);
        setLogFile(MS2PipelineManager.getLogFile(_dirAnalysis, _baseName), append);
        if (cluster)
            setStatusFile(MS2PipelineManager.getStatusFile(_dirAnalysis, _baseName));

        if (isFractions())
            header("Mascot search for " + _dirMzXML.getName());
        else
            header("Mascot search for " + filesMzXML[0].getName());
    }

    public MascotInputParser getInputParameters()
    {
        return getInputParameters(_fileMascotXML);
    }

    private MascotInputParser getInputParameters(File parametersFile)
    {
        BufferedReader inputReader = null;
        StringBuffer xmlBuffer = new StringBuffer();
        try
        {
            inputReader = new BufferedReader(new FileReader(parametersFile));
            String line;
            while ((line = inputReader.readLine()) != null)
                xmlBuffer.append(line).append("\n");
        }
        catch (IOException eio)
        {
            error("Failed to read Mascot input xml '" + parametersFile.getPath() + "'.");
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

        MascotInputParser parser = new MascotInputParser();
        parser.parse(xmlBuffer.toString());
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
            {
                error("Failed parsing Mascot input xml '" + parametersFile.getPath() + "'.\n" +
                        err.getMessage());
            }
            else
            {
                error("Failed parsing Mascot input xml '" + parametersFile.getPath() + "'.\n" +
                        "Line " + err.getLine() + ": " + err.getMessage());
            }
            return null;
        }
        return parser;
    }

    public void upload()
    {
        setStatus("LOADING");

        MascotInputParser parser = getInputParameters();
        if (parser == null)
            return;

        String paramDatabase = parser.getInputParameter("pipeline, database");
        if (paramDatabase == null)
        {
            error("Failed parsing Mascot input xml '" + _fileMascotXML.getPath() + "'.\n" +
                    "Missing required input parameter 'pipeline, database'");
            return;
        }
        String[] databases = paramDatabase.split(";");

        boolean isXpress = parser.getInputParameter("pipeline quantitation, residue label mass") != null;

        File fileExperimentXML;
        try
        {
            fileExperimentXML = writeExperimentXML(databases,
                                                    _dirAnalysis,
                                                    _dirAnalysis,
                                                    _filePepXML,
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
        String status = ExperimentPipelineJob.loadExperiment(this, source);
        _experimentRowId = source.getExperimentRowId();

        setStatus(status);
    }

    public void search()
    {
        setStatus("SEARCHING");

        /*
        0. pre-Mascot search: a) create working folder
        */
        if (isFractions())
        {
            error("Fractions not supported by mini-pipeline.");
            return;
        }
        File fileMzXML = _filesMzXML[0];
        File dirWork = new File(_dirAnalysis, _baseName + ".work");
        dirWork.mkdir();

        /*
        0. pre-Mascot search: b) write a copy of the effective configuration file "mascot.xml"
        */
        MascotInputParser parser = getInputParameters();
        if (parser == null)
            return;

        String paramDefaults = parser.getInputParameter("list path, default parameters");
        File fileDefaults;
        if (paramDefaults == null)
            fileDefaults = MS2PipelineManager.getDefaultInputFile(_uriRoot, "mascot");
        else
            fileDefaults = new File(_uriRoot.resolve(paramDefaults));
        parser.setInputParameter("list path, default parameters",
                fileDefaults.getAbsolutePath());

        String paramDatabase = parser.getInputParameter("pipeline, database");
        if (paramDatabase == null)
        {
            error("Failed parsing Mascot input xml '" + _fileMascotXML.getPath() + "'.\n" +
                    "Missing required input parameter 'pipeline, database'");
            return;
        }
        String[] databases = paramDatabase.split(";");

        MascotInputParser defaultParser = getInputParameters(fileDefaults);
        if (defaultParser == null)
        {
            error("Failed parsing default Mascot input xml '" + fileDefaults.getPath() + "'.");
            return;
        }

        String[] parameterNames = parser.getInputParameterNames ();
        for (String parameterName : parameterNames) {
            defaultParser.setInputParameter(parameterName, parser.getInputParameter(parameterName));
        }
        //defaultParser.setInputParameter("search, db", parser.getInputParameter("pipeline, database"));
        //defaultParser.setInputParameter("search, useremail", parser.getInputParameter("pipeline, email address"));
        defaultParser.setInputParameter("pipeline, user name", "LabKey User");

        String xml = defaultParser.getXML();

        BufferedWriter inputWriter = null;
        File fileInputXML = new File(dirWork, "input.xml");
        try
        {
            inputWriter = new BufferedWriter(new FileWriter(fileInputXML));
            inputWriter.write(xml);
        }
        catch (IOException eio)
        {
            error("Failed to write Mascot input file '" + fileInputXML + "'.");
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
                    error("Failed to write Mascot input file '" + fileInputXML + "'.");
                    return;
                }
            }
        }

        File fileInputMzXML = new File(dirWork, fileMzXML.getName());
        File fileOutputMgf = MS2PipelineManager.getMgfFile(dirWork, _baseName);
        File fileMgfFinal = MS2PipelineManager.getMgfFile(_dirAnalysis, _baseName);
        File fileOutputDat = MS2PipelineManager.getMascotOutFile(dirWork, _baseName);
        File fileDatFinal = MS2PipelineManager.getMascotOutFile(_dirAnalysis, _baseName);
        File filePepXMLRaw = new File(dirWork, _baseName + ".xml");
        File fileTgzRaw = new File(dirWork, _baseName + ".tgz");
        File filePepXMLBeforeProphet = new File(dirWork, _baseName + "-raw.xml");
        File filePepXML = MS2PipelineManager.getPepXMLFile(dirWork, _baseName);
        File fileTgz = new File(dirWork, _baseName + ".pep.tgz");
        File fileTgzFinal = new File(dirWork.getParent(), _baseName + ".pep.tgz");
        File fileProtXML = null;
        //handle the case that user may be generating TPP version of file
        File filePepXSL = MS2PipelineManager.getPepXSLIntermediatFile(dirWork, _baseName);
        File filePepSHTML = MS2PipelineManager.getPepSHTMLIntermediatFile(dirWork, _baseName);
        File fileProtXSL = MS2PipelineManager.getProtXSLIntermediatFile(dirWork, _baseName);
        File fileProtSHTML = MS2PipelineManager.getProtSHTMLIntermediatFile(dirWork, _baseName);
        //END
        int iReturn = 0;

        try
        {
            /*
            0. pre-Mascot search: c) translate the mzXML file to mgf for Mascot (msxml2other)
            */
            header("MzXML2Search output");

            MS2PipelineManager.copyFile (fileMzXML, fileInputMzXML);
            iReturn = runSubProcess(new ProcessBuilder("MzXML2Search",
                    "-mgf", fileInputMzXML.getName ()),
                    dirWork);
            if (iReturn != 0 || !fileOutputMgf.exists())
            {
                error("Failed to translate "+fileInputMzXML.getAbsolutePath()+" to "+fileOutputMgf.getAbsolutePath());
                return;
            }

            /*
            1. perform Mascot search
            */
            header("mascot client output");

            AppProps appProps = AppProps.getInstance();
            MascotClientImpl mascotClient = new MascotClientImpl(appProps.getMascotServer(), getLogger(),
                appProps.getMascotUserAccount(),appProps.getMascotUserPassword());
            mascotClient.setProxyURL(appProps.getMascotHTTPProxy());
            iReturn = mascotClient.search(fileInputXML.getAbsolutePath(),
                    fileOutputMgf.getAbsolutePath(), fileOutputDat.getAbsolutePath());
            //TODO: support fraction in future
            if (iReturn != 0 || !fileOutputDat.exists())
            {
                error("Failed running Mascot.");
                return;
            }

            /*
            5. translate Mascot result file to pep.xml format
            */
            setStatus("ANALYZING");
            header("Mascot2XML output");

            String sequenceDB = getSequenceDatabase(fileOutputDat);
            File fileSequenceDatabase = new File(_uriSequenceRoot.getPath(), sequenceDB);

            iReturn = runSubProcess(new ProcessBuilder("Mascot2XML",
                    fileOutputDat.getName(),
                    "-D" + fileSequenceDatabase.getAbsolutePath(),
                    "-xml"
                    ,"-notgz"
                    ,"-desc"
                    ,"-shortid"
                    ),
                    dirWork);
            if (iReturn != 0 || !filePepXMLRaw.exists())
            {
                error("Failed running Mascot2XML.");
                return;
            }
            if (fileTgzRaw.exists() && !fileTgzRaw.renameTo(fileTgz))
            {
                error("Failed rename "+fileTgzRaw.getAbsolutePath()+" to "+fileTgz.getAbsolutePath());
                return;
            }
            if (!filePepXMLRaw.renameTo(filePepXMLBeforeProphet))
            {
                error("Failed rename "+filePepXMLRaw.getAbsolutePath()+" to "+filePepXMLBeforeProphet.getAbsolutePath());
                return;
            }

            /*
            6. run peptide and protein prophets via xinteract
            */
            List<String> interactCmd = new ArrayList<String>();
            interactCmd.add("xinteract");
            interactCmd.add("-p0"); // unfiltered results.
            interactCmd.add("-nR"); // do not run Database/RefreshParser

            // Currently ProteinProphet relies on perl, so make sure the system has
            // a valid perl version.
            if (hasValidPerl())
            {
                fileProtXML = MS2PipelineManager.getProtXMLIntermediatFile(dirWork, _baseName);
                interactCmd.add("-Opt");
            }
            else
            {
                warn("No valid Perl version found on this machine.  " +
                    "Skipping ProteinProphet analysis.  " +
                    "Ask your system administrator to check the system path.");
            }

            String quantParam = getQuantitationCmd(parser, fileMzXML.getParentFile());
            if (quantParam != null)
                interactCmd.add(quantParam);

            interactCmd.add("-N" + filePepXML.getName());
            interactCmd.add(filePepXMLBeforeProphet.getPath());

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
        } catch (IOException e) {
            //TOOD: wch
            error("IOException", e);
            return;
        }

        File filePepXMLFinal = _filePepXML;
        File fileExperimentXML;
        try
        {
            boolean isXpress = parser.getInputParameter("pipeline quantitation, residue label mass") != null;
            fileExperimentXML = writeExperimentXML(databases,
                                                    dirWork,
                                                    _dirAnalysis,
                                                    filePepXMLFinal,
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
        else if (fileTgz.exists() && !fileTgz.renameTo(fileTgzFinal))
        {
            error("Failed to move " + fileTgz.getAbsolutePath() + " to " + fileTgzFinal.getAbsolutePath());
            return;
        }
        else if (!fileOutputMgf.renameTo(fileMgfFinal))
        {
            error("Failed to move " + fileOutputMgf.getAbsolutePath() + " to " + fileMgfFinal.getAbsolutePath());
            return;
        }
        else if (!fileOutputDat.renameTo(fileDatFinal))
        {
            error("Failed to move " + fileOutputDat.getAbsolutePath() + " to " + fileDatFinal.getAbsolutePath());
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
        else if (!filePepXMLBeforeProphet.delete())
        {
            error("Failed to delete " + filePepXMLBeforeProphet.getAbsolutePath());
            return;
        }
        else if (!fileInputXML.delete())
        {
            error("Failed to delete " + fileInputXML.getAbsolutePath());
            return;
        }
        else if (!fileInputMzXML.delete())
        {
            error("Failed to delete " + fileInputMzXML.getAbsolutePath());
            return;
        }
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
        else if (!dirWork.delete())
        {
            error("Failed to delete " + dirWork.getAbsolutePath());
            return;
        }

        setStatus("LOADING");
        FileXarSource source = new FileXarSource(fileExperimentXMLFinal);
        String status = ExperimentPipelineJob.loadExperiment(this, source);
        _experimentRowId = source.getExperimentRowId();

        setStatus(status);
    }

    private File writeExperimentXML(String[] databases,
                                    File workDir,
                                    File analysisDir,
                                    File pepXMLFile,
                                    boolean isXpress) throws IOException, SQLException
    {
        StringBuilder templateResource = new StringBuilder("org/labkey/ms2/templates/MS2SearchMascot");
        if (isFractions())
        {
            templateResource.append("Fractions");
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
            instanceDetailsSB.append(getInstanceDetailsSnippet(f, analysisDir, databaseFiles, _fileMascotXML));
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
        replaceString(sb, "MASCOT_XML_FILE_PATH",
                PathRelativizer.relativizePathUnix(analysisDir, _fileMascotXML));
        replaceString(sb, "PROTEIN_DATABASES", databaseSB.toString());
        if (!isFractions())
        {
            File mascotDatFile = MS2PipelineManager.getMascotOutFile(_dirAnalysis, _baseName);
            replaceString(sb, "MASCOT_DAT_FILE_PATH",
                    PathRelativizer.relativizePathUnix(analysisDir, mascotDatFile));
        }
        replaceString(sb, "INSTANCE_DETAILS", instanceDetailsSB.toString());

        File mgfFile = MS2PipelineManager.getMgfFile(_dirAnalysis, _baseName);
        replaceString(sb, "MGF_FILE_PATH",
                PathRelativizer.relativizePathUnix(analysisDir, mgfFile));

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

        File uniquifierFile = _fileMascotXML.getParentFile();
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

    private String getSequenceDatabase (File datFile) throws IOException
    {
        // return the sequence database queried against in this search
        final File dat = new File(datFile.getAbsolutePath());

        if (!NetworkDrive.exists(dat))
            throw new FileNotFoundException(datFile.getAbsolutePath() + " not found");

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
}