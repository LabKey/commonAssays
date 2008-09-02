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
package org.labkey.flow.controllers.protocol;

/**
 * User: kevink
 * Date: Aug 14, 2008 5:26:14 PM
 */
public class EditICSMetadataForm extends ProtocolForm
{
    String _metadata;

    public String getMetadata()
    {
        return _metadata;
    }

    public void setMetadata(String metadata)
    {
        _metadata = metadata;
    }


}
