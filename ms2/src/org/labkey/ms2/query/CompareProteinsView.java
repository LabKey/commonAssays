/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.ms2.query;

import org.labkey.api.data.TableInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;

import javax.servlet.ServletException;

/**
 * User: jeckels
 * Date: Apr 12, 2007
 */
public class CompareProteinsView extends AbstractRunCompareView
{
    public CompareProteinsView(ViewContext context, int runListIndex, boolean forExport)
    {
        super(context, runListIndex, forExport, MS2Schema.HiddenTableType.CompareProteinProphet.toString());
    }

    protected String getGroupingColumnName()
    {
        return "SeqId";
    }

    protected String getGroupHeader()
    {
        return "Protein Information";
    }

    public String getComparisonName()
    {
        return "Proteins";
    }

    protected TableInfo createTable()
    {
        return getSchema().createProteinProphetCompareTable(getViewContext().getRequest(), getViewContext().getRequest().getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME));
    }
}
