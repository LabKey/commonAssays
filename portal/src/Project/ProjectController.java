/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package Project;

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.security.ACL;
import org.labkey.api.util.AppProps;
import org.fhcrc.cpas.util.Pair;
import org.labkey.api.util.Search;
import org.labkey.api.view.*;
import org.labkey.api.module.FolderType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Enumeration;


@Jpf.Controller
public class ProjectController extends ViewController
{
    @Jpf.Action
    public Forward start() throws Exception
    {
        Container c = getContainer();
        HttpView.throwRedirect(c.getStartURL(getViewContext()).toString());
        return null;
    }

    @Jpf.Action
    /**
     * Default portal page used for any project folder.
     */
    protected Forward begin() throws Exception
    {
        requiresPermission(ACL.PERM_READ);        
        String title = null;
        Container c = getContainer(0);
        boolean appendPath = true;
        
        if (c != null)
        {
            FolderType folderType = c.getFolderType();
            if (!FolderType.NONE.equals(c.getFolderType()))
            {
                title = folderType.getStartPageLabel(getViewContext());
            }
            else if (c.equals(ContainerManager.getHomeContainer()))
            {
                title = AppProps.getInstance().getSystemShortName();
                appendPath = false;
            }
            else if (c.isProject())
                title = "Project " + c.getName();
            else
                title = c.getName();
        }

        return begin(title, appendPath);
    }


    private Forward begin(String title, boolean appendPath) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        
        HttpServletRequest request = getRequest();
        ViewURLHelper url = getViewURLHelper();
        if (null == url || url.getExtraPath().equals("/")) // handle RUN from workshop
            return new ViewForward("Project", "home", ContainerManager.HOME_PROJECT_PATH);

        Container c = getContainer(0);

        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext(), c);
        if (title != null)
            trailConfig.setTitle(title, appendPath);

        HttpView template = new HomeTemplate(getViewContext(), c, new VBox(), trailConfig);

        Portal.populatePortalView(getViewContext(), c.getId(), template, request.getContextPath());

        includeView(template);
        return null;
    }

    @Jpf.Action
    /**
     * Same as begin, but used for our home page. We retain the action
     * for compatibility with old bookmarks, but redirect so doesn't show
     * up any more...
     */
    protected Forward home() throws Exception
    {
        return new ViewForward("Project", "begin.view", ContainerManager.getHomeContainer());
    }


    @Jpf.Action
    protected Forward moveWebPart(MovePortletForm form) throws Exception
    {
        requiresPermission(ACL.PERM_ADMIN);

        Forward forward = form.getForward("begin", (Pair[]) null, true);

        Portal.WebPart[] parts = Portal.getParts(form.getPageId());
        if (null == parts)
            return forward;

        //Find the portlet. Theoretically index should be 1-based & consecutive, but
        //code defensively.
        int i;
        int index = form.getIndex();
        for (i = 0; i < parts.length; i++)
            if (parts[i].getIndex() == index)
                break;

        if (i == parts.length)
            return forward;

        Portal.WebPart part = parts[i];
        String location = part.getLocation();
        if (form.getDirection() == Portal.MOVE_UP)
        {
            for (int j = i - 1; j >= 0; j--)
            {
                if (location.equals(parts[j].getLocation()))
                {
                    int newIndex = parts[j].getIndex();
                    part.setIndex(newIndex);
                    parts[j].setIndex(index);
                    break;
                }
            }
        }
        else
        {
            for (int j = i + 1; j < parts.length; j++)
            {
                if (location.equals(parts[j].getLocation()))
                {
                    int newIndex = parts[j].getIndex();
                    part.setIndex(newIndex);
                    parts[j].setIndex(index);
                    break;
                }
            }
        }

        Portal.saveParts(form.getPageId(), parts);
        return forward;
    }

    @Jpf.Action
    protected Forward addWebPart(AddWebPartForm form) throws ServletException, URISyntaxException, SQLException
    {
        requiresPermission(ACL.PERM_ADMIN);

        Forward beginForward = form.getForward("begin", (Pair[]) null, true);

        WebPartFactory desc = Portal.getPortalPart(form.getName());
        if (null == desc)
            return beginForward;

        Portal.WebPart newPart = Portal.addPart(getContainer(), desc);

        if (desc.isEditable() && desc.showCustomizeOnInsert())
        {
            return form.getForward("showCustomizeWebPart",
                    new Pair[]{
                            new Pair("pageId", form.getPageId()),
                            new Pair("index", newPart.getIndex())
                    },
                    true);
        }
        else
            return beginForward;
    }

    @Jpf.Action
    protected Forward deleteWebPart(CustomizePortletForm form) throws Exception
    {
        requiresPermission(ACL.PERM_ADMIN);

        Forward forward = form.getForward("begin", (Pair[]) null, true);

        Portal.WebPart[] parts = Portal.getParts(form.getPageId());
        //Changed on us..
        if (null == parts || parts.length == 0)
            return forward;

        Portal.WebPart[] newParts = new Portal.WebPart[parts.length - 1];
        int index = form.getIndex();
        int partIndex = 0;
        for (Portal.WebPart part : parts)
            if (part.getIndex() != index)
                newParts[partIndex++] = part;

        Portal.saveParts(form.getPageId(), newParts);
        return forward;
    }


    @Jpf.Action
    protected Forward purge() throws ServletException, SQLException, IOException
    {
        requiresGlobalAdmin();

        int rows = Portal.purge();
        getResponse().getWriter().println("deleted " + rows + " portlets<br>");
        return null;
    }

    public static class CustomizeWebPartView extends AbstractCustomizeWebPartView
    {
        /**
         * Create a default web part that allows users to edit properties
         * described with propertyDescriptors. Assumes that the
         * WebPart being editied will be set in a parameter called "webPart"
         *
         * @param propertyDescriptors
         */
        public CustomizeWebPartView(PropertyDescriptor[] propertyDescriptors)
        {
            super("/Project/customizeWebPart.gm");
            //Get nicer display names for default property names
            for (PropertyDescriptor desc : propertyDescriptors)
            {
                String displayName = desc.getDisplayName();
                if (Character.isLowerCase(displayName.charAt(0)) && displayName.equals(desc.getName()))
                    desc.setDisplayName(ColumnInfo.captionFromName(displayName));
            }
            addObject("propertyDescriptors", propertyDescriptors);
        }
    }

    //
    // FORMS
    //

    /**
     * FormData get and set methods may be overwritten by the Form Bean editor.
     */
    public static class CreateForm extends ViewForm
    {
        private String name;


        public void setName(String name)
        {
            this.name = name;
        }


        public String getName()
        {
            return this.name;
        }
    }

    public static class AddWebPartForm extends CreateForm
    {
        private String pageId;
        private String location;

        public String getPageId()
        {
            return pageId;
        }

        public void setPageId(String pageId)
        {
            this.pageId = pageId;
        }

        public String getLocation()
        {
            return location;
        }

        public void setLocation(String location)
        {
            this.location = location;
        }

    }

    public static class CustomizePortletForm extends ViewForm
    {
        private int index;
        private String pageId;

        public int getIndex()
        {
            return index;
        }

        public void setIndex(int index)
        {
            this.index = index;
        }

        public String getPageId()
        {
            return pageId;
        }

        public void setPageId(String pageId)
        {
            this.pageId = pageId;
        }
    }

    @Jpf.Action
    protected Forward showCustomizeWebPart(CustomizePortletForm form) throws Exception
    {
        requiresPermission(ACL.PERM_ADMIN);

        Portal.WebPart webPart = Portal.getPart(form.getPageId(), form.getIndex());
        if (null == webPart)
            return form.getForward("begin", (Pair[]) null, true);

        WebPartFactory desc = Portal.getPortalPart(webPart.getName());
        assert (null != desc);
        if (null == desc)
            return form.getForward("begin", (Pair[]) null, true);

        HttpView v = desc.getEditView(webPart);
        assert(null != v);
        if (null == v)
            return form.getForward("begin", (Pair[]) null, true);

        HttpView template = new HomeTemplate(getViewContext(), getContainer(), v);
        includeView(template);
        return null;
    }


    @Jpf.Action
    protected Forward saveCustomizeWebPart(CustomizePortletForm form) throws Exception
    {
        requiresPermission(ACL.PERM_ADMIN);

        Portal.WebPart webPart = Portal.getPart(form.getPageId(), form.getIndex());

        Enumeration params = getRequest().getParameterNames();
        // TODO: Clean this up. Type checking... (though type conversion also must be done by the webpart)
        while (params.hasMoreElements())
        {
            String s = (String) params.nextElement();
            if (!"index".equals(s) && !"pageId".equals(s) && !"x".equals(s) && !"y".equals(s))
            {
                String value = getRequest().getParameter(s);
                if ("".equals(value.trim()))
                    webPart.getPropertyMap().remove(s);
                else
                    webPart.getPropertyMap().put(s, getRequest().getParameter(s));
            }
        }

        Portal.updatePart(getUser(), webPart);

        return form.getForward("begin", (Pair[]) null, true);
    }


    @Jpf.Action
    protected Forward search() throws Exception
    {
        Container c = getContainer();
        String searchTerm = (String)getViewContext().get("search");

        String html = Search.search(getUser(), c, searchTerm);
        HtmlView view = new HtmlView(html);

        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext(), c);
        trailConfig.setTitle("Search Results");

        HttpView template = new HomeTemplate(getViewContext(), c, view, trailConfig);
        return includeView(template);
    }


    public static class MovePortletForm extends CustomizePortletForm
    {
        private int direction;

        public int getDirection()
        {
            return direction;
        }

        public void setDirection(int direction)
        {
            this.direction = direction;
        }
    }

    public static class ButtonForm extends FormData
    {
        private String text;

        public String getText()
        {
            return text;
        }


        public void setText(String text)
        {
            this.text = text;
        }
    }


    public static class SearchWebPart extends GroovyView
    {
        public SearchWebPart()
        {
            super("/Project/search.gm");
        }

        @Override
        public void prepareWebPart(Object model) throws ServletException
        {
            super.prepareWebPart(model);
            setTitle("Search");
            String path = getViewContext().getViewURLHelper().getExtraPath();
            addObject("postURL", ViewURLHelper.toPathString("Project", "search", path));
            addObject("what", Search.getSearchResultNames());
        }
    }
}
