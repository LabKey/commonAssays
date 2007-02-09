package org.labkey.ms2;

import org.labkey.api.util.ContainerTree;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.data.Table;
import org.labkey.api.data.DataRegion;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: jeckels
* Date: Feb 8, 2007
*/
public class MS2RunHierarchyTree extends ContainerTree
{
    private static Logger _log = Logger.getLogger(MS2RunHierarchyTree.class);

    public MS2RunHierarchyTree(String rootPath, User user, int perm)
    {
        super(rootPath, user, perm);
    }

    public MS2RunHierarchyTree(String rootPath, User user, int perm, ViewURLHelper url)
    {
        super(rootPath, user, perm, url);
    }

    @Override
    protected void renderNode(StringBuilder html, Container parent, ViewURLHelper url, boolean isAuthorized, int level)
    {
        html.append("<tr>");
        String firstTd = "<td style=\"padding-left:" + 20 * level + "\">";
        html.append(firstTd);

        if (isAuthorized)
        {
            html.append("<a href=\"");
            url.setExtraPath(parent.getPath());
            html.append(url.getEncodedLocalURIString());
            html.append("\">");
        }

        html.append(PageFlowUtil.filter(parent.getName()));

        if (isAuthorized)
            html.append("</a>");

        html.append("</td></tr>\n");

        if (isAuthorized)
        {
            try
            {
                ResultSet rs = Table.executeQuery(MS2Manager.getSchema(), "SELECT Run, Description, FileName FROM " + MS2Manager.getTableInfoRuns() + " WHERE Container=? AND Deleted=?", new Object[]{parent.getId(), Boolean.FALSE});

                boolean moreRuns = rs.next();

                if (moreRuns)
                {
                    ViewURLHelper runUrl = url.clone();
                    runUrl.setAction("showRun");

                    html.append("<tr>");
                    html.append(firstTd);
                    html.append("<table>\n");

                    while (moreRuns)
                    {
                        int run = rs.getInt(1);
                        runUrl.replaceParameter("run", String.valueOf(run));
                        html.append("<tr><td>");
                        html.append("<input type=checkbox name='");
                        html.append(DataRegion.SELECT_CHECKBOX_NAME);
                        html.append("' value='");
                        html.append(run);
                        html.append("'></td><td><a href=\"");
                        html.append(runUrl.getEncodedLocalURIString());
                        html.append("\">");
                        html.append(PageFlowUtil.filter(rs.getString(2)));
                        html.append("</a></td><td>");
                        html.append(PageFlowUtil.filter(rs.getString(3)));
                        html.append("</td></tr>\n");
                        moreRuns = rs.next();
                    }

                    html.append("</table></td></tr>\n");
                }

                rs.close();
            }
            catch (SQLException e)
            {
                _log.error("renderHierarchyChildren", e);
            }
        }
    }
}
