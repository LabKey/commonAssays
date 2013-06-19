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

package org.labkey.ms1.view;

import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Controller;

/**
 * Context object for AdminView.jsp
 *
 * User: Dave
 * Date: Nov 1, 2007
 * Time: 11:10:05 AM
 */
public class AdminViewContext
{
    private int _numDeleted = 0;
    private ActionURL _purgeNowUrl;
    private boolean _purgeRunning = false;

    public AdminViewContext(int numDeleted)
    {
        _numDeleted = numDeleted;
        _purgeNowUrl = MS1Controller.getShowAdminURL();
        _purgeNowUrl.addParameter("purgeNow", "true");
    }

    public int getNumDeleted()
    {
        return _numDeleted;
    }

    public String getPurgeNowUrl()
    {
        return _purgeNowUrl.getLocalURIString();
    }

    public boolean isPurgeRunning()
    {
        return _purgeRunning;
    }

    public void setPurgeRunning(boolean purgeRunning)
    {
        _purgeRunning = purgeRunning;
    }
}
