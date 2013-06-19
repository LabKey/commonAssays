/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
package org.labkey.ms1.query;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.dialect.SqlDialect;

import java.util.Map;

/**
 * Features filter for associated peptide id is not null
 *
 * User: Dave
 * Date: Mar 13, 2008
 * Time: 12:04:28 PM
 */
public class PeptideNotNullFilter implements FeaturesFilter
{
    public SQLFragment getWhereClause(Map<String, String> aliasMap, SqlDialect dialect)
    {
        String pepDataAlias = aliasMap.get("ms2.PeptidesData");
        assert(null != pepDataAlias);
        return new SQLFragment(pepDataAlias + ".RowId IS NOT NULL");
    }
}
