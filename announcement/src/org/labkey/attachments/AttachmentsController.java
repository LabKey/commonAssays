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

package org.labkey.attachments;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.labkey.api.attachments.AttachmentForm;
import org.labkey.api.data.Container;
import org.labkey.api.security.ACL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewController;
import org.labkey.api.view.ViewForm;

import javax.servlet.ServletException;
import java.io.IOException;


@Jpf.Controller
public class AttachmentsController extends ViewController
{
    @Jpf.Action
    protected Forward begin(ViewForm form) throws ServletException, IOException
    {
        HttpView.throwNotFound();
        return null;
    }

    /**
     * Some entities (reports) can have their own ACLs. If they do, check
     * those too.
     * @param form
     * @return
     * @throws ServletException
     */

    // TODO: Move this to reports
    private boolean hasReadPermission(AttachmentForm form) throws ServletException
    {
        Container c = getContainer();
        String entityId = form.getEntityId();

        //If no read on container, give up.
        if (!c.hasPermission(getUser(), ACL.PERM_READ))
            return false;

        ACL acl = org.labkey.api.security.SecurityManager.getACL(c, entityId);
        //An empty acl really means one doesn't exist.
        if (null == acl || acl.isEmpty())
            return true;

        int perm = acl.getPermissions(getUser());

        return (perm & ACL.PERM_READ) != 0;
    }


    @Jpf.Action
    protected Forward download(AttachmentForm form) throws IOException, ServletException
    {
        if (!hasReadPermission(form))
            HttpView.throwUnauthorized();

//        AttachmentService.get().download(getResponse(), getUser(), form);

        return null;
    }
}
