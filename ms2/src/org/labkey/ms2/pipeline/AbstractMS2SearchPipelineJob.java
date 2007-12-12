package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.*;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewURLHelper;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 * User: brendanx
 * Date: Nov 11, 2007
 */
public abstract class AbstractMS2SearchPipelineJob extends PipelineJob
        implements MS2SearchJobSupport, TPPTask.JobSupport, XarGeneratorTask.JobSupport, XarLoaderTask.JobSupport
{
    private static String DATATYPE_SAMPLES = "Samples"; // Default
    private static String DATATYPE_FRACTIONS = "Fractions";
    private static String DATATYPE_BOTH = "Both";

    public static File getPepXMLConvertFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + "_raw.pep.xml");
    }

    protected Integer _experimentRowId;
    protected String _protocolName;
    protected String _baseName;
    protected File _dirSequenceRoot;
    protected File _dirMzXML;
    protected File[] _filesMzXML;
    protected File _dirAnalysis;
    protected File _fileInputXML;
    protected boolean _fractions;
    protected boolean _fromCluster;

    private Map<String, String> _parametersDefaults;
    private Map<String, String> _parametersOverrides;

    private transient Map<String, String> _parameters;

    public AbstractMS2SearchPipelineJob(String providerName,
                                        ViewBackgroundInfo info,
                                        String protocolName,
                                        File dirSequenceRoot,
                                        File fileInputXML,
                                        File filesMzXML[],
                                        boolean fromCluster) throws SQLException, IOException
    {
        super(providerName, info);
        _filesMzXML = filesMzXML;
        _dirMzXML = filesMzXML[0].getParentFile();
        _protocolName = protocolName;
        _dirSequenceRoot = dirSequenceRoot;

        _fileInputXML = fileInputXML;
        _dirAnalysis = _fileInputXML.getParentFile();
        _fromCluster = fromCluster;

        // Load parameter files
        _parametersOverrides = getInputParameters().getInputParameters();

        // Check for explicitly set default parameters.  Otherwise use the default.
        String paramDefaults = _parametersOverrides.get("list path, default parameters");
        File fileDefaults;
        if (paramDefaults != null)
            fileDefaults = new File(getRootDir().toURI().resolve(paramDefaults));
        else
        {
            MS2SearchPipelineProvider provider =(MS2SearchPipelineProvider)
                    PipelineService.get().getPipelineProvider(providerName);
            fileDefaults = provider.getProtocolFactory().getDefaultParametersFile(getRootDir());
        }

        _parametersDefaults = getInputParameters(fileDefaults).getInputParameters();

        // Make sure a sequence file is specified.
        String paramDatabase = getParameters().get("pipeline, database");
        if (paramDatabase == null)
            throw new IOException("Missing required input parameter 'pipeline, database'");

        // Set the fractions attribute correctly.
        String all = MS2PipelineManager._allFractionsMzXmlFileBase;
        _fractions = (filesMzXML.length > 1);
        if (_fractions)
        {
            String paramDataType = getParameters().get("pipeline, data type");
            if (!DATATYPE_BOTH.equalsIgnoreCase(paramDataType))
            {
                if (!NetworkDrive.exists(MS2PipelineManager.getAnnotationFile(_dirMzXML, all)) &&
                        !NetworkDrive.exists(MS2PipelineManager.getLegacyAnnotationFile(_dirMzXML, all)))
                {
                    _fractions = DATATYPE_FRACTIONS.equalsIgnoreCase(paramDataType);
                }
                else
                {
                    _fractions = !DATATYPE_SAMPLES.equalsIgnoreCase(paramDataType);
                }
            }
        }

        if (isFractions())
            _baseName = all;
        else
            _baseName = FileUtil.getBaseName(filesMzXML[0]);

        setLogFile(MS2PipelineManager.getLogFile(_dirAnalysis, _baseName), _fromCluster);

        boolean cluster = isPerlClusterAware() && AppProps.getInstance().hasPipelineCluster();
        if (cluster)
            setStatusFile(MS2PipelineManager.getStatusFile(_dirAnalysis, _baseName));
    }

    public AbstractMS2SearchPipelineJob(AbstractMS2SearchPipelineJob job, File fileFraction)
    {
        super(job);

        // Copy some parameters from the parent job.
        _experimentRowId = job._experimentRowId;
        _protocolName = job._protocolName;
        _dirSequenceRoot = job._dirSequenceRoot;
        _dirMzXML = job._dirMzXML;
        _fractions = job._fractions;
        _dirAnalysis = job._dirAnalysis;
        _fileInputXML = job._fileInputXML;
        _parameters = job._parameters;
        _parametersDefaults = job._parametersDefaults;
        _parametersOverrides = job._parametersOverrides;

        // Change parameters which are specific to the fraction job.
        _filesMzXML = new File[] { fileFraction };
        _baseName = FileUtil.getBaseName(fileFraction);
        if (getStatusFile() != getLogFile())
            setStatusFile(MS2PipelineManager.getStatusFile(_dirAnalysis, _baseName));
        setLogFile(MS2PipelineManager.getLogFile(_dirAnalysis, _baseName), false);
    }

    public String[] getTaskPipeline()
    {
        if (_filesMzXML.length > 1)
        {
            return new String[]
                    {
                        TPPTask.class.getName(),
                        XarGeneratorTask.class.getName(),
                        XarLoaderTask.class.getName()
                    };
        }
        else if (!isSamples())
        {
            return new String[]
                    {
                            getSearchTaskClass().getName()
                    };
        }

        return new String[]
                {
                    getSearchTaskClass().getName(),
                    TPPTask.class.getName(),
                    XarGeneratorTask.class.getName(),
                    XarLoaderTask.class.getName()
                };
    }

    abstract public String getSearchEngine();

    abstract public Class getSearchTaskClass();
    
    abstract public AbstractMS2SearchPipelineJob[] getSingleFileJobs();

    /**
     * Returns true, if this job supports a Perl based pipeline driven by pipe.pl.
     *
     * @return true if Perl pipeline
     */
    public boolean isPerlClusterAware()
    {
        return false;
    }

    public void run()
    {
        if (!_fromCluster && getSpectraFiles().length > 1)
        {
            for (PipelineJob job : getSingleFileJobs())
            {
                job.run();
                setErrors(getErrors() + job.getErrors());
            }
        }

        if (getErrors() == 0)
            super.run();
    }

    public File getParametersFile()
    {
        return _fileInputXML;
    }

    public BioMLInputParser getInputParameters() throws IOException
    {
        return getInputParameters(_fileInputXML);
    }

    public BioMLInputParser getInputParameters(File parametersFile) throws IOException
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

        BioMLInputParser parser = createInputParser();
        parser.parse(xmlBuffer.toString());
        if (parser.getErrors() != null)
        {
            XMLValidationParser.Error err = parser.getErrors()[0];
            if (err.getLine() == 0)
            {
                throw new IOException("Failed parsing input xml '" + parametersFile.getPath() + "'.\n" +
                        err.getMessage());
            }
            else
            {
                throw new IOException("Failed parsing input xml '" + parametersFile.getPath() + "'.\n" +
                        "Line " + err.getLine() + ": " + err.getMessage());
            }
        }
        return parser;
    }

    public BioMLInputParser createInputParser()
    {
        return new BioMLInputParser();
    }

    public Map<String, String> getParametersOverrides()
    {
        return _parametersOverrides;
    }

    public Map<String, String> getParameters()
    {
        if (_parameters == null)
        {
            _parameters = new HashMap<String, String>(_parametersDefaults);
            _parameters.putAll(_parametersOverrides);
        }

        return _parameters;
    }

    /**
     * Override to turn off PeptideProphet and ProteinProphet analysis.
     * @return true if Prophets should run.
     */
    public boolean isProphetEnabled()
    {
        return true;
    }

    /**
     * Override to turn on RefreshParser during TPP analysis.
     * @return true if RefreshParser should run.
     */
    public boolean isRefreshRequired()
    {
        return false;
    }

    public File getDataDirectory()
    {
        return _dirMzXML;
    }

    public File getAnalysisDirectory()
    {
        return _dirAnalysis;
    }

    public File[] getInteractInputFiles()
    {
        ArrayList<File> files = new ArrayList<File>();
        for (File fileSpectra : getSpectraFiles())
        {
            files.add(getPepXMLConvertFile(_dirAnalysis,
                    FileUtil.getBaseName(fileSpectra)));
        }
        return files.toArray(new File[files.size()]);
    }

    public File getSearchSpectraFile()
    {
        assert _filesMzXML.length == 1;
        return _filesMzXML[0];
    }

    public File getSearchNativeSpectraFile()
    {
        return null;    // No spectra conversion by default.
    }

    public File[] getSpectraFiles()
    {
        return _filesMzXML;
    }

    public File getSequenceRootDirectory()
    {
        return _dirSequenceRoot;
    }

    public File[] getSequenceFiles()
    {
        ArrayList<File> arrFiles = new ArrayList<File>();

        String paramDatabase = getParameters().get("pipeline, database");
        if (paramDatabase != null)
        {
            String[] databases = paramDatabase.split(";");
            for (String path : databases)
                arrFiles.add(MS2PipelineManager.getSequenceDBFile(_dirSequenceRoot.toURI(), path));
        }

        return arrFiles.toArray(new File[arrFiles.size()]);
    }

    public String getOutputBasename()
    {
        return _baseName;
    }

    public boolean isXPressQuantitation()
    {
        return "xpress".equalsIgnoreCase(getParameters().get("pipeline quantitation, algorithm"));
    }

    public ViewURLHelper getStatusHref()
    {
        if (_experimentRowId != null)
        {
            ViewURLHelper ret = getViewURLHelper().clone();
            ret.setPageFlow("Experiment");
            ret.setAction("details");
            ret.setExtraPath(getContainer().getPath());
            ret.deleteParameters();
            ret.addParameter("rowId", _experimentRowId.toString());
            return ret;
        }
        return null;
    }

    public String getDescription()
    {
        return MS2PipelineManager.getDataDescription(_dirMzXML, _baseName, _protocolName);
    }

    public boolean isFractions()
    {
        return _fractions;
    }

    public boolean isSamples()
    {
        return !_fractions || DATATYPE_BOTH.equalsIgnoreCase(getParameters().get("pipeline, data type"));
    }

    public void setExperimentRowId(int rowId)
    {
        _experimentRowId = rowId;
    }

    public Map<String, String> getXarTemplateReplacements() throws IOException
    {
        Map<String, String> replaceMap = new HashMap<String, String>();

        File dirRoot = getRootDir();
        File dirAnalysis = getAnalysisDirectory();
        File fileInput = getParametersFile();
        String baseName = getOutputBasename();

        replaceMap.put("SEARCH_NAME", getDescription());

        File[] databaseFiles = getSequenceFiles();
        StringBuilder databaseSB = new StringBuilder();
        for (File fileDatabase : databaseFiles)
            databaseSB.append(getStartingInputDataSnippet(fileDatabase, getAnalysisDirectory()));

        replaceMap.put("PROTEIN_DATABASES", databaseSB.toString());
        replaceMap.put("PROTEIN_DATABASE_DATALSIDS", getDataLSIDSnippet(databaseFiles, dirAnalysis, "FASTA"));

        StringBuilder mzxmlStartingInputsSB = new StringBuilder();
        StringBuilder instanceDetailsSB = new StringBuilder();

        File[] spectraFiles = getSpectraFiles();
        for (File fileSpectra : spectraFiles)
        {
            mzxmlStartingInputsSB.append(getStartingInputDataSnippet(fileSpectra, dirAnalysis));
            instanceDetailsSB.append(getInstanceDetailsSnippet(fileSpectra, dirAnalysis, databaseFiles, fileInput));
        }

        replaceMap.put("INSTANCE_DETAILS", instanceDetailsSB.toString());
        replaceMap.put("MZXML_DATALSIDS", getDataLSIDSnippet(spectraFiles, dirAnalysis, "mzXML"));
        replaceMap.put("MZXML_STARTING_INPUTS", mzxmlStartingInputsSB.toString());
        replaceMap.put("MZXML_PATHS", getSpectraFilePaths(dirAnalysis, spectraFiles));
        replaceMap.put("INPUT_XML_FILE_PATH",
                PathRelativizer.relativizePathUnix(dirAnalysis, fileInput));
        if (spectraFiles.length == 1)
        {
            File f = getSearchNativeSpectraFile();
            if (f != null)
                replaceMap.put("SPECTRA_CONVERT_FILE_PATH",  PathRelativizer.relativizePathUnix(dirAnalysis, f));

            f = getSearchNativeOutputFile();
            if (f != null)
                replaceMap.put("SEARCH_OUTPUT_FILE_PATH", PathRelativizer.relativizePathUnix(dirAnalysis, f));
        }

        replaceMap.put("PEP_XML_FILE_PATH",
                TPPTask.getPepXMLFile(_dirAnalysis, _baseName).getName());

        File fileProtXml = TPPTask.getProtXMLFile(dirAnalysis, baseName);
        if (!NetworkDrive.exists(fileProtXml))
        {
            File fileProtXMLInt = TPPTask.getProtXMLIntermediatFile(dirAnalysis, baseName);
            if (NetworkDrive.exists(fileProtXMLInt))
                fileProtXml = fileProtXMLInt;
        }
        replaceMap.put("PEP_PROT_XML_FILE_PATH", fileProtXml.getName());

        File fileUniquifier = dirAnalysis;
        if (getSpectraFiles().length == 1)
            fileUniquifier = new File(fileUniquifier, baseName);
        String uniquifier = PageFlowUtil.encode(
                PathRelativizer.relativizePathUnix(dirRoot, fileUniquifier)).replaceAll("%2F", "/");

        replaceMap.put("RUN-UNIQUIFIER", uniquifier);
        return replaceMap;
    }

    protected String getInstanceDetailsSnippet(File mzXMLFile, File analysisDir, File[] databaseFiles, File configFile) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("                      <exp:InstanceDetails>\n");
        sb.append("                        <exp:InstanceInputs>\n");
        sb.append("                          <exp:DataLSID DataFileUrl=\"");
        sb.append(PathRelativizer.relativizePathUnix(analysisDir, mzXMLFile));
        sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        sb.append("                          <exp:DataLSID DataFileUrl=\"");
        sb.append(PathRelativizer.relativizePathUnix(analysisDir, configFile));
        sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        for (File dbFile : databaseFiles)
        {
            sb.append("                          <exp:DataLSID DataFileUrl=\"");
            sb.append(PathRelativizer.relativizePathUnix(analysisDir, dbFile));
            sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        }
        sb.append("                        </exp:InstanceInputs>\n");
        sb.append("                      </exp:InstanceDetails>\n");
        return sb.toString();
    }

    protected String getStartingInputDataSnippet(File f, File analysisDir) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\t<exp:Data rdf:about=\"${AutoFileLSID}\">\n");
        sb.append("\t\t\t<exp:Name>");
        sb.append(f.getName());
        sb.append("</exp:Name>\n");
        sb.append("\t\t\t<exp:CpasType>Data</exp:CpasType>\n");
        sb.append("\t\t\t<exp:DataFileUrl>");
        sb.append(PathRelativizer.relativizePath(analysisDir, f));
        sb.append("</exp:DataFileUrl>\n");
        sb.append("\t\t</exp:Data>\n");
        return sb.toString();
    }

    protected String getDataLSIDSnippet(File[] files, File analysisDir, String baseRoleName) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for (File file : files)
        {
            sb.append("                                <exp:DataLSID DataFileUrl=\"");
            sb.append(PathRelativizer.relativizePathUnix(analysisDir, file));
            sb.append("\" RoleName=\"");
            sb.append(baseRoleName);
            sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        }
        return sb.toString();
    }

    protected String getSpectraFilePaths(File analysisDir, File[] jobSpectraFiles)
            throws IOException
    {
        // CONSIDER(brendanx): look at extension of passed in spectra files to one day support mzData.
        StringBuilder result = new StringBuilder();
        File dirData = getDataDirectory();
        File[] allSpectraFiles = dirData.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(".mzxml");
            }
        });
        Set<File> fileSet1 = new HashSet<File>(Arrays.asList(allSpectraFiles));
        Set<File> fileSet2 = new HashSet<File>(Arrays.asList(jobSpectraFiles));
        if (fileSet1.equals(fileSet2))
        {
            result.append(PathRelativizer.relativizePathUnix(analysisDir, dirData));
            result.append("*.mzxml");
        }
        else
        {
            for (File f : jobSpectraFiles)
            {
                if (result.length() > 0)
                {
                    result.append(";");
                }
                result.append(PathRelativizer.relativizePathUnix(analysisDir, f));
            }
        }

        return result.toString();
    }
}
