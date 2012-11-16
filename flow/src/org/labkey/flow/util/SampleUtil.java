package org.labkey.flow.util;

import org.apache.commons.lang3.ObjectUtils;
import org.labkey.api.util.Pair;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.data.FlowFCSFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: 11/14/12
 */
public class SampleUtil
{
    private static final String[] KEYWORDS = new String[] { "$FIL", "GUID", "$TOT", "$PAR", "$DATE", "$ETIM" };
    private static final int MIN_DISTANCE = 4;

    private static class FlowFCSFileList extends ArrayList<FlowFCSFile>
    {
        private FlowFCSFileList(int initialCapacity)
        {
            super(initialCapacity);
        }
    }

    public static int distance(String[] a, String[] b)
    {
        assert a.length == b.length;
        int dist = 0;
        for (int i = 0, len = a.length; i < len; i++)
            if (!ObjectUtils.equals(a[i], b[i]))
                dist++;

        return dist;
    }

    /**
     * Give the list of workspace samples and previously imported samples, calculate each
     * workspace sample's exact match and a list of partial matches.
     */
    public static Map<Workspace.SampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>> resolveSamples(List<Workspace.SampleInfo> samples, List<FlowFCSFile> files)
    {
        if (files.isEmpty())
            return Collections.emptyMap();

        Map<FlowFCSFile, String[]> fileKeywordMap = new IdentityHashMap<FlowFCSFile, String[]>();
        for (FlowFCSFile file : files)
        {
            // Don't include FCSFile wells created for attaching extra keywords.
            if (!file.isOriginalFCSFile())
                continue;

            //String[] values = file.getKeywords(KEYWORDS);
            Map<String, String> keywords = file.getKeywords(KEYWORDS);
            String[] values = new String[KEYWORDS.length];
            for (int i = 0, len = KEYWORDS.length; i < len; i++)
                values[i] = keywords.get(KEYWORDS[i]);

            fileKeywordMap.put(file, values);
        }

        Map<Workspace.SampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>> resolved = new LinkedHashMap<Workspace.SampleInfo, Pair<FlowFCSFile, List<FlowFCSFile>>>();

        for (Workspace.SampleInfo sample : samples)
        {
            Map<String, String> keywords = sample.getKeywords();
            String[] values = new String[KEYWORDS.length];
            for (int i = 0, len = KEYWORDS.length; i < len; i++)
                values[i] = keywords.get(KEYWORDS[i]);

            FlowFCSFileList[] candidates = new FlowFCSFileList[MIN_DISTANCE];
            for (int i = 0; i < MIN_DISTANCE; i++)
                candidates[i] = new FlowFCSFileList(5);

            // Calculate the difference between the FlowFCSFile and the Workspace.SampleInfo
            // and store the candidates ordered by their distance score (0 is exact match).
            for (FlowFCSFile file : files)
            {
                String[] fileKeywords = fileKeywordMap.get(file);
                if (fileKeywords == null)
                    continue;

                int dist = distance(values, fileKeywords);
                if (dist >= MIN_DISTANCE)
                    continue;

                candidates[dist].add(file);
            }

            FlowFCSFile perfectMatch = null;
            if (candidates[0].size() == 1)
                perfectMatch = candidates[0].get(0);

            List<FlowFCSFile> partialMatches = new ArrayList<FlowFCSFile>(10);
            for (int i = 0; i < candidates.length; i++)
                partialMatches.addAll(candidates[i]);

            resolved.put(sample, Pair.of(perfectMatch, partialMatches));
        }

        return resolved;
    }

}
