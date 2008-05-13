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

package org.labkey.ms2.pipeline.client;

/**
 * User: billnelson@uky.edu
 * Date: Apr 26, 2008
 */

/**
 * <code>EnzymeParseException</code>
 */
public class EnzymeParseException extends RuntimeException
{

    public EnzymeParseException()
    {
        super();
    }

    public EnzymeParseException(String s)
    {
        super(s);
    }

    public EnzymeParseException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public EnzymeParseException(Throwable throwable)
    {
        super(throwable);
    }
}
