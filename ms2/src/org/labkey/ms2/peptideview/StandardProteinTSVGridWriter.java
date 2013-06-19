/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

package org.labkey.ms2.peptideview;

import org.apache.log4j.Logger;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.ms2.Protein;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: adam
 * Date: Sep 6, 2006
 */
public class StandardProteinTSVGridWriter extends ProteinTSVGridWriter
{
    private static Logger _log = Logger.getLogger(StandardProteinTSVGridWriter.class);

    protected int _peptideIndex = -1;

    public StandardProteinTSVGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns)
    {
        super(proteinDisplayColumns, peptideDisplayColumns);
    }

    protected void addCalculatedValues(RenderContext ctx, ResultSet nestedRS) throws SQLException
    {
        Protein protein = new Protein();

        protein.setSequence((String) ctx.get("Sequence"));

        List<String> peptides = new ArrayList<>();

        while (nestedRS.next())
            peptides.add(nestedRS.getString(getPeptideIndex()));

        String[] peptideArray = new String[peptides.size()];
        protein.setPeptides(peptides.toArray(peptideArray));

        // Calculate amino acid coverage and add to the context for AACoverageColumn to see
        ctx.put("AACoverage", protein.getAAPercent());
    }


    private int getPeptideIndex() throws SQLException
    {
        if (_peptideIndex == -1)
        {
            _peptideIndex = _groupedRS.findColumn("Peptide");
        }
        return _peptideIndex;
    }
}
