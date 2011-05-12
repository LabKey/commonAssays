/*
 * Copyright (c) 2011 LabKey Corporation
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

package org.labkey.flow.analysis.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * User: kevink
 * Date: May 4, 2011
 *
 * Represents the name of a PopulationSet and not the full subset name path.
 * If the population name contains an illegal character, the entire
 * value is wrapped in curly brackets to escape the string.  If an end
 * curly is found in the escaped string it is preceeded by backslash '\'.
 */
public class PopulationName implements SubsetPart
{
    public static final Character ESCAPE_START = '{';
    public static final Character ESCAPE_END = '}';

    private static Set<Character> illegalChars = new HashSet<Character>();
    static
    {
        illegalChars.add('(');
        illegalChars.add(')');
        illegalChars.add('/');
        illegalChars.add(':');
        illegalChars.add('&');
        illegalChars.add('|');
        illegalChars.add('!');
        illegalChars.add(ESCAPE_START);
        illegalChars.add(ESCAPE_END);
    }

    public static final PopulationName ALL = PopulationName.fromString("*");

    final String _escaped;
    final String _raw;

    public static PopulationName fromString(String str)
    {
        if (str == null || str.length() == 0)
            return null;

        String raw = str;

        if (isEscaped(str))
        {
            raw = unescape(str);
        }
        else if (needsEscaping(str))
        {
            str = escape(str);
        }
        
        return new PopulationName(str, raw);
    }

    private PopulationName(String escaped, String raw)
    {
        _escaped = escaped;
        _raw = raw;
    }

    /** The population name which may be escaped. */
    public String getName()
    {
        return _escaped;
    }

    /** The original name which may require escaping. */
    public String getRawName()
    {
        return _raw;
    }

    /**
     * Appends the other PopulationName and removes any "++" or "--" caused by combining the names.
     * e.g., "FITC+" and "+L" becomes "FITC+L"
     * @param other The other name.
     * @return The composed PopulationName
     */
    public PopulationName compose(PopulationName other)
    {
        String suffix = other.getRawName();
        if (_raw.endsWith("+") && suffix.startsWith("+") ||
            _raw.endsWith("-") && suffix.startsWith("-"))
        {
            return PopulationName.fromString(_raw + suffix.substring(1));
        }
        return PopulationName.fromString(_raw + suffix);
    }

    public String toString(boolean escaped)
    {
        return escaped ? _escaped : _raw;
    }

    public String toString()
    {
        return _escaped;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PopulationName p = (PopulationName)o;
        return _escaped.equals(p._escaped);
    }

    @Override
    public int hashCode()
    {
        return _escaped.hashCode();
    }

    public boolean isEscaped()
    {
        return isEscaped(_escaped);
    }

    public static boolean isEscaped(String name)
    {
        return name.charAt(0) == ESCAPE_START.charValue() &&
               name.charAt(name.length()-1) == ESCAPE_END.charValue();
    }

    public static boolean needsEscaping(String name)
    {
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (illegalChars.contains(c))
                return true;
        }

        return false;
    }

    private static String escape(String str)
    {
        StringBuilder sb = new StringBuilder(str.length()+2);
        sb.append(ESCAPE_START);
        for (int i = 0; i < str.length(); i++)
        {
            char c = str.charAt(i);
            if (c == ESCAPE_END)
                sb.append('\\').append(ESCAPE_END);
            else
                sb.append(c);
        }
        sb.append(ESCAPE_END);
        return sb.toString();
    }

    private static String unescape(String str)
    {
        assert isEscaped(str);
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 1; i < str.length()-1; i++)
        {
            char c = str.charAt(i);
            if (c == '\\' && i < str.length()+2 && str.charAt(i+1) == ESCAPE_END)
            {
                i++;
                sb.append(ESCAPE_END);
            }
            else
                sb.append(c);
        }
        return sb.toString();
    }

    public static class NameTests extends Assert
    {
        void assertPopulation(PopulationName expected, PopulationName actual)
        {
            assertEquals(expected._raw, actual._raw);
            assertEquals(expected._escaped, actual._escaped);
        }

        @Test
        public void simple()
        {
            PopulationName expected = new PopulationName("abc", "abc");
            assertPopulation(expected, PopulationName.fromString("abc"));
        }

        @Test
        public void escaped()
        {
            PopulationName expected = new PopulationName("{one(two)}", "one(two)");
            assertPopulation(expected, PopulationName.fromString("one(two)"));
            assertPopulation(expected, PopulationName.fromString("{one(two)}"));
        }

        @Test
        public void escapeEscapes()
        {
            PopulationName expected = new PopulationName("{one{two\\}}", "one{two}");
            assertPopulation(expected, PopulationName.fromString("one{two}"));
            assertPopulation(expected, PopulationName.fromString("{one{two\\}}"));
        }


        @Test
        public void escapeEscapes2()
        {
            PopulationName expected = new PopulationName("{one{two{three\\}\\}}", "one{two{three}}");
            assertPopulation(expected, PopulationName.fromString("one{two{three}}"));
            assertPopulation(expected, PopulationName.fromString("{one{two{three\\}\\}}"));
        }

        @Test
        public void compose()
        {
            PopulationName left = PopulationName.fromString("L+");
            PopulationName right = PopulationName.fromString("+R");
            assertPopulation(PopulationName.fromString("L+R"), left.compose(right));
        }

        @Test
        public void compose2()
        {
            PopulationName left = PopulationName.fromString("L+");
            PopulationName right = PopulationName.fromString("-R");
            assertPopulation(PopulationName.fromString("L+-R"), left.compose(right));
        }

        @Test
        public void compose3()
        {
            PopulationName left = PopulationName.fromString("L");
            PopulationName right = PopulationName.fromString("R");
            assertPopulation(PopulationName.fromString("LR"), left.compose(right));
        }

    }
}
