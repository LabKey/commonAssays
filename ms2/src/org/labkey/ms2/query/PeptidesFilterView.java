package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.data.Container;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;

/**
 * User: jeckels
 * Date: Mar 3, 2008
 */
public class PeptidesFilterView extends QueryView
{
    private static final String PEPTIDE_FILTER_LINK_ID = "peptideFilterLink";
    private MS2Controller.PeptideFilteringComparisonForm _form;

    public PeptidesFilterView(User user, Container container, ActionURL currentURL, MS2Controller.PeptideFilteringComparisonForm form)
    {
        super(new MS2Schema(user, container), new QuerySettings(currentURL, MS2Controller.PEPTIDES_FILTER, MS2Schema.PEPTIDES_FILTER_TABLE_NAME));
        _form = form;
    }

    public void renderCustomizeViewLink(PrintWriter out)
    {
        ActionURL url = urlFor(QueryAction.chooseColumns);
        url.addParameter(QueryParam.defaultTab, "filter");
        out.write(textLink("edit peptide filter", url, PEPTIDE_FILTER_LINK_ID));
    }

    public QueryPicker getColumnListPicker(HttpServletRequest request)
    {
        QueryPicker basePicker = super.getColumnListPicker(request);
        return new QueryPicker("Peptide filter:", basePicker.getParamName(), _form.getCustomViewName(getViewContext()), basePicker.getChoices())
        {
            protected void appendOnChangeHandler(StringBuilder ret)
            {
                ActionURL url = urlFor(QueryAction.chooseColumns);
                url.deleteParameter(QueryParam.viewName.toString());
                url.addParameter(QueryParam.defaultTab, "filter");
                url.addParameter(QueryParam.viewName, "");
                ret.append(" onchange=\"document.getElementById('");
                ret.append(PEPTIDE_FILTER_LINK_ID);
                ret.append("').href='");
                ret.append(url.toString());
                ret.append("' + this.value; document.getElementById('customViewRadioButton').checked=true;\"");
            }
        };
    }

}
