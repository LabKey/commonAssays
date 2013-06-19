/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
package org.labkey.flow.util;

import org.labkey.api.util.Pair;
import org.labkey.flow.analysis.model.ISampleInfo;
import org.labkey.flow.data.FlowFCSFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * User: kevink
 * Date: 11/14/12
 */
public class SampleUtil
{
    private static final String[] KEYWORDS = new String[] { "$FIL", "GUID", "$TOT", "$PAR", "$DATE", "$ETIM", "EXPORT TIME" };
    private static final int MAX_MATCHES = KEYWORDS.length+1;
    private static final int MIN_MATCHES = 2;

    private static class FlowFCSFileList extends ArrayList<FlowFCSFile>
    {
        private FlowFCSFileList(int initialCapacity)
        {
            super(initialCapacity);
        }
    }

    public static int matches(String[] a, String[] b)
    {
        assert a.length == b.length;
        int dist = -1;
        for (int i = 0, len = a.length; i < len; i++)
            if (Objects.equals(a[i], b[i]))
                dist++;

        return dist;
    }

    private static String[] keywordValues(String name, Map<String, String> keywords)
    {
        String[] values = new String[KEYWORDS.length+1];
        values[0] = name;
        for (int i = 0, len = KEYWORDS.length; i < len; i++)
        {
            String value = keywords.get(KEYWORDS[i]);
            if (value != null)
                value = value.trim();
            values[i+1] = value;
        }

        return values;
    }

    /**
     * Give the list of workspace samples and previously imported samples, calculate each
     * workspace sample's exact match and a list of partial matches.
     */
    public static Map<ISampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>> resolveSamples(List<? extends ISampleInfo> samples, List<FlowFCSFile> files)
    {
        if (files.isEmpty())
            return Collections.emptyMap();

        // Don't include FCSFile wells created for attaching extra keywords.
        List<FlowFCSFile> originalFiles = new ArrayList<>(files.size());
        Map<String, FlowFCSFile> originalFileMap = new HashMap<>();
        for (FlowFCSFile file : files)
        {
            if (!file.isOriginalFCSFile())
                continue;

            originalFiles.add(file);
            originalFileMap.put(file.getName(), file);
        }

        Map<FlowFCSFile, String[]> fileKeywordMap = new IdentityHashMap<>();

        Map<ISampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>> resolved = new LinkedHashMap<>();

        for (ISampleInfo sample : samples)
        {
            FlowFCSFile perfectMatch = null;
            List<FlowFCSFile> partialMatches = new ArrayList<>(10);

            Map<String, String> keywords = sample.getKeywords();
            if (keywords.size() == 0)
            {
                // No keywords available. Match only based on name
                FlowFCSFile file = originalFileMap.get(sample.getSampleName());
                if (file == null)
                    file = originalFileMap.get(sample.getSampleId());

                if (file != null)
                {
                    perfectMatch = file;
                    partialMatches.add(file);
                }
            }
            else
            {
                // Match based on keyword values
                String name = sample.getSampleName();
                if (name == null || name.length() == 0)
                    name = sample.getFilename();
                String[] values = keywordValues(name, keywords);

                FlowFCSFileList[] candidates = new FlowFCSFileList[MAX_MATCHES];
                for (int i = 0; i < MAX_MATCHES; i++)
                    candidates[i] = new FlowFCSFileList(5);

                // Calculate the difference between the FlowFCSFile and the Workspace.SampleInfo
                // and store the candidates ordered by their matches score (higher is better.)
                int maxMatches = 0;
                for (FlowFCSFile file : originalFiles)
                {
                    String[] fileKeywords = fileKeywordMap.get(file);
                    if (fileKeywords == null)
                    {
                        fileKeywords = keywordValues(file.getName(), file.getKeywords(KEYWORDS));
                        fileKeywordMap.put(file, fileKeywords);
                    }

                    int matches = matches(values, fileKeywords);
                    if (matches < MIN_MATCHES)
                        continue;

                    if (matches > maxMatches)
                        maxMatches = matches;

                    candidates[matches].add(file);
                }

                if (maxMatches >= MIN_MATCHES && candidates[maxMatches].size() == 1)
                    perfectMatch = candidates[maxMatches].get(0);

                for (int i = candidates.length-1; i >= 0; i--)
                    partialMatches.addAll(candidates[i]);
            }

            resolved.put(sample, Pair.of(perfectMatch, partialMatches));
        }

        return resolved;
    }

}
