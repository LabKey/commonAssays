package org.labkey.ms2.search;

import org.labkey.api.view.JspView;

/**
 * User: jeckels
 * Date: Feb 6, 2007
 */
public class ProteinSearchWebPart extends JspView<ProteinSearchBean>
{
    public static final String NAME = "Protein Search";

    public ProteinSearchWebPart()
    {
        this(false);
    }

    public ProteinSearchWebPart(boolean horizontal)
    {
        super("/org/labkey/ms2/search/searchProteins.jsp");
        setTitle(NAME);
        setModel(new ProteinSearchBean(horizontal));
    }


}
