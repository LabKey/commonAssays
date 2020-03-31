/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

package org.labkey.api.util;

import java.util.List;

/**
 * mass spec data FileType  class
 * for .mzxml, .mzxml.gz, .mzml, etc
 * <p/>
 * Created: Jan 6, 2010
 *
 * Would like to rename to MassSpecDataFileType but keeping lower case to retain compatibility with existing
 * XML configuration files.
 *
 * @author bpratt
 */
public class massSpecDataFileType extends FileType
{
    /**
     * the normal constructor, gets you mzXML, mzXML.gz,
     * and mzML if the pwiz DLL is available
     */
    public massSpecDataFileType()
    {
        super(".mzXML", gzSupportLevel.SUPPORT_GZ);
        setCaseSensitiveOnCaseSensitiveFileSystems(true);
    }

    /**
     * use this constructor for things like ".msprefix.mzxml",
     * still adds .mzML if the pwiz DLL is available
     */
   public massSpecDataFileType(List<String> suffixes, String defaultSuffix)
    {
        super(suffixes, defaultSuffix, false, gzSupportLevel.SUPPORT_GZ);
        setCaseSensitiveOnCaseSensitiveFileSystems(true);
    }
}
