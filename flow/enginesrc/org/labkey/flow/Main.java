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

import org.apache.xmlbeans.XmlOptions;
import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.analysis.model.StatisticSet;
import org.labkey.flow.analysis.web.ScriptAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.LinkedHashSet;
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
            return FlowJoWorkspace.readWorkspace(is);
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

    private static void executeConvertWorkspace(File outDir, File workspaceFile, Set<String> groupNames, Set<String> sampleIds, Set<StatisticSet> stats)
    {
        FlowJoWorkspace workspace = readWorkspace(workspaceFile);
        ScriptDocument doc = ScriptDocument.Factory.newInstance();
        doc.addNewScript();

        String groupName = null;
        if (groupNames.size() > 0)
            groupName = groupNames.iterator().next();

        String sampleId = null;
        if (sampleIds.size() > 0)
            sampleId = sampleIds.iterator().next();

        ScriptAnalyzer.makeAnalysisDef(doc.getScript(), workspace, groupName, sampleId, stats);

        try
        {
            String name = "labkey-" + workspaceFile.getName();
            XmlOptions options = new XmlOptions();
            options.setSavePrettyPrint();
            doc.save(new File(outDir, name), options);
        }
        catch (IOException ioe)
        {
            System.err.println("Error: " + ioe.getMessage());
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
        usage.append("  -g  -- group name(s) from FlowJo workspace. May appear more than once.\n");
        usage.append("  -s  -- sample name(s) from FlowJo workspace. May appear more than once.\n");
        usage.append("  -S  -- statistic name(s). See available stats from list below. May appear more than once.\n");
        usage.append("\n");
        usage.append("Command is one of:\n");
        usage.append("  convert-workspace  -- converts a FlowJo workspace xml into a LabKey xml file\n");
        usage.append("  analysis           -- generate analysis results\n");
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
        Set<String> groupArgs = new LinkedHashSet<String>();
        Set<String> sampleArgs = new LinkedHashSet<String>();
        Set<StatisticSet> statArgs = EnumSet.of(StatisticSet.workspace, StatisticSet.count, StatisticSet.frequencyOfParent);

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
                    groupArgs.add(args[i]);
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
            else if ("convert-workspace".equals(arg) || "analysis".equals(arg))
            {
                commandArg = arg;
                break;
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

        if ("convert-workspace".equals(commandArg))
            executeConvertWorkspace(outDir, workspaceFile, groupArgs, sampleArgs, statArgs);
        else if ("analysis".equals(commandArg))
            executeAnalysis(outDir, workspaceFile, fcsDir);
        else
        {
            usage("Unknown command: " + commandArg);
            return;
        }

    }

}
