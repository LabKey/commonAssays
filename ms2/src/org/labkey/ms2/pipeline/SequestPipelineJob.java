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
import org.labkey.api.util.*;

import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.data.Container;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

/**
 * SequestPipelineJob class
 * <p/>
 * Created: Oct 4, 2005
 *
 * @author bmaclean
 */
public class SequestPipelineJob extends AbstractMS2SearchPipelineJob
{
    private static final String SEQUEST = MS2PipelineProvider.SEQUEST;

    private File _fileSequestXML;

    public SequestPipelineJob(ViewBackgroundInfo info,
                              String name,
                              URI uriRoot,
                              URI uriSequenceRoot,
                              File filesMzXML[],
                              File fileInputXML,
                              boolean cluster,
                              boolean append)
    {
        super(SequestLocalPipelineProvider.name, info, filesMzXML, name, uriRoot, uriSequenceRoot);

        _fileSequestXML = fileInputXML;
        if (isFractions())
            _baseName = "all";
        else
            _baseName = MS2PipelineManager.getBaseName(filesMzXML[0]);
        _dirAnalysis = _fileSequestXML.getParentFile();
        _filePepXML = MS2PipelineManager.getPepXMLFile(_dirAnalysis, _baseName);
        setLogFile(MS2PipelineManager.getLogFile(_dirAnalysis, _baseName), append);
        if (cluster)
            setStatusFile(MS2PipelineManager.getStatusFile(_dirAnalysis, _baseName));

        if (isFractions())
            header("Sequest search for " + _dirMzXML.getName());
        else
            header("Sequest search for " + filesMzXML[0].getName());
    }

    public SequestInputParser getInputParameters()
    {
        return getInputParameters(_fileSequestXML);
    }

    private SequestInputParser getInputParameters(File parametersFile)
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
            error("Failed to read Sequest input xml '" + parametersFile.getPath() + "'.");
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
                    error("Failed to close Sequest input xml '" + parametersFile.getPath() + "'.");
                }
            }
        }

        SequestInputParser parser = new SequestInputParser();
        parser.parse(xmlBuffer.toString());
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
            {
                error("Failed parsing Sequest input xml '" + parametersFile.getPath() + "'.\n" +
                    err.getMessage());
            }
            else
            {
                error("Failed parsing Sequest input xml '" + parametersFile.getPath() + "'.\n" +
                    "Line " + err.getLine() + ": " + err.getMessage());
            }
            return null;
        }
        return parser;
    }

    public void upload()
    {
        setStatus("LOADING");

        SequestInputParser parser = getInputParameters();
        if (parser == null)
            return;

        String paramDatabase = parser.getInputParameter("pipeline, database");
        if (paramDatabase == null)
        {
            error("Failed parsing Sequest input xml '" + _fileSequestXML.getPath() + "'.\n" +
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
        String status = ExperimentPipelineJob.loadExperiment(this, source, false);
        _experimentRowId = source.getExperimentRowId();

        setStatus(status);
    }

    public void search()
    {
        /*
        0. pre-Sequest search: a) create working folder
        */
        //TODO
        if (isFractions())
        {
            error("Fractions not supported by mini-pipeline.");
            return;
        }
        File dirWork = new File(_dirAnalysis, _baseName + ".work");
        dirWork.mkdir();

        File fileSequestParams = new File(dirWork.getParentFile(), "sequest.params");
        File fileSequestRawXml = MS2PipelineManager.getSequestRawXmlFile(dirWork, _baseName);
        File filePepXML = MS2PipelineManager.getPepXMLFile(dirWork, _baseName);
        File fileProtXML = MS2PipelineManager.getProtXMLIntermediatFile(dirWork, _baseName);
        File filePepXSL = MS2PipelineManager.getPepXSLIntermediatFile(dirWork, _baseName);
        File filePepSHTML = MS2PipelineManager.getPepSHTMLIntermediatFile(dirWork, _baseName);
        File fileProtXSL = MS2PipelineManager.getProtXSLIntermediatFile(dirWork, _baseName);
        File fileProtSHTML = MS2PipelineManager.getProtSHTMLIntermediatFile(dirWork, _baseName);
        File fileProtXmlFinal = MS2PipelineManager.getProtXMLFile(_dirAnalysis, _baseName);
        File fileInputXML = new File(dirWork, "input.xml");

        ArrayList<File> tgzArchives = new ArrayList<File>();
        ArrayList<File> pepProphetInputFiles = new ArrayList<File>();
        String[] databases = null;
        SequestInputParser parser = null;
        int iReturn;

        for (File fileMzXML : _filesMzXML)
        {
            setStatus("SEARCHING");
            _baseName = MS2PipelineManager.getBaseName(fileMzXML);
            File fileInputMzXML = new File(dirWork, fileMzXML.getName());
            File dirSequestWork = new File(dirWork, _baseName);
            File workingSequestParams = new File(dirSequestWork, "sequest.params");
            File fileOutputDta = MS2PipelineManager.getDtaFile(dirWork, _baseName);
            File fileTgz = new File(dirWork, _baseName + ".pep.tgz");
            File filePepXMLRaw = new File(dirWork, _baseName + ".xml");

            /*
            0. pre-Sequest search: b) write a copy of the effective configuration file "sequest.xml"
            */
            parser = getInputParameters();
            if (parser == null)
                return;

            String paramDefaults = parser.getInputParameter("list path, default parameters");
            File fileDefaults;
            if (paramDefaults == null)
                fileDefaults = MS2PipelineManager.getDefaultInputFile(_uriRoot, SEQUEST);
            else
                fileDefaults = new File(_uriRoot.resolve(paramDefaults));
            parser.setInputParameter("list path, default parameters",
                fileDefaults.getAbsolutePath());

            String paramDatabase = parser.getInputParameter("pipeline, database");
            if (paramDatabase == null)
            {
                error("Failed parsing Sequest input xml '" + _fileSequestXML.getPath() + "'.\n" +
                    "Missing required input parameter 'pipeline, database'");
                return;
            }
            databases = paramDatabase.split(";");

            SequestInputParser defaultParser = getInputParameters(fileDefaults);
            if (defaultParser == null)
            {
                error("Failed parsing default Sequest input xml '" + fileDefaults.getPath() + "'.");
                return;
            }
            SequestParamsBuilder sequestParamsBuilder = new SequestParamsBuilder(defaultParser, _uriSequenceRoot);
            String[] parameterNames = parser.getInputParameterNames();
            for (String parameterName : parameterNames)
            {
                defaultParser.setInputParameter(parameterName, parser.getInputParameter(parameterName));
            }
            defaultParser.setInputParameter("list path, sequest parameters", "sequest.params");
            defaultParser.setInputParameter("search, useremail", parser.getInputParameter("pipeline, email address"));
            defaultParser.setInputParameter("search, username", "LabKey User");

            String xml = defaultParser.getXML();

            BufferedWriter inputWriter = null;

            try
            {
                inputWriter = new BufferedWriter(new FileWriter(fileInputXML));
                inputWriter.write(xml);
            }
            catch (IOException eio)
            {
                error("Failed to write Sequest input file '" + fileInputXML + "'.");
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
                        error("Failed to close Sequest input file '" + fileInputXML + "'.");
                    }
                }
            }


            String parseError = sequestParamsBuilder.initXmlValues();
            if (!parseError.equals(""))
            {
                error(parseError);
                return;
            }
            //END

            try
            {
                /*
                0. pre-Sequest search: c) translate the mzXML file to dta for Sequest (msxml2other)
                */
                header("MzXML2Search output");
                Mzxml2SearchParams m2s = new Mzxml2SearchParams();
                Collection<Param> params = m2s.getParams();
                ArrayList<String> command = new ArrayList<String>();
                command.add("MzXML2Search");
                command.add("-dta");
                command.add("-M2");
                Collection<String> inputXmlParams = convertParams(params,defaultParser);
                if ( inputXmlParams == null) return;
                command.addAll(inputXmlParams);
                command.add(fileInputMzXML.getName());

                MS2PipelineManager.copyFile(fileMzXML, fileInputMzXML);

                iReturn = runSubProcess(new ProcessBuilder(command), dirWork);
                if (iReturn != 0 || !fileOutputDta.exists())
                {
                    error("Failed to translate " + fileInputMzXML.getAbsolutePath() + " to " + fileOutputDta.getAbsolutePath());
                    return;
                }

                /*
                1. perform Sequest search
                */

                String sequestParamsText = null;
                try
                {
                    sequestParamsText = sequestParamsBuilder.getSequestParamsText();
                }
                catch (SequestParamsException e)
                {
                    error(e.getMessage());
                }

                inputWriter = null;
                try
                {
                    inputWriter = new BufferedWriter(new FileWriter(fileSequestParams));
                    inputWriter.write(sequestParamsText);
                }
                catch (IOException eio)
                {
                    error("Failed to write sequest.params file '" + fileSequestParams + "'.");
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
                            error("Failed to close sequest.params file '" + fileSequestParams + "'.");
                        }
                    }
                }

                MS2PipelineManager.copyFile(fileSequestParams, workingSequestParams);

                AppProps appProps = AppProps.getInstance();
                if("".equals(appProps.getSequestServer()))
                {
                    String sequestExecutable = findSequestExecutable();

                    int logLines = 10;
                    header("sequest output; truncated after first " + logLines + " lines.");


                    iReturn = runSubProcess(new ProcessBuilder(sequestExecutable,
                        "*.dta"),
                        dirSequestWork,logLines);

                    File[] outputFiles = dirSequestWork.listFiles(new FileFilter()
                    {
                        public boolean accept(File f)
                        {
                            if (f.isDirectory())
                                return false;
                            String name = f.getName();
                            return name.endsWith(".out");
                        }
                    });
                    
                    if (iReturn != 0 || outputFiles.length < 1)
                    {
                        error("Failed running Sequest.");
                        return;
                    }

                    header("Out2Xml output.");
                    String reportedPeptides = defaultParser.getInputParameter("sequest, num_output_lines");
                    if(reportedPeptides == null)
                    {
                        error("No value set for sequest.xml label: sequest, num_output_lines.");
                        return;
                    }
                    iReturn = runSubProcess(new ProcessBuilder("Out2Xml", dirSequestWork.getName(),"1"), dirWork);
                    if (iReturn != 0 || !filePepXMLRaw.exists())
                    {
                        error("Failed running Out2Xml.");
                        return;
                    }
                    if (!filePepXMLRaw.renameTo(fileSequestRawXml))
                    {
                        error("Failed rename " + filePepXMLRaw.getAbsolutePath() + " to " + fileSequestRawXml.getAbsolutePath());
                        return;
                    }
                }
                else
                {
                    SequestClient sequestClient = new SequestClient(appProps.getSequestServer(), getLogger());
                    iReturn = sequestClient.search(fileSequestParams.getAbsolutePath(),fileMzXML.getAbsolutePath(),fileSequestRawXml.getAbsolutePath(),inputXmlParams);
                }

                if (iReturn != 0 || !fileSequestRawXml.exists())
                {
                    error("Failed running Sequest.");
                    return;
                }

                /*
                5. translate Sequest result file to pep.xml format
                */
                setStatus("ANALYZING");
                info("creating tgz archive of dta and out.");
                try
                {
                    iReturn = runSubProcess(new ProcessBuilder("bsdtar.exe", "czf", fileTgz.getAbsolutePath(), "*"), dirSequestWork);
                }
                catch (RunProcessException e)
                {
                    error(e.getMessage());
                    return;
                }

                if (iReturn != 0 || !fileTgz.exists())
                {
                    error("Failed creating tgz archive of dta and out.");
                    return;
                }
                else
                {
                    tgzArchives.add(fileTgz);
                }

                pepProphetInputFiles.add(fileSequestRawXml);

                if (!fileInputMzXML.delete())
                {
                    error("Failed to delete " + fileInputMzXML.getAbsolutePath());
                    return;
                }
                if (!FileUtil.deleteDir(fileOutputDta))
                {
                    error("Failed to delete " + fileOutputDta.getAbsolutePath());
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
            catch (IOException e)
            {
                //TOOD: wch
                error("IOException", e);
                return;
            }
        }

        try
        {
            /*
            6. run peptide and protein prophets via xinteract
            */
            List<String> interactCmd = new ArrayList<String>();
            interactCmd.add("xinteract");
            interactCmd.add("-p0"); // unfiltered results.

            // Currently ProteinProphet relies on perl, so make sure the system has
            // a valid perl version.
            if (hasValidPerl())
            {
                interactCmd.add("-Opt");
            }

            // Add any necessary quantiation parameters.
            String paramQuant = parser.getInputParameter("pipeline quantitation, residue label mass");
            if (paramQuant != null)
            {
                List<String> xpressOpts = new ArrayList<String>();
                String[] quantSpecs = paramQuant.split(",");
                for (String spec : quantSpecs)
                {
                    String[] specVals = spec.split("@");
                    if (specVals.length != 2)
                        continue;
                    String mass = specVals[0].trim();
                    String aa = specVals[1].trim();
                    xpressOpts.add("-n" + aa + "," + mass);
                }

                paramQuant = parser.getInputParameter("pipeline quantitation, mass tolerance");
                if (paramQuant != null)
                    xpressOpts.add("-m");

                paramQuant = parser.getInputParameter("pipeline quantitation, fix");
                if (paramQuant != null)
                {
                    if ("heavy".equalsIgnoreCase(paramQuant))
                        xpressOpts.add("-H");
                    else if ("light".equalsIgnoreCase(paramQuant))
                        xpressOpts.add("-L");
                }

                paramQuant = parser.getInputParameter("pipeline quantitation, fix elution reference");
                if (paramQuant != null)
                {
                    String refFlag = "-f";
                    if ("peak".equalsIgnoreCase(paramQuant))
                        refFlag = "-F";
                    paramQuant = parser.getInputParameter("pipeline quantitation, fix elution difference");
                    if (paramQuant != null)
                        xpressOpts.add(refFlag + paramQuant);
                }

                paramQuant = parser.getInputParameter("pipeline quantitation, metabolic search type");
                if (paramQuant != null)
                {
                    if ("normal".equalsIgnoreCase(paramQuant))
                        xpressOpts.add("-M");
                    else if ("heavy".equalsIgnoreCase(paramQuant))
                        xpressOpts.add("-N");
                }

                interactCmd.add("-X\"" + StringUtils.join(xpressOpts.iterator(), ' ') + "\"");
            }

            interactCmd.add("-N" + filePepXML.getName());
            for (File summaryXml : pepProphetInputFiles)
            {
                interactCmd.add(summaryXml.getPath());
            }
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

        for (File pepXmlBeforeProphet : pepProphetInputFiles)
        {
            if (!pepXmlBeforeProphet.delete())
            {
                error("Failed to delete " + pepXmlBeforeProphet.getAbsolutePath());
                return;
            }
        }

        File fileTgzFinal = null;

        for (File tgzFile : tgzArchives)
        {
            fileTgzFinal = new File(dirWork.getParent(), tgzFile.getName());
            if (!tgzFile.renameTo(fileTgzFinal))
            {
                error("Failed to move " + tgzFile.getAbsolutePath() + " to " + fileTgzFinal.getAbsolutePath());
                return;
            }
        }
        if (!filePepXML.renameTo(filePepXMLFinal))
        {
            error("Failed to move " + filePepXML.getAbsolutePath() + " to " + filePepXMLFinal.getAbsolutePath());
            return;
        }
        else if (fileProtXML != null &&
            !fileProtXML.renameTo(fileProtXmlFinal))
        {
            error("Failed to move " + fileProtXML.getAbsolutePath() + " to " + MS2PipelineManager.getProtXMLFile(_dirAnalysis, _baseName).getAbsolutePath());
            return;
        }
        else if (!fileExperimentXML.renameTo(new File(_dirAnalysis, fileExperimentXML.getName())))
        {
            error("Failed to move " + fileExperimentXML.getAbsolutePath() + " to " + (new File(_dirAnalysis, fileExperimentXML.getName())).getAbsolutePath());
            return;
        }
        else if (!fileInputXML.delete())
        {
            error("Failed to delete " + fileInputXML.getAbsolutePath());
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
        String status = ExperimentPipelineJob.loadExperiment(this, source, false);
        _experimentRowId = source.getExperimentRowId();

        if (fileTgzFinal == null || !fileTgzFinal.delete())
        {
            error("Failed to delete .dat/.out tgz archive: " + fileTgzFinal.getAbsolutePath());
            return;
        }

        setStatus(status);
    }

    private File writeExperimentXML(String[] databases,
                                    File workDir,
                                    File analysisDir,
                                    File pepXMLFile,
                                    boolean isXpress) throws IOException, SQLException
    {
        StringBuilder templateResource = new StringBuilder("org/labkey/ms2/templates/MS2SearchSequest");
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
            instanceDetailsSB.append(getInstanceDetailsSnippet(f, analysisDir, databaseFiles, _fileSequestXML));
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
        replaceString(sb, "SEQ_XML_FILE_PATH",
            PathRelativizer.relativizePathUnix(analysisDir, _fileSequestXML));
        replaceString(sb, "PROTEIN_DATABASES", databaseSB.toString());
        if (!isFractions())
        {
            File sequestOutFile = MS2PipelineManager.getSequestOutFile(_dirAnalysis, _baseName + ".-raw.xml");
            replaceString(sb, "SEQUEST_XML_FILE_PATH",
                PathRelativizer.relativizePathUnix(analysisDir, sequestOutFile));
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

        File uniquifierFile = _fileSequestXML.getParentFile();
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

    private String findSequestExecutable()
    {
            String sequestExecutable;
            int sequest32;
            int sequest33;
            ProcessBuilder pb;
            Process p;
            pb = new ProcessBuilder("sequest27.exe","-h");
            try
            {
                p = pb.start();
                sequest32 = p.waitFor();
            }
            catch(Exception e)
            {
                sequest32 = 1;
            }
            pb = new ProcessBuilder("sequest.exe","-h");
            try
            {
                p = pb.start();
                sequest33 = p.waitFor();
            }
            catch(Exception e)
            {
                sequest33 = 1;
            }


            if(sequest33 == 0) sequestExecutable = "sequest.exe";
            else if(sequest32 == 0) sequestExecutable = "sequest27.exe";
            else
            {
                error("Can't find the Sequest executable on the path. Looking for sequest.exe or sequest27.exe.");
                return null;
            }
            return sequestExecutable;
    }
    Collection<String> convertParams(Collection<Param> params,BioMLInputParser defaultParser)
    {
        ArrayList<String> command = new ArrayList<String>();
        for (Param param : params)
        {
            String value = defaultParser.getInputParameter(param.getInputXmlLabels().get(0));
            if (value == null || value.equals("")) continue;
            param.setValue(value);
            String parserError = param.validate();
            if (!parserError.equals(""))
            {
                error(parserError);
                return null;
            }
            command.add(param.convert());
        }
        return command;
    }
}
