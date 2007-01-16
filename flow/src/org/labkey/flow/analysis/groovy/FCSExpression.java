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
