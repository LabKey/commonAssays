/*
 * Copyright (c) 2007-2016 LabKey Corporation
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
package org.labkey.ms2.pipeline.sequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.ToolExecutionException;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.pipeline.cmd.TaskPath;
import org.labkey.api.reader.Readers;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.writer.PrintWriters;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.FastaCheckTask;
import org.labkey.ms2.pipeline.client.ParameterNames;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * <code>SequestSearchTask</code>
 */
public class UWSequestSearchTask extends AbstractMS2SearchTask<UWSequestSearchTask.Factory>
{
    private static final String SEQUEST_PARAMS = "sequest.params";
    private static final String MAKE_DB_PARAMS = "makedb.params";

    private static final String SEQUEST_ACTION_NAME = "Sequest Search";
    private static final String SEQUEST_DECOY_ACTION_NAME = "Sequest Decoy Search";
    private static final String MAKEDB_ACTION_NAME = "MakeDB";
    public static final String FASTA_DECOY_INPUT_ROLE = "DecoyFASTA";

    public static final String MASS_TYPE_PARENT = "sequest, mass_type_parent";

    public static final String USE_INDEX_PARAMETER_NAME = "pipeline, use index";
    public static final String INDEX_FILE_NAME_PARAMETER_NAME = "pipeline, index file name";

    public static final FileType INDEX_FILE_TYPE = new FileType(".hdr");
    public static final FileType SEQUEST_OUTPUT_FILE_TYPE = new FileType(".sqt");
    public static final FileType SEQUEST_DECOY_OUTPUT_FILE_TYPE = new FileType(".decoy.sqt");

    private static final Object INDEX_LOCK = new Object();

    public static class Factory extends AbstractSequestSearchTaskFactory
    {
        public Factory()
        {
            super(UWSequestSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new UWSequestSearchTask(this, job);
        }

        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(MAKEDB_ACTION_NAME, SEQUEST_ACTION_NAME, SEQUEST_DECOY_ACTION_NAME);
        }
    }

    protected UWSequestSearchTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    public SequestPipelineJob getJob()
    {
        return (SequestPipelineJob)super.getJob();
    }

    private File getIndexFileWithoutExtension() throws PipelineJobException
    {
        File fastaFile = getJob().getSequenceFiles()[0];
        File fastaRoot = getJob().getSequenceRootDirectory();

        Map<String, String> params = getJob().getParameters();
        String indexFileName = params.get(INDEX_FILE_NAME_PARAMETER_NAME);
        if (indexFileName == null)
        {
            // Build one based on a CRC of the parameters that define an index file
            StringBuilder sb = new StringBuilder();
            sb.append("Enzyme-");
            sb.append(params.get(ParameterNames.ENZYME));
            sb.append(".MinParentMH-");
            sb.append(params.get(AbstractMS2SearchTask.MINIMUM_PARENT_M_H));
            sb.append(".MaxParentMH-");
            sb.append(params.get(AbstractMS2SearchTask.MAXIMUM_PARENT_M_H));
            sb.append(".MaxMissedCleavages-");
            sb.append(params.get(AbstractMS2SearchTask.MAXIMUM_MISSED_CLEAVAGE_SITES));
            sb.append(".MassTypeParent-");
            sb.append(params.get(UWSequestSearchTask.MASS_TYPE_PARENT));
            sb.append(".StaticMod-");
            sb.append(params.get(ParameterNames.STATIC_MOD));

            CRC32 crc = new CRC32();
            crc.update(toBytes(sb.toString()));

            indexFileName = fastaFile.getName() + "_" + crc.getValue();
        }

        String relativeDirPath = FileUtil.relativePath(fastaFile.getParentFile().getPath(), fastaRoot.getPath());
        File indexDir;
        if (_factory.getIndexRootDir() == null)
        {
            indexDir = new File(new File(fastaRoot, relativeDirPath), "index");
        }
        else
        {
            indexDir = new File(new File(_factory.getIndexRootDir()), relativeDirPath);
        }
        indexDir.mkdirs();
        if (!indexDir.isDirectory())
        {
            throw new PipelineJobException("Failed to create index directory " + indexDir);
        }

        return new File(indexDir, indexFileName);
    }

    private static byte[] toBytes(String s)
    {
        return s == null ? new byte[] { 0 } : s.getBytes(StringUtilsLabKey.DEFAULT_CHARSET);
    }

    private boolean usesIndex()
    {
        Map<String, String> params = getJob().getParameters();
        String indexUsage = params.get(USE_INDEX_PARAMETER_NAME);
        return "true".equalsIgnoreCase(indexUsage) || "1".equalsIgnoreCase(indexUsage) || "yes".equalsIgnoreCase(indexUsage);
    }

    private List<File> getFASTAOrIndexFiles(List<RecordedAction> actions) throws PipelineJobException
    {
        if (!usesIndex())
        {
            return Arrays.asList(getJob().getSequenceFiles());
        }

        File indexFileBase = getIndexFileWithoutExtension();
        File indexFile = new File(indexFileBase.getParentFile(), indexFileBase.getName() + INDEX_FILE_TYPE.getDefaultSuffix());

        synchronized (INDEX_LOCK)
        {
            if (!indexFile.exists())
            {
                assert getJob().getSequenceFiles().length == 1 : "Only one FASTA is supported when using indices";

                getJob().setStatus("CREATING FASTA INDEX");
                getJob().info("Creating a FASTA index for " + getJob().getSequenceFiles()[0] + " as " + indexFileBase);

                // Create a makedb.params to control the index creation
                File fileWorkParams = _wd.newFile(MAKE_DB_PARAMS);
                UWSequestParamsBuilder builder = new UWSequestParamsBuilder(getJob().getParameters(), getJob().getSequenceRootDirectory(), SequestParams.Variant.makedb, null);
                builder.initXmlValues();
                builder.writeFile(fileWorkParams);

                // Invoke makedb
                List<String> args = new ArrayList<>();
                File makeDBExecutable = new File(_factory.getSequestInstallDir(), "makedb");
                args.add(makeDBExecutable.getAbsolutePath());
                args.add("-O" + indexFileBase);
                args.add("-P" + fileWorkParams.getAbsolutePath());
                ProcessBuilder pb = new ProcessBuilder(args);

                // In order to find sort.exe, use the Sequest directory as the working directory
                File dir = makeDBExecutable.getParentFile();
                getJob().runSubProcess(pb, dir);

                RecordedAction action = new RecordedAction(MAKEDB_ACTION_NAME);
                action.addInput(getJob().getSequenceFiles()[0], "FASTA");
                action.addInput(fileWorkParams, "MakeDB Params");
                action.addOutput(indexFile, "FASTA Index", false);
                action.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(args, " "));

                actions.add(action);

                try
                {
                    _wd.outputFile(fileWorkParams);
                }
                catch (IOException e)
                {
                    throw new PipelineJobException(e);
                }

                // Set the status back to the search
                getJob().setStatus("SEARCH RUNNING");
            }
        }

        return Collections.singletonList(indexFile);
    }

    @NotNull
    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            List<RecordedAction> actions = new ArrayList<>();
            // Copy so that we can add our own values
            Map<String, String> params = new HashMap<>(getJob().getParameters());
            params.put("list path, sequest parameters", SEQUEST_PARAMS);
            params.put("search, useremail", params.get(PipelineJob.PIPELINE_EMAIL_ADDRESS_PARAM));
            params.put("search, username", "CPAS User");

            List<File> sequenceFiles = getFASTAOrIndexFiles(actions);

            File fileMzXML = _factory.findInputFile(getJob());
            File fileMzXMLWork = _wd.inputFile(fileMzXML, true);

            // Write out sequest.params file
            File fileWorkParams = _wd.newFile(SEQUEST_PARAMS);

            List<File> decoySequenceFiles = FastaCheckTask.getDecoySequenceFiles(getJob());

            File sequestLogFileWork = SEQUEST_OUTPUT_FILE_TYPE.getFile(_wd.getDir(), getJob().getBaseName());

            _wd.newFile(sequestLogFileWork.getName());

            List<String> sequestArgs = performSearch(_wd.getDir(), params, sequenceFiles, fileMzXMLWork, fileWorkParams, sequestLogFileWork);
            File decoyResults = null;
            if (!decoySequenceFiles.isEmpty())
            {
                File decoyDir = new File(_wd.getDir(), "decoy");
                decoyDir.mkdir();
                getJob().getLogger().info("Performing a decoy search with " + decoySequenceFiles);
                File fileWorkDecoyParams = new File(decoyDir, "sequest.params");
                File decoySubResults = SEQUEST_OUTPUT_FILE_TYPE.getFile(decoyDir, getJob().getBaseName());
                performSearch(decoyDir, params, decoySequenceFiles, fileMzXMLWork, fileWorkDecoyParams, decoySubResults);

                decoyResults = SEQUEST_DECOY_OUTPUT_FILE_TYPE.getFile(_wd.getDir(), getJob().getBaseName());
                getJob().getLogger().info("Copying decoy results from " + decoySubResults + " to " + decoyResults + ", file is " + decoySubResults.length());
                FileUtil.copyFile(decoySubResults, decoyResults);
                FileUtil.deleteDir(decoyDir);
            }

            try (WorkDirectory.CopyingResource ignored = _wd.ensureCopyingLock())
            {
                RecordedAction sequestAction = new RecordedAction(SEQUEST_ACTION_NAME);
                sequestAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(sequestArgs, " "));
                sequestAction.addOutput(_wd.outputFile(fileWorkParams), "SequestParams", true);
                sequestAction.addOutput(_wd.outputFile(sequestLogFileWork), "SequestResults", false);
                for (File file : sequenceFiles)
                {
                    sequestAction.addInput(file, FASTA_INPUT_ROLE);
                }
                for (File file : decoySequenceFiles)
                {
                    sequestAction.addInput(file, FASTA_DECOY_INPUT_ROLE);
                }
                if (decoyResults != null)
                {
                    sequestAction.addOutput(_wd.outputFile(decoyResults), "SequestDecoyResults", false);
                }
                sequestAction.addInput(fileMzXML, SPECTRA_INPUT_ROLE);
                _wd.discardFile(fileMzXMLWork);
                _wd.acceptFilesAsOutputs(Collections.emptyMap(), sequestAction);

                actions.add(sequestAction);
            }

            return new RecordedActionSet(actions);
        }
        catch(IOException e)
        {
            throw new PipelineJobException(e);
        }
    }

    private static final Pattern SEQUEST_VERSION = Pattern.compile(".*(\\d\\d\\d\\d\\.\\d+\\.\\d+).*");

    private List<String> performSearch(File workingDir, Map<String, String> params, List<File> sequenceFiles, File fileMzXMLWork, File paramsFile, File resultsFile)
            throws IOException, PipelineJobException
    {
        UWSequestParamsBuilder builder = new UWSequestParamsBuilder(params, getJob().getSequenceRootDirectory(), SequestParams.Variant.uwsequest, sequenceFiles);
        builder.initXmlValues();
        builder.writeFile(paramsFile);

        String version = getJob().getParameters().get("sequest, version");
        String sequestPath = PipelineJobService.get().getExecutablePath("sequest." + PipelineJobService.VERSION_SUBSTITUTION, _factory.getSequestInstallDir(), null, version, getJob().getLogger());
        if (sequestPath.endsWith("/sequest"))
        {
            // Sequest versions have the binaries themselves being renamed, not the parent directories,
            // so hack off the trailing "/sequest" on the path
            sequestPath = sequestPath.substring(0, sequestPath.length() - "/sequest".length());
        }
        String sequestVersion = determineSequestVersion(sequestPath, workingDir);
        if (sequestVersion != null)
        {
            getJob().info("Running Sequest " + sequestVersion);
        }

        // Perform Sequest search
        List<String> sequestArgs = new ArrayList<>();
        sequestArgs.add(sequestPath);
        sequestArgs.addAll(_factory.getSequestOptions());
        sequestArgs.add(FileUtil.relativize(workingDir, fileMzXMLWork, false));
        ProcessBuilder sequestPB = new ProcessBuilder(sequestArgs);
        try (Writer writer = PrintWriters.getPrintWriter(resultsFile))
        {
            writeParams(writer, paramsFile, getJob().getLogger(), getJob().getSequenceFiles()[0], sequestVersion);
        }
        getJob().runSubProcess(sequestPB, workingDir, resultsFile, 10, true);
        return sequestArgs;
    }

    private String determineSequestVersion(String sequestPath, File workingDir) throws PipelineJobException, IOException
    {
        File versionFile = new File(workingDir, "sequest.version");
        ProcessBuilder versionPB = new ProcessBuilder(sequestPath);
        try
        {
            getJob().runSubProcess(versionPB, workingDir, versionFile, 0, false);
        }
        catch (ToolExecutionException ignored)
        {
            // Sequest returns a non-zero exit code when invoked without arguments
        }

        if (versionFile.exists())
        {
            try (FileInputStream headerIn = new FileInputStream(versionFile))
            {
                return parseSequestVersion(Readers.getReader(headerIn));
            }
            finally
            {
                versionFile.delete();
            }
        }
        return null;
    }

    private String parseSequestVersion(Reader innerReader) throws IOException
    {
        BufferedReader reader = new BufferedReader(innerReader);
        String line;
        while ((line = reader.readLine()) != null)
        {
            Matcher matcher = SEQUEST_VERSION.matcher(line);
            if (matcher.matches())
            {
                return matcher.group(1).replace(".", "");
            }
        }

        return null;
    }

    private static final String version = "H\tSQTGenerator SEQUEST\nH\tSQTGeneratorVersion\t2.7\n";
    private static final String version2 = "H\tComment\tSEQUEST was written by J Eng and JR Yates, III\n";
    private static final String version3 = "H\tComment\tSEQUEST ref. J. Am. Soc. Mass Spectrom., 1994, v. 4, p. 976\n";
    private static final String version4 = "H\tComment\tSEQUEST is licensed to Finnigan Corp.\n";
    private static final String version5 = "H\tComment\tInvoked through LabKey Server Pipeline\n";
    private static final String version6 = "H\tComment\tHeader output code adapted from run_ms2, written by Rovshan Sadygov\n";
    private static final String credit = " Molecular Biotechnology, Univ. of Washington, J.Eng/J.Yates";
    private static final String license = " Licensed to Finnigan Corp., A Division of ThermoQuest Corp.";


    private static void writeParams(Writer writer, File paramsFile, Logger logger, File fastaFile, String sequestVersion) throws IOException
    {
        int ixcorr = 0;
        float[] aa_mass = new float[256];
        String line, szDbase = "", szDate;
//          String szTemp1, szTemp2, szIonSeries;
        String szIonSeries = "";
//          String szTemp3, szDisplay;
        String szDisplay = "";
//          String szTemp4, szTemp6;
        List<Pair<String, String>> diffMods = new ArrayList<>();
        String diffModCharacters = "*#@^~$";
        float mdif1, mdif2, mdif3, mstat, ion_cutoff;
        String szDiff1, szDiff2, szDiff3, sDif1;
        String sDif2, sDif3;
        String szStatMode = "", szMass_Accuracy = "";
        String szMass_AccPrecursor = "", szMass_AccFr = "";
        String szEnzymeSpec = "";
        String PIsotope = "", FIsotope = "", szIons = "", szMaxDiffMod = "";
        String szOutPutLines = "";
        long N_AA, N_dup, N_rm_pr, N_rd_fr;
        long N_prot, flag_stat, flag_diff, N_a, N_b, N_y;
        long N_enz, N_diff_mod;
        N_prot = N_AA = flag_stat = flag_diff = N_dup = N_rm_pr = 0;
        mdif1 = mdif2 = mdif3 = mstat = ion_cutoff = 0.0f;
        N_rd_fr = N_a = N_b = N_y = N_enz = N_diff_mod = 0;

        try (BufferedReader reader = Readers.getReader(paramsFile))
        {
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.startsWith("database_name"))
                    szDbase = getSingleValue(line);
                else if (line.startsWith("peptide_mass_tolerance"))
                {
                    String szTemp3 = getSingleValue(line);
                    szMass_Accuracy = String.format(" ~ %s, ", szTemp3);
                    szMass_AccPrecursor = szTemp3;
                }
                else if (line.startsWith("xcorr_mode"))
                {
                    String szTemp3 = getSingleValue(line);
                    ixcorr = Integer.parseInt(szTemp3);
                }
                else if (line.startsWith("ion_series"))
                {
                    String[] values = line.split("\\s");
                    if (values.length < 6)
                    {
                        logger.error("Error: ion_series");
                    }
                    else
                    {
                        try
                        {
                            N_a = Integer.parseInt(values[2]);
                            N_b = Integer.parseInt(values[3]);
                            N_y = Integer.parseInt(values[4]);

                            if (N_a == 0)
                                szIonSeries = "IonSeries nA";
                            else if (N_a == 1)
                                szIonSeries = "IonSeries A";

                            if (N_b == 0)
                                szIonSeries += "nB";
                            if (N_b == 1)
                                szIonSeries += "B";
                            if (N_y == 0)
                                szIonSeries += "nY";
                            if (N_y == 1)
                                szIonSeries += "Y";
                            szIonSeries += line.charAt(13);
                            szIons = line.substring(13);
                        }
                        catch (NumberFormatException e)
                        {
                            logger.error("Error: ion_series");
                        }
                    }

                }
                /*else if(strstr(line, "fragment_ion_tolerance") != null &&
                  line[0] == 'f') { */
                else if (line.startsWith("fragment_ion_tolerance"))
                {
                    String szTemp3 = getSingleValue(line);
                    szMass_Accuracy += String.format("fragment tol = %s, ", szTemp3);
                    szMass_AccFr = szTemp3;
                }
                else if (line.startsWith("num_output_lines"))
                {
                    String szTemp3 = getSingleValue(line);
                    szDisplay = String.format(" display top %s", szTemp3);
                    szOutPutLines = szTemp3;
                }
                else if (line.startsWith("num_description_lines"))
                {
                    String szTemp3 = getSingleValue(line);
                    szDisplay += String.format("/%s,", szTemp3);
                }
                else if (line.startsWith("print_duplicate_references"))
                {
                    try
                    {
                        N_dup = Integer.parseInt(getSingleValue(line));
                    }
                    catch (NumberFormatException e)
                    {
                        logger.error("Error: print_duplicate_references");
                    }
                }
                else if (line.startsWith("enzyme_number"))
                {
                    try
                    {
                        N_enz = Integer.parseInt(getSingleValue(line));
                    }
                    catch (NumberFormatException e)
                    {
                        logger.error("Error: enzyme_number");
                    }
                }
                /*
                else if(strstr(line, "diff_search_options") != null) {
                  if(8 != sscanf(line, "%s%s%s%s%s%s%s%s", szTemp1,szTemp2,szTemp3,
                         szTemp4,szTemp5,szTemp6,szTemp7,szTemp8))
                printf("Did not read the diffmod line correctly\n");
                  if(1 ==  COPY_DIFFMODE(szTemp3, &mdif1,szSign1)) {
                flag_diff = 1;
                sprintf(szDiff1, szTemp4);
                  }
                  if(1 ==  COPY_DIFFMODE(szTemp5, &mdif2,szSign2)) {
                flag_diff = 1;
                sprintf(szDiff2, szTemp6);
                  }
                  if(1 ==  COPY_DIFFMODE(szTemp7, &mdif3,szSign3)) {
                flag_diff = 1;
                sprintf(szDiff3, szTemp8);
                  }
                }
                */
                /*JDE 9/16/2011 changing this to read an arbitrary number of diff mods*/
                else if (line.startsWith("diff_search_options"))
                {
                    processDiffLine(line, diffModCharacters, diffMods, logger);
                }
                else if (line.startsWith("max_num_differential_AA_per_mod"))
                {
                    try
                    {
                        N_diff_mod = Integer.parseInt(getSingleValue(line));
                    }
                    catch (NumberFormatException e)
                    {
                        logger.error("Problem: max_num_differential_AA_per_mod");
                    }
                    szMaxDiffMod = String.format("%d", N_diff_mod);
                }
                else if (line.startsWith("diff_search_count"))
                {

                    String[] ss = getValues(line);
                    List<String> diffSearchCounts = new ArrayList<>();
                    szMaxDiffMod = "";
                    for (String s : ss)
                    {
                        if (s.isEmpty() || !Character.isDigit(s.charAt(0)))
                        {
                            break;
                        }
                        diffSearchCounts.add(s);
                    }
                    for (int index = 0; index < diffSearchCounts.size() - 1; ++index)
                    {
                        szMaxDiffMod += diffSearchCounts.get(index);
                        szMaxDiffMod += ",";
                    }
                    szMaxDiffMod += diffSearchCounts.get(diffSearchCounts.size() - 1);
                }
                else if (line.startsWith("nucleotide_reading_frame"))
                {
                    try
                    {
                        N_rd_fr = Integer.parseInt(getSingleValue(line));
                    }
                    catch (NumberFormatException e)
                    {
                        logger.error("Problem: nucleotide_reading_frame\n");
                    }
                }
                else if (line.startsWith("mass_type_parent"))
                {
                    String szTemp3 = getSingleValue(line);
                    if ("0".equals(szTemp3))
                    {
                        szMass_Accuracy += "AVG/";
                        PIsotope = "AVG";
                    }
                    else if ("1".equals(szTemp3))
                    {
                        szMass_Accuracy += "MONO/";
                        PIsotope = "MONO";
                    }
                    else
                    {
                        logger.warn("Did not recognize parent mass type\n");
                    }
                }
                else if (line.startsWith("remove_precursor_peak"))
                {
                    try
                    {
                        N_rm_pr = Integer.parseInt(getSingleValue(line));
                    }
                    catch (NumberFormatException e)
                    {
                        logger.error("Error: remove_precursor_peak\n");
                    }
                }
                else if (line.startsWith("mass_type_fragment"))
                {
                    String szTemp3 = getSingleValue(line);
                    if ("0".equals(szTemp3))
                    {
                        szMass_Accuracy += "AVG\n";
                        FIsotope = "AVG";
                    }
                    else if ("1".equals(szTemp3))
                    {
                        szMass_Accuracy += "MONO\n";
                        FIsotope = "MONO";
                    }
                    else
                    {
                        logger.error("Did not recognize fragment mass type\n");
                    }
                }
                else if (line.startsWith("ion_cutoff_percentage"))
                {
                    try
                    {
                        ion_cutoff = Float.parseFloat(getSingleValue(line));
                    }
                    catch (NumberFormatException e)
                    {
                        logger.error("Error: ion_cutoff_percentage");
                    }
                    ion_cutoff = 100.0f * ion_cutoff;
                    szDisplay += String.format(" ion %% = %.1f, CODE = %d%d%d0", ion_cutoff, N_dup, N_rm_pr, N_rd_fr);
                }
                else if (line.startsWith("add_"))
                {
                    /* looks like on each ornithine, we get the line cut off at 116.\0 followed immediately by
                  the Alg-Display line*/
                    try
                    {
                        mstat = Float.parseFloat(getSingleValue(line));
                    }
                    catch (NumberFormatException e)
                    {
                        logger.warn("Did not read all of the StatMode line\n");
                    }
                    if (0.0 != mstat)
                    {
                        flag_stat = 1;
                        szStatMode += String.format("H\tStaticMod\t%c=%.3f\n", line.charAt(4), mstat + aa_mass[line.charAt(4)]);
                    }
                }
                else if (line.startsWith("[SEQUEST_ENZYME_INFO]"))
                {
                    //sprintf(szTemp1,"%d", N_enz);
                    while ((line = reader.readLine()) != null)
                    {
                        String[] values = line.split("\\s+");
                        if (values.length < 4)
                            logger.error("Enzyme Reader only got " + values.length + " values instead of the expected 4");
                        else
                        {
                            if (values[0].length() > 1)
                            {
                                values[0] = values[0].substring(0, 1);
                            }
                            if (N_enz == Integer.parseInt(values[0]))
                            {
                                szEnzymeSpec = values[1];
                                break;
                            }
                        }
                        /*	  if(line.startsWith(szTemp1, strlen(szTemp1))) { */
                        /*
                     if(strncmp(line, szTemp1, strlen(szTemp1)) == 0) {
                       if(3 != sscanf(line, "%s%s%s", szTemp2,szTemp3,szTemp4))
                         logger.error("Error: Enzyme\n");
                       sprintf(szEnzymeSpec, "Enzyme:%s (%d)", szTemp3, N_enz);
                       break;
                       } */
                    }
                }
            }
            //exit(0);

            try (BufferedReader fastaReader = Readers.getReader(fastaFile))
            {
                while ((line = fastaReader.readLine()) != null)
                {
                    if (line.startsWith(">"))
                        N_prot++;
                    else
                        N_AA += line.length() - 1;
                }
            }

            if (0 == ixcorr)
            {
                writer.write(String.format("%s%s", version, version2));
                if (sequestVersion != null)
                {
                    writer.write("H\tSEQUESTVersion\t" + sequestVersion + "\n");
                }
                writer.write(version3);
                writer.write("H\tComment\tSEQUEST ref. Eng,J.K.; McCormack A.L.; Yates J.R.\n");
                writer.write(version4);
                writer.write(String.format("%s%s", version5, version6));
            }
            else if (1 == ixcorr)
            {
                writer.write("H\tSQTGenerator\tEE-normalized SEQUEST\n");
                writer.write(String.format("%s%s", version2, version3));
                writer.write("H\tComment\tSEQUEST ref. Eng,J.K.; McCormack A.L.; Yates J.R.\n");
                writer.write(version4);
                writer.write("H\tComment\tNormalized SEQUEST ref. MacCoss,M.J.; Wu C.C; Yates J.R.\n");
                writer.write("H\tComment\tNormalized SEQUEST ref. Anal. Chem. 2002, v. 74, p. 5593\n");
                writer.write(String.format("%s%s", version5, version6));
            }
            else if (2 == ixcorr)
            {
                writer.write("H\tSQTGenerator\tET-normalized SEQUEST\n");
                writer.write(version3);
                writer.write("H\tComment\tSEQUEST ref. Eng,J.K.; McCormack A.L.; Yates J.R.\n");
                writer.write(version4);
                writer.write("H\tComment\tNormalized SEQUEST ref. Sadygov R.G.; Yates J.R.\n");
                writer.write("H\tComment\tNormalized SEQUEST ref. to be published\n");
                writer.write(String.format("%s%s", version5, version6));
            }

            // TODO - use real times?
            szDate = DateUtil.formatDateTime(new Date(), "M/d/y, H:mm a");
            writer.write(String.format("H\tStartTime %s\n", szDate));
            //          strftime(szDate, 22, "%m/%d/%Y, %I:%M %p", localtime(&tEndTime));
            writer.write(String.format("H\tEndTime %s\n", szDate));
            writer.write(String.format("H\tDatabase\t%s\n", szDbase));
            writer.write(String.format("H\tDBSeqLength\t%d\n", N_AA));
            writer.write(String.format("H\tDBLocusCount\t%d\n", N_prot));
            writer.write(String.format("H\tPrecursorMasses\t%s\n", PIsotope));
            writer.write(String.format("H\tFragmentMasses\t%s\n", FIsotope));
            writer.write(String.format("H\tAlg-PreMassTol\t%s\n", szMass_AccPrecursor));
            writer.write(String.format("H\tAlg-FragMassTol\t%s\n", szMass_AccFr));
            writer.write(String.format("H\tAlg-XCorrMode\t%d\n", ixcorr));
            //writer.write(" # amino acids = %ld, # proteins = %d, %s\n%s%s%s\n",
            //  N_AA, N_prot, szDbase,szMass_Accuracy,szIonSeries,
            //  szDisplay);
            line = " ";
            if (diffMods.size() > 0)
            {
                line += String.format("max_diff_mod = %d\n ", N_diff_mod);
                /*
                if(0.0 != mdif1) {
                  sprintf(line+strlen(line),"(%s* %s%.3f) ",szDiff1, szSign1,mdif1);
                  sprintf(sDif1,"%s*=%s%.3f ",szDiff1, szSign1,mdif1);
                }
                if(0.0 != mdif2) {
                  sprintf(line+strlen(line),"(%s# %s%.3f) ",szDiff2, szSign2,mdif2);
                  sprintf(sDif2,"%s#=%s%.3f ",szDiff2, szSign2,mdif2);
                }
                if(0.0 != mdif3) {
                  sprintf(line+strlen(line),"(%s@ %s%.3f) ",szDiff3, szSign3,mdif3);
                  sprintf(sDif3,"%s@=%s%.3f ",szDiff3, szSign3,mdif3);
                }
                //writer.write("%s", line);
                */
            }
            if (szStatMode != null && szStatMode.length() > 0)
                writer.write(szStatMode);
            //writer.write("H\tStaticMod\t%s\n",szStatMode);
            for (Pair<String, String> p : diffMods)
            {
                writer.write(String.format("H\tDiffMod\t%s=%s\n", p.first, p.second));
            }
            /*
            if(0 != mdif1)
              writer.write("H\tDiffMod\t%s\n",sDif1);
            if(0 != mdif2)
              writer.write("H\tDiffMod\t%s\n",sDif2);
            if(0 != mdif3)
              writer.write("H\tDiffMod\t%s\n",sDif3);
            */
            if (diffMods.size() > 0)
            {
                writer.write(String.format("H\tAlg-MaxDiffMod\t%s\n", szMaxDiffMod));
            }
            /*
            if(0 != mdif1 || 0 != mdif2 || 0 != mdif3)
              writer.write("H\tAlg-MaxDiffMod\t%s",szMaxDiffMod);
            */

            writer.write(String.format("H\tAlg-DisplayTop\t%s\n", szOutPutLines));
            writer.write(String.format("H\tAlg-IonSeries\t%s\n", szIons));
            writer.write(String.format("H\tEnzymeSpec\t%s\n", szEnzymeSpec));
            /*
            if(flag_stat)
              writer.write("%s %s\n", szStatMode,szEnzyme);
            else if(!flag_stat && flag_diff)
              writer.write(" %s\n", szEnzyme);
            else if (!flag_stat && !flag_diff)
              writer.write(" %s\n", szEnzyme);
            */
        }
    }

    private static String getSingleValue(String line)
    {
        return getValues(line)[0];
    }

    private static String[] getValues(String line)
    {
        if (!line.contains("="))
        {
            throw new IllegalArgumentException("No '=' in line: " + line);
        }
        return line.substring(line.indexOf("=") + 1).trim().split("\\s");
    }

    private static void processDiffLine(String line, String diffModCharacters, List<Pair<String, String>> diffMods, Logger logger)
    {
        //input is line, which is the diff_search_options line from a sequest parameters file
        //the output is diffMods which is a vector with a pair of strings for each differential modification
        //the first string is the aminio acid being modified, along with its symbolic representation the second is the mass of the differential mod
        //which is guaranteed to either have a + or - sign in front of it
        //read in the line using a string stream
        diffMods.clear();
        String[] values = line.split("\\s");
        String aminoAcid;
        String diffModMass;
        int diffModCharIndex = -1;
        char diffModChar;
        // Start at 2 so we skip the parameter name and the =
        for (int i = 2; i < values.length; i++)
        {
            diffModMass = values[i];
            diffModCharIndex += 1;
            //read in and process the amino acid
            if (i + 1 < values.length)
            {
                aminoAcid = values[++i];
                if (diffModCharIndex < diffModCharacters.length())
                {
                    diffModChar = diffModCharacters.charAt(diffModCharIndex);
                    aminoAcid += diffModChar;
                }
                else
                {
                    logger.warn("More differential modifications were specified in the sequest.params file than this build");
                    logger.warn("of run_ms_ssh was designed to handle (" + diffModCharacters.length() + ") please treat these results");
                    logger.warn("with caution");
                }
            }
            else
            {
                //there is an amino acid but no corresponding differential modification mass
                logger.error("Trouble reading the differential modification line in sequest.params!");
                logger.error("Could not find the amino acid for differential mod " + diffModMass);
                break;
            }

            //make sure the diff mod mass can be read as a float and is not equal to zero
            try
            {
                float diffMass = Float.parseFloat(diffModMass);
                if (diffMass != 0.0f)
                {
                    //now we know that the diff mod mass is a valid float and is not zero
                    // prefix positive values with +
                    diffMods.add(new Pair<>(aminoAcid, (diffMass > 0 ? "+" : "") + Float.toString(diffMass)));
                }
            }
            catch (NumberFormatException e)
            {
                logger.error("Trouble reading a differential mod mass for amino acid " + aminoAcid);
            }
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testParse() throws IOException
        {
            Mockery context = new Mockery();
            context.setImposteriser(ClassImposteriser.INSTANCE);
            PipelineJob job = context.mock(PipelineJob.class);
            Factory factory = context.mock(Factory.class);
            UWSequestSearchTask task = new UWSequestSearchTask(factory, job);
            assertEquals("2011011", task.parseSequestVersion(new StringReader("SEQUEST ver. UW 2011.01.1 MacCoss Lab, Genome Sciences")));

            assertEquals("20120112", task.parseSequestVersion(new StringReader("SEQUEST ver. UW 2012.01.12 MacCoss Lab, Genome Sciences")));

            assertEquals("2012012", task.parseSequestVersion(new StringReader(" SEQUEST version UW2012.01.2 MacCoss Lab, Genome Sciences")));

            assertEquals(null, task.parseSequestVersion(new StringReader("SEQUEST ver. UW 202.01.12 MacCoss Lab, Genome Sciences")));
            assertEquals(null, task.parseSequestVersion(new StringReader("SEQUEST ver. ")));
            assertEquals(null, task.parseSequestVersion(new StringReader("")));
        }
    }

    public static void main(String... args) throws Exception
    {
        try (Writer writer = PrintWriters.getPrintWriter(new FileOutputStream("c:/temp/headers.txt")))
        {
            writeParams(writer, new File("c:/temp/sequestProduction.params"), Logger.getLogger(UWSequestSearchTask.class), new File("c:/temp/databases/149Proteins.fsa"), "2050059");
        }
    }
}
