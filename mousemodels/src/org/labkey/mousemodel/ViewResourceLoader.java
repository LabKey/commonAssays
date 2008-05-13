/*
 * Copyright (c) 2004-2008 Fred Hutchinson Cancer Research Center
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
package org.labkey.mousemodel;

import java.io.InputStream;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.labkey.api.view.ViewServlet;

public class ViewResourceLoader extends ResourceLoader
{
    public long getLastModified(Resource arg0)
    {
        return 0;
    }

    public InputStream getResourceStream(String str) throws ResourceNotFoundException
    {
        return ViewServlet.getViewServletContext().getResourceAsStream(str);
    }

    public void init(ExtendedProperties arg0)
    {

    }

    public boolean isSourceModified(Resource arg0)
    {
        return false;
    }
} 
