/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
import org.labkey.api.pipeline.*;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.fhcrc.cpas.exp.xml.SimpleTypeNames;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.*;
import java.util.Arrays;

/**
 * <code>TPPTask</code> PipelineJob task to run the TPP (xinteract) for further
 * analysis on a pepXML file generated by running a pepXML converter on a search
 * engine's raw output.  This task may run PeptideProphet, ProteinProphet,
 * Quantitation, and batch fractions into a single pepXML.
 */
public class TPPTask extends WorkDirectoryTask<TPPTask.Factory>
{
    public static final FileType FT_PEP_XML = new FileType(".pep.xml");
    public static final FileType FT_PROT_XML = new FileType(".prot.xml");
    public static final FileType FT_INTERMEDIATE_PROT_XML = new FileType(".pep-prot.xml");
    public static final FileType FT_TPP_PROT_XML = new FileType("-prot.xml");

    /** Map of optional file outputs from the TPP to their input role names */
    public static final Map<FileType, String> FT_OPTIONAL_AND_IGNORABLES = new HashMap<FileType, String>();

    static
    {
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

        // Additonal output from ProteinProphet, added as part of TPP 4.0 or so
        FT_OPTIONAL_AND_IGNORABLES.put(new FileType(".pep-prot.xml_senserr.txt"), "ProtSensErr");
    }

    private static final FileType FT_PEP_XSL = new FileType(".pep.xsl");
    private static final FileType FT_PEP_SHTML = new FileType(".pep.shtml");
    private static final FileType FT_INTERMEDIATE_PROT_XSL = new FileType(".pep-prot.xsl");
    private static final FileType FT_INTERMEDIATE_PROT_SHTML = new FileType(".pep-prot.shtml");

    private static final String PEPTIDE_PROPHET_ACTION_NAME = "PeptideProphet";
    private static final String PROTEIN_PROPHET_ACTION_NAME = "ProteinProphet";
    private static final String PEPTIDE_QUANITATION_ACTION_NAME = "Peptide Quantitation";
    private static final String PROTEIN_QUANITATION_ACTION_NAME = "Protein Quantitation";

    public static final String PEP_XML_INPUT_ROLE = "PepXML";
    public static final String PROT_XML_INPUT_ROLE = "ProtXML";

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
        if (FT_INTERMEDIATE_PROT_XML.isType(file))
        {
            return FT_INTERMEDIATE_PROT_XML;
        }
        if (FT_TPP_PROT_XML.isType(file))
        {
            return FT_TPP_PROT_XML;
        }
        return null;
    }

    public static File getProtXMLIntermediateFile(File dirAnalysis, String baseName)
    {
        return FT_INTERMEDIATE_PROT_XML.newFile(dirAnalysis, baseName);
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
        File[] getInteractInputFiles();

        /**
         * List of mzXML files to use as inputs to "xinteract" quantitation.
         */
        File[] getInteractSpectraFiles();

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

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new TPPTask(this, job);
        }

        public FileType[] getInputTypes()
        {
            return new FileType[] { FT_PEP_XML };
        }

        public String getStatusName()
        {
            return "ANALYSIS";
        }

        public boolean isParticipant(PipelineJob job) throws IOException, SQLException
        {
            return job.getJobSupport(JobSupport.class).isSamples();
        }

        public boolean isJobComplete(PipelineJob job)
        {
            JobSupport support = job.getJobSupport(JobSupport.class);
            String baseName = support.getBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            if (!NetworkDrive.exists(getPepXMLFile(dirAnalysis, baseName)))
                return false;

            return !support.isProphetEnabled() || NetworkDrive.exists(getProtXMLFile(dirAnalysis, baseName));
        }

        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(PEPTIDE_PROPHET_ACTION_NAME, PEPTIDE_QUANITATION_ACTION_NAME, PROTEIN_PROPHET_ACTION_NAME, PROTEIN_QUANITATION_ACTION_NAME);
        }

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

        public boolean isParticipant(PipelineJob job) throws IOException, SQLException
        {
            return job.getJobSupport(JobSupport.class).isFractions();
        }

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

    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            Map<String, String> params = getJob().getParameters();

            List<RecordedAction> actions = new ArrayList<RecordedAction>();

            // First step takes all the pepXMLs as inputs and either runs PeptideProphet (non-join) or rolls them up (join)
            RecordedAction pepXMLAction = new RecordedAction(PEPTIDE_PROPHET_ACTION_NAME);
            actions.add(pepXMLAction);

            // Set mzXML directory only if needed.
            File dirMzXml = null;

            // TODO: mzXML files may be required, and input disk space requirements
            //          may be too great to copy to a temporary directory.
            File[] inputFiles = getJobSupport().getInteractInputFiles();
            File[] inputWorkFiles = new File[inputFiles.length];
            for (File fileInput : inputFiles)
            {
                pepXMLAction.addInput(fileInput, "RawPepXML");
            }

            if (inputFiles.length > 0)
            {
                WorkDirectory.CopyingResource lock = null;
                try
                {
                    lock = _wd.ensureCopyingLock();
                    for (int i = 0; i < inputFiles.length; i++)
                        inputWorkFiles[i] = _wd.inputFile(inputFiles[i], false);

                    if (isSpectraProcessor(params))
                    {
                        File[] spectraFiles = getJobSupport().getInteractSpectraFiles();
                        for (int i = 0; i < spectraFiles.length; i++)
                        {
                            spectraFiles[i] = _wd.inputFile(spectraFiles[i], false);
                            if (dirMzXml == null)
                                dirMzXml = spectraFiles[i].getParentFile();
                        }
                    }
                }
                finally
                {
                    if (lock != null) { lock.release(); }
                }
            }

            RecordedAction peptideQuantAction = null;
            String[] quantParams = null;

            if (dirMzXml != null)
            {
                quantParams = getQuantitationCmd(params, _wd.getRelativePath(dirMzXml));
                if (quantParams != null)
                {
                    peptideQuantAction = new RecordedAction(PEPTIDE_QUANITATION_ACTION_NAME);
                    String algorithm = getQuantitionAlgorithm(params);
                    peptideQuantAction.setDescription(algorithm + " " + peptideQuantAction.getName());
                    peptideQuantAction.addParameter(new RecordedAction.ParameterType("Quantitation algorithm", "terms.labkey.org#QuantitationAlgorithm", SimpleTypeNames.STRING), algorithm);
                    actions.add(peptideQuantAction);
                }
            }

            File fileWorkPepXML = _wd.newFile(FT_PEP_XML);

            String ver = getTPPVersion(getJob());
            List<String> interactCmd = new ArrayList<String>();
            String xinteractPath = PipelineJobService.get().getExecutablePath("xinteract", "tpp", ver);
            File xinteractFile = new File(xinteractPath);
            interactCmd.add(xinteractPath);

            if (!getJobSupport().isProphetEnabled())
            {
                interactCmd.add("-nP"); // no Prophet analysis
            }
            else
            {
                StringBuffer prophetOpts = new StringBuffer("-Opt");
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

                String paramMinProb = params.get("pipeline prophet, min probability");
                if (paramMinProb == null || paramMinProb.length() == 0)
                    paramMinProb = params.get("pipeline prophet, min peptide probability");
                if (paramMinProb != null && paramMinProb.length() > 0)
                    interactCmd.add("-p" + paramMinProb);
                
                paramMinProb = params.get("pipeline prophet, min protein probability");
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
                    String algorithm = getQuantitionAlgorithm(params);
                    proteinQuantAction.setDescription(algorithm + " " + proteinQuantAction.getName());
                    proteinQuantAction.addParameter(new RecordedAction.ParameterType("Quantitation algorithm", "terms.labkey.org#QuantitationAlgorithm", SimpleTypeNames.STRING), algorithm);
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
                String path = "";
                for (String envName : builder.environment().keySet())
                {
                    // Not sure what the casing for that PATH environment variable is going to be, so check in
                    // case insensitive way
                    if ("PATH".equalsIgnoreCase(envName))
                    {
                        pathEnvName = envName;
                        path = builder.environment().get(pathEnvName);
                        break;
                    }
                }
                path = xinteractFile.getParentFile().getAbsolutePath() + File.pathSeparatorChar + path;
                builder.environment().put(pathEnvName, path);
            }
            builder.environment().put("WEBSERVER_ROOT", StringUtils.trimToEmpty(new File(xinteractPath).getParent()));
            getJob().runSubProcess(builder, _wd.getDir());

            WorkDirectory.CopyingResource lock = null;
            try
            {
                lock = _wd.ensureCopyingLock();
                File filePepXML = _wd.outputFile(fileWorkPepXML);

                // Set up the first step with the right outputs
                pepXMLAction.addOutput(filePepXML, PEP_XML_INPUT_ROLE, false);

                File fileProtXML = null;

                if (getJobSupport().isProphetEnabled())
                {
                    // If we ran ProteinProphet, set up a step with the right inputs and outputs
                    File fileWorkProtXML = _wd.newFile(FT_INTERMEDIATE_PROT_XML);
                    if (!NetworkDrive.exists(fileWorkProtXML))
                    {
                        // TPP 4.0 changed the name of the output file from .pep-prot.xml to .prot.xml. If we can't
                        // find a file with the old name, try the new one
                        _wd.discardFile(fileWorkProtXML);
                        fileWorkProtXML = _wd.newFile(FT_PROT_XML);
                    }

                    fileProtXML = _wd.outputFile(fileWorkProtXML, FT_PROT_XML.getName(_wd.getDir(), getJobSupport().getBaseName()));

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
            }
            finally
            {
                if (lock != null) { lock.release(); }
            }

            // Deal with possible TPP outputs, if TPP was not XML_ONLY
            _wd.discardFile(_wd.newFile(FT_PEP_XSL));
            _wd.discardFile(_wd.newFile(FT_PEP_SHTML));
            _wd.discardFile(_wd.newFile(FT_INTERMEDIATE_PROT_XSL));
            _wd.discardFile(_wd.newFile(FT_INTERMEDIATE_PROT_SHTML));

            // If no combined analysis is coming or this is the combined analysis, remove
            // the raw pepXML file(s).
            if (!getJobSupport().isFractions() || inputFiles.length > 1)
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
    
    private String getQuantitionAlgorithm(Map<String, String> params)
    {
        String paramAlgorithm = params.get("pipeline quantitation, algorithm");
        if (paramAlgorithm == null)
            return null;
        if (!"q3".equalsIgnoreCase(paramAlgorithm) && !"xpress".equalsIgnoreCase(paramAlgorithm))
            return null;    // CONSIDER: error message.
        return paramAlgorithm;
    }

    private String[] getQuantitationCmd(Map<String, String> params, String pathMzXml) throws FileNotFoundException
    {
        String paramAlgorithm = getQuantitionAlgorithm(params);
        if (paramAlgorithm == null)
            return null;

        List<String> quantOpts = new ArrayList<String>();

        String paramQuant = params.get("pipeline quantitation, residue label mass");
        if (paramQuant != null)
            getLabelOptions(paramQuant, quantOpts);

        paramQuant = params.get("pipeline quantitation, mass tolerance");
        if (paramQuant != null)
            quantOpts.add("-m" + paramQuant);

        paramQuant = params.get("pipeline quantitation, heavy elutes before light");
        if (paramQuant != null)
            if("yes".equalsIgnoreCase(paramQuant))
                quantOpts.add("-b");

        paramQuant = params.get("pipeline quantitation, fix");
        if (paramQuant != null)
        {
            if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-H");
            else if ("light".equalsIgnoreCase(paramQuant))
                quantOpts.add("-L");
        }

        paramQuant = params.get("pipeline quantitation, fix elution reference");
        if (paramQuant != null)
        {
            String refFlag = "-f";
            if ("peak".equalsIgnoreCase(paramQuant))
                refFlag = "-F";
            paramQuant = params.get("pipeline quantitation, fix elution difference");
            if (paramQuant != null)
                quantOpts.add(refFlag + paramQuant);
        }

        paramQuant = params.get("pipeline quantitation, metabolic search type");
        if (paramQuant != null)
        {
            if ("normal".equalsIgnoreCase(paramQuant))
                quantOpts.add("-M");
            else if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-N");
        }

        quantOpts.add("-d\"" + pathMzXml + "\"");

        if ("xpress".equals(paramAlgorithm))
            return new String[] { "-X" + StringUtils.join(quantOpts.iterator(), ' ') };

        String paramMinPP = params.get("pipeline quantitation, min peptide prophet");
        if (paramMinPP != null)
            quantOpts.add("--minPeptideProphet=" + paramMinPP);
        String paramMaxDelta = params.get("pipeline quantitation, max fractional delta mass");
        if (paramMaxDelta != null)
            quantOpts.add("--maxFracDeltaMass=" + paramMaxDelta);
        String paramCompatQ3 = params.get("pipeline quantitation, q3 compat");
        if ("yes".equalsIgnoreCase(paramCompatQ3))
            quantOpts.add("--compat");

        String ver = params.get("pipeline, msinspect ver");

        // NOTE: Because the java command-line for msInspect gets passed as a
        // single argument to xinteract, it cannot support spaces in any
        // of its arguments. If the path to java.exe contains spaces,
        // then just use "java", and rely on it being on the path.
        String javaPath = PipelineJobService.get().getJavaPath();
        if (javaPath.indexOf(' ') >= 0)
            javaPath = "java";
        return new String[] {
            "-C1" + javaPath + " " + 
                (_factory.getJavaVMOptions() == null ? "-Xmx1024M" : _factory.getJavaVMOptions())
                + " -jar " + PipelineJobService.get().getJarPath("viewerApp.jar", "msinspect", ver)
                + " --q3 " + StringUtils.join(quantOpts.iterator(), ' ')
                ,
                "-C2Q3ProteinRatioParser"
        };
    }

    private void getLabelOptions(String paramQuant, List<String> quantOpts)
    {
        String[] quantSpecs = paramQuant.split(",");
        for (String spec : quantSpecs)
        {
            String[] specVals = spec.split("@");
            if (specVals.length != 2)
                continue;
            String mass = specVals[0].trim();
            String aa = specVals[1].trim();
            quantOpts.add("-n" + aa + "," + mass);
        }
    }
}
