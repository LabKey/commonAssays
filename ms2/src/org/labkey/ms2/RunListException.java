/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.LabKeyError;
import org.springframework.validation.BindException;

import java.util.Collection;
import java.util.Collections;

/**
 * User: jeckels
* Date: Jan 22, 2008
*/
public class RunListException extends Exception
{
    private Collection<String> _messages;

    public RunListException(String message)
    {
        this(Collections.singletonList(message));
    }

    public RunListException(Collection<String> messages)
    {
        _messages = messages;
    }

    public Collection<String> getMessages()
    {
        return _messages;
    }

    public String getMessage()
    {
        return StringUtils.join(_messages, '\n');
    }

    public void addErrors(BindException errors)
    {
        for (String message : _messages)
        {
            errors.addError(new LabKeyError(message));
        }
    }
}
