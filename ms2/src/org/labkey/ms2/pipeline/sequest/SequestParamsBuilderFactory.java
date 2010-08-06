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

package org.labkey.ms2.pipeline.sequest;

import java.io.File;
import java.net.URI;
import java.util.Map;

/**
 * User: billnelson@uky.edu
 * Date: Jan 23, 2007
 * Time: 4:45:24 PM
 */
public class SequestParamsBuilderFactory {
    public static SequestParamsBuilder createVersion1Builder(Map<String, String> sequestInputParams, File sequenceRoot)
    {
         return new SequestParamsV1Builder(sequestInputParams, sequenceRoot);
    }

    public static SequestParamsBuilder createVersion2Builder(Map<String, String> sequestInputParams, File sequenceRoot)
    {
         return new SequestParamsV2Builder(sequestInputParams, sequenceRoot);
    }
}
