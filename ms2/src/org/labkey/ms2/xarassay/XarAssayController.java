/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.ms2.xarassay;

import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.PipelineDataCollectorRedirectAction;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.springframework.validation.BindException;

import java.io.File;
import java.io.FileFilter;
import java.util.List;


public class XarAssayController extends SpringActionController
{
    private static final DefaultActionResolver _resolver =
            new DefaultActionResolver(XarAssayController.class, XarAssayUploadAction.class);


    public XarAssayController()
    {
        super();
        setActionResolver(_resolver);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleRedirectAction
    {
        public ActionURL getRedirectURL(Object o) throws Exception
        {
            return getContainer().getStartURL(getViewContext());
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class UploadRedirectAction extends PipelineDataCollectorRedirectAction
    {
        protected FileFilter getFileFilter()
        {
            return XarAssayPipelineProvider.FILE_FILTER;
        }

        protected ActionURL getUploadURL(ExpProtocol protocol)
        {
            return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getContainer(), protocol, XarAssayUploadAction.class);
        }

        protected List<File> validateFiles(BindException errors, List<File> files)
        {
            return files;
        }
    }
}
