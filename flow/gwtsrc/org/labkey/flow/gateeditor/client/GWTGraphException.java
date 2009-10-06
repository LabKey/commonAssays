/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.flow.gateeditor.client;

import org.labkey.flow.gateeditor.client.model.GWTGraphOptions;

/**
 * User: kevink
 * Date: Oct 2, 2009
 */
public class GWTGraphException extends Exception
{
    private GWTGraphOptions graphOptions;

    public GWTGraphException()
    {
    }

    public GWTGraphException(String msg, GWTGraphOptions options, Throwable cause)
    {
        super(msg, cause);
        graphOptions = options;
    }

    public GWTGraphOptions getGraphOptions()
    {
        return graphOptions;
    }

}
