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

package attachments;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.fhcrc.cpas.attachments.AttachmentForm;
import org.fhcrc.cpas.attachments.AttachmentService;
import org.fhcrc.cpas.data.Container;
import org.fhcrc.cpas.data.CoreSchema;
import org.fhcrc.cpas.security.ACL;
import org.fhcrc.cpas.security.User;
import org.fhcrc.cpas.util.ContainerUtil;
import org.fhcrc.cpas.view.HttpView;
import org.fhcrc.cpas.view.ViewController;
import org.fhcrc.cpas.view.ViewForm;

import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;


@Jpf.Controller
public class AttachmentsController extends ViewController
{
    @Jpf.Action
    protected Forward begin(ViewForm form) throws ServletException, IOException
    {
        HttpView.throwNotFound();
        return null;
    }

    @Jpf.Action
    protected Forward download(AttachmentForm form) throws IOException, ServletException
    {
        if (!hasReadPermission(form))
            HttpView.throwUnauthorized();

        AttachmentService.get().download(getViewContext(), form);

        return null;
    }

    /**
     * Some entities (reports) can have their own ACLs. If they do, check
     * those too.
     * @param form
     * @return
     * @throws ServletException
     */
    private boolean hasReadPermission(AttachmentForm form) throws ServletException
    {
        Container c = form.getContainer();
        String entityId = form.getEntityId();

        //If no read on container, give up.
        if (!c.hasPermission(getUser(), ACL.PERM_READ))
            return false;

        ACL acl = org.fhcrc.cpas.security.SecurityManager.getACL(c, entityId);
        //An empty acl really means one doesn't exist.
        if (null == acl || acl.isEmpty())
            return true;

        int perm = acl.getPermissions(getUser());

        return (perm & ACL.PERM_READ) != 0;
    }

    @Jpf.Action
    protected Forward deleteAttachment(AttachmentForm form) throws Exception
    {
        form.requiresPermission(ACL.PERM_DELETE);

        return includeView(AttachmentService.get().delete(form));
    }


    @Jpf.Action
    protected Forward showConfirmDelete(AttachmentForm form) throws Exception
    {
        form.requiresPermission(ACL.PERM_DELETE);

        return includeView(AttachmentService.get().getConfirmDeleteView(form));
    }


    @Jpf.Action
    protected Forward showAddAttachment(AttachmentForm form) throws Exception
    {
        return includeView(AttachmentService.get().getAddAttachmentView(form));
    }


    @Jpf.Action
    protected Forward addAttachment(AttachmentForm form) throws Exception
    {
        //Use form's container because legacy URLs to attachments use funny container path
        form.requiresPermission(ACL.PERM_UPDATE);

        return includeView(AttachmentService.get().add(form));
    }


    @Jpf.Action
    protected Forward purge() throws ServletException, SQLException, IOException
    {
        User user = (User) getRequest().getUserPrincipal();
        if (!user.isAdministrator())
            HttpView.throwUnauthorized();
        int rows = ContainerUtil.purgeTable(CoreSchema.getInstance().getTableInfoDocuments(), null);
        getResponse().getWriter().println("deleted " + rows + " documents<br>");
        return null;
    }
}
