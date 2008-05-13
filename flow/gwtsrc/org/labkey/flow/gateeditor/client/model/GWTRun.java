/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GWTRun implements IsSerializable, Serializable
{
    int runId;
    String name;


    public int getRunId()
    {
        return runId;
    }

    public void setRunId(int runId)
    {
        this.runId = runId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GWTRun))
            return false;
        return runId == ((GWTRun) other).runId;
    }

    public int hashCode()
    {
        return runId;
    }
}
