/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.flow.controllers.run;

/**
 * User: kevink
 * Date: Apr 18, 2008 4:49:37 PM
 */
public class DownloadRunForm extends RunForm
{
    private Integer eventCount;
    private boolean skipMissing;

    public Integer getEventCount()
    {
        return eventCount;
    }

    public void setEventCount(Integer eventCount)
    {
        this.eventCount = eventCount;
    }

    public boolean isSkipMissing()
    {
        return skipMissing;
    }

    public void setSkipMissing(boolean skipMissing)
    {
        this.skipMissing = skipMissing;
    }
}

