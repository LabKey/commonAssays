/*
 * Copyright (c) 2005-2017 LabKey Corporation
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

package org.labkey.flow.analysis.web;

import org.jetbrains.annotations.NotNull;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.SubsetPart;

import java.io.Serializable;
import java.util.Comparator;

public class SubsetSpec implements Serializable
{
    static private class SubsetFormatException extends FlowException
    {
        public SubsetFormatException(String subset, String error)
        {
            super("Malformed subset '" + subset + "' : " + error);
        }
    }

    final SubsetSpec _parent;
    final SubsetPart _subset;

    public SubsetSpec(SubsetSpec parent, @NotNull SubsetPart part)
    {
//        The @NotNull annotation should have caught this but did not (Exception Report 25013/ Issue 30837).
        if(part == null)
        {
            throw new IllegalArgumentException("The annotation preventing a null part failed.");
        }

        _parent = parent;
        _subset = part;
        // NOT SURE: Disable the following check to allow subset expression mid-subsetspec so we can generate aliases from boolean gates.
        // NOT SURE: The alias isn't needed for backward compat so we could just ignore these aliases.
        //if (_parent != null && _parent.isExpression())
        //{
        //    throw new SubsetFormatException(toString(), "Subset expressions must be last");
        //}
    }

    public SubsetSpec createChild(@NotNull SubsetPart part)
    {
        return new SubsetSpec(this, part);
    }

    public SubsetSpec createChild(@NotNull SubsetSpec spec)
    {
        return spec.addRoot(this);
    }

    /**
     * Creates a new SubsetSpec from raw strings.  The final string may be a boolean expression.
     *
     * @param rawStrings
     * @return
     */
    static public SubsetSpec fromParts(String... rawStrings)
    {
        if (rawStrings == null || rawStrings.length == 0)
            return null;

        SubsetSpec spec = null;
        for (int i = 0; i < rawStrings.length; i++)
        {
            String str = rawStrings[i];
            if (i == rawStrings.length -1 && ___isExpression(str))
                spec = new SubsetSpec(spec, SubsetExpression.expression(str));
            else
                spec = new SubsetSpec(spec, PopulationName.fromString(str));
        }

        if(spec != null && spec.getSubset() == null)
        {
            throw new FlowException("A SubsetSpec with a null Subset is not valid.");
        }

        return spec;
    }

    /**
     * Parses a SubsetSpec from an unesacped string and assumes there are no '/' characters in population names.
     * @param rawString
     * @return
     */
    static public SubsetSpec fromUnescapedString(String rawString)
    {
        String[] parts = rawString.split("/");
        return fromParts(parts);
    }

    /**
     * Parses a SubsetSpec from an escaped string.  Any special characters (e.g., "(" or "/") have been escaped
     * in each population by surrounding the population name with "{}".
     *
     * @param strSubset
     * @return
     * @see {@link org.labkey.flow.analysis.model.PopulationName#fromString(String)}.
     */
    static public SubsetSpec fromEscapedString(String strSubset)
    {
        if (strSubset == null || strSubset.length() == 0)
        {
            return null;
        }
        SubsetSpec spec = SubsetExpression.subset(strSubset);

        if(spec != null && spec.getSubset() == null)
        {
            throw new FlowException("A SubsetSpec with a null Subset is not valid.");
        }

        return spec;
    }

    public SubsetSpec getParent()
    {
        return _parent;
    }

    /** Returns an array containing PopulationName and SubsetExpression. */
    public SubsetPart[] getSubsets()
    {
        if (_parent == null)
            return new SubsetPart[]{_subset};
        SubsetPart[] parents = _parent.getSubsets();
        SubsetPart[] ret = new SubsetPart[parents.length + 1];
        System.arraycopy(parents, 0, ret, 0, parents.length);
        ret[parents.length] = _subset;
        return ret;
    }

    public SubsetPart getSubset()
    {
        return _subset;
    }

    public PopulationName getPopulationName()
    {
        if (_subset instanceof PopulationName)
            return (PopulationName) _subset;
        return null;
    }

    public SubsetExpression getExpression()
    {
        if (_subset instanceof SubsetExpression)
            return (SubsetExpression) _subset;
        return null;
    }

    public int hashCode()
    {
        int ret = _subset.hashCode();
        if (_parent != null)
        {
            ret ^= _parent.hashCode();
        }
        return ret;
    }

    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        SubsetSpec other = (SubsetSpec) obj;
        if (other._parent == null)
        {
            if (_parent != null)
                return false;
        }
        else
        {
            if (!other._parent.equals(_parent))
                return false;
        }

        return _subset.equals(other._subset);
    }

	private transient String _toString = null;
    private transient String _toEscapedString = null;

    public String toString(boolean escaped, boolean withinExpression)
    {
        if (escaped)
        {
            if (_toEscapedString == null)
                _toEscapedString = _toString(true, withinExpression);
            return _toEscapedString;
        }
        else
        {
            if (_toString == null)
                _toString = _toString(false, withinExpression);
            return _toString;
        }
    }

    private String _toString(boolean escape, boolean withinExpression)
    {
        StringBuilder sb = new StringBuilder();
        if (_parent != null)
            sb.append(_parent.toString(escape, withinExpression)).append("/");

        if (_subset instanceof PopulationName)
        {
            sb.append(_subset.toString(escape, withinExpression));
        }
        else if (_subset instanceof SubsetExpression)
        {
            String s = ((SubsetExpression)_subset).toString(_parent, escape);
            if (!s.startsWith("(") || !s.endsWith(")"))
                s = "(" + s + ")";
            sb.append(s);
        }

        return sb.toString();
    }

    // print in escaped form
    public String toString()
    {
        return toString(true, false);
    }

    public SubsetSpec removeRoot()
    {
        if (_parent == null)
            return null;
        return new SubsetSpec(_parent.removeRoot(), _subset);
    }

    public SubsetSpec getRoot()
    {
        if (_parent == null)
        {
            return this;
        }
        return _parent.getRoot();
    }

    public SubsetSpec addRoot(SubsetSpec root)
    {
        if (_parent == null)
        {
            return new SubsetSpec(root, _subset);
        }
        return new SubsetSpec(_parent.addRoot(root), _subset);
    }

    static public Comparator<SubsetSpec> COMPARATOR = (spec1, spec2) ->
    {
        if (spec1 == spec2)
            return 0;
        if (spec1 == null)
            return -1;
        if (spec2 == null)
            return 1;

        return spec1.toString().compareTo(spec2.toString());
    };


    static public int compare(SubsetSpec spec1, SubsetSpec spec2)
    {
        return COMPARATOR.compare(spec1, spec2);
    }

    public int getDepth()
    {
        if (_parent == null)
            return 1;
        return 1 + _parent.getDepth();
    }

    // UNDONE: remove this
    static public boolean ___isExpression(String str)
    {
        return str.startsWith("(") && str.endsWith(")");
    }

    public boolean isExpression()
    {
        return _subset instanceof SubsetExpression;
    }

    public boolean hasAncestor(SubsetSpec spec)
    {
        if (spec == null)
            return true;

        SubsetSpec parent = this;
        while (parent != null)
        {
            if (parent.equals(spec))
                return true;
            parent = parent.getParent();
        }
        return false;
    }

    static public SubsetSpec commonAncestor(SubsetSpec spec1, SubsetSpec spec2)
    {
        if (spec1 == null || spec2 == null)
            return null;
        while (spec1 != null && !spec2.hasAncestor(spec1))
            spec1 = spec1.getParent();

        return spec1;
    }
}
