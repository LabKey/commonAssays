/*
 * Copyright (c) 2005-2010 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein.tools;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.urls.StandardPieURLGenerator;
import org.jfree.data.general.PieDataset;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;

import java.util.Map;
import java.util.Set;

/**
 * User: tholzman
 * Date: Oct 31, 2005
 * Time: 4:52:16 PM
 */
public class GOPieURLGenerator extends StandardPieURLGenerator
{
    protected ActionURL _url;

    public GOPieURLGenerator(ActionURL url)
    {
        _url = url;
    }

    public String generateURL(PieDataset dataset, Comparable key, int pieIndex)
    {
        Map<String, Set<Integer>> extra = ((ProteinPieDataset) dataset).getExtraInfo();
        if (extra == null) return null;
        Set<Integer> sqids = extra.get(key);
        if (sqids == null) return null;

        ActionURL url = _url.clone();
        url.addParameter("sliceTitle", key.toString());
        url.addParameter("sqids", StringUtils.join(sqids, ","));
        return PageFlowUtil.filter(url);
    }
}

