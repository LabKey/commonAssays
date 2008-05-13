/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.ms2.compare;

/**
 * User: jeckels
 * Date: Oct 6, 2006
 */
public class RunColumn
{
    private final String _label;
    private final String _name;
    private final String _aggregate;
    private String _formatString;

    public RunColumn(String label, String name, String aggregate, String formatString)
    {
        _label = label;
        _name = name;
        _aggregate = aggregate;
        _formatString = formatString;
    }

    public RunColumn(String label, String name, String aggregate)
    {
        this(label, name, aggregate, null);
    }
    
    public String getLabel()
    {
        return _label;
    }

    public String getName()
    {
        return _name;
    }

    public String getAggregate()
    {
        return _aggregate;
    }

    public String getFormatString()
    {
        return _formatString;
    }
}
