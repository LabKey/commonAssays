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
    private File _fileInputXML;
    File _fileSequestParamsLocal;
    File _fileSequestParamsRemote;
    File _fileProtXml;
    //input params from form and default file.
    private File _fileSequestXML;
    //input params from form
    private SequestInputParser _parser;
    private String[] _databases;
    private boolean _isXpress;
    public static final String FRACTIONS = "fractions";
    public static final String BOTH = "both";
    public static final String SAMPLE = "samples";
    private String _dataType = FRACTIONS;
    private boolean _isFractions;

    public SequestPipelineJob(ViewBackgroundInfo info,
                              String name,
                              URI uriRoot,
                              URI uriSequenceRoot,
                              File filesMzXML[],
                              File fileInputXML,
                              boolean isFractions,
                              boolean append)
    {
        super(SequestLocalPipelineProvider.name, info, filesMzXML, name, uriRoot, uriSequenceRoot);

        _fileSequestXML = fileInputXML;
        _dirAnalysis = _fileSequestXML.getParentFile();
        _isFractions = isFractions;

        if (isFractions)
            _baseName = "all";
        else
            _baseName = MS2PipelineManager.getBaseName(filesMzXML[0]);

        _filePepXML = MS2PipelineManager.getPepXMLFile(_dirAnalysis, _baseName);
        setLogFile(MS2PipelineManager.getLogFile(_dirAnalysis, _baseName), append);

       header("Sequest search for " + _dirAnalysis.getName());
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
        _parser = getInputParameters();
        if (_parser == null)
            return;
        String paramDatabase = _parser.getInputParameter("pipeline, database");
        if (paramDatabase == null)
        {
            error("Failed parsing Sequest input xml '" + _fileSequestXML.getPath() + "'.\n" +
                "Missing required input parameter 'pipeline, database'");
            return;
        }
        _databases = paramDatabase.split(";");
        _isXpress = _parser.getInputParameter("pipeline quantitation, residue label mass") != null;
        File fileExperimentXML;
        try
        {

            fileExperimentXML = writeExperimentXML(false,_dirAnalysis);
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
        _parser = null;
        _fileSequestParamsLocal = new File(_dirAnalysis, "sequest.params");
        _fileSequestParamsRemote = new File(_dirAnalysis, "remote.params");
        _fileInputXML = new File(_dirAnalysis, "input.xml");
        ArrayList<File> tgzArchives = new ArrayList<File>();
        ArrayList<File> htmlArchives = new ArrayList<File>();
        ArrayList<File> pepProphetInputFiles = new ArrayList<File>();
        String parserError = initInputParams();
        if(!parserError.equals(""))
        {
            error(parserError);
            return;
        }
        parserError = createSequestParamFile();
        if(!parserError.equals(""))
        {
            error(parserError);
            return;
        }


        for(File fileMzXML:_filesMzXML)
        {
            setStatus("SEARCHING");
            String fileBaseName = MS2PipelineManager.getBaseName(fileMzXML);
            File dirWork = new File(_dirAnalysis, fileBaseName + ".work");
            dirWork.mkdir();


            File fileSequestParamsWorking = new File(dirWork, "sequest.params");
            try
            {
            MS2PipelineManager.copyFile(_fileSequestParamsLocal, fileSequestParamsWorking);
            }
            catch(IOException e)
            {
                error("Error while copying sequest.params to '" + fileSequestParamsWorking.getAbsolutePath() + "'.");
                return;
            }
            File fileSequestSummary =  MS2PipelineManager.getSequestSummaryFile(dirWork, fileBaseName);
            File fileSequestRawXml = MS2PipelineManager.getSequestRawXmlFile(dirWork, fileBaseName);
            File fileInputMzXML = new File(dirWork, fileMzXML.getName());
            File dirOutputDta = MS2PipelineManager.getDtaFile(dirWork, fileBaseName);
            File fileTgz = new File(dirWork, fileBaseName + ".pep.tgz");

            int iReturn;
            try
            {
                /*
                0. pre-Sequest search: c) translate the mzXML file to dta for Sequest (msxml2other)
                */
                header("MzXML2Search output");
                Mzxml2SearchParams mzxml2SearchParams = new Mzxml2SearchParams();
                Collection<Param> params = mzxml2SearchParams.getParams();
                ArrayList<String> command = new ArrayList<String>();
                command.add("MzXML2Search");
                command.add("-dta");
                command.add("-M2");
                Collection<String> inputXmlParams = convertParams(params,_parser);
                if ( inputXmlParams == null) return;
                command.addAll(inputXmlParams);
                command.add(fileInputMzXML.getName());

                MS2PipelineManager.copyFile(fileMzXML, fileInputMzXML);

                iReturn = runSubProcess(new ProcessBuilder(command), dirWork);
                if (iReturn != 0 || !dirOutputDta.exists())
                {
                    error("Failed to translate " + fileInputMzXML.getAbsolutePath() + " to " + dirOutputDta.getAbsolutePath());
                    return;
                }
                /*
                1. perform Sequest search
                */
                AppProps appProps = AppProps.getInstance();

                SequestClientImpl sequestClient = new SequestClientImpl(appProps.getSequestServer(), getLogger());
                iReturn = sequestClient.search(_fileSequestParamsRemote.getAbsolutePath(),fileMzXML.getAbsolutePath(),
                        fileSequestSummary.getAbsolutePath(),inputXmlParams);

                if (iReturn != 0 || !fileSequestSummary.exists())
                {
                    error("Failed running Sequest.");
                    return;
                }
                info("Converting Sequest summary file to pepXML.");

                iReturn = runSubProcess(new ProcessBuilder("Sequest2Xml", fileSequestSummary.getName()), dirWork);
                if (iReturn != 0 || !fileSequestRawXml.exists())
                {
                    error("Failed running Sequest2Xml.");
                    return;
                }

                File fileHtmlFinal = new File(_dirAnalysis, fileSequestSummary.getName());
                if(!fileHtmlFinal.exists())
                {
                    if (!fileSequestSummary.renameTo(fileHtmlFinal))
                    {
                        error("Failed to move " + fileSequestSummary.getAbsolutePath() + " to " + fileHtmlFinal.getAbsolutePath());
                        return;
                    }
                }

                info("creating tgz archive of dta.");
                try
                {
                    iReturn = runSubProcess(new ProcessBuilder("bsdtar.exe", "czf", fileTgz.getAbsolutePath(), "*"), dirOutputDta);
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


                if (!FileUtil.deleteDir(dirOutputDta))
                {
                    error("Failed to delete " + dirOutputDta.getAbsolutePath());
                    return;
                }
            }
            catch (Exception e)
            {
                error("Sequest search exception: ",e);
                return;
            }
            
            if(!_isFractions)
            {
                ArrayList<File> pep = new ArrayList<File>();
                pep.add(fileSequestRawXml);
                ArrayList<File> tgz = new ArrayList<File>();
                tgz.add(fileTgz);
                parserError = xinteract(pep, tgz ,dirWork);
                if(parserError.length() > 0)
                {
                    error(parserError);
                    return;
                }
//                if( _dataType.equalsIgnoreCase(BOTH))
//                    pepProphetInputFiles.add(fileSequestRawXml);
            }
            else
            {
                htmlArchives.add(fileSequestSummary);
                pepProphetInputFiles.add(fileSequestRawXml);
                tgzArchives.add(fileTgz);
            }
        }
        if(!pepProphetInputFiles.isEmpty())
        {
            parserError = xinteract( pepProphetInputFiles, tgzArchives,_dirAnalysis);
            if(parserError.length() > 0)
            {
                error(parserError);
            }
        }
    }

    private String xinteract(ArrayList<File> pepProphetInputFiles,ArrayList<File> tgzArchives, File dirWork)
    {
        setStatus("ANALYZING");
        int iReturn;
        if(pepProphetInputFiles.size() == 0) return "No raw pepXml file to analyze.";
        //boolean _isFractions = pepProphetInputFiles.size() > 1;
        String outputBaseName = (_isFractions)?"all":MS2PipelineManager.getBaseName(pepProphetInputFiles.get(0));

        File fileProtXML = MS2PipelineManager.getProtXMLIntermediatFile(dirWork, outputBaseName);
        _fileProtXml = MS2PipelineManager.getProtXMLFile(_dirAnalysis, outputBaseName);
        File filePepXML = MS2PipelineManager.getPepXMLFile(dirWork, outputBaseName);
         _filePepXML = MS2PipelineManager.getPepXMLFile(_dirAnalysis, outputBaseName);
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
            else
            {
                error("Cannot run the protein prophet. PERL either doesn't exist or is a cygwin version.");
            }
            // Add any necessary quantiation parameters.
            String quantParam = getQuantitationCmd(_parser, _dirMzXML);
            if (quantParam != null)
                interactCmd.add(quantParam);
//
//            String paramQuant = _parser.getInputParameter("pipeline quantitation, residue label mass");
//            if (paramQuant != null)
//            {
//                List<String> xpressOpts = new ArrayList<String>();
//                String[] quantSpecs = paramQuant.split(",");
//                try
//                {
//                    String[] massRes;
//                    if(quantSpecs.length == 1)
//                    {
//                        massRes = parseResidueMass(quantSpecs[0]);
//                        xpressOpts.add("-l" + massRes[1]);
//                        xpressOpts.add("-r" + massRes[0]);
//                    }
//                    else
//                    {
//                        for (String spec : quantSpecs)
//                        {
//                            massRes = parseResidueMass(spec);
//                            xpressOpts.add("-n" + massRes[1] + "," + massRes[0]);
//                        }
//                    }
//                }
//                catch(SequestParamsException e)
//                {
//                    error("The value for pipeline quantitation, residue label mass was not valid." + e.getMessage());
//                }
//
//                paramQuant = _parser.getInputParameter("pipeline quantitation, mass tolerance");
//                if (paramQuant != null)
//                try
//                {
//                    paramQuant = new Double(paramQuant).toString();
//                    xpressOpts.add("-m" + paramQuant);
//                }
//                catch(NumberFormatException e)
//                {
//                    error("The value set for pipeline quantitation, mass tolerance must be a float." + e.getMessage());
//                }
//
//                paramQuant = _parser.getInputParameter("pipeline quantitation, fix");
//                if (paramQuant != null)
//                {
//                    if ("heavy".equalsIgnoreCase(paramQuant))
//                        xpressOpts.add("-H");
//                    else if ("light".equalsIgnoreCase(paramQuant))
//                        xpressOpts.add("-L");
//                }
//
//                paramQuant = _parser.getInputParameter("pipeline quantitation, fix elution reference");
//                if (paramQuant != null)
//                {
//                    String refFlag = "-f";
//                    if ("peak".equalsIgnoreCase(paramQuant))
//                        refFlag = "-F";
//                    paramQuant = _parser.getInputParameter("pipeline quantitation, fix elution difference");
//                    if (paramQuant != null)
//                        xpressOpts.add(refFlag + paramQuant);
//                }
//
//                paramQuant = _parser.getInputParameter("pipeline quantitation, metabolic search type");
//                if (paramQuant != null)
//                {
//                    if ("normal".equalsIgnoreCase(paramQuant))
//                        xpressOpts.add("-M");
//                    else if ("heavy".equalsIgnoreCase(paramQuant))
//                        xpressOpts.add("-N");
//                }
//
//                interactCmd.add("-X\"" + StringUtils.join(xpressOpts.iterator(), ' ') + "\"");
//            }

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
                return "Failed running xinteract.";
            }
        }
        catch (RunProcessException erp)
        {
            // Handled in runSubProcess
            return "";
        }
        catch (InterruptedException ei)
        {
            // Handled in runSubProcess
            return "";
        }

        File fileExperimentXML;
        try
        {

            fileExperimentXML = writeExperimentXML(_isFractions, dirWork);
        }
        catch (IOException e)
        {
            return "Failed writing XAR file";
        }
        catch (SQLException e)
        {
            return "Failed writing XAR file";
        }

        File fileExperimentXMLFinal = new File(_dirAnalysis, fileExperimentXML.getName());


        ArrayList<File> tgz = new ArrayList<File>();
        for (File tgzFile : tgzArchives)
        {
            File fileTgzFinal;
            fileTgzFinal = new File(_dirAnalysis, tgzFile.getName());
            if (!tgzFile.renameTo(fileTgzFinal))
            {
                return "Failed to move " + tgzFile.getAbsolutePath() + " to " + fileTgzFinal.getAbsolutePath();
            }
            tgz.add(fileTgzFinal);
        }

        for (File rawPepFile : pepProphetInputFiles)
        {
            File fileRawPepFinal;
            fileRawPepFinal = new File(_dirAnalysis, rawPepFile.getName());
            if(!fileRawPepFinal.exists())
            {
                if (!rawPepFile.renameTo(fileRawPepFinal))
                {
                    return "Failed to move " + rawPepFile.getAbsolutePath() + " to " + fileRawPepFinal.getAbsolutePath();
                }
            }
        }

        if (!filePepXML.renameTo(_filePepXML))
        {
            return "Failed to move " + filePepXML.getAbsolutePath() + " to " + _filePepXML.getAbsolutePath();
        }
        else if (fileProtXML != null &&
            !fileProtXML.renameTo(_fileProtXml))
        {
            return "Failed to move " + fileProtXML.getAbsolutePath() + " to " + MS2PipelineManager.getProtXMLFile(_dirAnalysis, _baseName).getAbsolutePath();
        }
        else if (!fileExperimentXML.renameTo(new File(_dirAnalysis, fileExperimentXML.getName())))
        {
            return "Failed to move " + fileExperimentXML.getAbsolutePath() + " to " +
                    (new File(_dirAnalysis, fileExperimentXML.getName())).getAbsolutePath();
        }

        else if (!_isFractions && !FileUtil.deleteDir(dirWork))
        {
            return "Failed to delete " + dirWork.getAbsolutePath();
        }
        else if(_isFractions && !FileUtil.deleteSubDirs(dirWork))

        setStatus("LOADING");
        FileXarSource source = new FileXarSource(fileExperimentXMLFinal);
        String status = ExperimentPipelineJob.loadExperiment(this, source, false);
        _experimentRowId = source.getExperimentRowId();

        for (File tgzFile : tgz)
        {
            if (!tgzFile.delete())
            {
                return "Failed to delete " + tgzFile.getAbsolutePath() + ".";
            }
        }

        setStatus(status);
        return "";
    }

    private File writeExperimentXML(boolean isFractions, File workDir) throws IOException, SQLException
    {
        StringBuilder templateResource = new StringBuilder("org/labkey/ms2/templates/MS2SearchSequest");
        if (isFractions)
        {
            templateResource.append("Fractions");
        }
        if (_isXpress)
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
        File[] databaseFiles = new File[_databases.length];
        for (int i = 0; i < _databases.length; i++)
        {
            String database = _databases[i];
            databaseFiles[i] = MS2PipelineManager.getSequenceDBFile(_uriSequenceRoot, database);
            databaseSB.append(getStartingInputDataSnippet(databaseFiles[i], _dirAnalysis));
        }

        StringBuilder mzxmlStartingInputsSB = new StringBuilder();
        StringBuilder instanceDetailsSB = new StringBuilder();
        for (File f : _filesMzXML)
        {
            mzxmlStartingInputsSB.append(getStartingInputDataSnippet(f, _dirAnalysis));
            instanceDetailsSB.append(getInstanceDetailsSnippet(f, _dirAnalysis, databaseFiles, _fileSequestXML));
        }

        String mzXMLPaths = getMzXMLPaths(_dirAnalysis);
        
        Container c = _info.getContainer();
        PipelineService service = PipelineService.get();
        PipelineService.PipeRoot pr = service.findPipelineRoot(c);
        if (pr == null)
            throw new FileNotFoundException("Failed to find a pipeline root for " + c.getPath());
        File containerRoot = new File(pr.getUri(c));

        String searchName = getDescription();

        replaceString(sb, "SEARCH_NAME", searchName);
        replaceString(sb, "PROTEIN_DATABASE_DATALSIDS", getDataLSIDSnippet(databaseFiles, _dirAnalysis, "FASTA"));
        replaceString(sb, "MZXML_DATALSIDS", getDataLSIDSnippet(_filesMzXML, _dirAnalysis, "mzXML"));
        replaceString(sb, "MZXML_STARTING_INPUTS", mzxmlStartingInputsSB.toString());
        replaceString(sb, "MZXML_PATHS", mzXMLPaths);
        replaceString(sb, "SEQUEST_HTML_FILE_PATH",
        PathRelativizer.relativizePathUnix(_dirAnalysis, new File(_dirAnalysis, _baseName + ".html")));
        replaceString(sb, "RAW_PEP_XML_FILE_PATH",
        PathRelativizer.relativizePathUnix(_dirAnalysis, new File(_dirAnalysis, _baseName + ".xml")));
        replaceString(sb, "PROTEIN_DATABASES", databaseSB.toString());
        File sequestConfigFile = new File(_dirAnalysis,"sequest.xml");
        replaceString(sb, "SEQUEST_XML_FILE_PATH",
        PathRelativizer.relativizePathUnix(_dirAnalysis, sequestConfigFile));
        replaceString(sb, "INSTANCE_DETAILS", instanceDetailsSB.toString());
        String pepXMLFilePath = PathRelativizer.relativizePathUnix(_dirAnalysis, _filePepXML);
        replaceString(sb, "PEP_XML_FILE_PATH", pepXMLFilePath);

        replaceString(sb, "PEP_PROT_XML_FILE_PATH",
            PathRelativizer.relativizePathUnix(_dirAnalysis, _fileProtXml));

        File uniquifierFile = _fileSequestXML.getParentFile();
        if (!isFractions)
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

    private String initInputParams()
    {
            /*
            0. pre-Sequest search: b) write a copy of the effective configuration file "sequest.xml"
            */
            _parser = getInputParameters();
            if (_parser == null)
                return "The value for input.xml was null.";

            String paramDefaults = _parser.getInputParameter("list path, default parameters");
            File fileDefaults;
            if (paramDefaults == null)
                fileDefaults = MS2PipelineManager.getDefaultInputFile(_uriRoot, SEQUEST);
            else
                fileDefaults = new File(_uriRoot.resolve(paramDefaults));
            _parser.setInputParameter("list path, default parameters",
                fileDefaults.getAbsolutePath());

            SequestInputParser defaultParser = getInputParameters(fileDefaults);
            if (defaultParser == null)
            {
                return "Failed parsing default Sequest input xml '" + fileDefaults.getPath() + "'.";
            }
            String[] parameterNames = _parser.getInputParameterNames();

            for (String parameterName : parameterNames)
            {
                defaultParser.setInputParameter(parameterName, _parser.getInputParameter(parameterName));
            }
            defaultParser.setInputParameter("list path, sequest parameters", "sequest.params");
            defaultParser.setInputParameter("search, useremail", _parser.getInputParameter("pipeline, email address"));
            defaultParser.setInputParameter("search, username", "CPAS User");

            String xml = defaultParser.getXML();

            BufferedWriter inputWriter = null;

            try
            {
                inputWriter = new BufferedWriter(new FileWriter(_fileInputXML));
                inputWriter.write(xml);
            }
            catch (IOException eio)
            {
                return "Failed to write Sequest input file '" + _fileInputXML + "'.";
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
                        error("Failed to close Sequest input file '" + _fileInputXML + "'.");
                    }
                }
            }

            String paramDatabase = defaultParser.getInputParameter("pipeline, database");
            if (paramDatabase == null)
            {
                return "Failed parsing Sequest input xml '" + _fileSequestXML.getPath() + "'.\n" +
                    "Missing required input parameter 'pipeline, database'";
            }

            _databases = paramDatabase.split(";");
            _dataType = defaultParser.getInputParameter("pipeline, data type");
            if (_dataType == null)
            {
               _dataType = FRACTIONS;
               warn("Failed parsing Sequest input xml '" + _fileSequestXML.getPath() + "'.\n" +
                    "Missing input parameter 'pipeline, datatype'.");
            }
            if((_dataType.equalsIgnoreCase(FRACTIONS) && _filesMzXML.length == 1)
                    || _dataType.equalsIgnoreCase(SAMPLE))
            {
                _isFractions = false;
            }

            _databases = paramDatabase.split(";");
            _isXpress = _parser.getInputParameter("pipeline quantitation, residue label mass") != null;
            _parser = defaultParser;
            return "";
    }

    private String createSequestParamFile()
    {
            SequestParamsBuilder sequestParamsV2Builder =
            SequestParamsBuilderFactory.createVersion2Builder(_parser, _uriSequenceRoot);
            SequestParamsBuilder sequestParamsV1Builder =
                    SequestParamsBuilderFactory.createVersion1Builder(_parser, _uriSequenceRoot);
            String parseError = sequestParamsV2Builder.initXmlValues();
            if (!parseError.equals(""))
            {
                return parseError;
            }
            parseError = sequestParamsV1Builder.initXmlValues();
            if (!parseError.equals(""))
            {
                return parseError;
            }
            try
            {
                sequestParamsV2Builder.writeFile(_fileSequestParamsRemote);
                sequestParamsV1Builder.writeFile(_fileSequestParamsLocal);

            }
            catch (SequestParamsException e)
            {
                error(e.getMessage());
            }
        return "";
    }
     //returns mass,residue from 9.0@C
    private String[] parseResidueMass(String value) throws SequestParamsException
    {
        String[] specVals = value.split("@");
                    if (specVals.length != 2)
                        throw new SequestParamsException("'" + value + " is not a valid residue mass string.");
                    specVals[0] = specVals[0].trim();
                    specVals[1] = specVals[1].trim();
        return specVals;
    }
}
