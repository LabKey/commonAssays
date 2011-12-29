/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectory;
import org.labkey.api.pipeline.cmd.TaskPath;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.UnexpectedException;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.AbstractMS2SearchTaskFactory;
import org.labkey.ms2.pipeline.TPPTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    public static class Factory extends AbstractMS2SearchTaskFactory<Factory>
    {
        private File _sequestInstallDir;
        private File _indexRootDir;
        private List<String> _sequestOptions = new ArrayList<String>();

        public Factory()
        {
            super(UWSequestSearchTask.class);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new UWSequestSearchTask(this, job);
        }

        public boolean isJobComplete(PipelineJob job)
        {
            SequestPipelineJob support = (SequestPipelineJob) job;
            String baseName = support.getBaseName();
            String baseNameJoined = support.getJoinedBaseName();
            File dirAnalysis = support.getAnalysisDirectory();

            // Fraction roll-up, completely analyzed sample pepXML, or the raw pepXML exist
            return NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseNameJoined)) ||
                   NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseName)) ||
                   NetworkDrive.exists(AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis, baseName));
        }


        public List<String> getProtocolActionNames()
        {
            return Arrays.asList(MAKEDB_ACTION_NAME, SEQUEST_ACTION_NAME, SEQUEST_DECOY_ACTION_NAME);
        }

        public String getGroupParameterName()
        {
            return "sequest";
        }

        public String getSequestInstallDir()
        {
            return _sequestInstallDir == null ? null : _sequestInstallDir.getAbsolutePath();
        }

        public void setSequestInstallDir(String sequestInstallDir)
        {
            if (sequestInstallDir != null)
            {
                _sequestInstallDir = new File(sequestInstallDir);
                NetworkDrive.exists(_sequestInstallDir);
                if (!_sequestInstallDir.isDirectory())
                {
//                    throw new IllegalArgumentException("No such Sequest install dir: " + sequestInstallDir);
                }
            }
            else
            {
                _sequestInstallDir = null;
            }
        }

        public List<String> getSequestOptions()
        {
            return _sequestOptions;
        }

        public void setSequestOptions(List<String> sequestOptions)
        {
            _sequestOptions = sequestOptions;
        }

        public String getIndexRootDir()
        {
            return _indexRootDir == null ? null : _indexRootDir.getAbsolutePath();
        }

        public void setIndexRootDir(String indexRootDir)
        {
            if (indexRootDir != null)
            {
                _indexRootDir = new File(indexRootDir);
                NetworkDrive.exists(_indexRootDir);
                if (!_indexRootDir.isDirectory())
                {
                    throw new IllegalArgumentException("No such index root dir: " + indexRootDir);
                }
            }
            else
            {
                _indexRootDir = null;
            }
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
            sb.append(params.get(org.labkey.ms2.pipeline.client.ParamParser.ENZYME));
            sb.append(".MinParentMH-");
            sb.append(params.get(AbstractMS2SearchTask.MINIMUM_PARENT_M_H));
            sb.append(".MaxParentMH-");
            sb.append(params.get(AbstractMS2SearchTask.MAXIMUM_PARENT_M_H));
            sb.append(".MaxMissedCleavages-");
            sb.append(params.get(AbstractMS2SearchTask.MAXIMUM_MISSED_CLEAVAGE_SITES));
            sb.append(".MassTypeParent-");
            sb.append(params.get(UWSequestSearchTask.MASS_TYPE_PARENT));
            sb.append(".StaticMod-");
            sb.append(params.get(org.labkey.ms2.pipeline.client.ParamParser.STATIC_MOD));

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
        try
        {
            return s == null ? new byte[] { 0 } : s.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new UnexpectedException(e);
        }
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
                List<String> args = new ArrayList<String>();
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

    public RecordedActionSet run() throws PipelineJobException
    {
        try
        {
            List<RecordedAction> actions = new ArrayList<RecordedAction>();
            Map<String, String> params = getJob().getParameters();
            params.put("list path, sequest parameters", SEQUEST_PARAMS);
            params.put("search, useremail", params.get("pipeline, email address"));
            params.put("search, username", "CPAS User");

            List<File> sequenceFiles = getFASTAOrIndexFiles(actions);

            File fileMzXML = _factory.findInputFile(getJob().getDataDirectory(), getJob().getBaseName());
            File fileMzXMLWork = _wd.inputFile(fileMzXML, true);

            // Write out sequest.params file
            File fileWorkParams = _wd.newFile(SEQUEST_PARAMS);

            List<File> decoySequenceFiles = new ArrayList<File>();
            for (File sequenceFile : sequenceFiles)
            {
                int index = sequenceFile.getName().lastIndexOf(".");
                if (index != -1)
                {
                    String decoyName = sequenceFile.getName().substring(0, index) + "-rev" + sequenceFile.getName().substring(index);
                    File decoyFile = new File(sequenceFile.getParentFile(), decoyName);
                    if (decoyFile.isFile())
                    {
                        decoySequenceFiles.add(decoyFile);
                    }
                }
            }

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

            WorkDirectory.CopyingResource lock = null;
            try
            {
                lock = _wd.ensureCopyingLock();
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
                _wd.acceptFilesAsOutputs(Collections.<String, TaskPath>emptyMap(), sequestAction);

                actions.add(sequestAction);
            }
            finally
            {
                if (lock != null) { lock.release(); }
            }

            return new RecordedActionSet(actions);
        }
        catch(IOException e)
        {
            throw new PipelineJobException(e);
        }
    }

    private List<String> performSearch(File workingDir, Map<String, String> params, List<File> sequenceFiles, File fileMzXMLWork, File paramsFile, File resultsFile)
            throws IOException, PipelineJobException
    {
        UWSequestParamsBuilder builder = new UWSequestParamsBuilder(params, getJob().getSequenceRootDirectory(), SequestParams.Variant.uwsequest, sequenceFiles);
        builder.initXmlValues();
        builder.writeFile(paramsFile);

        // Perform Sequest search
        List<String> sequestArgs = new ArrayList<String>();
        File sequestExecutable = new File(_factory.getSequestInstallDir(), "sequest");
        sequestArgs.add(sequestExecutable.getAbsolutePath());
        sequestArgs.addAll(_factory.getSequestOptions());
        sequestArgs.add(FileUtil.relativize(workingDir, fileMzXMLWork, false));
        ProcessBuilder sequestPB = new ProcessBuilder(sequestArgs);
        Writer writer = new FileWriter(resultsFile);
        try
        {
            writer.write("H       SQTGenerator SEQUEST\n");
            writer.write("H       SQTGeneratorVersion 2011.01.0\n");
            writer.write("H       Comment Invoked through LabKey Server Pipeline\n");
        }
        finally
        {
            writer.close();
        }
        getJob().runSubProcess(sequestPB, workingDir, resultsFile, 10, true);
        return sequestArgs;
    }
}
