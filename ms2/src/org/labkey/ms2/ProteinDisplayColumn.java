/*
 * Copyright (c) 2006-2007 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * User: adam
 * Date: Aug 3, 2006
 * Time: 10:42 AM
 */
public class ProteinDisplayColumn extends DataColumn
{
    public ProteinDisplayColumn(ColumnInfo col)
    {
        super(col);
        setLinkTarget("prot");
    }

    public void addQueryColumns(Set<ColumnInfo> set)
    {
        super.addQueryColumns(set);
        FieldKey seqIdKey = FieldKey.fromParts("SeqId");
        FieldKey proteinKey = FieldKey.fromParts("Protein");

        Map<FieldKey, ColumnInfo> colMap = QueryService.get().getColumns(getColumnInfo().getParentTable(), Arrays.asList(seqIdKey, proteinKey));
        set.addAll(colMap.values());
    }
}
