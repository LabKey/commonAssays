package org.labkey.ms2.search;

import org.labkey.api.view.JspView;
import org.labkey.ms2.MS2Controller;

/**
 * User: cnathe
 * Date: 3/29/13
 */
public class MS2SearchWebpart extends JspView<ProteinSearchBean>
{
    // TODO: fix class name to remove MS2 reference (in trunk)
    public static final String NAME = "Mass Spec Search (Tabbed)";
    public static final String TITLE = "Mass Spec Search";

    public MS2SearchWebpart()
    {
        super("/org/labkey/ms2/search/ms2TabbedSearch.jsp");
        setTitle(TITLE);
    }
}
