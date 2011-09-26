/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlOptions;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.StatisticSet;
import org.labkey.flow.analysis.web.ScriptAnalyzer;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: Apr 17, 2011
 */
public class Main
{
    private static FlowJoWorkspace readWorkspace(File file)
    {
        InputStream is = null;
        try
        {
            is = new FileInputStream(file);
            return FlowJoWorkspace.readWorkspace(file.getPath(), is);
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

    private static void executeListSamples(File workspaceFile, Set<PopulationName> groupNames)
    {
        FlowJoWorkspace workspace = readWorkspace(workspaceFile);

        // Hash the group and sample Analysis to see if they are equivalent
        Map<Analysis, PopulationName> analysisToGroup = new HashMap<Analysis, PopulationName>();
        Map<PopulationName, Analysis> groupAnalyses = workspace.getGroupAnalyses();
        for (PopulationName groupName : groupAnalyses.keySet())
        {
            Analysis analysis = groupAnalyses.get(groupName);
            if (analysisToGroup.containsKey(analysis))
                System.out.printf("warning: group analyses '%s' and '%s' are identical.\n", groupName, analysisToGroup.get(analysis));
            analysisToGroup.put(analysis, groupName);
        }

        Map<Analysis, List<String>> analysisToSamples = new HashMap<Analysis, List<String>>();
        for (FlowJoWorkspace.SampleInfo sample : workspace.getSamples())
        {
            Analysis analysis = workspace.getSampleAnalysis(sample);
            List<String> samples = analysisToSamples.get(analysis);
            if (samples == null)
                analysisToSamples.put(analysis, samples = new ArrayList<String>());

            samples.add(sample.getLabel());
        }

        for (FlowJoWorkspace.GroupInfo group : workspace.getGroups())
        {
            if (groupNames.isEmpty() || groupNames.contains(group.getGroupName()))
            {
                System.out.printf("Group: %s\n", group.getGroupName());

                if (group.getSampleIds().size() == 0)
                {
                    System.out.println("  no samples in group");
                }
                else
                {
                    System.out.print("  ");
                    String sep = "";
                    int lineLen = 2;
                    for (String sampleId : group.getSampleIds())
                    {
                        FlowJoWorkspace.SampleInfo sample = workspace.getSample(sampleId);
                        if (lineLen + sep.length() + sample.getLabel().length() > 80)
                        {
                            System.out.println(sep);
                            System.out.print("  ");
                            lineLen = 2;
                        }
                        else
                        {
                            System.out.print(sep);
                        }
                        System.out.print(sample.getLabel());
                        sep = ", ";
                        lineLen += sep.length() + sample.getLabel().length();
                    }
                    System.out.println();
                }
                System.out.println("");

                // Remove the group analysis from the sample analysis map
                Analysis analysis = groupAnalyses.get(group.getGroupName());
                analysisToSamples.remove(analysis);
            }
        }

        // Any remaining analyses must be different from the original group analyses
        if (!analysisToSamples.isEmpty())
        {
            System.out.println("Samples with modified analysis:");
            for (Map.Entry<Analysis, List<String>> entry : analysisToSamples.entrySet())
            {
                Analysis analysis = entry.getKey();
                List<String> samples = entry.getValue();
                System.out.printf("  %s: %s\n", analysis.getName(), StringUtils.join(samples, ", "));
            }
        }
    }

    private static void writeAnalysis(File outDir, String name, FlowJoWorkspace workspace, PopulationName groupName, String sampleId, Set<StatisticSet> stats)
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
        FlowJoWorkspace workspace = readWorkspace(workspaceFile);

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
            for (FlowJoWorkspace.SampleInfo sampleInfo : workspace.getSamples())
            {
                if (writeAll || sampleIds.contains(sampleInfo.getSampleId()) || sampleIds.contains(sampleInfo.getLabel()))
                    writeAnalysis(outDir, "sample-" + sampleInfo.getLabel() + ".xml", workspace, null, sampleInfo.getSampleId(), stats);
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
            Document doc = FlowJoWorkspace.parseXml(is);

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

    private static void executeAnalysis(File outDir, File workspaceFile, File fcsDir)
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
        usage.append("Usage: ").append(Main.class.getName()).append(" -w workspace [-f fcs dir] [-g group] [-s sample] [-S stat] [-o out] command\n");
        usage.append("\n");
        usage.append("  -o  -- output directory. Defaults to current directory.\n");
        usage.append("  -f  -- directory containing FCS files.\n");
        usage.append("  -w  -- either a FlowJo workspace xml or a LabKey workspace xml file\n");
        usage.append("  -g  -- group name from FlowJo workspace. May appear more than once.\n");
        usage.append("  -s  -- sample id or name from FlowJo workspace. May appear more than once.\n");
        usage.append("  -S  -- statistic name. See available stats from list below. May appear more than once.\n");
        usage.append("\n");
        usage.append("Command is one of:\n");
        usage.append("  parse              -- reads workspace; does nothing\n");
        usage.append("  analysis           -- generate analysis results\n");
        usage.append("  convert-workspace  -- converts a FlowJo workspace xml into a LabKey script file\n");
        usage.append("  trim-workspace     -- trims FlowJo workspace down to only required xml elements\n");
        usage.append("  list-samples       -- lists the groups and samples in the FlowJo workspace xml file\n");
        usage.append("\n");
        usage.append("Currently supported statistics:\n");
        for (StatisticSet stat : StatisticSet.values())
            if (stat != StatisticSet.existing)
                usage.append("  ").append(stat.name()).append(": ").append(stat.getLabel()).append("\n");

        System.err.println(usage.toString());
    }

    public static void main(String[] args)
    {
        String workspaceArg = null;
        String fcsArg = null;
        String outArg = null;
        String commandArg = null;
        Set<PopulationName> groupArgs = new LinkedHashSet<PopulationName>();
        Set<String> sampleArgs = new LinkedHashSet<String>();
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
            else if ("parse".equals(arg) || "analysis".equals(arg) || "convert-workspace".equals(arg) || "trim-workspace".equals(arg) || "list-samples".equals(arg))
            {
                commandArg = arg;
                break;
            }
            else
            {
                usage("Unknown argument '" + arg + "'");
                return;
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

        File workspaceFile = new File(workspaceArg);
        if (!workspaceFile.isFile())
        {
            System.err.println("workspace file doesn't exist: " + workspaceArg);
            return;
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

        if ("parse".equals(commandArg))
            readWorkspace(workspaceFile);
        else if ("analysis".equals(commandArg))
            executeAnalysis(outDir, workspaceFile, fcsDir);
        else if ("convert-workspace".equals(commandArg))
            executeConvertWorkspace(outDir, workspaceFile, groupArgs, sampleArgs, statArgs);
        else if ("trim-workspace".equals(commandArg))
            executeTrimWorkspace(outDir, workspaceFile);
        else if ("list-samples".equals(commandArg))
            executeListSamples(workspaceFile, groupArgs);
        else
        {
            usage("Unknown command: " + commandArg);
            return;
        }

    }

}
