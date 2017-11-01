/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

package org.labkey.flow;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlOptions;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Tuple3;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.api.writer.VirtualFile;
import org.labkey.api.writer.ZipUtil;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.ExternalAnalysis;
import org.labkey.flow.analysis.model.IWorkspace;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.SampleIdMap;
import org.labkey.flow.analysis.model.StatisticSet;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.analysis.model.WorkspaceParser;
import org.labkey.flow.analysis.web.ScriptAnalyzer;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.ObjectType;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

/**
 * User: kevink
 * Date: Apr 17, 2011
 */
public class Main
{
    private enum AnalysisResultsOutputFormat
    {
        tsv, xar, flowjoxml
    }

    private static Workspace readWorkspace(File file, boolean printWarnings)
    {
        try
        {
            Workspace workspace = Workspace.readWorkspace(file);
            if (printWarnings && workspace.getWarnings().size() > 0)
            {
                for (String warning : workspace.getWarnings())
                    System.out.println("warning: " + warning);
            }
            return workspace;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static File uniqueFile(File dir, String name)
    {
        File file = new File(dir, name);
        if (file.exists())
        {
            String base = name;
            String ext = "";
            int dot = name.lastIndexOf(".");
            if (dot > -1)
            {
                base = name.substring(0, dot);
                ext = name.substring(dot);
            }

            for (int i = 1; file.exists(); i++)
                file = new File(dir, base + i + ext);
        }

        return file;
    }

    private static int MAX_LINE_LEN = 80;
    private static void printWrapped(PrintStream out, String indent, Collection<? extends Object> os)
    {
        if (os.size() == 0)
            return;

        int lineLen = indent.length();
        String sep = "";
        for (Object o : os)
        {
            String word = o.toString();
            if (lineLen + sep.length() + word.length() > MAX_LINE_LEN)
            {
                out.println(sep);
                out.print(indent);
                lineLen = indent.length();
            }
            else
            {
                out.print(sep);
            }
            out.print(word);
            sep = ", ";
            lineLen += sep.length() + word.length();
        }
    }

    private static void executeListSamples(File workspaceFile, Set<PopulationName> groupNames)
    {
        Workspace workspace = readWorkspace(workspaceFile, false);

        // First, hash the group analysis...
        Map<Analysis, PopulationName> analysisToGroup = new HashMap<>();
        Map<PopulationName, Analysis> groupAnalyses = workspace.getGroupAnalyses();
        for (PopulationName groupName : groupAnalyses.keySet())
        {
            Analysis analysis = groupAnalyses.get(groupName);
            if (analysisToGroup.containsKey(analysis))
                System.out.printf("warning: group analyses '%s' and '%s' are identical.\n", groupName, analysisToGroup.get(analysis));
            analysisToGroup.put(analysis, groupName);
        }

        // ... then, hash the sample analysis
        Map<Analysis, List<Workspace.SampleInfo>> analysisToSamples = new HashMap<>();
        Map<String, List<Workspace.SampleInfo>> sampleLabels = new HashMap<>();
        for (Workspace.SampleInfo sample : workspace.getSamplesComplete())
        {
            Analysis analysis = workspace.getSampleAnalysis(sample);
            List<Workspace.SampleInfo> samples = analysisToSamples.get(analysis);
            if (samples == null)
                analysisToSamples.put(analysis, samples = new ArrayList<>());

            samples.add(sample);

            List<Workspace.SampleInfo> dups = sampleLabels.get(sample.getLabel());
            if (dups == null)
                sampleLabels.put(sample.getLabel(), dups = new ArrayList<>());
            dups.add(sample);
        }

        // CONSIDER: move warning into FlowJoWorkspace parsing code, but I'm not sure if it is an error to have two samples representing the same FCS file.
        // Check for duplicate sample labels
        for (List<Workspace.SampleInfo> dups : sampleLabels.values())
        {
            if (dups.size() > 1)
                System.out.println("warning: duplicate sample labels: " + StringUtils.join(dups, ", "));
        }

        // Using the group and sample analysis hash maps,
        // print the group analysis first and remove the group analysis from the analysisToSamples hash map
        // leaving behind any modified sample analysis.
        for (Workspace.GroupInfo group : workspace.getGroups())
        {
            if (groupNames.isEmpty() || groupNames.contains(group.getGroupName()))
            {
                System.out.printf("Group %s: %s\n", group.getGroupId(), group.getGroupName());

                if (group.getSampleIds().size() == 0)
                {
                    System.out.println("  no samples in group");
                }
                else
                {
                    List<String> sampleIDs = group.getSampleIds();
                    List<Workspace.SampleInfo> samples = new ArrayList<>(sampleIDs.size());
                    for (String sampleId : sampleIDs)
                    {
                        Workspace.SampleInfo sample = workspace.getSample(sampleId);
                        samples.add(sample);
                    }
                    String indent = "  ";
                    System.out.print(indent);
                    printWrapped(System.out, indent, samples);
                    System.out.println();
                }
                System.out.println();

                // Remove the group analysis from the sample analysis map
                Analysis analysis = groupAnalyses.get(group.getGroupName());
                analysisToSamples.remove(analysis);
            }
        }

        // Any remaining analyses must be different from the original group analyses
        if (!analysisToSamples.isEmpty())
        {
            System.out.println("Samples with modified analysis:");
            for (Map.Entry<Analysis, List<Workspace.SampleInfo>> entry : analysisToSamples.entrySet())
            {
                Analysis analysis = entry.getKey();
                List<Workspace.SampleInfo> samples = entry.getValue();
                String header = String.format("  %s: ", samples.get(0));
                String indent = StringUtils.repeat(" ", header.length());
                System.out.print(header);
                printWrapped(System.out, indent, samples);
                System.out.println();
            }
        }
    }

    private static void writeAnalysis(File outDir, String name, Workspace workspace, PopulationName groupName, String sampleId, Set<StatisticSet> stats)
    {
        ScriptDocument doc = ScriptDocument.Factory.newInstance();
        doc.addNewScript();
        ScriptAnalyzer.makeAnalysisDef(doc.getScript(), workspace, groupName, sampleId, stats);

        try
        {
            XmlOptions options = new XmlOptions();
            options.setSavePrettyPrint();
            doc.save(new File(outDir, name), options);
        }
        catch (IOException ioe)
        {
            System.err.println("Error: " + ioe.getMessage());
        }
    }

    private static void executeConvertWorkspace(File outDir, File workspaceFile, Set<PopulationName> groupNames, Set<String> sampleIds, Set<StatisticSet> stats)
    {
        Workspace workspace = readWorkspace(workspaceFile, false);

        boolean writeAll = groupNames.isEmpty() && sampleIds.isEmpty();
        if (writeAll || !groupNames.isEmpty())
        {
            Map<PopulationName, Analysis> groupAnalyses = workspace.getGroupAnalyses();
            for (PopulationName groupName : groupAnalyses.keySet())
            {
                if (writeAll || groupNames.contains(groupName))
                    writeAnalysis(outDir, "group-" + groupName + ".xml", workspace, groupName, null, stats);
            }
        }

        if (writeAll || !sampleIds.isEmpty())
        {
            for (Workspace.SampleInfo sampleInfo : workspace.getSamplesComplete())
            {
                if (writeAll || sampleIds.contains(sampleInfo.getSampleId()) || sampleIds.contains(sampleInfo.getLabel()))
                {
                    String sampleId = sampleInfo.getSampleId();
                    String sampleLabel = sampleInfo.getLabel();
                    if (sampleLabel == sampleId)
                        sampleLabel = "unlabeled";

                    String fileName = String.format("sample-%s-%s.xml", sampleLabel, sampleId);
                    writeAnalysis(outDir, fileName, workspace, null, sampleInfo.getSampleId(), stats);
                }
            }
        }
    }

    private static void executeTrimWorkspace(File outDir, File workspaceFile)
    {
        File outFile = uniqueFile(outDir, workspaceFile.getName());

        InputStream is = null;
        try
        {
            is = new FileInputStream(workspaceFile);
            Document doc = WorkspaceParser.parseXml(is);

            Source source = new DOMSource(doc);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StreamResult result = new StreamResult(new FileOutputStream(outFile));
            t.transform(source, result);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (is != null) try { is.close(); } catch (IOException ioe) { }
        }
    }

    private static void writeAnalysisResults(File outDir, SampleIdMap<AttributeSet> keywords, SampleIdMap<AttributeSet> results, SampleIdMap<CompensationMatrix> matrices, AnalysisResultsOutputFormat outputFormat)
    {
        if (outputFormat == AnalysisResultsOutputFormat.tsv)
        {
            FileSystemFile rootDir = new FileSystemFile(outDir);
            AnalysisSerializer writer = new AnalysisSerializer(rootDir);
            try
            {
                writer.writeAnalysis(keywords, results, matrices);
            }
            catch (IOException ioe)
            {
                System.err.println("Error writing analysis results: " + ioe.getMessage());
                ioe.printStackTrace(System.err);
            }
        }
        else if (outputFormat == AnalysisResultsOutputFormat.flowjoxml)
        {
            // UNDONE: write flowjo xml format
        }
        else if (outputFormat == AnalysisResultsOutputFormat.xar)
        {
            // UNDONE: write XAR format
        }
    }

    private static Tuple3<SampleIdMap<AttributeSet>, SampleIdMap<AttributeSet>, SampleIdMap<CompensationMatrix>> readWorkspaceAnalysisResults(File workspaceFile, File fcsDir, Set<PopulationName> groupNames, Set<String> sampleIds, Set<StatisticSet> stats)
    {
        Workspace workspace = readWorkspace(workspaceFile, true);

        Set<Workspace.SampleInfo> sampleInfos = workspace.getSamples(groupNames, sampleIds);

        SampleIdMap<AttributeSet> keywords = new SampleIdMap<>();
        SampleIdMap<AttributeSet> analysis = new SampleIdMap<>();
        SampleIdMap<CompensationMatrix> matrices = new SampleIdMap<>();
        for (Workspace.SampleInfo sampleInfo : sampleInfos)
        {
            if (analysis.containsName(sampleInfo.getLabel()))
            {
                System.err.printf("warning: sample label '%s' appears on more than one sample info", sampleInfo.getLabel());
                continue;
            }

            AttributeSet attrs = workspace.getSampleAnalysisResults(sampleInfo);
            if (attrs != null)
                analysis.put(sampleInfo, attrs);


            Map<String, String> sampleKeywords = sampleInfo.getKeywords();
            if (sampleKeywords != null && !sampleKeywords.isEmpty())
            {
                AttributeSet keywordAttrs = new AttributeSet(ObjectType.fcsKeywords, null);
                keywordAttrs.setKeywords(sampleKeywords);
                keywords.put(sampleInfo, keywordAttrs);
            }

            CompensationMatrix matrix = sampleInfo.getCompensationMatrix();
            if (matrix != null)
                matrices.put(sampleInfo, matrix);

            // UNDONE: generate graphs if fcs file is available
        }

        return Tuple3.of(keywords, analysis, matrices);
    }

    private static ExternalAnalysis readTsvAnalysisResults(File analysisResultsFile, File fcsDir, Set<PopulationName> groupNames, Set<String> sampleIds, Set<StatisticSet> stats)
    {
        VirtualFile rootDir;
        if (analysisResultsFile.getName().endsWith(".zip"))
        {
            // NOTE: Duplicated code in AnalysisScriptController
            File statisticsFile = null;
            java.util.zip.ZipFile zipFile;
            try
            {
                zipFile = new java.util.zip.ZipFile(analysisResultsFile);
            }
            catch (IOException e)
            {
                System.err.println("Error reading analysis results: Could not read zip file: " + e.getMessage());
                return null;
            }

            String zipBaseName = FileUtil.getBaseName(analysisResultsFile);
            ZipEntry zipEntry = zipFile.getEntry(AnalysisSerializer.STATISTICS_FILENAME);
            if (zipEntry == null)
                zipEntry = zipFile.getEntry(zipBaseName + "/" + AnalysisSerializer.STATISTICS_FILENAME);

            if (zipEntry == null)
            {
                System.err.println("Error reading analysis results: Couldn't find '" + AnalysisSerializer.STATISTICS_FILENAME + "' or '" + zipBaseName + "/" + AnalysisSerializer.STATISTICS_FILENAME + "' in the zip archive.");
                return null;
            }

            // UNDONE: instead of unzipping into temp dir, make zip VirtualFile impl readable.
            try
            {
                File tmpDir = FileUtil.createTempDirectory("flow");
                tmpDir.deleteOnExit();

                ZipUtil.unzipToDirectory(analysisResultsFile, tmpDir);
                statisticsFile = new File(tmpDir, zipEntry.getName());
                rootDir = new FileSystemFile(statisticsFile.getParentFile());
            }
            catch (IOException ioe)
            {
                System.err.println("Error unzipping analysis results: " + ioe.getMessage());
                return null;
            }
        }
        else
        {
            rootDir = new FileSystemFile(analysisResultsFile);
        }

        AnalysisSerializer reader = new AnalysisSerializer(rootDir);
        try
        {
            // UNDONE: filter results by sampleIDs and stats.  Groups can't be supported.
            return reader.readAnalysis();
        }
        catch (Exception e)
        {
            System.err.println("Error reading analysis results: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static IWorkspace readAnalysisResults(File analysisResultsFile, File fcsDir, Set<PopulationName> groupNames, Set<String> sampleIds, Set<StatisticSet> stats)
    {
        if (WorkspaceParser.isFlowJoWorkspace(analysisResultsFile))
        {
            //return readWorkspaceAnalysisResults(analysisResultsFile, fcsDir, groupNames, sampleIds, stats);
            return readWorkspace(analysisResultsFile, true);
        }
        else if (analysisResultsFile.getName().endsWith(".xar"))
        {
            // UNDONE: read XAR analysis
            throw new RuntimeException("Not yet implemented");
        }
        else
        {
            return readTsvAnalysisResults(analysisResultsFile, fcsDir, groupNames, sampleIds, stats);
        }
    }

    private static void executeConvertAnalysis(File outDir, File workspaceOrAnalysisResults, File fcsDir, Set<PopulationName> groupNames, Set<String> sampleIds, Set<StatisticSet> stats, AnalysisResultsOutputFormat outputFormat)
    {
        Tuple3<SampleIdMap<AttributeSet>, SampleIdMap<AttributeSet>, SampleIdMap<CompensationMatrix>> results = null;//readAnalysisResults(workspaceOrAnalysisResults, fcsDir, groupNames, sampleIds, stats);
        if (results == null)
            return;

        writeAnalysisResults(outDir, results.first, results.second, results.third, outputFormat);
    }

    private static void executeRunAnalysis(File outDir, File workspaceFile, File fcsDir, Set<PopulationName> groupNames, Set<String> sampleIds, Set<StatisticSet> stats, AnalysisResultsOutputFormat outputFormat)
    {

    }

    private static void usage()
    {
        usage(null);
    }

    private static void usage(String message)
    {
        if (message != null)
        {
            System.err.println(message);
            System.err.println();
        }

        StringBuilder usage = new StringBuilder();
        String progName = Main.class.getName();
        usage.append("Usage: ").append(progName).append(" [-w workspace] [-r results] [-f fcs dir] [-g group] [-s sample] [-S stat] [-o out] [-F (tsv|xar|flowjoxml)] command\n");
        usage.append("\n");
        usage.append("  -w   -- analysis definition: FlowJo workspace xml or LabKey workspace xml file.\n"); // Add gatingML
        usage.append("  -r   -- analysis results: FlowJo workspace xml, tsv, or XAR.\n"); // add ACS
        usage.append("  -f   -- directory containing FCS files.\n");
        usage.append("\n");
        usage.append("Selection options:\n");
        usage.append("  -g   -- group id or name from FlowJo workspace. May appear more than once.\n");
        usage.append("  -s   -- sample id or name from analysis definition. May appear more than once.\n");
        usage.append("  -S   -- statistic name. See available stats from list below. May appear more than once.\n");
        usage.append("\n");
        usage.append("Output options:\n");
        usage.append("  -o   -- output directory. Defaults to current directory.\n");
        usage.append("  -F   -- write out analysis results in the given format (tsv, xar, or flowjoxml; defaults to tsv)\n");
        usage.append("\n");
        usage.append("Command is one of:\n");
        usage.append("  parse              -- reads workspace; does nothing\n");
        usage.append("  run-analysis       -- use LabKey's engine to generate analysis results\n");
        usage.append("  convert-analysis   -- convert analysis results into a different format (requires -r argument)\n");
        usage.append("  convert-workspace  -- converts a FlowJo workspace xml into a LabKey script file\n");
        usage.append("  trim-workspace     -- trims FlowJo workspace down to only required xml elements\n");
        usage.append("  list-samples       -- lists the groups and samples in the FlowJo workspace xml file\n");
        usage.append("\n");
        usage.append("Currently supported statistics:\n");
        for (StatisticSet stat : StatisticSet.values())
            if (stat != StatisticSet.existing)
                usage.append("  ").append(stat.name()).append(": ").append(stat.getLabel()).append("\n");

        usage.append("\n");
        usage.append("Examples:\n");
        usage.append("\n");
        usage.append("  Convert from FlowJo xml into tsv files and back into a FlowJo workspace:\n");
        usage.append("    ").append(progName).append(" -r workspace.xml -o out1 -F tsv convert-analysis\n");
        usage.append("    ").append(progName).append(" -r out1 -o out2 -F flowjoxml convert-analysis\n");
        usage.append("\n");
        usage.append("  Execute LabKey analysis and save as XAR:\n");
        usage.append("    ").append(progName).append(" -w labkey.xml -f fcsfiles -F xar run-analysis\n");

        System.err.println(usage.toString());
    }

    public static void main(String[] args)
    {
        String workspaceArg = null;
        String analysisResultsArg = null;
        String fcsArg = null;
        String outArg = null;
        String outputFormatArg = null;
        String commandArg = null;
        Set<PopulationName> groupArgs = new LinkedHashSet<>();
        Set<String> sampleArgs = new LinkedHashSet<>();
        Set<StatisticSet> statArgs = EnumSet.noneOf(StatisticSet.class);

        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if ("-w".equals(arg) || "--workspace".equals(arg))
            {
                if (++i < args.length)
                    workspaceArg = args[i];
                else
                {
                    usage("--workspace requires argument");
                    return;
                }
            }
            else if ("-r".equals(arg) || "--results".equals(arg))
            {
                if (++i < args.length)
                    analysisResultsArg = args[i];
                else
                {
                    usage("--results requires argument");
                    return;
                }
            }
            else if ("-f".equals(arg) || "--fcs".equals(arg))
            {
                if (++i < args.length)
                    fcsArg = args[i];
                else
                {
                    usage("--fcs requires argument");
                    return;
                }
            }
            else if ("-o".equals(arg) || "--out".equals(arg))
            {
                if (++i < args.length)
                    outArg = args[i];
                else
                {
                    usage("--out requires argument");
                    return;
                }
            }
            else if ("-g".equals(arg) || "--group".equals(arg))
            {
                if (++i < args.length)
                {
                    PopulationName name = PopulationName.fromString(args[i]);
                    groupArgs.add(name);
                }
                else
                {
                    usage("--group requires argument");
                    return;
                }
            }
            else if ("-s".equals(arg) || "--sample".equals(arg))
            {
                if (++i < args.length)
                    sampleArgs.add(args[i]);
                else
                {
                    usage("--sample requires argument");
                    return;
                }
            }
            else if ("-S".equals(arg) || "--statistic".equals(arg))
            {
                if (++i < args.length)
                {
                    try
                    {
                        statArgs.add(StatisticSet.valueOf(args[i]));
                    }
                    catch (IllegalArgumentException e)
                    {
                        usage("statistic '" + args[i] + "' not supported");
                        return;
                    }
                }
                else
                {
                    usage("--statistic requires argument");
                    return;
                }
            }
            else if ("-F".equals(arg) || "--format".equals(arg))
            {
                if (++i < args.length)
                    outputFormatArg = args[i];
                else
                {
                    usage("--format requires argument");
                    return;
                }
            }
            else if ("-tsv".equals(arg))
            {
                outputFormatArg = "tsv";
            }
            else if ("-xar".equals(arg))
            {
                outputFormatArg = "xar";
            }
            else if ("-flowjoxml".equals(arg))
            {
                outputFormatArg = "flowjoxml";
            }
            else
            {
                commandArg = arg;
            }
        }

        if (commandArg == null)
        {
            usage();
            return;
        }

        if (outArg == null)
            outArg = System.getProperty("user.dir");

        File outDir = new File(outArg);
        if (!outDir.isDirectory())
        {
            System.err.println("out directory doesn't exist: " + outArg);
            return;
        }

        File workspaceFile = null;
        if (!commandArg.equals("convert-analysis"))
        {
            if (workspaceArg == null)
            {
                System.err.println(commandArg + " command requires workspace");
                return;
            }
            workspaceFile = new File(workspaceArg);
            if (!workspaceFile.isFile())
            {
                System.err.println("workspace file doesn't exist: " + workspaceArg);
                return;
            }
        }
        else if (commandArg.equals("convert-analysis"))
        {
            if (analysisResultsArg == null)
            {
                System.err.println("convert-analysis command requires workspace");
                return;
            }
            // NOTE: Re-using the workspaceFile even though it really is analysis results.  Consider collapsing '-r' and '-w' arguments.
            workspaceFile = new File(analysisResultsArg);
            if (!workspaceFile.isFile())
            {
                System.err.println("analysis results file doesn't exist: " + workspaceFile);
                return;
            }
        }

        File fcsDir = null;
        if (fcsArg != null)
        {
            fcsDir = new File(fcsArg);
            if (!fcsDir.isDirectory())
            {
                System.err.println("fcs directory doesn't exist: " + fcsArg);
                return;
            }
        }

        if (statArgs.isEmpty())
            statArgs = EnumSet.of(StatisticSet.workspace);


        AnalysisResultsOutputFormat outputFormat = AnalysisResultsOutputFormat.tsv;
        if (outputFormatArg != null)
        {
            try
            {
                outputFormat = AnalysisResultsOutputFormat.valueOf(outputFormatArg);
            }
            catch (IllegalFormatException nfe)
            {
                System.err.println("output format '" + outputFormatArg + "' not supported.");
                return;
            }
        }


        if ("parse".equals(commandArg))
            readWorkspace(workspaceFile, true);
        else if ("convert-analysis".equals(commandArg))
            executeConvertAnalysis(outDir, workspaceFile, fcsDir, groupArgs, sampleArgs, statArgs, outputFormat);
        else if ("run-analysis".equals(commandArg))
            executeRunAnalysis(outDir, workspaceFile, fcsDir, groupArgs, sampleArgs, statArgs, outputFormat);
        else if ("convert-workspace".equals(commandArg))
            executeConvertWorkspace(outDir, workspaceFile, groupArgs, sampleArgs, statArgs);
        else if ("trim-workspace".equals(commandArg))
            executeTrimWorkspace(outDir, workspaceFile);
        else if ("list-samples".equals(commandArg))
            executeListSamples(workspaceFile, groupArgs);
        else
        {
            usage("Unknown command: " + commandArg);
        }

    }

}
