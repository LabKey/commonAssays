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
package org.labkey.flow.webparts;

import org.labkey.api.view.JspView;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.SimpleWebPartFactory;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Nov 13, 2008
 * Time: 10:32:43 AM
 */
public class FlowFiles extends JspView<Object>
{
    public FlowFiles()
    {
        super(FlowFiles.class, "flowfiles.jsp", null);
        setTitle("Flow Files");
    }

    public static WebPartFactory FACTORY  = new SimpleWebPartFactory("Flow Files", FlowFiles.class);
}
