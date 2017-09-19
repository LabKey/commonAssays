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
package org.labkey.flow.persist;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.collections.RowMapFactory;
import org.labkey.api.data.TSVMapWriter;
import org.labkey.api.data.TSVWriter;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.Readers;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.Tuple3;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.api.writer.PrintWriters;
import org.labkey.api.writer.VirtualFile;
import org.labkey.api.writer.ZipUtil;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.ExternalAnalysis;
import org.labkey.flow.analysis.model.SampleIdMap;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads/writes a directory of stats and graphs suitable for interchange with R and LabKey.
 *
 * Top-level statistics.tsv and graphs.tsv files.  The graphs.tsv may contain relative paths
 * to images.  By convention, each sample should get its own directory of graphs.
 * <pre>
 * +-&lt;root&gt;/
 *   |- run-level numbers and plots?
 *   |
 *   |- keywords.tsv     -- ?
 *   |- statistics.tsv   -- Name, StatName1, StatName2, ...
 *   |                   or Name, Population, Statistic, Value
 *   |                   or Name, Population, StatName1, StatName2, ...
 *   |                   or Name, Population, Parameter, StatName1, StatName2, ...
 *   |
 *   |- graphs.tsv       -- Name, Population, Graph, Path
 *   |
 *   +- sample1/         -- graphs divided by sample
 *     |- graph01.png    -- graph names may be friendly or just a MD5-sum hash
 *     `- graph02.png
 *
 * </pre>
 *
 * It would be easy to support statistics.tsv and graph.tsv files per-sample as well:
 * <pre>
 * +-&lt;root&gt;/
 *   |- run-level numbers and plots?
 *   |
 *   +- sample1/
 *   | |- keywords.tsv   -- ?
 *   | |- statistics.tsv -- Name, Population, Statistic, Value
 *   | |- graphs.tsv     -- Name, Population, Graph, Path
 *   | |- graph01.png
 *   | `- graph02.png
 *   |
 *   +- sample2/
 *   | |- ...
 *   ...
 * </pre>
 *
 */
public class AnalysisSerializer
{
    public static final String KEYWORDS_FILENAME = "keywords.tsv";
    public static final String STATISTICS_FILENAME = "statistics.tsv";
    public static final String GRAPHS_FILENAME = "graphs.tsv";
    public static final String COMPENSATION_FILENAME = "compensation.tsv";

    private static final String CLEAN_KEYWORD_PREFIX = "Keyword_";

    private final JobLog _log;
    private final VirtualFile _rootDir;

    private enum KeywordColumnName
    {
        ID,
        Sample,
        Keyword,
        Value
    }

    private enum StatColumnName
    {
        ID,
        Sample,
        Population,
        Parameter,
        Statistic,
        Value
    }

    private enum GraphColumnName
    {
        ID,
        Sample,
        Population,
        Graph,
        Path
    }

    private enum CompensationColumnName
    {
        ID,
        Sample,
        Path
    }

    public enum FCSFilesColumnName
    {
        ID,
        Sample,
        Path
    }

    public enum Options
    {
        ShortStatNames,
        FriendlyImageNames,
        FormatGroupBySample,
        FormatGroupBySamplePopulation,
        FormatGroupBySamplePopulationParameter,
        FormatRowPerStatistic,
        FormatRowPerKeyword
    }

    // Used for testing
    public interface JobLog
    {
        void info(String msg);
        void info(String msg, Throwable t);
        void warn(String msg);
        void error(String msg);
        void error(String msg, Throwable t);
    }

    private static class Log4JAdapter implements JobLog
    {
        Logger _logger;

        private Log4JAdapter(Logger logger)
        {
            _logger = logger;
        }

        @Override
        public void info(String msg)
        {
            _logger.info(msg);
        }

        @Override
        public void info(String msg, Throwable t)
        {
            _logger.info(msg, t);
        }

        @Override
        public void warn(String msg)
        {
            _logger.warn(msg);
        }

        @Override
        public void error(String msg)
        {
            _logger.error(msg);
        }

        @Override
        public void error(String msg, Throwable t)
        {
            _logger.error(msg, t);
        }
    }

    private static class ConsoleLog implements JobLog
    {
        @Override
        public void info(String msg)
        {
            System.out.println(msg);
        }

        @Override
        public void info(String msg, Throwable t)
        {
            System.out.println(msg);
            t.printStackTrace(System.out);
        }

        @Override
        public void warn(String msg)
        {
            System.out.println("WARN: " + msg);
        }

        @Override
        public void error(String msg)
        {
            System.err.println(msg);
        }

        @Override
        public void error(String msg, Throwable t)
        {
            System.err.println(msg);
            t.printStackTrace(System.err);
        }
    }

    public static File extractArchive(File file, File tempDir)
            throws IOException
    {
        File statisticsFile = null;
        if (file.getName().equals(AnalysisSerializer.STATISTICS_FILENAME))
        {
            statisticsFile = file;
        }
        else if (file.isDirectory() && new File(file, AnalysisSerializer.STATISTICS_FILENAME).isFile())
        {
            statisticsFile = new File(file, AnalysisSerializer.STATISTICS_FILENAME);
        }
        else if (file.getName().endsWith(".zip"))
        {
            tempDir.mkdir();

            ZipFile zipFile = new ZipFile(file);

            File importDir;
            String zipBaseName = FileUtil.getBaseName(file);
            ZipEntry zipEntry = zipFile.getEntry(AnalysisSerializer.STATISTICS_FILENAME);
            if (zipEntry != null)
            {
                // Create extra directory under tempDir to extract into.
                importDir = new File(tempDir, zipBaseName);
            }
            else
            {
                zipEntry = zipFile.getEntry(zipBaseName + "/" + AnalysisSerializer.STATISTICS_FILENAME);
                importDir = tempDir;
            }

            if (zipEntry == null)
                throw new IOException("Couldn't find '" + AnalysisSerializer.STATISTICS_FILENAME + "' or '" + zipBaseName + "/" + AnalysisSerializer.STATISTICS_FILENAME + "' in the zip archive.");

            //File importDir = File.createTempFile(zipBaseName, null, tempDir);
            if (importDir.exists() && !FileUtil.deleteDir(importDir))
                throw new IOException("Could not delete the directory \"" + importDir + "\"");

            ZipUtil.unzipToDirectory(file, importDir);
            statisticsFile = new File(importDir, zipEntry.getName());
        }

        if (statisticsFile == null)
            return null;

        return statisticsFile.getParentFile();
    }

    public static ExternalAnalysis readAnalysis(File file) throws IOException
    {
        VirtualFile vf = new FileSystemFile(file);
        AnalysisSerializer as = new AnalysisSerializer(ExternalAnalysis.LOG, vf);
        return as.readAnalysis();
    }

    public AnalysisSerializer(JobLog log, VirtualFile rootDir)
    {
        _log = log;
        _rootDir = rootDir;
    }

    public AnalysisSerializer(Logger log, VirtualFile rootDir)
    {
        _log = new Log4JAdapter(log);
        _rootDir = rootDir;
    }

    public AnalysisSerializer(VirtualFile rootDir)
    {
        _log = new ConsoleLog();
        _rootDir = rootDir;
    }

    private void readStatsRowPerStatistic(TabLoader loader, SampleIdMap<AttributeSet> results)
    {
        Map<String, StatisticSpec> stats = new TreeMap<>();
        int index = 0;
        for (Map<String, Object> row : loader)
        {
            index++;
            String id = (String)row.get(StatColumnName.ID.toString());
            String name = (String)row.get(StatColumnName.Sample.toString());
            String population = (String)(row.get(StatColumnName.Population.toString()));
            String statistic = (String)(row.get(StatColumnName.Statistic.toString()));
            Double value = (Double)row.get(StatColumnName.Value.toString());

            if (StringUtils.isEmpty(name))
            {
                _log.error(String.format("Sample name on row %d must not be empty", index));
                continue;
            }

            if (StringUtils.isEmpty(statistic))
            {
                _log.error(String.format("Statistic name on row %d must not be empty", index));
                continue;
            }

            if (value == null)
            {
                _log.error(String.format("Statistic value on row %d must not be empty", index));
                continue;
            }

            // Previous versions of archive format didn't emit sample ID.  Fallback to using sample name as the id.
            if (id == null)
                id = name;

            AttributeSet attrs = results.computeIfAbsent(id, name, k -> new AttributeSet(ObjectType.fcsAnalysis, null));

            String spec = (population == null ? "" : population + ":") + statistic;
            StatisticSpec stat = stats.computeIfAbsent(spec, StatisticSpec::new);
            // XXX: catch SubsetException ?
            attrs.setStatistic(stat, value);
        }
    }

    private void readStatsGroupedBySample(TabLoader loader, SampleIdMap<AttributeSet> results)
    {
        int index = 0;
        Map<String, StatisticSpec> stats = null;
        for (Map<String, Object> row : loader)
        {
            index++;
            String id = (String)row.get(StatColumnName.ID.toString());
            String name = (String)row.get(StatColumnName.Sample.toString());
            if (StringUtils.isEmpty(name))
            {
                _log.error(String.format("Sample name on row %d must not be empty", index));
                continue;
            }

            // Previous versions of archive format didn't emit sample ID.  Fallback to using sample name as the id.
            if (id == null)
                id = name;

            AttributeSet attrs = results.computeIfAbsent(id, name, k -> new AttributeSet(ObjectType.fcsAnalysis, null));

            if (stats == null)
            {
                stats = new LinkedHashMap<>();
                for (String statistic : row.keySet())
                {
                    if (StringUtils.isEmpty(statistic))
                    {
                        _log.error("Statistic name must not be empty");
                        continue;
                    }

                    if (statistic.equalsIgnoreCase(StatColumnName.ID.toString()) || statistic.equalsIgnoreCase(StatColumnName.Sample.toString()))
                        continue;

                    // XXX: catch SubsetException ?
                    StatisticSpec stat = new StatisticSpec(statistic);
                    stats.put(statistic, stat);
                }
            }

            for (Map.Entry<String, StatisticSpec> entry : stats.entrySet())
            {
                Double value = (Double)row.get(entry.getKey());
                if (value == null)
                    continue;

                attrs.setStatistic(entry.getValue(), value);
            }
        }
    }

    private void readStatsGroupedBySamplePopulation(TabLoader loader, SampleIdMap<AttributeSet> results)
    {
        int index = 0;
        Map<String, StatisticSpec> stats = null;
        for (Map<String, Object> row : loader)
        {
            index++;
            String id = (String)row.get(StatColumnName.ID.toString());
            String name = (String)row.get(StatColumnName.Sample.toString());
            if (StringUtils.isEmpty(name))
            {
                _log.error(String.format("Sample name on row %d must not be empty", index));
                continue;
            }

            String population = (String)row.get(StatColumnName.Population.toString());
            SubsetSpec subset = (population == null ? null : SubsetSpec.fromEscapedString(population));

            // Previous versions of archive format didn't emit sample ID.  Fallback to using sample name as the id.
            if (id == null)
                id = name;

            AttributeSet attrs = results.computeIfAbsent(id, name, k -> new AttributeSet(ObjectType.fcsAnalysis, null));

            if (stats == null)
            {
                stats = new LinkedHashMap<>();
                for (String statistic : row.keySet())
                {
                    if (StringUtils.isEmpty(statistic))
                    {
                        _log.error("Statistic name must not be empty");
                        continue;
                    }

                    if (statistic.equalsIgnoreCase(StatColumnName.ID.toString()) || statistic.equalsIgnoreCase(StatColumnName.Sample.toString()) || statistic.equalsIgnoreCase(StatColumnName.Population.toString()))
                        continue;

                    // For the %ile(NN) parameter, extract the "NN" number from between the parens.
                    String param = null;
                    int openParen = statistic.indexOf("(");
                    int closeParen = statistic.indexOf(")");
                    if (openParen > 0 && closeParen > openParen)
                    {
                        param = statistic.substring(openParen + 1, closeParen);
                        statistic = statistic.substring(0, openParen);
                    }

                    StatisticSpec.STAT stat = StatisticSpec.STAT.fromString(statistic);
                    StatisticSpec statSpec = new StatisticSpec(null, stat, param);
                    stats.put(statistic, statSpec);
                }
            }

            for (Map.Entry<String, StatisticSpec> entry : stats.entrySet())
            {
                Double value = (Double)row.get(entry.getKey());
                if (value == null)
                    continue;

                StatisticSpec spec = entry.getValue();
                if (subset != null)
                    spec = new StatisticSpec(subset, spec.getStatistic(), spec.getParameter());
                attrs.setStatistic(spec, value);
            }
        }
    }

    private void readStatsGroupedBySamplePopulationParameter(TabLoader loader, SampleIdMap<AttributeSet> results)
    {
        int index = 0;
        Map<String, StatisticSpec> stats = null;
        for (Map<String, Object> row : loader)
        {
            index++;
            String id = (String)row.get(StatColumnName.ID.toString());
            String name = (String)row.get(StatColumnName.Sample.toString());
            if (StringUtils.isEmpty(name))
            {
                _log.error(String.format("Sample name on row %d must not be empty", index));
                continue;
            }

            String population = (String)row.get(StatColumnName.Population.toString());
            SubsetSpec subset = (population == null ? null : SubsetSpec.fromEscapedString(population));

            String parameter = (String)row.get(StatColumnName.Parameter.toString());
            if (StringUtils.isEmpty(parameter))
            {
                _log.error(String.format("Parameter name on row %d must not be empty", index));
                continue;
            }

            // Previous versions of archive format didn't emit sample ID.  Fallback to using sample name as the id.
            if (id == null)
                id = name;

            AttributeSet attrs = results.computeIfAbsent(id, name, k -> new AttributeSet(ObjectType.fcsAnalysis, null));

            if (stats == null)
            {
                stats = new LinkedHashMap<>();
                for (String statistic : row.keySet())
                {
                    if (StringUtils.isEmpty(statistic))
                    {
                        _log.error("Statistic name must not be empty");
                        continue;
                    }

                    if (statistic.equalsIgnoreCase(StatColumnName.ID.toString()) || statistic.equalsIgnoreCase(StatColumnName.Sample.toString()) || statistic.equalsIgnoreCase(StatColumnName.Population.toString()) || statistic.equalsIgnoreCase(StatColumnName.Parameter.toString()))
                        continue;

                    // For the %ile(NN) parameter, extract the "NN" number from between the parens.
                    String param = null;
                    int openParen = statistic.indexOf("(");
                    int closeParen = statistic.indexOf(")");
                    if (openParen > 0 && closeParen > openParen)
                    {
                        param = statistic.substring(openParen + 1, closeParen);
                        statistic = statistic.substring(0, openParen);
                    }

                    StatisticSpec.STAT stat = StatisticSpec.STAT.fromString(statistic);
                    StatisticSpec statSpec = new StatisticSpec(null, stat, param);
                    stats.put(statistic, statSpec);
                }
            }

            for (Map.Entry<String, StatisticSpec> entry : stats.entrySet())
            {
                Double value = (Double)row.get(entry.getKey());
                if (value == null)
                    continue;

                // Combine the current parameter with the spec's "NN" parameter part.
                StatisticSpec spec = entry.getValue();
                String param = parameter;
                if (spec.getParameter() != null)
                    param = param + ":" + spec.getParameter();

                // If either the subset is present or we have modified the parameter, create a new spec
                //noinspection StringEquality
                if (subset != null || param != parameter)
                    spec = new StatisticSpec(subset, spec.getStatistic(), param);

                attrs.setStatistic(spec, value);
            }
        }
    }

    private void readStatistics(InputStream statFile, SampleIdMap<AttributeSet> results) throws IOException
    {
        try (Reader reader = Readers.getReader(statFile))
        {
            TabLoader loader = new TabLoader(reader, true);
            loader.setUnescapeBackslashes(false);
            loader.setInferTypes(false);

            // Determine if the stat file is long-skinny or short-wide
            boolean foundIdColumn = false;
            boolean foundSampleColumn = false;
            boolean foundPopulationColumn = false;
            boolean foundParameterColumn = false;
            boolean foundStatisticColumn = false;
            boolean foundValueColumn = false;
            ColumnDescriptor[] columns = loader.getColumns();
            for (ColumnDescriptor col : columns)
            {
                if (col.name.equalsIgnoreCase(StatColumnName.ID.toString()))
                    foundIdColumn = true;
                else if (col.name.equalsIgnoreCase(StatColumnName.Sample.toString()))
                    foundSampleColumn = true;
                else if (col.name.equalsIgnoreCase(StatColumnName.Population.toString()))
                    foundPopulationColumn = true;
                else if (col.name.equalsIgnoreCase(StatColumnName.Parameter.toString()))
                    foundParameterColumn = true;
                else if (col.name.equalsIgnoreCase(StatColumnName.Statistic.toString()))
                    foundStatisticColumn = true;
                else if (col.name.equalsIgnoreCase(StatColumnName.Value.toString()))
                {
                    foundValueColumn = true;
                    col.clazz = Double.class;
                }
                else
                {
                    // assume any other column is a Double stat value
                    col.clazz = Double.class;
                }
            }

            if (!foundSampleColumn)
                throw new RuntimeException("Statistics file must contain a 'Sample' column.");

            if (!foundIdColumn)
                _log.warn("Statistics file doesn't contain an 'ID' column.  The sample ID can be used to uniquely identify samples when sample name is not unique.");

            if (foundPopulationColumn && foundStatisticColumn && foundValueColumn)
                readStatsRowPerStatistic(loader, results);
            else if (foundPopulationColumn && foundParameterColumn)
                readStatsGroupedBySamplePopulationParameter(loader, results);
            else if (foundPopulationColumn)
                readStatsGroupedBySamplePopulation(loader, results);
            else
                readStatsGroupedBySample(loader, results);
        }
    }

    private void readKeywordsRowPerKeyword(TabLoader loader, SampleIdMap<AttributeSet> results)
    {
        int index = 0;
        for (Map<String, Object> row : loader)
        {
            index++;
            String id = (String)row.get(KeywordColumnName.ID.toString());
            String name = (String)row.get(KeywordColumnName.Sample.toString());
            String keyword = (String)(row.get(KeywordColumnName.Keyword.toString()));
            String value = (String)row.get(StatColumnName.Value.toString());

            if (StringUtils.isEmpty(name))
            {
                _log.error(String.format("Sample name on row %d must not be empty", index));
                continue;
            }

            if (StringUtils.isEmpty(keyword))
            {
                _log.error(String.format("Keyword name on row %d must not be empty", index));
                continue;
            }

            // Trim keyword values to null -- FCS files encode empty keyword values as a single space character.
            value = StringUtils.trimToNull(value);

            // Previous versions of archive format didn't emit sample ID.  Fallback to using sample name as the id.
            if (id == null)
                id = name;

            AttributeSet attrs = results.computeIfAbsent(id, name, k -> new AttributeSet(ObjectType.fcsKeywords, null));

            attrs.setKeyword(keyword, value);
        }
    }

    private void readKeywordsGroupedBySample(TabLoader loader, Map<String, String> remappedKeywords, SampleIdMap<AttributeSet> results)
    {
        int index = 0;

        for (Map<String, Object> row : loader)
        {
            index++;
            String id = (String)row.get(KeywordColumnName.ID.toString());
            String name = (String)row.get(KeywordColumnName.Sample.toString());
            if (StringUtils.isEmpty(name))
            {
                _log.error(String.format("Sample name on row %d must not be empty", index));
                continue;
            }

            // Previous versions of archive format didn't emit sample ID.  Fallback to using sample name as the id.
            if (id == null)
                id = name;

            AttributeSet attrs = results.computeIfAbsent(id, name, k -> new AttributeSet(ObjectType.fcsKeywords, null));

            for (String prefixedKeyword : remappedKeywords.keySet())
            {
                // Trim keyword values to null -- FCS files encode empty keyword values as a single space character.
                String value = StringUtils.trimToNull((String)row.get(prefixedKeyword));

                // Get the actual keyword with the "Keyword_" prefix removed
                String actualKeyword = remappedKeywords.get(prefixedKeyword);
                attrs.setKeyword(actualKeyword, value);
            }
        }
    }

    private void readKeywords(InputStream statFile, SampleIdMap<AttributeSet> results) throws IOException
    {
        try (Reader reader = Readers.getReader(statFile))
        {
            TabLoader loader = new TabLoader(reader, true);
            loader.setUnescapeBackslashes(false);
            loader.setInferTypes(false);

            // mapping from prefixed keyword to actual keyword
            Map<String, String> remappedKeywords = new CaseInsensitiveHashMap<>(new LinkedHashMap<>());

            // Determine if the stat file is long-skinny or short-wide
            boolean foundIdColumn = false;
            boolean foundSampleColumn = false;
            boolean foundKeywordColumn = false;
            boolean foundValueColumn = false;
            boolean foundExtraColumns = false;
            ColumnDescriptor[] columns = loader.getColumns();
            for (int i = 0; i < columns.length; i++)
            {
                ColumnDescriptor col = columns[i];
                if (!foundIdColumn && col.name.equalsIgnoreCase(KeywordColumnName.ID.toString()))
                    foundIdColumn = true;
                else if (!foundSampleColumn && col.name.equalsIgnoreCase(KeywordColumnName.Sample.toString()))
                    foundSampleColumn = true;
                else if (!foundKeywordColumn && col.name.equalsIgnoreCase(KeywordColumnName.Keyword.toString()))
                    foundKeywordColumn = true;
                else if (!foundValueColumn && col.name.equalsIgnoreCase(KeywordColumnName.Value.toString()))
                    foundValueColumn = true;
                else
                {
                    foundExtraColumns = true;

                    // assume any other column is a String keyword value
                    col.clazz = String.class;

                    if (StringUtils.isEmpty(col.name))
                    {
                        _log.error(String.format("Keyword name must not be empty in column '%d'", i));
                        continue;
                    }

                    if (col.name.startsWith(CLEAN_KEYWORD_PREFIX))
                    {
                        // remove the "Keyword_" prefix that was added during export
                        String actualKeyword = col.name.substring(CLEAN_KEYWORD_PREFIX.length());
                        remappedKeywords.put(col.name, actualKeyword);
                    }
                    else
                    {
                        // prefix the keyword columns with "Keyword_" to avoid the "Sample" keyword from colliding with the Sample column
                        String prefixed = CLEAN_KEYWORD_PREFIX + col.name;
                        remappedKeywords.put(prefixed, col.name);
                        col.name = prefixed;
                    }
                }
            }

            if (!foundSampleColumn)
                throw new RuntimeException("Keywords file must contain a 'Sample' column.");

            if (!foundIdColumn)
                _log.warn("Keywords file doesn't contain an 'ID' column.  The sample ID can be used to uniquely identify samples when sample name is not unique.");

            if (foundKeywordColumn && foundValueColumn)
                readKeywordsRowPerKeyword(loader, results);
            else
                readKeywordsGroupedBySample(loader, remappedKeywords, results);
        }
    }

    private void readGraphs(InputStream graphsFile, SampleIdMap<AttributeSet> results) throws IOException
    {
        try (Reader reader = Readers.getReader(graphsFile))
        {
            TabLoader loader = new TabLoader(reader, true);
            loader.setUnescapeBackslashes(false);
            loader.setInferTypes(false);

            boolean foundIdColumn = false;
            boolean foundSampleColumn = false;
            boolean foundPopulationColumn = false;
            boolean foundGraphColumn = false;
            boolean foundPathColumn = false;
            ColumnDescriptor[] columns = loader.getColumns();
            for (ColumnDescriptor col : columns)
            {
                if (col.name.equalsIgnoreCase(GraphColumnName.ID.toString()))
                    foundIdColumn = true;
                else if (col.name.equalsIgnoreCase(GraphColumnName.Sample.toString()))
                    foundSampleColumn = true;
                else if (col.name.equalsIgnoreCase(GraphColumnName.Population.toString()))
                    foundPopulationColumn = true;
                else if (col.name.equalsIgnoreCase(GraphColumnName.Graph.toString()))
                    foundGraphColumn = true;
                else if (col.name.equalsIgnoreCase(GraphColumnName.Path.toString()))
                    foundPathColumn = true;
            }

            if (!foundSampleColumn || !foundPopulationColumn || !foundGraphColumn || !foundPathColumn)
                throw new RuntimeException("Graphs file must contain 'Sample', 'Population', 'Graph', and 'Path' columns.");

            if (!foundIdColumn)
                _log.warn("Keywords file doesn't contain an 'ID' column.  The sample ID can be used to uniquely identify samples when sample name is not unique.");

            Map<String, GraphSpec> graphs = new TreeMap<>();
            int index = 0;
            ROWS_LOOP: for (Map<String, Object> row : loader)
            {
                index++;
                String id = (String)row.get(GraphColumnName.ID.toString());
                String name = (String)row.get(GraphColumnName.Sample.toString());
                String population = (String)row.get(GraphColumnName.Population.toString());
                String graph = (String)row.get(GraphColumnName.Graph.toString());
                String path = (String)row.get(GraphColumnName.Path.toString());

                if (StringUtils.isEmpty(name))
                {
                    _log.error(String.format("Sample name on row %d must not be empty", index));
                    continue;
                }

                if (StringUtils.isEmpty(graph))
                {
                    _log.error(String.format("Graph name on row %d must not be empty", index));
                    continue;
                }

                if (path == null)
                {
                    _log.error(String.format("Graph path on row %d must not be empty", index));
                    continue;
                }

                // Previous versions of archive format didn't emit sample ID.  Fallback to using sample name as the id.
                if (id == null)
                    id = name;

                AttributeSet attrs = results.computeIfAbsent(id, name, k -> new AttributeSet(ObjectType.fcsAnalysis, null));

                if (!graph.startsWith("(") && !graph.endsWith(")"))
                    graph = "(" + graph + ")";

                String spec = (population == null ? "" : population) + graph;
                GraphSpec graphSpec = graphs.computeIfAbsent(spec, GraphSpec::new);
                // XXX: catch SubsetException ?

                String[] pathParts = path.split("/");
                VirtualFile dir = _rootDir;
                for (int i = 0; i < pathParts.length-1; i++)
                {
                    dir = dir.getDir(pathParts[i]);
                    if (dir == null)
                    {
                        _log.info(String.format("Skipping graph '%s' for sample '%s'.  Image directory '%s' not found in dir '%s'", spec, name, pathParts[i], dir.getLocation()));
                        continue ROWS_LOOP;
                    }
                }

                String imageFile = pathParts[pathParts.length-1];
                InputStream image = dir.getInputStream(imageFile);
                if (image == null)
                {
                    _log.info(String.format("Skipping graph '%s' for sample '%s'.  Image file '%s' not found in dir '%s'", spec, name, imageFile, dir.getLocation()));
                    continue;
                }

                byte[] b = IOUtils.toByteArray(image);
                image.close();
                attrs.setGraph(graphSpec, b);
            }
        }
    }

    private void readCompMatrices(InputStream compensationFile, SampleIdMap<CompensationMatrix> matrices) throws IOException
    {
        try (Reader reader = Readers.getReader(compensationFile))
        {
            TabLoader loader = new TabLoader(reader, true);
            loader.setUnescapeBackslashes(false);
            loader.setInferTypes(false);

            // Determine if the stat file is long-skinny or short-wide
            boolean foundIdColumn = false;
            boolean foundSampleColumn = false;
            boolean foundPathColumn = false;
            ColumnDescriptor[] columns = loader.getColumns();
            for (ColumnDescriptor col : columns)
            {
                if (col.name.equalsIgnoreCase(CompensationColumnName.ID.toString()))
                    foundIdColumn = true;
                if (col.name.equalsIgnoreCase(CompensationColumnName.Sample.toString()))
                    foundSampleColumn = true;
                else if (col.name.equalsIgnoreCase(CompensationColumnName.Path.toString()))
                    foundPathColumn = true;
            }

            if (!foundSampleColumn || !foundPathColumn)
                throw new RuntimeException("Compensation file must contain a 'Sample' and 'Path' column.");

            if (!foundIdColumn)
                _log.warn("Keywords file doesn't contain an 'ID' column.  The sample ID can be used to uniquely identify samples when sample name is not unique.");

            readCompMatrices(loader, matrices);
        }
    }

    private void readCompMatrices(TabLoader loader, SampleIdMap<CompensationMatrix> matrices) throws IOException
    {
        int index = 0;
        Map<String, CompensationMatrix> compPaths = new HashMap<>();
        for (Map<String, Object> row : loader)
        {
            index++;
            String id = StringUtils.trimToNull((String)row.get(CompensationColumnName.ID.toString()));
            String sampleName = StringUtils.trimToNull((String)row.get(CompensationColumnName.Sample.toString()));
            String path = StringUtils.trimToNull((String)row.get(CompensationColumnName.Path.toString()));

            if (sampleName == null || path == null)
            {
                _log.error(String.format("Compensation file requires sample name and path on line %d", index));
                continue;
            }

            CompensationMatrix matrix = compPaths.get(path);
            if (matrix == null)
            {
                InputStream is = _rootDir.getInputStream(path);
                if (is == null)
                {
                    _log.error(String.format("Compensation matrix '%s' not found", path));
                    continue;
                }

                try
                {
                    matrix = new CompensationMatrix(is);
                }
                catch (IOException ioe)
                {
                    throw ioe;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }

                compPaths.put(path, matrix);
            }

            // Previous versions of archive format didn't emit sample ID.  Fallback to using sample name as the id.
            if (id == null)
                id = sampleName;

            matrices.put(id, sampleName, matrix);
        }
    }

    public ExternalAnalysis readAnalysis() throws IOException
    {
        Tuple3<SampleIdMap<AttributeSet>, SampleIdMap<AttributeSet>, SampleIdMap<CompensationMatrix>> analysis = readAnalysisTuple();
        if (analysis == null)
            return null;

        String name = new File(_rootDir.getLocation()).getName();
        return new ExternalAnalysis(name, analysis.first, analysis.second, analysis.third);
    }

    // Read keywords, stats, graphs, and compensation matrices.
    public Tuple3<SampleIdMap<AttributeSet>, SampleIdMap<AttributeSet>, SampleIdMap<CompensationMatrix>> readAnalysisTuple() throws IOException
    {
        SampleIdMap<AttributeSet> keywords = new SampleIdMap<>();
        SampleIdMap<AttributeSet> results = new SampleIdMap<>();
        SampleIdMap<CompensationMatrix> matrices = new SampleIdMap<>();

        InputStream keywordsFile = _rootDir.getInputStream(KEYWORDS_FILENAME);
        if (keywordsFile != null)
        {
            _log.info(String.format("Reading keywords from '%s/%s'", _rootDir.getLocation(), KEYWORDS_FILENAME));
            readKeywords(keywordsFile, keywords);
        }

        InputStream statisticsFile = _rootDir.getInputStream(STATISTICS_FILENAME);
        if (statisticsFile != null)
        {
            _log.info(String.format("Reading statistics from '%s/%s'", _rootDir.getLocation(), STATISTICS_FILENAME));
            readStatistics(statisticsFile, results);
        }

        InputStream graphsFile = _rootDir.getInputStream(GRAPHS_FILENAME);
        if (graphsFile != null)
        {
            _log.info(String.format("Reading graphs from '%s/%s'", _rootDir.getLocation(), GRAPHS_FILENAME));
            readGraphs(graphsFile, results);
        }

        InputStream compensationFile = _rootDir.getInputStream(COMPENSATION_FILENAME);
        if (compensationFile != null)
        {
            _log.info(String.format("Reading comp. matrices from '%s/%s'", _rootDir.getLocation(), COMPENSATION_FILENAME));
            readCompMatrices(compensationFile, matrices);
        }

        /*
        // Look for directories containing either a statistics or graphs file.
        FileFilter sampleDirFilter = new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return file.isDirectory() && (new File(file, STATISTICS_FILENAME).isFile() || new File(file, GRAPHS_FILENAME).isFile());
            }
        };

        for (File sampleDir : _rootDir.listFiles(sampleDirFilter))
        {
            File sampleStatisticsFile = new File(sampleDir, STATISTICS_FILENAME);
            if (sampleStatisticsFile.isFile())
                readStatistics(sampleStatisticsFile, results);

            File sampleGraphsFile = new File(sampleDir, GRAPHS_FILENAME);
            if (sampleGraphsFile.isFile())
                readGraphs(sampleGraphsFile, results);
        }
        */

        if (keywords.isEmpty() && results.isEmpty())
        {
            _log.error("Nothing to import");
            return Tuple3.of(SampleIdMap.emptyMap(), SampleIdMap.emptyMap(), SampleIdMap.emptyMap());
        }
        else
        {
            _log.info(String.format("Read %d keyword samples, %d result samples, and %d comp. matrices.", keywords.size(), results.size(), matrices.size()));
        }

        return Tuple3.of(keywords, results, matrices);
    }

    private @NotNull Pair<List<String>, List<Map<String, Object>>> writeStatisticsRowPerStatistic(SampleIdMap<AttributeSet> analysis, boolean shortStatNames)
    {
        List<String> columns = new ArrayList<>();
        columns.add(StatColumnName.ID.toString());
        columns.add(StatColumnName.Sample.toString());
        columns.add(StatColumnName.Population.toString());
        columns.add(StatColumnName.Statistic.toString());
        columns.add(StatColumnName.Value.toString());

        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : analysis.idSet())
        {
            AttributeSet attrs = analysis.getById(id);
            if (attrs == null)
                continue;

            String name = analysis.getNameForId(id);

            Map<StatisticSpec, Double> statistics = attrs.getStatistics();
            for (StatisticSpec stat : statistics.keySet())
            {
                Double value = statistics.get(stat);

                // now create a row to be written
                List<Object> values = new ArrayList<>(columns.size());
                values.add(id);
                values.add(name);
                values.add(stat.getSubset());
                StatisticSpec statOnly = new StatisticSpec(null, stat.getStatistic(), stat.getParameter());
                values.add(shortStatNames ? statOnly.toShortString(true) : statOnly.toString());
                values.add(value);

                rows.add(rowMapFactory.getRowMap(values));
            }
        }

        return Pair.of(columns, rows);
    }

    private @NotNull Pair<List<String>, List<Map<String, Object>>> writeStatisticsGroupedBySample(SampleIdMap<AttributeSet> analysis, boolean shortStatNames)
    {
        // collect all used statistics
        Set<StatisticSpec> stats = new TreeSet<>();
        for (String id : analysis.idSet())
        {
            AttributeSet attrs = analysis.getById(id);
            if (attrs == null)
                continue;

            stats.addAll(attrs.getStatisticNames());
        }

        List<String> columns = new ArrayList<>();
        columns.add(StatColumnName.ID.toString());
        columns.add(StatColumnName.Sample.toString());
        for (StatisticSpec stat : stats)
            columns.add(shortStatNames ? stat.toShortString(true) : stat.toString());

        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : analysis.idSet())
        {
            AttributeSet attrs = analysis.getById(id);
            if (attrs == null)
                continue;

            String name = analysis.getNameForId(id);

            // now create a row to be written
            List<Object> values = new ArrayList<>(columns.size());
            values.add(id);
            values.add(name);

            Map<StatisticSpec, Double> statistics = attrs.getStatistics();
            for (StatisticSpec stat : stats)
            {
                Double value = statistics.get(stat);
                values.add(value);
            }

            rows.add(rowMapFactory.getRowMap(values));
        }

        return Pair.of(columns, rows);
    }

    private @NotNull Pair<List<String>, List<Map<String, Object>>> writeStatisticsGroupedBySamplePopulation(SampleIdMap<AttributeSet> analysis, boolean shortStatNames)
    {
        // collect all used statistics with the population removed
        Set<StatisticSpec> stats = new TreeSet<>();
        for (String id : analysis.idSet())
        {
            AttributeSet attrs = analysis.getById(id);
            if (attrs == null)
                continue;

            for (StatisticSpec stat : attrs.getStatisticNames())
                stats.add(new StatisticSpec(null, stat.getStatistic(), stat.getParameter()));
        }

        // Columns in the statistics.tsv output
        List<String> columns = new ArrayList<>(2+stats.size());
        columns.add(StatColumnName.ID.toString());
        columns.add(StatColumnName.Sample.toString());
        columns.add(StatColumnName.Population.toString());
        for (StatisticSpec stat : stats)
            columns.add(shortStatNames ? stat.toShortString(true) : stat.toString());

        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : analysis.idSet())
        {
            AttributeSet attrs = analysis.getById(id);
            if (attrs == null)
                continue;

            String name = analysis.getNameForId(id);

            // collect the samples statistics grouped by population
            Map<SubsetSpec, Map<StatisticSpec, Double>> populations = new TreeMap<>(SubsetSpec.COMPARATOR);
            Map<StatisticSpec, Double> statistics = attrs.getStatistics();
            for (Map.Entry<StatisticSpec, Double> entry : statistics.entrySet())
            {
                SubsetSpec pop = entry.getKey().getSubset();
                StatisticSpec s = new StatisticSpec(null, entry.getKey().getStatistic(), entry.getKey().getParameter());
                Double value = entry.getValue();

                Map<StatisticSpec, Double> statsByPopulation = populations.computeIfAbsent(pop, k -> new HashMap<>());

                statsByPopulation.put(s, value);
            }

            // now create the rows to be written
            for (Map.Entry<SubsetSpec, Map<StatisticSpec, Double>> entry : populations.entrySet())
            {
                List<Object> values = new ArrayList<>(columns.size());
                values.add(id);
                values.add(name);
                values.add(entry.getKey());

                for (StatisticSpec stat : stats)
                {
                    Double d = entry.getValue().get(stat);
                    values.add(d);
                }

                rows.add(rowMapFactory.getRowMap(values));
            }
        }

        return Pair.of(columns, rows);
    }

    private @NotNull Pair<List<String>, List<Map<String, Object>>> writeStatisticsGroupedBySamplePopulationParameter(SampleIdMap<AttributeSet> analysis, boolean shortStatNames)
    {
        // collect all used statistics with the population and parameter removed
        Set<StatisticSpec> stats = new TreeSet<>();
        Set<String> parameters = new TreeSet<>();
        for (String id : analysis.idSet())
        {
            AttributeSet attrs = analysis.getById(id);
            if (attrs == null)
                continue;

            for (StatisticSpec stat : attrs.getStatisticNames())
            {
                String parameter = stat.getParameter();
                // add the parameter name part to parameters set, keep the remaining part (e.g. "NN" for "%ile(parm:NN)" type stats)
                if (parameter != null)
                {
                    int indexColon = parameter.indexOf(":");
                    if (indexColon > 0)
                    {
                        parameters.add(parameter.substring(0, indexColon));
                        parameter = parameter.substring(indexColon+1);
                    }
                    else
                    {
                        parameters.add(parameter);
                        parameter = null;
                    }
                }
                stats.add(new StatisticSpec(null, stat.getStatistic(), parameter));
            }
        }

        // Columns in the statistics.tsv output
        List<String> columns = new ArrayList<>(2+stats.size());
        columns.add(StatColumnName.ID.toString());
        columns.add(StatColumnName.Sample.toString());
        columns.add(StatColumnName.Population.toString());
        columns.add(StatColumnName.Parameter.toString());
        for (StatisticSpec stat : stats)
            columns.add(shortStatNames ? stat.toShortString(true) : stat.toString());

        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : analysis.idSet())
        {
            AttributeSet attrs = analysis.getById(id);
            if (attrs == null)
                continue;

            String name = analysis.getNameForId(id);

            // collect the samples statistics grouped by population and parameter
            Map<Pair<SubsetSpec, String>, Map<StatisticSpec, Double>> map = new TreeMap<>(SUBSET_POPULATION_COMPARATOR);
            Map<StatisticSpec, Double> statistics = attrs.getStatistics();
            for (Map.Entry<StatisticSpec, Double> entry : statistics.entrySet())
            {
                SubsetSpec pop = entry.getKey().getSubset();
                StatisticSpec.STAT stat = entry.getKey().getStatistic();

                String parameter = entry.getKey().getParameter();
                String remainingParameter = null;
                if (parameter != null)
                {
                    int indexColon = parameter.indexOf(":");
                    if (indexColon > 0)
                    {
                        remainingParameter = parameter.substring(indexColon+1);
                        parameter = parameter.substring(0, indexColon);
                    }
                }

                Pair<SubsetSpec, String> pair = new Pair<>(pop, parameter);

                StatisticSpec s = new StatisticSpec(null, stat, remainingParameter);
                Double value = entry.getValue();

                Map<StatisticSpec, Double> statsByPopulationParameter = map.computeIfAbsent(pair, k -> new HashMap<>());

                statsByPopulationParameter.put(s, value);
            }

            // now create the rows to be written
            for (Map.Entry<Pair<SubsetSpec, String>, Map<StatisticSpec, Double>> entry : map.entrySet())
            {
                List<Object> values = new ArrayList<>(columns.size());
                values.add(id);
                values.add(name);
                values.add(entry.getKey().first);
                values.add(entry.getKey().second);

                for (StatisticSpec stat : stats)
                {
                    Double d = entry.getValue().get(stat);
                    values.add(d);
                }

                rows.add(rowMapFactory.getRowMap(values));
            }
        }

        return Pair.of(columns, rows);
    }

    private final Comparator<Pair<SubsetSpec, String>> SUBSET_POPULATION_COMPARATOR = (pair1, pair2) ->
    {
        if (pair1 == pair2)
            return 0;
        if (pair1 == null)
            return -1;
        if (pair2 == null)
            return 1;

        int firstCompare = SubsetSpec.COMPARATOR.compare(pair1.first, pair2.first);
        if (firstCompare != 0)
            return firstCompare;

        if (pair1.second != null)
            return pair1.second.compareTo(pair2.second);

        // pair1.second is null; check if pair2.second is null as well
        if (pair2.second == null)
            return 0;

        // pai1.second is null, pair2.second is non-null
        return -1;
    };

    private void writeStatistics(SampleIdMap<AttributeSet> analysis, EnumSet<Options> saveOptions) throws IOException
    {
        if (analysis == null || analysis.isEmpty())
            return;

        boolean shortStatNames = saveOptions.contains(Options.ShortStatNames);

        Pair<List<String>, List<Map<String, Object>>> results;
        if (saveOptions.contains(Options.FormatRowPerStatistic))
            results = writeStatisticsRowPerStatistic(analysis, shortStatNames);
        else if (saveOptions.contains(Options.FormatGroupBySample))
            results = writeStatisticsGroupedBySample(analysis, shortStatNames);
        else if (saveOptions.contains(Options.FormatGroupBySamplePopulationParameter))
            results = writeStatisticsGroupedBySamplePopulationParameter(analysis, shortStatNames);
        else //default: (saveOptions.contains(Options.FormatGroupBySamplePopulation))
            results = writeStatisticsGroupedBySamplePopulation(analysis, shortStatNames);

        // write the tsv file
        if (results.second.size() > 0)
        {
            OutputStream statisticsFile = _rootDir.getOutputStream(STATISTICS_FILENAME);

            try (PrintWriter pw = PrintWriters.getPrintWriter(statisticsFile); TSVWriter writer = new TSVMapWriter(results.first, results.second))
            {
                writer.write(pw);
            }
        }
    }

    private @NotNull Pair<List<String>, List<Map<String, Object>>> writeKeywordsRowPerKeyword(SampleIdMap<AttributeSet> keywords)
    {
        List<String> columns = new ArrayList<>();
        columns.add(KeywordColumnName.ID.toString());
        columns.add(KeywordColumnName.Sample.toString());
        columns.add(KeywordColumnName.Keyword.toString());
        columns.add(KeywordColumnName.Value.toString());

        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : keywords.idSet())
        {
            AttributeSet attrs = keywords.getById(id);
            if (attrs == null)
                continue;

            String name = keywords.getNameForId(id);

            Map<String, String> keys = attrs.getKeywords();
            for (String key : keys.keySet())
            {
                String value = keys.get(key);

                // now create a row to be written
                List<Object> values = new ArrayList<>(columns.size());
                values.add(id);
                values.add(name);
                values.add(key);
                values.add(value);

                rows.add(rowMapFactory.getRowMap(values));
            }
        }

        return Pair.of(columns, rows);
    }

    private Pair<List<String>, List<Map<String, Object>>> writeKeywordsGroupedBySample(SampleIdMap<AttributeSet> keywords)
    {
        // collect all keywords used in the header row
        Set<String> allCleanKeywords = new TreeSet<>();

        // mapping from cleaned keyword to actual keyword
        Map<String, String> remappedKeywords = new CaseInsensitiveHashMap<>();

        for (String id : keywords.idSet())
        {
            AttributeSet attrs = keywords.getById(id);
            if (attrs == null)
                continue;

            for (String keyword : attrs.getKeywordNames())
            {
                String cleanKeyword = keyword;
                if (keyword.equalsIgnoreCase(KeywordColumnName.ID.toString()) || keyword.equalsIgnoreCase(KeywordColumnName.Sample.toString()))
                {
                    cleanKeyword = CLEAN_KEYWORD_PREFIX + keyword;
                }

                if (allCleanKeywords.contains(cleanKeyword))
                    continue;

                allCleanKeywords.add(cleanKeyword);
                remappedKeywords.put(cleanKeyword, keyword);
            }
        }

        List<String> columns = new ArrayList<>();
        columns.add(KeywordColumnName.ID.toString());
        columns.add(KeywordColumnName.Sample.toString());
        columns.addAll(allCleanKeywords);

        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : keywords.idSet())
        {
            AttributeSet attrs = keywords.getById(id);
            if (attrs == null)
                continue;

            String name = keywords.getNameForId(id);

            // now create a row to be written
            List<Object> values = new ArrayList<>(columns.size());
            values.add(id);
            values.add(name);

            Map<String, String> keys = attrs.getKeywords();
            for (String cleanKey : allCleanKeywords)
            {
                String actualKey = remappedKeywords.get(cleanKey);
                String value = keys.get(actualKey);

                values.add(value);
            }
            rows.add(rowMapFactory.getRowMap(values));
        }

        return Pair.of(columns, rows);
    }

    private void writeKeywords(SampleIdMap<AttributeSet> keywords, EnumSet<Options> saveOptions) throws IOException
    {
        if (keywords == null || keywords.isEmpty())
            return;

        Pair<List<String>, List<Map<String, Object>>> results;
        if (saveOptions.contains(Options.FormatRowPerKeyword))
            results = writeKeywordsRowPerKeyword(keywords);
        else /* default: if (saveOptions.contains(Options.FormatGroupBySample)) */
            results = writeKeywordsGroupedBySample(keywords);

        // write the tsv file
        if (results.second.size() > 0)
        {
            OutputStream statisticsFile = _rootDir.getOutputStream(KEYWORDS_FILENAME);

            try (PrintWriter pw = PrintWriters.getPrintWriter(statisticsFile); TSVWriter writer = new TSVMapWriter(results.first, results.second))
            {
                writer.write(pw);
            }
        }
    }

    private void writeCompMatrices(SampleIdMap<CompensationMatrix> matrices, EnumSet<Options> saveOptions) throws IOException
    {
        if (matrices == null || matrices.isEmpty())
            return;

        List<String> columns = Arrays.asList(
                CompensationColumnName.ID.toString(),
                CompensationColumnName.Sample.toString(),
                CompensationColumnName.Path.toString());
        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);
        List<Map<String, Object>> rows = new ArrayList<>();

        Map<CompensationMatrix, String> compPaths = new HashMap<>();
        for (String id : matrices.idSet())
        {
            CompensationMatrix matrix = matrices.getById(id);
            if (matrix == null)
                continue;

            String name = matrices.getNameForId(id);

            String path = compPaths.get(matrix);
            if (path == null)
            {
                // write out new matrix
                path = _rootDir.makeLegalName(matrix.getName());
                compPaths.put(matrix, path);

                OutputStream os = _rootDir.getOutputStream(path);
                IOUtils.write(matrix.toExportFormat(), os);
                os.close();
            }

            rows.add(rowMapFactory.getRowMap(id, name, path));
        }

        // write the tsv file
        if (rows.size() > 0)
        {
            OutputStream statisticsFile = _rootDir.getOutputStream(COMPENSATION_FILENAME);

            try (PrintWriter pw = PrintWriters.getPrintWriter(statisticsFile); TSVWriter writer = new TSVMapWriter(columns, rows))
            {
                writer.write(pw);
            }
        }
    }

    public static String generateFriendlyImageName(GraphSpec graph)
    {
        StringBuilder path = new StringBuilder();
        if (graph.getSubset() == null)
            path.append("Ungated");
        else
            path.append(graph.getSubset().toString().replaceAll("/", "~"));
        if (graph.getParameters() != null && graph.getParameters().length > 0)
        {
            path.append(" (");

            String[] parameters = graph.getParameters();
            for (int i = 0, length = parameters.length; i < length; i++)
            {
                String param = parameters[i];
                if (CompensationMatrix.isParamCompensated(param))
                    param = "comp-" + param.substring(1, param.length() - 1);

                if (i > 0)
                    path.append(", ");
                path.append(param);
            }

            path.append(")");
        }
        path.append(".png");
        return path.toString();
    }

    private void writeGraphs(SampleIdMap<AttributeSet> analysis, boolean friendlyImageNames) throws IOException
    {
        if (analysis == null || analysis.isEmpty())
            return;

        List<String> columns = new ArrayList<>();
        columns.add(GraphColumnName.ID.toString());
        columns.add(GraphColumnName.Sample.toString());
        columns.add(GraphColumnName.Population.toString());
        columns.add(GraphColumnName.Graph.toString());
        columns.add(GraphColumnName.Path.toString());

        RowMapFactory<Object> rowMapFactory = new RowMapFactory<>(columns);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : analysis.idSet())
        {
            AttributeSet attrs = analysis.getById(id);
            if (attrs == null)
                continue;

            String name = analysis.getNameForId(id);

            String graphDirName = FileUtil.getBaseName(id); // NOTE: We used to use the name for the graph dir, but it isn't always unique
            VirtualFile graphDir = null;

            Map<GraphSpec, byte[]> graphs = attrs.getGraphs();
            for (GraphSpec graph : graphs.keySet())
            {
                byte[] bytes = graphs.get(graph);
                if (bytes == null || bytes.length == 0)
                    continue;

                // create a nice name for the image or use a md5 sum hash
                String imageName;
                if (friendlyImageNames)
                    imageName = generateFriendlyImageName(graph);
                else
                    imageName = FileUtil.md5sum(bytes) + ".png";
                imageName = _rootDir.makeLegalName(imageName);

                // write the image bytes
                if (graphDir == null)
                    graphDir = _rootDir.getDir(graphDirName);
                OutputStream os = graphDir.getOutputStream(imageName);
                IOUtils.write(bytes, os);
                os.flush();
                os.close();

                // create a row to be written
                List<Object> values = new ArrayList<>(columns.size());
                values.add(id);
                values.add(name);
                values.add(graph.getSubset()); // XXX: need to write "" instead of null
                String graphParams = null;
                if (graph.getParameters() != null && graph.getParameters().length > 0)
                {
                    graphParams = StringUtils.join(graph.getParameters(), ":");
                }
                values.add(graphParams);
                values.add(_rootDir.makeLegalName(graphDirName) + "/" + imageName);

                rows.add(rowMapFactory.getRowMap(values));
            }
        }

        // write the graphs.tsv
        if (rows.size() > 0)
        {
            OutputStream statisticsFile = _rootDir.getOutputStream(GRAPHS_FILENAME);

            try (PrintWriter pw = PrintWriters.getPrintWriter(statisticsFile); TSVWriter writer = new TSVMapWriter(columns, rows))
            {
                writer.write(pw);
            }
        }
    }

    public void writeAnalysis(SampleIdMap<AttributeSet> keywords, SampleIdMap<AttributeSet> analysis, SampleIdMap<CompensationMatrix> matrices) throws IOException
    {
        writeAnalysis(keywords, analysis, matrices, EnumSet.noneOf(Options.class));
    }

    public void writeAnalysis(SampleIdMap<AttributeSet> keywords, SampleIdMap<AttributeSet> analysis, SampleIdMap<CompensationMatrix> matrices, EnumSet<Options> saveOptions) throws IOException
    {
        writeGraphs(analysis, saveOptions.contains(Options.FriendlyImageNames));
        writeStatistics(analysis, saveOptions);
        writeKeywords(keywords, saveOptions);
        writeCompMatrices(matrices, saveOptions);
    }

    public static class TestCase extends Assert
    {
        static class TestLog implements AnalysisSerializer.JobLog
        {
            List<String> _info = new ArrayList<>();
            List<String> _warn = new ArrayList<>();
            List<String> _error = new ArrayList<>();

            public void info(String msg) { _info.add(msg); }
            public void info(String msg, Throwable t) { _info.add(msg); }

            public void warn(String msg) { _warn.add(msg); }

            public void error(String msg) { _error.add(msg); }
            public void error(String msg, Throwable t) { _error.add(msg); }
        }

        @Test
        public void testRead() throws Exception
        {
            File dir = JunitUtil.getSampleData(null,  "flow/external-analysis");
            FileSystemFile rootDir = new FileSystemFile(dir);
            TestLog log = new TestLog();
            AnalysisSerializer serializer = new AnalysisSerializer(log, rootDir);
            Tuple3<SampleIdMap<AttributeSet>, SampleIdMap<AttributeSet>, SampleIdMap<CompensationMatrix>> tuple = serializer.readAnalysisTuple();

            SampleIdMap<AttributeSet> keywords = tuple.first;
            SampleIdMap<AttributeSet> results = tuple.second;
            SampleIdMap<CompensationMatrix> matrices = tuple.third;

            assertEquals("Expected to load two samples", 2, results.size());
            assertTrue(results.containsName("118760.fcs"));
            assertTrue(results.containsName("119043.fcs"));

            List<AttributeSet> attrsList = results.getByName("119043.fcs");
            assertEquals("Expected only one attribute set for 119043.fcs", 1, attrsList.size());
            AttributeSet attrs = attrsList.get(0);
            assertEquals("Expected 117 statistics for 119043.fcs", 117, attrs.getStatisticNames().size());
            assertEquals("Expected 12 statistics for 119043.fcs", 13, attrs.getGraphNames().size());
        }
    }
}
