/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Sep 7, 2006
 * Time: 8:26:21 PM
 */
public class SequestParam extends Param
{

    private String comment;
    private boolean passThrough;

    public SequestParam(
        int sortOrder,
        String value,
        String name,
        List<String> inputXmlLabels,
        String comment,
        IInputXMLConverter converter,
        IParamsValidator validator,
        boolean isPassThrough)
    {
        super(sortOrder,
            value,
            name,
            inputXmlLabels,
            converter,
            validator);
        this.comment = comment;
        this.passThrough = isPassThrough;
    }

    public SequestParam(
        int sortOrder,
        String value,
        String name,
        String comment,
        IInputXMLConverter converter,
        IParamsValidator validator,
        boolean isPassThrough)
    {
        super(sortOrder,
            value,
            name,
            converter,
            validator);
        this.comment = comment;
        this.passThrough = isPassThrough;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getComment()
    {
        return comment;
    }


    public boolean isPassThrough()
    {
        return passThrough;
    }

    public void setPassThrough(boolean passThrough)
    {
        this.passThrough = passThrough;
    }

    @Override
    public SequestParam setInputXmlLabels(String... inputXmlLabel)
    {
        super.setInputXmlLabels(inputXmlLabel);
        String autoLabel = "sequest, " + getName();
        if (!getInputXmlLabels().contains(autoLabel))
        {
            this.inputXmlLabels.add(autoLabel);
        }
        return this;
    }
}
