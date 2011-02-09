/*
 * Copyright (c) 2008-2011 LabKey Corporation
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

import org.labkey.api.query.FieldKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.CrosstabTableInfo;
import org.labkey.api.data.Sort;
import org.labkey.api.data.AggregateColumnInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
 * Date: Feb 1, 2008
 */
public class PeptideCrosstabView extends AbstractQueryCrosstabView
{
    public PeptideCrosstabView(MS2Schema schema, MS2Controller.PeptideFilteringComparisonForm form, ActionURL url)
    {
        super(schema, form, url, MS2Schema.HiddenTableType.PeptideCrosstab);
    }

    protected TableInfo createTable()
    {
        return _schema.createPeptideCrosstabTable(_form, getViewContext());
    }

    @Override
    protected Sort getBaseSort()
    {
        return new Sort(CrosstabTableInfo.getDefaultSortString() + ",Peptide");
    }

    protected FieldKey getComparisonColumn()
    {
        return FieldKey.fromParts(AggregateColumnInfo.NAME_PREFIX + "COUNT_RowId");
     }
}