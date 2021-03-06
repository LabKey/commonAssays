/*
 * Copyright (c) 2005-2011 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein.tools;

import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.data.general.PieDataset;

import java.text.AttributedString;

public class GOPieSectionLabelGenerator implements PieSectionLabelGenerator
{
    @Override
    public AttributedString generateAttributedSectionLabel(PieDataset pieDataset, Comparable comparable)
    {
        return new AttributedString(generateSectionLabel(pieDataset, comparable));
    }

    @Override
    public String generateSectionLabel(PieDataset p, Comparable key)
    {
        String retVal = key.toString();
        if (retVal.startsWith("GO:")) retVal = retVal.substring(11);
        int n = p.getValue(key).intValue();
        retVal += "[" + n + "]";
        return retVal;
    }
}

