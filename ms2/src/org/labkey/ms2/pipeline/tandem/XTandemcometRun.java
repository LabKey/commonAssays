/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
package org.labkey.ms2.pipeline.tandem;

import org.labkey.ms2.pipeline.tandem.XCometRun;
import org.labkey.ms2.MS2RunType;

/**
 * XTandemcometRun class
 * <p/>
 * Created: May 18, 2006
 *
 * @author bmaclean
 */
public class XTandemcometRun extends XCometRun
{
    public MS2RunType getRunType()
    {
        return MS2RunType.XTandemcomet;
    }
}
