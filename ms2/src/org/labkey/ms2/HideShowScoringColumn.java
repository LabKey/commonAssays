/*
 * Copyright (c) 2007 LabKey Corporation
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

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.DisplayElement;

import java.util.List;
import java.util.Map;
import java.io.Writer;
import java.io.IOException;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
class HideShowScoringColumn extends SimpleDisplayColumn
{
    private ActionButton btnScoring;

    public HideShowScoringColumn(ButtonBar bb)
    {
        List<DisplayElement> buttons = bb.getList();
        for (DisplayElement button : buttons)
        {
            if (MS2Controller.CAPTION_SCORING_BUTTON.equals(button.getCaption()))
                btnScoring = (ActionButton) button;
        }
    }

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        if (btnScoring == null)
            return;

        Map cols = ctx.getRow();
        Integer peptideCount = (Integer) cols.get("PeptideCount");
        Integer revPeptideCount = (Integer) cols.get("NegativeHitCount");

        // Show the scoring button, if one of the rows contains over 50% reversed peptides.
        if (revPeptideCount.intValue() > peptideCount / 3)
            btnScoring.setVisible(true);
    }
}
