/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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
import org.labkey.api.data.ColumnInfo;

import java.util.Set;

/**
 * User: arauch
 * Date: Sep 18, 2004
 */
public class HydrophobicityColumn extends AbstractPeptideDisplayColumn
{
    private ColumnInfo _colInfo;

    public HydrophobicityColumn(ColumnInfo colInfo)
    {
        super();
        _colInfo = colInfo;
        setCaption("H");
        setName("h");
        setDescription("Hydrophobicity");
        setFormatString("0.00");
        setWidth("50");
        setTextAlign("right");
    }

    @Override
    public ColumnInfo getColumnInfo()
    {
        return _colInfo;
    }

    @Override
    public Object getValue(RenderContext ctx)
    {
        String peptide = (String) getColumnValue(ctx, _colInfo, "Peptide");
        if (null != peptide)
            return MS2Peptide.hydrophobicity(peptide);
        else
            return 0;
    }

    @Override
    public Class getValueClass()
    {
        return Double.class;
    }

    public static String getAlgorithmVersion()
    {
        return MS2Peptide.getHydrophobicityAlgorithm();
    }

    @Override
    public void addQueryColumns(Set<ColumnInfo> set)
    {
        set.add(_colInfo);
    }
}
