/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
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
public class PopulationName implements SubsetPart, Comparable<PopulationName>
{
    public static final Character ESCAPE_START = '{';
    public static final Character ESCAPE_END = '}';

    public static final Character MAC_ENDASH = (char)208;
    public static final Character MAC_EMDASH = (char)209;

    // Characters not allowed to appear within a top-level population name.
    private static Set<Character> illegalTopLevelChars = new HashSet<>();
    static
    {
        illegalTopLevelChars.add('/');
        illegalTopLevelChars.add(ESCAPE_START);
        illegalTopLevelChars.add(ESCAPE_END);
    }

    // Characters not allowed to appear within a population name inside of boolean expression.
    private static Set<Character> illegalExpressionChars = new HashSet<>();
    static
    {
        illegalExpressionChars.add('(');
        illegalExpressionChars.add(')');
        illegalExpressionChars.add('/');
        illegalExpressionChars.add('&');
        illegalExpressionChars.add('|');
        illegalExpressionChars.add('!');
        illegalExpressionChars.add(ESCAPE_START);
        illegalExpressionChars.add(ESCAPE_END);
    }

    public static final PopulationName ALL = PopulationName.fromString("*");

    final String _escaped;
    final String _expressionEscaped;
    final String _raw;

    public static PopulationName fromString(String str)
    {
        if (str == null || str.length() == 0)
            return null;

        // replace MacRoman em-dash and en-dash
        str = StringUtils.replaceChars(str, MAC_ENDASH, '-');
        str = StringUtils.replaceChars(str, MAC_EMDASH, '-');

        String raw = str;

        if (isEscaped(str))
            raw = unescape(str);

        String escaped = raw;
        if (needsTopLevelEscaping(raw))
            escaped = escape(raw);

        String expressionEscaped = raw;
        if (needsExpressionLevelEscaping(raw))
            expressionEscaped = escape(raw);
        
        return new PopulationName(raw, escaped, expressionEscaped);
    }

    private PopulationName(String raw, String escaped, String expressionEscaped)
    {
        _raw = raw;
        _escaped = escaped;
        _expressionEscaped = expressionEscaped;
    }

    /** The population name which may be escaped for use in a SubsetSpec path part outside of a SubsetExpression. */
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

    public String toString(boolean escape, boolean withinExpression)
    {
        if (!escape)
            return _raw;

        return withinExpression ? _expressionEscaped : _escaped;
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

    @Override
    public int compareTo(PopulationName o)
    {
        return _escaped.compareTo(o._escaped);
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

    public static boolean needsTopLevelEscaping(String name)
    {
        return needsEscaping(illegalTopLevelChars, name);
    }

    public static boolean needsExpressionLevelEscaping(String name)
    {
        return needsEscaping(illegalExpressionChars, name);
    }

    private static boolean needsEscaping(Set<Character> illegalChars, String name)
    {
        // only boolean expressions are allowed to start with paren
        if (name.startsWith("("))
            return true;

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
            assertEquals("Raw mismatch", expected._raw, actual._raw);
            assertEquals("Escaped mismatch", expected._escaped, actual._escaped);
            assertEquals("Expression escaped mismatch", expected._expressionEscaped, actual._expressionEscaped);
        }

        @Test
        public void simple()
        {
            PopulationName expected = new PopulationName("abc", "abc", "abc");
            assertPopulation(expected, PopulationName.fromString("abc"));
        }

        @Test
        public void parensAllowed()
        {
            // NOTE: parens are escaped only if they are the first character or we are within an expression.
            PopulationName expected = new PopulationName("one(two)", "one(two)", "{one(two)}");
            assertPopulation(expected, PopulationName.fromString("one(two)"));
            assertPopulation(expected, PopulationName.fromString("{one(two)}"));
        }

        @Test
        public void parensEscaped()
        {
            // NOTE: parens are escaped only if they are the first character or we are within an expression.
            PopulationName expected = new PopulationName("(one two)", "{(one two)}", "{(one two)}");
            assertPopulation(expected, PopulationName.fromString("(one two)"));
            assertPopulation(expected, PopulationName.fromString("{(one two)}"));
        }

        @Test
        public void slashEscaped()
        {
            PopulationName expected = new PopulationName("one/two", "{one/two}", "{one/two}");
            assertPopulation(expected, PopulationName.fromString("one/two"));
            assertPopulation(expected, PopulationName.fromString("{one/two}"));
        }

        @Test
        public void expressionEscaped()
        {
            PopulationName expected = new PopulationName("one&two", "one&two", "{one&two}");
            assertPopulation(expected, PopulationName.fromString("one&two"));
            assertPopulation(expected, PopulationName.fromString("{one&two}"));
        }

        @Test
        public void escapeEscapes()
        {
            PopulationName expected = new PopulationName("one{two}", "{one{two\\}}", "{one{two\\}}");
            assertPopulation(expected, PopulationName.fromString("one{two}"));
            assertPopulation(expected, PopulationName.fromString("{one{two\\}}"));
        }

        @Test
        public void escapeEscapes2()
        {
            PopulationName expected = new PopulationName("one{two{three}}", "{one{two{three\\}\\}}", "{one{two{three\\}\\}}");
            assertPopulation(expected, PopulationName.fromString("one{two{three}}"));
            assertPopulation(expected, PopulationName.fromString("{one{two{three\\}\\}}"));
        }

        @Test
        public void compat_11_1()
        {
            assertEquals("-", PopulationName.fromString("-").toString());
            assertEquals("HLA-DR+", PopulationName.fromString("HLA-DR+").toString());
            assertEquals("APC CD3+", PopulationName.fromString("APC CD3+").toString());
            assertEquals("FSC-A, SSC-A subset", PopulationName.fromString("FSC-A, SSC-A subset").toString());
            assertEquals("<Alexa 680-A>, <APC-A> subset", PopulationName.fromString("<Alexa 680-A>, <APC-A> subset").toString());
            assertEquals("Q9: CD159a (NKG2a)-, HLA Dr+", PopulationName.fromString("Q9: CD159a (NKG2a)-, HLA Dr+").toString());
            assertEquals("PD-1 & 95 +", PopulationName.fromString("PD-1 & 95 +").toString());
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
