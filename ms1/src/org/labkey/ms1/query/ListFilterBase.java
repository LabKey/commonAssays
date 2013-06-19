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

/**
 * Base class for all filters that deal with lists of items
 *
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 10:01:12 AM
 */
public class ListFilterBase
{
    protected String genListSQL(int[] values)
    {
        StringBuilder sql = new StringBuilder();

        for(int val : values)
        {
            sql.append(val);
            sql.append(",");
        }

        //trim off last comma
        sql.deleteCharAt(sql.length() - 1);
        return sql.toString();
    }


    protected String genListSQL(Object[] values)
    {
        return genListSQL(values, true);
    }

    protected String genListSQL(Object[] values, boolean quoteValues)
    {
        StringBuilder sql = new StringBuilder();

        for(Object val : values)
        {
            if(null != val)
            {
                if(quoteValues)
                    sql.append("'");

                sql.append(val.toString());
                sql.append(quoteValues ? "'," : ",");
            }
        }

        //trim off last comma
        sql.deleteCharAt(sql.length() - 1);
        return sql.toString();
    }
    
}
