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
