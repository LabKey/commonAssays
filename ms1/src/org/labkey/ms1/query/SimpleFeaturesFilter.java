/*
 * Copyright (c) 2007 LabKey Software Foundation
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
import org.labkey.api.data.SqlDialect;
import org.labkey.api.ms1.MS1Service;

import java.util.Map;

/**
 * Simple column-level filter implementation of FeaturesFilter. Use this class
 * to set a simple filter on the FeaturesTableInfo.
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Apr 9, 2008
 * Time: 11:02:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleFeaturesFilter implements FeaturesFilter
{
    public enum Operator
    {
        equals("="),
        not_equals("!="),
        greater_than(">"),
        greater_than_or_equal_to(">="),
        less_than("<"),
        less_than_or_equal_to("<="),
        is_null(" IS NULL "),
        is_not_null(" IS NOT NULL");

        private final String _sqlOper;

        private Operator(String sqlOper)
        {
            _sqlOper = sqlOper;
        }

        public String getSqlOperator()
        {
            return _sqlOper;
        }
    }

    private String _columnName;
    private Object _value;
    private Operator _operator;

    public SimpleFeaturesFilter(String columnName, Object value)
    {
        this(columnName, value, Operator.equals);
    }

    public SimpleFeaturesFilter(String columnName, Object value, Operator operator)
    {
        _columnName = columnName;
        _value = value;
        _operator = operator;
    }

    public SQLFragment getWhereClause(Map<String, String> aliasMap, SqlDialect dialect)
    {
        String featuresAlias = aliasMap.get(MS1Service.Tables.Features.name());
        assert(null != featuresAlias);
        return new SQLFragment(featuresAlias + "." + _columnName + _operator.getSqlOperator() + "?", _value);
    }
}
