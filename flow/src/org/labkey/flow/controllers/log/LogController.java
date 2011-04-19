/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.flow.controllers.log;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowLog;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.data.LogField;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;

public class LogController extends BaseFlowController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(LogController.class);

    public LogController() throws Exception
    {
        setActionResolver(_actionResolver);
    }


    @RequiresNoPermission
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(new ActionURL(FlowController.BeginAction.class, getContainer()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class ShowLogAction extends SimpleViewAction
    {
        Page _page;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            _page = (Page)getFlowPage("showLog.jsp");
            _page.log = getLog();
            if (_page.log == null)
            {
                throw new NotFoundException();
            }
            return new JspView(_page);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, _page.log, "Log " + _page.log.getName());
        }
    }


    public FlowLog getLog() throws SQLException
    {
        return FlowLog.fromLogId(getIntParam(FlowParam.logId));
    }


    static public abstract class Page extends FlowPage<LogController>
    {
        public FlowLog log;
        private String lsidLast;
        private FlowObject dataObjectLast;

        public String formatValue(int index, String[] headers, String[] values)
        {
            if (index > values.length)
                return "";
            String value = values[index];
            if (index > headers.length)
                return h(value);
            String header = headers[index];
            try
            {
                LogField field = LogField.valueOf(header);
                switch (field)
                {
                    case objectURI:
                        FlowObject dataObj;
                        if (StringUtils.equals(value, lsidLast))
                        {
                            dataObj = dataObjectLast;
                        }
                        else
                        {
                            dataObjectLast = dataObj = FlowDataObject.fromLSID(value);
                            lsidLast = value;
                        }

                        if (dataObj != null)
                        {
                            return textLink(dataObj.getName(), dataObj.urlShow());
                        }
                        break;
                }
            }
            catch (Exception e)
            {
                Logger.getLogger(LogController.class).error("unexpected exception", e);
            }
            return h(value);
        }
    }
}
