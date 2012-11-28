/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

package org.labkey.ms2;

import org.labkey.api.data.*;

import java.util.Map;
import java.util.List;

/**
 * User: jeckels
 * Date: May 5, 2006
 */
public class FirstProteinDisplayColumn extends SimpleDisplayColumn
{
    private final FirstProteinType _type;
    private final ProteinGroupProteins _proteins;

    public FirstProteinDisplayColumn(String caption, FirstProteinType type, ProteinGroupProteins proteins)
    {
        setCaption(caption);
        _type = type;
        _proteins = proteins;
    }

    public Object getValue(RenderContext ctx)
    {
        Map row = ctx.getRow();
        String columnName = "ProteinGroupId";
        Number id = (Number)row.get(columnName);
        if (id == null)
        {
            columnName = "RowId";
            id = (Number)row.get(columnName);
        }

        List<ProteinSummary> summaries = _proteins.getSummaries(id.intValue(), ctx, columnName);
        if (summaries == null)
        {
            return "ERROR - No matching proteins found";
        }
        ProteinSummary firstSummary = summaries.get(0);
        return _type.getValue(firstSummary);
    }

    public enum FirstProteinType
    {
        NAME
        {
            public String getValue(ProteinSummary proteinSummary)
            {
                return proteinSummary.getName();
            }
        },
        BEST_NAME
        {
            public String getValue(ProteinSummary proteinSummary)
            {
                return proteinSummary.getBestName();
            }
        },
        DESCRIPTION
        {
            public String getValue(ProteinSummary proteinSummary)
            {
                return proteinSummary.getDescription();
            }
        },
        BEST_GENE_NAME
        {
            public String getValue(ProteinSummary proteinSummary)
            {
                return proteinSummary.getBestGeneName();
            }
        };

        public abstract String getValue(ProteinSummary proteinSummary);
    }

}
