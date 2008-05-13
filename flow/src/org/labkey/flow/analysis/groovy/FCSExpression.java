/*
 * Copyright (c) 2005-2008 LabKey Corporation
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

package org.labkey.flow.analysis.groovy;

import org.labkey.flow.analysis.model.FCSHeader;

import java.util.Map;
import java.util.HashMap;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.lang.Binding;
import org.codehaus.groovy.control.CompilationFailedException;

/**
 */
public class FCSExpression
    {
    private static GroovyClassLoader loader = new GroovyClassLoader(FCSExpression.class.getClassLoader());
    private Class<? extends Script> _groovyClass;

    public FCSExpression(String expression) throws CompilationFailedException
        {
        _groovyClass = loader.parseClass(expression);
        }

    public Object eval(FCSHeader fcs)
        {
        try
            {
            HashMap context = new HashMap();
            context.put("keywords", fcs.getKeywords());
            Script instance = _groovyClass.newInstance();
            instance.setBinding(new Binding(context));
            return instance.run();
            }
        catch (Exception x)
            {
            return "Error evaluating expression:" + x.toString();
            }
        }
    }
