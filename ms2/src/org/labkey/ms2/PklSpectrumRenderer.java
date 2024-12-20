/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import java.io.IOException;

/**
 * User: adam
 * Date: May 10, 2006
 * Time: 12:51:22 PM
 */
public class PklSpectrumRenderer extends AbstractTextSpectrumRenderer
{
    public PklSpectrumRenderer(MS2Controller.ExportForm form, String filenamePrefix, String extension) throws IOException
    {
        super(form, filenamePrefix, extension);
    }

    @Override
    public void renderFirstLine(Spectrum spectrum)
    {
        _out.println(df4.format(spectrum.getMZ()) + " 1 " + spectrum.getCharge());
    }
}
