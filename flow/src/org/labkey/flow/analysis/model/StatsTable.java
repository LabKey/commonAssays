/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

package org.labkey.flow.analysis.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a <Table> element in the flowjo workspace file. 
 */
public class StatsTable
    {
    String _name;
    List _columns = new ArrayList();

    public void addColumn(Column column)
        {
        _columns.add(column);
        }
    public List getColumns()
        {
        return _columns;
        }

    public String getName()
        {
        return _name;
        }
    public void setName(String name)
        {
        _name = name;
        }

    static public class Column
        {
        String _subset;
        String _statistic;
        String _parameter;
        public String getSubset()
            {
            return _subset;
            }
        public void setSubset(String subset)
            {
            _subset = subset;
            }
        public String getStatistic()
            {
            return _statistic;
            }
        public void setStatistic(String statistic)
            {
            _statistic = statistic;
            }
        public String getParameter()
            {
            return _parameter;
            }
        public void setParameter(String parameter)
            {
            _parameter = parameter;
            }
        public String getHeader()
            {
            String ret = getSubset() + ":" + getStatistic();
            if (_parameter != null && _parameter.length() != 0)
                {
                ret += "(" + _parameter + ")";
                }
            return ret;
            }
        }
    }
