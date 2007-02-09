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
