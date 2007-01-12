package org.labkey.nab;

import java.io.Serializable;

/**
 * User: brittp
 * Date: Aug 31, 2006
 * Time: 8:04:47 PM
 */
public abstract class SafeTextConverter<T> implements Serializable
{
    private static final long serialVersionUID = -8338877129594854956L;
    private String text;
    private T _convertedValue;

    public static class BooleanConverter extends SafeTextConverter<Boolean>
    {
        public BooleanConverter(Boolean defaultValue)
        {
            super(defaultValue);
        }

        protected Boolean convertTextToValue(String text)
        {
            return Boolean.parseBoolean(text);
        }
    }

    public static class DoubleConverter extends SafeTextConverter<Double>
    {
        public DoubleConverter(Double defaultValue)
        {
            super(defaultValue);
        }

        protected Double convertTextToValue(String text)
        {
            return Double.parseDouble(text);
        }
    }

    public static class PercentConverter extends SafeTextConverter<Integer>
    {
        public PercentConverter(Integer defaultValue)
        {
            super(defaultValue);
        }

        protected Integer convertTextToValue(String text)
        {
            text = text.trim();
            if (text.endsWith("%"))
                text = text.substring(0, text.length() - 1);
            int idx;
            if ((idx = text.indexOf(".")) > 0)
                text = text.substring(0, idx);
            return Integer.parseInt(text);
        }
    }


    public SafeTextConverter(T defaultValue)
    {
        _convertedValue = defaultValue;
        text = null;
    }

    public T getValue()
    {
        return _convertedValue;
    }

    public void setValue(T value)
    {
        _convertedValue = value;
        text = null;
    }

    public String getText()
    {
        if (null != text)
            return text;

        if (_convertedValue == null)
            return null;

        return String.valueOf(_convertedValue);
    }

    public void setText(String textValue)
    {
        try
        {
            _convertedValue = convertTextToValue(textValue);
            text = null;
        }
        catch (Exception x)
        {
            text = textValue;
            _convertedValue = null;
        }
    }

    protected abstract T convertTextToValue(String text);

    protected String convertValueToText(T value)
    {
        return String.valueOf(value);
    }

}
