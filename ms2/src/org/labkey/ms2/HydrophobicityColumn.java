/*
 * Copyright (c) 2004-2007 Fred Hutchinson Cancer Research Center
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

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.ColumnInfo;

import java.util.Set;

/**
 * User: arauch
 * Date: Sep 18, 2004
 */
public class HydrophobicityColumn extends SimpleDisplayColumn
{
    private ColumnInfo _colInfo;

    public HydrophobicityColumn()
    {
        this(null);
    }

    public HydrophobicityColumn(ColumnInfo colInfo)
    {
        super();
        _colInfo = colInfo;
        setCaption("H");
        setName("h");
        setFormatString("0.00");
        setWidth("50");
        setTextAlign("right");
    }

    public Object getValue(RenderContext ctx)
    {
        String peptide = (String) ctx.get(_colInfo == null ? "Peptide" : _colInfo.getAlias());
        if (null != peptide)
            return MS2Peptide.hydrophobicity(peptide);
        else
            return 0;
    }

    public Class getValueClass()
    {
        return Double.class;
    }

    public static String getAlgorithmVersion()
    {
        return MS2Peptide.getHydrophobicityAlgorithm();
    }

    public void addQueryColumns(Set<ColumnInfo> set)
    {
        if (_colInfo == null)
        {
            set.add(MS2Manager.getTableInfoPeptides().getColumn("Peptide"));
        }
        else
        {
            set.add(_colInfo);
        }
    }
}
