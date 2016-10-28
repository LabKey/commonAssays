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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.TPPTask;
import org.labkey.ms2.pipeline.client.ParameterNames;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * <code>SequestSearchTask</code>
 */
public class SequestSearchTask extends AbstractMS2SearchTask<SequestSearchTask.Factory>
{
    public static final String SEQUEST_PARAMS = "sequest.params";
    private static final String MAKE_DB_PARAMS = "makedb.params";

    private static final String SEQUEST_ACTION_NAME = "Sequest Search";
    private static final String MZXML2SEARCH_ACTION_NAME = "MzXML2Search";
    private static final String MAKEDB_ACTION_NAME = "MakeDB";

    public static final String MASS_TYPE_PARENT_SUFFIX = ", mass_type_parent";
    public static final String MASS_TYPE_INDEX = "sequest, mass_type_index";

    public static final String USE_INDEX_PARAMETER_NAME = "pipeline, use index";
    public static final String INDEX_FILE_NAME_PARAMETER_NAME = "pipeline, index file name";

    public static final FileType INDEX_FILE_TYPE = new FileType(".hdr");
    public static final FileType SEQUEST_LOG_FILE_TYPE = new FileType(".sequestLog");
    public static final FileType SEQUEST_PARAMS_FILE_TYPE = new FileType(".sequest.params");

    private static final Object INDEX_LOCK = new Object();

    // useful for creating an output filename that honors config preference for gzipped output
    public static File getNativeOutputFile(File dirAnalysis, String baseName,
                                           FileType.gzSupportLevel gzSupport)
    {
        return AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis,baseName,gzSupport);
    }

    public static class Factory extends AbstractSequestSearchTaskFactory
    {
        public Factory()
        {
            super(SequestSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new SequestSearchTask(this, job);
        }

        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(MAKEDB_ACTION_NAME, MZXML2SEARCH_ACTION_NAME, SEQUEST_ACTION_NAME);
        }
    }

    protected SequestSearchTask(Factory factory, PipelineJob job)
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
            sb.append(".MassTypeIndex-");
            sb.append(params.get(SequestSearchTask.MASS_TYPE_INDEX));
            sb.append(".StaticMod-");
            sb.append(params.get(ParameterNames.STATIC_MOD));
            sb.append(".FASTAModified-");
            sb.append(fastaFile.lastModified());
            sb.append(".FASTASize-");
            sb.append(fastaFile.length());

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
                SequestParamsBuilder builder = new ThermoSequestParamsBuilder(getJob().getParameters(), getJob().getSequenceRootDirectory(), SequestParams.Variant.makedb, null);
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
            params.put("search, useremail", params.get(PipelineJob.PIPELINE_USERNAME_PARAM));
            params.put("search, username", "CPAS User");

            List<File> sequenceFiles = getFASTAOrIndexFiles(actions);

            // Don't let the total path name get too long. The actual name doesn't matter much, but we need
            // to avoid collisions so we can't just truncate the path after n characters
            boolean useGUIDFilename = getJob().getBaseName().length() > 20;
            String dtaDirName = useGUIDFilename ? GUID.makeGUID() : getJob().getBaseName();
            File dirOutputDta = new File(_wd.getDir(), dtaDirName);
            File fileMzXML = _factory.findInputFile(getJob());
            String tppVersion = TPPTask.getTPPVersion(getJob());

            // out2xml will need the mzXML file in the parent directory of the DTA directory in order to look up
            // retention times, so make a copy in the right place
            File localMzXML = _wd.inputFile(fileMzXML, true);

            // Translate the mzXML file to dta using MzXML2Search
            convertToDTA(params, dirOutputDta, localMzXML, tppVersion, actions);
            File dtaListFile = writeDtaList(dirOutputDta);

            // Write out sequest.params file
            File fileWorkParams = _wd.newFile(SEQUEST_PARAMS);

            SequestParamsBuilder builder = new ThermoSequestParamsBuilder(params, getJob().getSequenceRootDirectory(), SequestParams.Variant.thermosequest, sequenceFiles);
            builder.initXmlValues();
            builder.writeFile(fileWorkParams);

            // Have a copy in both the work directory to retain with the results, and in the dta subdirectory for
            // Sequest to use
            FileUtils.copyFileToDirectory(fileWorkParams, dirOutputDta);

            // Perform Sequest search
            List<String> sequestArgs = new ArrayList<>();
            File sequestExecutable = new File(_factory.getSequestInstallDir(), "sequest");
            sequestArgs.add(sequestExecutable.getAbsolutePath());
            sequestArgs.addAll(_factory.getSequestOptions());
            sequestArgs.add("-R" + dtaListFile.getAbsolutePath());
            sequestArgs.add("-F" + dirOutputDta.getAbsolutePath());
            // Trailing argument that makes Sequest not barf
            sequestArgs.add("x");
            ProcessBuilder sequestPB = new ProcessBuilder(sequestArgs);
            File sequestLogFileWork = SEQUEST_LOG_FILE_TYPE.getFile(_wd.getDir(), getJob().getBaseName());
            _wd.newFile(sequestLogFileWork.getName());
            boolean copySequestLogFile = true;
            try
            {
                getJob().runSubProcess(sequestPB, dirOutputDta, sequestLogFileWork, 200, false);

                // out2xml assumes that the mzXML file base name will match the DTA directory name, so rename the file
                // temporarily
                File guidMzXMLFile = new File(localMzXML.getParent(), AbstractMS2SearchProtocol.FT_MZXML.getDefaultName(dtaDirName));
                if (useGUIDFilename)
                {
                    localMzXML.renameTo(guidMzXMLFile);
                }

                // Convert to pepXML using out2xml
                List<String> out2XMLArgs = new ArrayList<>();
                String out2XMLPath = PipelineJobService.get().getExecutablePath("out2xml", null, "tpp", tppVersion, getJob().getLogger());
                out2XMLArgs.add(out2XMLPath);
                String enzyme =
                    new ThermoSequestParamsBuilder(params, null).getSupportedEnzyme(params.get(ParameterNames.ENZYME));
                Out2XmlParams out2XmlParams = new Out2XmlParams();
                out2XMLArgs.add(dirOutputDta.getName());
                out2XMLArgs.add("1");
                out2XmlParams.getParam("-E").setValue(enzyme);
                out2XMLArgs.addAll(convertParams(out2XmlParams.getParams(), params));
                ProcessBuilder out2XMLPB = new ProcessBuilder(out2XMLArgs);
                out2XMLPB.environment().put("WEBSERVER_ROOT", StringUtils.trimToEmpty(new File(out2XMLPath).getParent()));
                getJob().runSubProcess(out2XMLPB, _wd.getDir());

                // Rename it back
                if (useGUIDFilename)
                {
                    guidMzXMLFile.renameTo(localMzXML);
                }

                File pepXmlFile = TPPTask.getPepXMLFile(_wd.getDir(), getJob().getBaseName());
                if (!pepXmlFile.exists())
                {
                    // If we used an alternative name to keep the path from getting too long, rename the resulting pepXML
                    // to match our standard convention
                    File altPepXmlFile = TPPTask.getPepXMLFile(_wd.getDir(), dtaDirName);
                    if (altPepXmlFile.exists())
                    {
                        altPepXmlFile.renameTo(pepXmlFile);
                    }
                }

                if (!pepXmlFile.exists())
                    throw new IOException("Failed running out2xml or Sequest - could not find expected file: " + pepXmlFile);

                FileUtil.deleteDir(dirOutputDta);
                if (dirOutputDta.exists())
                    throw new IOException("Failed to delete DTA directory " + dirOutputDta.getAbsolutePath());

                File fileWorkPepXMLRaw = AbstractMS2SearchPipelineJob.getPepXMLConvertFile(_wd.getDir(),
                        getJob().getBaseName(),
                        getJob().getGZPreference());

                // Replacements that need to be made in the pepXML file so that we can resolve other files
                // during the import process
                Map<String, String> replacements = new HashMap<>();
                if (usesIndex())
                {
                    assert sequenceFiles.size() == 1;
                    // We want the pepXML file to point at the FASTA file, not at the indexed copy
                    String indexPath = sequenceFiles.get(0).getAbsolutePath();
                    String fastaPath = getJob().getSequenceFiles()[0].getAbsolutePath();
                    replacements.put(indexPath, fastaPath);
                    getJob().info("Replacing index path (" + indexPath + ") with FASTA path (" + fastaPath + ")");
                }

                if (useGUIDFilename)
                {
                    // The GUID name is only used while running the search, so make sure that the pepXML
                    // points at the real file name so that we can resolve its spectra later
                    replacements.put(dtaDirName, getJob().getBaseName());
                    getJob().info("Replacing GUID mzXML name (" + guidMzXMLFile.getName() + ") with real mzXML name (" + localMzXML.getName() + ")");
                }

                if (replacements.isEmpty())
                {
                    if (!pepXmlFile.renameTo(fileWorkPepXMLRaw))
                    {
                        throw new PipelineJobException("Failed to rename " + pepXmlFile + " to " + fileWorkPepXMLRaw);
                    }
                }
                else
                {
                    rewritePepXML(fileWorkPepXMLRaw, pepXmlFile, replacements);
                }
                // All tools have completed successfully, we won't need to copy the log file separately
                copySequestLogFile = false;

                // TODO: TGZ file is only required to get spectra loaded into CPAS.  Fix to use mzXML instead.
                try (WorkDirectory.CopyingResource lock = _wd.ensureCopyingLock())
                {
                    RecordedAction sequestAction = new RecordedAction(SEQUEST_ACTION_NAME);
                    sequestAction.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(sequestArgs, " "));
                    // Copy to a name that's unique to this file and won't conflict between searches in the same directory
                    File jobSpecificSequestParamsFile = SEQUEST_PARAMS_FILE_TYPE.getFile(fileWorkParams.getParentFile(), getJob().getBaseName());
                    FileUtils.moveFile(fileWorkParams, jobSpecificSequestParamsFile);
                    sequestAction.addOutput(_wd.outputFile(jobSpecificSequestParamsFile), "SequestParams", true);
                    sequestAction.addOutput(_wd.outputFile(fileWorkPepXMLRaw), "RawPepXML", true);
                    sequestAction.addOutput(_wd.outputFile(sequestLogFileWork), "SequestLog", false);
                    for (File file : sequenceFiles)
                    {
                        sequestAction.addInput(file, FASTA_INPUT_ROLE);
                    }
                    sequestAction.addInput(dirOutputDta, SPECTRA_INPUT_ROLE);

                    actions.add(sequestAction);
                }
            }
            finally
            {
                if (copySequestLogFile)
                {
                    // Something went wrong. Usually we don't bother copying files back after an error, but the user
                    // will likely need the Sequest log file to try to make sense of things.
                    _wd.outputFile(sequestLogFileWork);
                }
            }

            return new RecordedActionSet(actions);
        }
        catch(IOException e)
        {
            throw new PipelineJobException(e);
        }
    }

    /**
     * Rewrite the pepXML file so that it points to the FASTA file instead of the index file because the TPP and
     * the MS2 loading code don't know how to parse the index files.
     */
    private void rewritePepXML(File fileWorkPepXMLRaw, File pepXmlFile, Map<String, String> substitutions) throws PipelineJobException
    {
        try (InputStream fIn = new FileInputStream(pepXmlFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fIn));
             OutputStream fOut = new FileOutputStream(fileWorkPepXMLRaw);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fOut)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                // Do all of the replacements
                for (Map.Entry<String, String> entry : substitutions.entrySet())
                {
                    line = line.replace(entry.getKey(), entry.getValue());
                }
                writer.write(line);
                writer.newLine();
            }
        }
        catch (IOException e)
        {
            throw new PipelineJobException(e);
        }

        if (!pepXmlFile.delete())
        {
            throw new PipelineJobException("Failed to delete file: " + pepXmlFile);
        }
    }

    private void convertToDTA(Map<String, String> params, File dirOutputDta, File fileMzXML, String tppVersion, List<RecordedAction> actions)
            throws IOException, PipelineJobException
    {
        if (!dirOutputDta.mkdir())
            throw new IOException("Failed to create output directory for DTA files '" + dirOutputDta + "'.");
        ArrayList<String> mzXML2SearchArgs = new ArrayList<>();
        mzXML2SearchArgs.add(PipelineJobService.get().getExecutablePath("MzXML2Search", null, "tpp", tppVersion, getJob().getLogger()));
        mzXML2SearchArgs.add("-dta");
        mzXML2SearchArgs.add("-O" + dirOutputDta.getName());
        Mzxml2SearchParams mzXml2SearchParams = new Mzxml2SearchParams();
        Collection<String> inputXmlParams = convertParams(mzXml2SearchParams.getParams(), params);
        mzXML2SearchArgs.addAll(inputXmlParams);
        mzXML2SearchArgs.add(fileMzXML.getAbsolutePath());

        RecordedAction action = new RecordedAction(MZXML2SEARCH_ACTION_NAME);
        action.addParameter(RecordedAction.COMMAND_LINE_PARAM, StringUtils.join(mzXML2SearchArgs, " "));
        action.addInput(fileMzXML, AbstractMS2SearchTask.SPECTRA_INPUT_ROLE);
        action.addOutput(dirOutputDta, "DTA", true);

        actions.add(action);

        getJob().runSubProcess(new ProcessBuilder(mzXML2SearchArgs), _wd.getDir());
    }

    private File writeDtaList(File dirOutputDta) throws IOException
    {
        File[] dtaFiles = dirOutputDta.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(".dta");
            }
        });
        File result = new File(dirOutputDta, "DtaFiles.txt");
        OutputStream out = null;
        try
        {
            out = new FileOutputStream(result);
            PrintWriter writer = new PrintWriter(out);
            for (File dtaFile : dtaFiles)
            {
                writer.println(dtaFile.getName());
            }
            writer.flush();
        }
        finally
        {
            if (out != null) { try { out.close(); } catch (IOException e) {} }
        }
        return result;
    }

    static Collection<String> convertParams(Collection<Param> converters, Map<String, String> paramsXml) throws SequestParamsException
    {
        ArrayList<String> paramsCmd = new ArrayList<>();
        for (Param conv : converters)
        {
            String value = paramsXml.get(conv.getInputXmlLabels().get(0));
            if (value == null || value.equals(""))
            {
                if(conv.getValue() == null || conv.getValue().equals(""))
                    continue;
            }
            else
            {
                conv.setValue(value);
            }

            String parserError = conv.validate();
            if (!"".equals(parserError))
                throw new SequestParamsException(parserError);
            if(!conv.convert(";").equals(""))
                paramsCmd.add(conv.convert(";"));
        }

        return paramsCmd;
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testOut2XmlAll() throws SequestParamsException
        {
            Out2XmlParams out2XmlParams = new Out2XmlParams();
            assertEquals(Collections.<String>emptyList(), convertParams(out2XmlParams.getParams(), Collections.emptyMap()));
            assertEquals(Collections.<String>emptyList(), convertParams(out2XmlParams.getParams(), Collections.singletonMap("out2xml, all", "0")));
            assertEquals(Collections.<String>emptyList(), convertParams(out2XmlParams.getParams(), Collections.singletonMap("out2xml, all", "")));

            assertEquals(Arrays.asList("-all"), convertParams(out2XmlParams.getParams(), Collections.singletonMap("out2xml, all", "1")));
        }

        @Test
        public void testOut2XmlEnzyme() throws SequestParamsException
        {
            assertEquals(Arrays.asList("-EtestEnzyme"), convertParams(new Out2XmlParams().getParams(), Collections.singletonMap("out2xml, enzyme", "testEnzyme")));
            assertEquals(Collections.<String>emptyList(), convertParams(new Out2XmlParams().getParams(), Collections.singletonMap("out2xml, enzyme", "")));
        }
    }
}
