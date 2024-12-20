/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.ToolExecutionException;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.pipeline.WorkDirectoryTask;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.Pair;
import org.labkey.api.util.PepXMLFileType;
import org.labkey.api.util.ProtXMLFileType;
import org.labkey.ms2.pipeline.client.ParameterNames;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>TPPTask</code> PipelineJob task to run the TPP (xinteract) for further
 * analysis on a pepXML file generated by running a pepXML converter on a search
 * engine's raw output.  This task may run PeptideProphet, ProteinProphet,
 * Quantitation, and batch fractions into a single pepXML.
 */
public class TPPTask extends WorkDirectoryTask<TPPTask.Factory>
{
    // note that TPP also handles these formats as .gz files (ex. .pep.xml.gz)
    public static final FileType FT_PEP_XML = new PepXMLFileType();
    public static final FileType FT_PROT_XML = new ProtXMLFileType();

    /** Map of optional file outputs from the TPP to their input role names */
    public static final Map<FileType, String> FT_OPTIONAL_AND_IGNORABLES = new HashMap<>();

    static
    {
        // Outputs from ProphetModels.pl, added as part of TPP 4.3 or so
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".prot.xml_senserr.txt"), "ProtSensErr");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".prot.xml.gz_senserr.txt"), "ProtSensErr");

        // Outputs from ProphetModels.pl, added as part of TPP 4.2 or so
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep_FVAL_1.png"), "PepPropModel1");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep_FVAL_2.png"), "PepPropModel2");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep_FVAL_3.png"), "PepPropModel3");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep_FVAL_4.png"), "PepPropModel4");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep_FVAL_5.png"), "PepPropModel5");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep_IPPROB.png"), "iPropPepProb");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep_PPPROB.png"), "PepPropProb");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".prot_IPPROB.png"), "iPropProtProb");
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".prot_PPPROB.png"), "ProtPropProb");

        // Additional output from ProteinProphet, added as part of TPP 4.0 or so
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep-prot.xml_senserr.txt"), "ProtSensErr");

        // Additional output from ProteinProphet, added as part of TPP 4.6 or so
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".prot-MODELS.html"), "ProteinProphetModel");
    }

    private static final FileType FT_PEP_XSL = new FileType(".pep.xsl");
    private static final FileType FT_PEP_SHTML = new FileType(".pep.shtml");
    private static final FileType FT_INTERMEDIATE_PROT_XSL = new FileType(".pep-prot.xsl");
    private static final FileType FT_INTERMEDIATE_PROT_SHTML = new FileType(".pep-prot.shtml");

    /** Our desired extension for the final output */
    private static final FileType FT_LIBRA_QUANTITATION = new FileType(".libra.tsv");

    private static final String PEPTIDE_PROPHET_ACTION_NAME = "PeptideProphet";
    private static final String PROTEIN_PROPHET_ACTION_NAME = "ProteinProphet";
    private static final String PEPTIDE_QUANITATION_ACTION_NAME = "Peptide Quantitation";
    private static final String PROTEIN_QUANITATION_ACTION_NAME = "Protein Quantitation";

    public static final String PEP_XML_INPUT_ROLE = "PepXML";
    public static final String PROT_XML_INPUT_ROLE = "ProtXML";
    public static final String LIBRA_CONFIG_INPUT_ROLE = "LibraConfig";
    public static final String LIBRA_OUTPUT_ROLE = "LibraOutput";

    // All of the supported quantitation engines

    public static String getTPPVersion(PipelineJob job)
    {
        return job.getParameters().get("pipeline tpp, version");
    }

    public static File getPepXMLFile(File dirAnalysis, String baseName)
    {
        return FT_PEP_XML.newFile(dirAnalysis, baseName);
    }

    public static boolean isPepXMLFile(File file)
    {
        return FT_PEP_XML.isType(file);
    }

    public static File getProtXMLFile(File dirAnalysis, String baseName)
    {
        return FT_PROT_XML.newFile(dirAnalysis, baseName);
    }

    public static boolean isProtXMLFile(File file)
    {
        return getProtXMLFileType(file) != null;
    }

    public static FileType getProtXMLFileType(File file)
    {
        if (FT_PROT_XML.isType(file))
        {
            return FT_PROT_XML;
        }
        return null;
    }

    /**
     * Interface for support required from the PipelineJob to run this task,
     * beyond the base PipelineJob methods.
     */
    public interface JobSupport extends MS2PipelineJobSupport
    {
        /**
         * List of pepXML files to use as inputs to "xinteract".
         */
        List<File> getInteractInputFiles();

        /**
         * List of mzXML files to use as inputs to "xinteract" quantitation.
         */
        List<File> getInteractSpectraFiles();

        /**
         * True if PeptideProphet and ProteinProphet can be run on the input files.
         */
        boolean isProphetEnabled();

        /**
         * True if RefreshParser should run.
         */
        boolean isRefreshRequired();
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        private String _javaVMOptions;

        public Factory()
        {
            super(TPPTask.class);
        }

        public Factory(String name)
        {
            super(TPPTask.class, name);    
        }

        @Override
        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new TPPTask(this, job);
        }

        @Override
        public List<FileType> getInputTypes()
        {
            return Collections.singletonList(FT_PEP_XML);
        }

        @Override
        public String getStatusName()
        {
            return "ANALYSIS";
        }

        @Override
        public boolean isParticipant(PipelineJob job)
        {
            return job.getJobSupport(JobSupport.class).isSamples();
        }

        @Override
        public boolean isJobComplete(PipelineJob job)
        {
            JobSupport support = job.getJobSupport(JobSupport.class);
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            if (!NetworkDrive.exists(getPepXMLFile(dirAnalysis, baseName)))
                return false;

            return !support.isProphetEnabled() || NetworkDrive.exists(getProtXMLFile(dirAnalysis, baseName));
        }

        @Override
        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(PEPTIDE_PROPHET_ACTION_NAME, PEPTIDE_QUANITATION_ACTION_NAME, PROTEIN_PROPHET_ACTION_NAME, PROTEIN_QUANITATION_ACTION_NAME);
        }

        @Override
        public String getGroupParameterName()
        {
            return "tpp";
        }

        public String getJavaVMOptions()
        {
            return _javaVMOptions;
        }

        public void setJavaVMOptions(String javaVMOptions)
        {
            _javaVMOptions = javaVMOptions;
        }
    }

    public static class FactoryJoin extends Factory
    {
        public FactoryJoin()
        {
            super("join");

            setJoin(true);
        }

        @Override
        public boolean isParticipant(PipelineJob job)
        {
            return job.getJobSupport(JobSupport.class).isFractions();
        }

        @Override
        public String getGroupParameterName()
        {
            return "tpp fractions";
        }
    }

    protected TPPTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public JobSupport getJobSupport()
    {
        return getJob().getJobSupport(JobSupport.class);
    }

    @Override
    @NotNull
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            Map<String, String> params = getJob().getParameters();

            List<RecordedAction> actions = new ArrayList<>();

            QuantitationAlgorithm quantitationAlgorithm = getQuantitionAlgorithm(params);
            // Non-null if we're doing Libra quantitation
            @Nullable Pair<File, String> quantConfigFile = quantitationAlgorithm == null ? null : quantitationAlgorithm.getConfigFile(params, getJob().getPipeRoot(), _wd);

            // First step takes all the pepXMLs as inputs and either runs PeptideProphet (non-join) or rolls them up (join)
            RecordedAction pepXMLAction = new RecordedAction(PEPTIDE_PROPHET_ACTION_NAME);
            actions.add(pepXMLAction);

            // Set mzXML directory only if needed.
            File dirMzXml = null;

            // TODO: mzXML files may be required, and input disk space requirements
            //          may be too great to copy to a temporary directory.
            List<File> inputFiles = getJobSupport().getInteractInputFiles();
            List<File> inputWorkFiles = new ArrayList<>(inputFiles.size());
            for (File fileInput : inputFiles)
            {
                pepXMLAction.addInput(fileInput, "RawPepXML");
            }

            List<File> spectraFiles = new ArrayList<>();

            boolean proteinProphetOutput = getJobSupport().isProphetEnabled();
            if (inputFiles.size() > 0)
            {
                try (WorkDirectory.CopyingResource lock = _wd.ensureCopyingLock())
                {
                    for (File inputFile : inputFiles)
                        inputWorkFiles.add(_wd.inputFile(inputFile, false));

                    // Always copy spectra files to be local, since PeptideProphet wants them as of TPP 4.6.3
                    for (File spectraFile : getJobSupport().getInteractSpectraFiles())
                    {
                        spectraFiles.add(_wd.inputFile(spectraFile, true));
                        if (dirMzXml == null)
                            dirMzXml = spectraFile.getParentFile();
                    }
                }
            }

            RecordedAction peptideQuantAction = null;
            String[] quantParams = null;

            if (dirMzXml != null)
            {
                if (quantitationAlgorithm != null)
                {
                    quantParams = getQuantitationCmd(params, _wd.getRelativePath(dirMzXml), quantConfigFile);
                    peptideQuantAction = new RecordedAction(PEPTIDE_QUANITATION_ACTION_NAME);
                    peptideQuantAction.setDescription(quantitationAlgorithm.name() + " " + peptideQuantAction.getName());
                    peptideQuantAction.addParameter(new RecordedAction.ParameterType("Quantitation algorithm", "terms.labkey.org#QuantitationAlgorithm", PropertyType.STRING), quantitationAlgorithm.name());
                    if (quantConfigFile != null)
                    {
                        peptideQuantAction.addInput(quantConfigFile.getKey(), quantConfigFile.getValue());
                    }
                    actions.add(peptideQuantAction);
                }
            }

            File fileWorkPepXML = _wd.newFile(FT_PEP_XML);

            String ver = getTPPVersion(getJob());
            List<String> interactCmd = new ArrayList<>();
            String xinteractPath = PipelineJobService.get().getExecutablePath("xinteract", null, "tpp", ver, getJob().getLogger());
            File xinteractFile = new File(xinteractPath);
            interactCmd.add(xinteractPath);

            if (!getJobSupport().isProphetEnabled())
            {
                interactCmd.add("-nP"); // no Prophet analysis
            }
            else
            {
                String[] versionParts = ver == null ? new String[0] : ver.split("\\.");
                int majorVersion = -1;
                if (versionParts.length > 0)
                {
                    try
                    {
                        majorVersion = Integer.parseInt(versionParts[0]);
                    }
                    catch (NumberFormatException ignored) {}
                }
                StringBuilder prophetOpts = new StringBuilder("-Op");
                // Issue 32442 - TPP 5.0+ don't accept the 't' argument to xinteract to suppress plot generation
                if (majorVersion < 5)
                {
                    prophetOpts.append("t");
                }

                if ("yes".equalsIgnoreCase(params.get("pipeline prophet, accurate mass")))
                    prophetOpts.append("A");
                if ("yes".equalsIgnoreCase(params.get("pipeline prophet, allow multiple instruments")))
                    prophetOpts.append("w");

                if (!"2.9.9".equals(ver))
                {
                    if (params.get("pipeline prophet, peptide extra iterations") == null)
                        interactCmd.add("-x20");    // 20 iterations extra for good measure if the user hasn't specified a count
                    
                    if (!"3.0.2".equals(ver))
                    {
                        // prophetOpts.append("F");
                        if ("yes".equalsIgnoreCase(params.get("pipeline prophet, use hydrophobicity")))
                            prophetOpts.append("R");
                        if ("yes".equalsIgnoreCase(params.get("pipeline prophet, use pI")))
                            prophetOpts.append("I");
                        String decoyTag = params.get("pipeline prophet, decoy tag");
                        if (decoyTag != null && !"".equals(decoyTag))
                            interactCmd.add("-d" + decoyTag);
                    }
                }

                if (params.get("pipeline prophet, peptide extra iterations") != null)
                    interactCmd.add("-x" + params.get("pipeline prophet, peptide extra iterations"));

                interactCmd.add(prophetOpts.toString());

                if (!getJobSupport().isRefreshRequired())
                    interactCmd.add("-nR");

                String paramMinProb = params.get(ParameterNames.MIN_PEPTIDE_PROPHET_PROBABILITY);
                if (paramMinProb == null || paramMinProb.length() == 0)
                    paramMinProb = params.get("pipeline prophet, min peptide probability");
                if (paramMinProb != null && paramMinProb.length() > 0)
                    interactCmd.add("-p" + paramMinProb);
                
                paramMinProb = params.get(ParameterNames.MIN_PROTEIN_PROPHET_PROBABILITY);
                if (paramMinProb != null && paramMinProb.length() > 0)
                    interactCmd.add("-pr" + paramMinProb);
            }

            RecordedAction proteinQuantAction = null;

            if (quantParams != null)
            {
                interactCmd.addAll(Arrays.asList(quantParams));

                if (getJobSupport().isProphetEnabled())
                {
                    proteinQuantAction = new RecordedAction(PROTEIN_QUANITATION_ACTION_NAME);
                    proteinQuantAction.setDescription(quantitationAlgorithm.name() + " " + proteinQuantAction.getName());
                    proteinQuantAction.addParameter(new RecordedAction.ParameterType("Quantitation algorithm", "terms.labkey.org#QuantitationAlgorithm", PropertyType.STRING), quantitationAlgorithm.name());
                    if (quantConfigFile != null)
                    {
                        proteinQuantAction.addInput(quantConfigFile.getKey(), quantConfigFile.getValue());
                    }
                }
            }

            interactCmd.add("-N" + fileWorkPepXML.getName());

            for (File fileInput : inputWorkFiles)
                interactCmd.add(_wd.getRelativePath(fileInput));

            ProcessBuilder builder = new ProcessBuilder(interactCmd);
            // Add the TPP directory to the PATH so that xinteract can find it
            if (null != xinteractFile.getParentFile() && xinteractFile.getParentFile().exists())
            {
                String pathEnvName = "PATH";
                String perlLibEnvName = "PERLLIB";
                String path = "";
                String perlLib = "";
                for (String envName : builder.environment().keySet())
                {
                    // Not sure what the casing for that PATH environment variable is going to be, so check in
                    // case insensitive way
                    if (pathEnvName.equalsIgnoreCase(envName))
                    {
                        pathEnvName = envName;
                        path = builder.environment().get(pathEnvName);
                    }
                    if (perlLibEnvName.equalsIgnoreCase(envName))
                    {
                        perlLibEnvName = envName;
                        perlLib = builder.environment().get(perlLibEnvName);
                    }
                }
                path = xinteractFile.getParentFile().getAbsolutePath() + File.pathSeparatorChar + path;
                builder.environment().put(pathEnvName, path);
                perlLib = xinteractFile.getParentFile().getAbsolutePath() + File.pathSeparatorChar + perlLib;
                builder.environment().put(perlLibEnvName, perlLib);
            }
            builder.environment().put("WEBSERVER_ROOT", StringUtils.trimToEmpty(new File(xinteractPath).getParent()));
            // tell more modern TPP tools to run headless (so no perl calls etc) bpratt 4-14-09
            builder.environment().put("XML_ONLY", "1");
            // tell TPP tools not to mess with tmpdirs, we handle this at higher level
            builder.environment().put("WEBSERVER_TMP","");

            // Copy the Perl file, if it exists, to the working directory so it can be run if there's real Perl installed
            // See issue 19572
            try
            {
                File realTppModelsFile = new File(PipelineJobService.get().getExecutablePath("tpp_models.pl", null, "tpp", ver, getJob().getLogger()));
                if (realTppModelsFile.exists())
                {
                    _wd.inputFile(realTppModelsFile, true);
                }
            }
            catch (FileNotFoundException ignored)
            {
                // As long as we're running with the no-op Perl implementation, we'll be fine. Otherwise, job will
                // error out
            }

            try
            {
                getJob().runSubProcess(builder, _wd.getDir());
            }
            catch (ToolExecutionException e)
            {
                // We want to ignore the ProteinProphet error that results if PeptideProphet fails to model
                // all change states. It results in an exit code of 1 from xinteract
                boolean ignoreError = false;
                if (e.getExitCode() == 1)
                {
                    File logFile = getJob().getLogFile();
                    try (RandomAccessFile file = new RandomAccessFile(logFile, "r"))
                    {
                        // Look at the last 1K of the log file
                        file.seek(Math.max(0, file.length() - 1024));
                        String line;
                        while ((line = file.readLine()) != null)
                        {
                            // Check for the specific error message to make sure we don't silently continue for
                            // other types of errors that create the same exit code
                            if (line.endsWith(": no data - quitting"))
                            {
                                // Safe to ignore error, and don't expect a prot.xml output file
                                proteinProphetOutput = false;
                                ignoreError = true;
                            }
                        }
                    }
                }
                if (!ignoreError)
                {
                    throw e;
                }
            }

            try (WorkDirectory.CopyingResource lock = _wd.ensureCopyingLock())
            {
                File filePepXML = _wd.outputFile(fileWorkPepXML);

                // Set up the first step with the right outputs
                pepXMLAction.addOutput(filePepXML, PEP_XML_INPUT_ROLE, false);

                File fileProtXML = null;

                if (proteinProphetOutput)
                {
                    // If we ran ProteinProphet, set up a step with the right inputs and outputs
                    File fileWorkProtXML = _wd.newFile(FT_PROT_XML);

                    fileProtXML = _wd.outputFile(fileWorkProtXML, FT_PROT_XML.getDefaultName(getJobSupport().getBaseName()));

                    // Second step optionally runs ProteinProphet on the pepXML
                    RecordedAction protXMLAction = new RecordedAction(PROTEIN_PROPHET_ACTION_NAME);
                    protXMLAction.addInput(filePepXML, PEP_XML_INPUT_ROLE);
                    protXMLAction.addOutput(fileProtXML, PROT_XML_INPUT_ROLE, false);
                    actions.add(protXMLAction);

                    // Newer releases of the TPP write out some additional file types that don't really use, but
                    // we need to deal with them so that we don't complain about unexpected files
                    for (Map.Entry<FileType, String> entry : FT_OPTIONAL_AND_IGNORABLES.entrySet())
                    {
                        File workFile = _wd.newFile(entry.getKey());
                        {
                            // Check if it exists
                            if (!NetworkDrive.exists(workFile))
                            {
                                // If not, that's OK
                                _wd.discardFile(workFile);
                            }
                            else
                            {
                                // If so, then grab it and mark as an output
                                File outputFile = _wd.outputFile(workFile);
                                protXMLAction.addOutput(outputFile, entry.getValue(), false);
                            }
                        }
                    }
                }

                if (peptideQuantAction != null)
                {
                    for (File file : getJobSupport().getInteractSpectraFiles())
                    {
                        peptideQuantAction.addInput(file, "mzXML");
                    }
                    peptideQuantAction.addInput(filePepXML, PEP_XML_INPUT_ROLE);
                    peptideQuantAction.addOutput(filePepXML, "QuantPepXML", false);
                }

                if (proteinQuantAction != null && fileProtXML != null)
                {
                    proteinQuantAction.addInput(fileProtXML, PROT_XML_INPUT_ROLE);
                    proteinQuantAction.addInput(filePepXML, PEP_XML_INPUT_ROLE);
                    proteinQuantAction.addOutput(fileProtXML, "QuantProtXML", false);
                    // Add this here so that it's the last step in the TPP sequence
                    actions.add(proteinQuantAction);
                }

                if (quantConfigFile != null)
                {
                    // Rename from the static name quantitation.tsv to <BASE_NAME>.libra.tsv
                    File libraOutputWork = new File(_wd.getDir(), "quantitation.tsv");
                    File libraOutput = _wd.outputFile(libraOutputWork, FT_LIBRA_QUANTITATION.getName(_wd.getDir(), getJobSupport().getBaseName()));
                    proteinQuantAction.addOutput(libraOutput, LIBRA_OUTPUT_ROLE, false);
                }
            }

            // Deal with possible TPP outputs, if TPP was not XML_ONLY
            _wd.discardFile(_wd.newFile(FT_PEP_XSL));
            _wd.discardFile(_wd.newFile(FT_PEP_SHTML));
            _wd.discardFile(_wd.newFile(FT_INTERMEDIATE_PROT_XSL));
            _wd.discardFile(_wd.newFile(FT_INTERMEDIATE_PROT_SHTML));

            // We don't need the extra copy of the spectra files
            for (File spectraFile : spectraFiles)
            {
                _wd.discardFile(spectraFile);
            }

            // If no combined analysis is coming or this is the combined analysis, remove
            // the raw pepXML file(s).
            if (!getJobSupport().isFractions() || inputFiles.size() > 1)
            {
                for (File fileInput : inputFiles)
                {
                    if (!fileInput.delete())
                        getJob().warn("Failed to delete intermediate file " + fileInput);
                }
            }

            // All the programs are launched through the same XInteract command, so set the same command line on them all
            for (RecordedAction action : actions)
            {
                action.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(interactCmd, " "));
            }

            return new RecordedActionSet(actions);
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }
    }

    private boolean isSpectraProcessor(Map<String, String> params)
    {
        // Spectrum file(s) required to do quantitation.
        return (getQuantitionAlgorithm(params) != null);
    }
    
    private QuantitationAlgorithm getQuantitionAlgorithm(Map<String, String> params)
    {
        String algorithmName = params.get(ParameterNames.QUANTITATION_ALGORITHM);
        if (algorithmName == null)
            return null;
        for (QuantitationAlgorithm algorithm : QuantitationAlgorithm.values())
        {
            if (algorithm.name().equalsIgnoreCase(algorithmName))
            {
                return algorithm;
            }
        }
        return null;
    }

    private String[] getQuantitationCmd(Map<String, String> params, String pathMzXml, Pair<File, String> configFile) throws FileNotFoundException, PipelineJobException
    {
        QuantitationAlgorithm paramAlgorithm = getQuantitionAlgorithm(params);
        if (paramAlgorithm == null)
            return null;

        return paramAlgorithm.getCommand(params, pathMzXml, _factory, configFile);
    }

    public static class TestCase extends Assert
    {
        private Mockery _context;
        private Factory _factory;
        private PipelineJob _job;

        public TestCase()
        {
            _context = new Mockery();
            _context.setImposteriser(ClassImposteriser.INSTANCE);
            _factory = _context.mock(Factory.class);
            _job = _context.mock(PipelineJob.class);
        }

        @Test
        public void testQuantAlgorithmSelection()
        {
            TPPTask task = new TPPTask(_factory, _job);
            assertNull(task.getQuantitionAlgorithm(Collections.singletonMap(ParameterNames.QUANTITATION_ALGORITHM, "Q3a")));
            assertEquals(QuantitationAlgorithm.libra, task.getQuantitionAlgorithm(Collections.singletonMap(ParameterNames.QUANTITATION_ALGORITHM, "libra")));
            assertEquals(QuantitationAlgorithm.xpress, task.getQuantitionAlgorithm(Collections.singletonMap(ParameterNames.QUANTITATION_ALGORITHM, "xpress")));
        }

        @Test  @SuppressWarnings("RedundantThrows")  // IntelliJ believes the throws clause is not needed, but javac strongly disagrees
        public void testXpressCommandLine() throws PipelineJobException, FileNotFoundException
        {
            Map<String, String> params1 = new HashMap<>();
            params1.put("pipeline quantitation, residue label mass", "4.027@[,4.027@K");
            params1.put("residue, modification mass", "28.029@K,28.029@[,57.02146@C");
            params1.put("residue, potential modification mass", "0.984016@N,15.99491@M,4.027@K,4.027@[");
            assertTrue(Arrays.equals(new String[] { "-X-nn,4.027 -nK,4.027 -d\"/pathToMzXML\"" }, QuantitationAlgorithm.xpress.getCommand(params1, "/pathToMzXML", _factory, null)));

            Map<String, String> params2 = new HashMap<>();
            params2.put("pipeline quantitation, residue label mass", "4.027@A,4.027@K");
            params2.put("residue, modification mass", "28.029@K,28.029@A,57.02146@C");
            params2.put("residue, potential modification mass", "0.984016@N,15.99491@M,4.027@K,4.027@A");
            assertTrue(Arrays.equals(new String[] { "-X-nA,4.027 -nK,4.027 -d\"/pathToMzXML\"" }, QuantitationAlgorithm.xpress.getCommand(params2, "/pathToMzXML", _factory, null)));

            Map<String, String> params3 = new HashMap<>();
            params3.put("pipeline quantitation, residue label mass", "4.027@]");
            params3.put("residue, modification mass", "28.029@K,28.029@],57.02146@C");
            params3.put("residue, potential modification mass", "0.984016@N,15.99491@M,4.027@K,4.027@]");
            assertTrue(Arrays.equals(new String[] { "-X-nc,4.027 -d\"/pathToMzXML\"" }, QuantitationAlgorithm.xpress.getCommand(params3, "/pathToMzXML", _factory, null)));
        }
    }
}
