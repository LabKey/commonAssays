package org.labkey.ms2.pipeline;

import org.labkey.api.exp.pipeline.XarGeneratorId;
import org.labkey.api.exp.pipeline.XarLoaderId;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.util.*;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: brendanx
 * Date: Nov 11, 2007
 */
public abstract class AbstractMS2SearchPipelineJob extends AbstractFileAnalysisJob
        implements MS2SearchJobSupport, TPPTask.JobSupport, XarGeneratorId.JobSupport, XarLoaderId.JobSupport
{
    enum Pipelines
    {
        finishPerlCluster;

        public TaskId getTaskId()
        {
            return new TaskId(getClass().getEnclosingClass(), toString());
        }
    }

    private static String DATATYPE_SAMPLES = "Samples"; // Default
    private static String DATATYPE_FRACTIONS = "Fractions";
    private static String DATATYPE_BOTH = "Both";

    public static File getPepXMLConvertFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + "_raw.pep.xml");
    }

    protected File _dirSequenceRoot;
    protected boolean _fractions;
    protected boolean _fromCluster;

    public AbstractMS2SearchPipelineJob(AbstractMS2SearchProtocol protocol,
                                        String providerName,
                                        ViewBackgroundInfo info,
                                        String protocolName,
                                        File dirSequenceRoot,
                                        File fileParameters,
                                        File filesInput[],
                                        boolean fromCluster) throws SQLException, IOException
    {
        super(protocol, providerName, info, protocolName, fileParameters, filesInput, fromCluster);

        _dirSequenceRoot = dirSequenceRoot;
        _fromCluster = fromCluster;

        // Make sure a sequence file is specified.
        String paramDatabase = getParameters().get("pipeline, database");
        if (paramDatabase == null)
            throw new IOException("Missing required input parameter 'pipeline, database'");

        // Set the fractions attribute correctly.
        _fractions = (filesInput.length > 1);
        if (_fractions)
        {
            String paramDataType = getParameters().get("pipeline, data type");
            if (!DATATYPE_BOTH.equalsIgnoreCase(paramDataType))
            {
                if (!NetworkDrive.exists(MS2PipelineManager.getAnnotationFile(getDataDirectory())) &&
                        !NetworkDrive.exists(MS2PipelineManager.getLegacyAnnotationFile(getDataDirectory())))
                {
                    _fractions = DATATYPE_FRACTIONS.equalsIgnoreCase(paramDataType);
                }
                else
                {
                    _fractions = !DATATYPE_SAMPLES.equalsIgnoreCase(paramDataType);
                }
            }
        }

        if (isPerlClusterAware() && AppProps.getInstance().hasPipelineCluster())
            setStatusFile(FT_CLUSTER_STATUS.newFile(getAnalysisDirectory(), getBaseName()));
    }

    public AbstractMS2SearchPipelineJob(AbstractMS2SearchPipelineJob job, File fileFraction)
    {
        super(job, fileFraction);

        // Copy some parameters from the parent job.
        _dirSequenceRoot = job._dirSequenceRoot;
        _fractions = job._fractions;

        // Change parameters which are specific to the fraction job.
        if (getStatusFile() != getLogFile())
            setStatusFile(FT_CLUSTER_STATUS.newFile(getAnalysisDirectory(), getBaseName()));
    }

    public TaskId getTaskPipelineId()
    {
        if (_fromCluster)
            return Pipelines.finishPerlCluster.getTaskId();

        return null;
    }

    public File findInputFile(String name)
    {
        if (getInputType().isType(name))
            return new File(getDataDirectory(), name);
        
        return new File(getAnalysisDirectory(), name);
    }

    public File findOutputFile(String name)
    {
        if (AbstractMS2SearchProtocol.FT_MZXML.isType(name))
            return new File(getDataDirectory(), name);
        
        return new File(getAnalysisDirectory(), name);
    }

    abstract public String getSearchEngine();

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
        // TODO: Get rid of this, and use branch and join instead.
        if (!_fromCluster && getInputFiles().length > 1)
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

    public File[] getInteractInputFiles()
    {
        ArrayList<File> files = new ArrayList<File>();
        for (File fileSpectra : getInputFiles())
        {
            files.add(getPepXMLConvertFile(getAnalysisDirectory(),
                    FileUtil.getBaseName(fileSpectra)));
        }
        return files.toArray(new File[files.size()]);
    }

    public File getSearchSpectraFile()
    {
        assert getInputFiles().length == 1;
        return getInputFiles()[0];
    }

    public File getSearchNativeSpectraFile()
    {
        return null;    // No spectra conversion by default.
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

    public boolean isXPressQuantitation()
    {
        return "xpress".equalsIgnoreCase(getParameters().get("pipeline quantitation, algorithm"));
    }

    public boolean isFractions()
    {
        return _fractions;
    }

    public boolean isSamples()
    {
        return !_fractions || DATATYPE_BOTH.equalsIgnoreCase(getParameters().get("pipeline, data type"));
    }

/////////////////////////////////////////////////////////////////////////////
//  Experiment writing
    
    public Map<String, String> getXarTemplateReplacements() throws IOException
    {
        Map<String, String> replaceMap = new HashMap<String, String>();

        File dirRoot = getRootDir();
        File dirAnalysis = getAnalysisDirectory();
        File fileInput = getParametersFile();
        String baseName = getBaseName();

        replaceMap.put("SEARCH_NAME", getDescription());

        File[] databaseFiles = getSequenceFiles();
        StringBuilder databaseSB = new StringBuilder();
        for (File fileDatabase : databaseFiles)
            databaseSB.append(getStartingInputDataSnippet(fileDatabase, getAnalysisDirectory()));

        replaceMap.put("PROTEIN_DATABASES", databaseSB.toString());
        replaceMap.put("PROTEIN_DATABASE_DATALSIDS", getDataLSIDSnippet(databaseFiles, dirAnalysis, "FASTA"));

        StringBuilder mzxmlStartingInputsSB = new StringBuilder();
        StringBuilder instanceDetailsSB = new StringBuilder();

        File[] spectraFiles = getInputFiles();
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
                TPPTask.getPepXMLFile(getAnalysisDirectory(), getBaseName()).getName());

        File fileProtXml = TPPTask.getProtXMLFile(dirAnalysis, baseName);
        if (!NetworkDrive.exists(fileProtXml))
        {
            File fileProtXMLInt = TPPTask.getProtXMLIntermediatFile(dirAnalysis, baseName);
            if (NetworkDrive.exists(fileProtXMLInt))
                fileProtXml = fileProtXMLInt;
        }
        replaceMap.put("PEP_PROT_XML_FILE_PATH", fileProtXml.getName());

        File fileUniquifier = dirAnalysis;
        if (getInputFiles().length == 1)
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
