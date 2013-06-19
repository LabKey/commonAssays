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
 * Interface for objects that wish to establish base filters on a FeaturesTableInfo
 *
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 9:45:50 AM
 */
public interface FeaturesFilter
{
    /**
     * Implementations should return a SQLFragment suitable for inclusion in a WHERE clause
     * @param aliasMap A map of fully-qualified table names to table aliases
     * @param dialect The SqlDialect for the current database
     * @return A SQLFragment for the clause
     */
    public SQLFragment getWhereClause(Map<String, String> aliasMap, SqlDialect dialect);
}
