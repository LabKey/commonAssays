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

package org.labkey.ms2;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.XarContext;
import org.labkey.api.security.User;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * User: adam
 * Date: Jul 23, 2007
 * Time: 11:06:32 AM
 */
public class MascotDatImporter extends MS2Importer
{
    public MascotDatImporter(User user, Container c, String description, String fullFileName, Logger log, XarContext context)
    {
        super(context, user, c, description, fullFileName, log);
    }

    @Override
    protected String getType()
    {
        return "Mascot";
    }

    @Override
    public void importRun(MS2Progress progress) throws IOException, XMLStreamException
    {
        throw new UnsupportedOperationException();
    }
}
