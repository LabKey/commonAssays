package org.labkey.ms2.search;

import org.labkey.api.view.JspView;

/**
 * User: jeckels
 * Date: Feb 6, 2007
 */
public class ProteinSearchWebPart extends JspView
{
    public static final String NAME = "Protein Search";

    public ProteinSearchWebPart()
    {
        super("/org/labkey/ms2/search/searchProteins.jsp");
        setTitle(NAME);
    }


}
