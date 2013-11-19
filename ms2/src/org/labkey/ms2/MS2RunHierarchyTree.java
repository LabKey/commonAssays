/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.ContainerTree;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: jeckels
* Date: Feb 8, 2007
*/
public class MS2RunHierarchyTree extends ContainerTree
{
    String getSelectionKey()
    {
        return "hierarchyRuns";
    }

    public MS2RunHierarchyTree(String rootPath, User user, ActionURL url)
    {
        super(rootPath, user, ReadPermission.class, url);
    }

    @Override
    protected void renderNode(StringBuilder html, Container parent, ActionURL url, boolean isAuthorized, int level)
    {
        html.append("<tr>");
        String firstTd = "<td style=\"padding-left:" + 20 * level + "\">";
        html.append(firstTd);

        if (isAuthorized)
        {
            html.append("<a href=\"");
            url.setContainer(parent);
            html.append(url.getEncodedLocalURIString());
            html.append("\">");
        }

        html.append(PageFlowUtil.filter(parent.getName()));

        if (isAuthorized)
            html.append("</a>");

        html.append("</td></tr>\n");

        if (isAuthorized)
        {
            try (ResultSet rs = new SqlSelector(MS2Manager.getSchema(), "SELECT Run, Description, FileName FROM " + MS2Manager.getTableInfoRuns() + " WHERE Container=? AND Deleted=?", parent, Boolean.FALSE).getResultSet())
            {
                boolean moreRuns = rs.next();

                if (moreRuns)
                {
                    ActionURL runUrl = url.clone();
                    runUrl.setAction(MS2Controller.ShowRunAction.class);

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
                        html.append("' onclick=\"sendCheckbox(this, ").append(PageFlowUtil.filterQuote(getSelectionKey())).append(", [this.value], this.checked);\"");
                        html.append("></td><td><a href=\"");
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
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
    }
}
