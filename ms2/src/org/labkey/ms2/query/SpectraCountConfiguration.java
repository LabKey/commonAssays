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

package org.labkey.ms2.query;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collections;

/**
 * User: jeckels
* Date: Jan 18, 2008
*/
public class SpectraCountConfiguration
{
    final private boolean _groupedByCharge;
    final private boolean _groupedByPeptide;
    final private boolean _groupedByProtein;

    final private boolean _usingProteinProphet;

    final private String _tableName;
    final private String _description;

    public static final Set<SpectraCountConfiguration> VALID_CONFIGS;

    static
    {
        LinkedHashSet<SpectraCountConfiguration> configs = new LinkedHashSet<>();
        configs.add(new SpectraCountConfiguration(true, false, false, false));
        configs.add(new SpectraCountConfiguration(true, true, false, false));
        configs.add(new SpectraCountConfiguration(true, false, true, true));
        configs.add(new SpectraCountConfiguration(true, false, true, false));
        configs.add(new SpectraCountConfiguration(true, true, true, true));
        configs.add(new SpectraCountConfiguration(true, true, true, false));
        configs.add(new SpectraCountConfiguration(false, false, true, false));
        configs.add(new SpectraCountConfiguration(false, false, true, true));
        VALID_CONFIGS = Collections.unmodifiableSet(configs);
    }


    public SpectraCountConfiguration(boolean groupedByPeptide, boolean groupedByCharge, boolean groupedByProtein, boolean usingProteinProphet)
    {
        _groupedByCharge = groupedByCharge;
        _groupedByPeptide = groupedByPeptide;
        _groupedByProtein = groupedByProtein;
        _usingProteinProphet = usingProteinProphet;

        assert _groupedByCharge || _groupedByPeptide || _groupedByProtein : "Must group by at least one column";
        assert !_usingProteinProphet || _groupedByProtein : "To use ProteinProphet info you must group by protein";

        StringBuilder queryName = new StringBuilder("SpectraCount");
        StringBuilder description = new StringBuilder("");
        if (_groupedByPeptide)
        {
            queryName.append("Peptide");
            description.append("Peptide sequence");
        }
        if (_groupedByCharge)
        {
            queryName.append("Charge");
            if (description.length() == 0)
            {
                description.append("Peptide charge");
            }
            else
            {
                description.append(", peptide charge");
            }
        }
        if (_groupedByProtein)
        {
            queryName.append("Protein");
            if (_usingProteinProphet)
            {
                queryName.append("PP");
                if (description.length() == 0)
                {
                    description.append("ProteinProphet protein assignment");
                }
                else
                {
                    description.append(", ProteinProphet protein assignment");
                }
            }
            else
            {
                if (description.length() == 0)
                {
                    description.append("Search engine protein assignment");
                }
                else
                {
                    description.append(", search engine protein assignment");
                }
            }
        }

        _tableName = queryName.toString();
        _description = description.toString();
    }

    public boolean isGroupedByCharge()
    {
        return _groupedByCharge;
    }

    public boolean isGroupedByPeptide()
    {
        return _groupedByPeptide;
    }

    public boolean isGroupedByProtein()
    {
        return _groupedByProtein;
    }

    public boolean isUsingProteinProphet()
    {
        return _usingProteinProphet;
    }

    public String getTableName()
    {
        return _tableName;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpectraCountConfiguration that = (SpectraCountConfiguration) o;

        if (_groupedByCharge != that._groupedByCharge) return false;
        if (_groupedByPeptide != that._groupedByPeptide) return false;
        if (_groupedByProtein != that._groupedByProtein) return false;
        if (_usingProteinProphet != that._usingProteinProphet) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (_groupedByCharge ? 1 : 0);
        result = 31 * result + (_groupedByPeptide ? 1 : 0);
        result = 31 * result + (_groupedByProtein ? 1 : 0);
        result = 31 * result + (_usingProteinProphet ? 1 : 0);
        return result;
    }

    public String getDescription()
    {
        return _description;
    }

    public String toString()
    {
        return getDescription();
    }

    public static SpectraCountConfiguration findByTableName(String tableName)
    {
        for (SpectraCountConfiguration config : VALID_CONFIGS)
        {
            if (config.getTableName().equalsIgnoreCase(tableName))
            {
                return config;
            }
        }
        return null;
    }
}
