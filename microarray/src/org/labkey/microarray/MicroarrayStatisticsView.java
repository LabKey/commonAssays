/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.microarray;

import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.exp.ExperimentRunFilter;

/**
 * User: jeckels
 * Date: Feb 7, 2008
 */
public class MicroarrayStatisticsView extends JspView<MicroarrayStatisticsView.MicroarraySummaryBean>
{
    public static class MicroarraySummaryBean
    {
        private final int _runCount;

        public MicroarraySummaryBean(int runCount)
        {
            _runCount = runCount;
        }

        public int getRunCount()
        {
            return _runCount;
        }
    }

    public MicroarrayStatisticsView(ViewContext ctx)
    {
        super("/org/labkey/microarray/statistics.jsp");

        ExperimentRunFilter filter = MicroarrayRunFilter.INSTANCE;
        int runCount = filter.getRunCount(ctx.getUser(), ctx.getContainer());

        setModelBean(new MicroarraySummaryBean(runCount));
    }
}
