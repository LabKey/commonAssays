/*
 * Copyright (c) 2005-2009 LabKey Corporation
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

import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.SubsetPart;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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
    final PopulationName _subset;
    final SubsetExpression _expr;

    public SubsetSpec(SubsetSpec parent, PopulationName subset)
    {
        _parent = parent;
        _subset = subset;
        _expr = null;
        if (_parent != null && _parent.isExpression())
        {
            throw new SubsetFormatException(toString(), "Subset expressions must be last");
        }
    }

    public SubsetSpec(SubsetSpec parent, SubsetExpression expr)
    {
        _parent = parent;
        _subset = null;
        _expr = expr;
        if (_parent != null && _parent.isExpression())
        {
            throw new SubsetFormatException(toString(), "Subset expressions must be last");
        }
    }

    public SubsetSpec createChild(PopulationName subset)
    {
        return new SubsetSpec(this, subset);
    }

    public SubsetSpec createChild(SubsetExpression expr)
    {
        return new SubsetSpec(this, expr);
    }

    /**
     * Creates a new SubsetSpec from raw strings.  The final string may be a boolean expression.
     *
     * @param rawStrings
     * @return
     */
    static public SubsetSpec fromParts(String[] rawStrings)
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
        return SubsetExpression.subset(strSubset);
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
        ret[parents.length] = _subset != null ? _subset : _expr;
        return ret;
    }

    public SubsetPart getSubset()
    {
        return _subset != null ? _subset : _expr;
    }

    public PopulationName getPopulationName()
    {
        return _subset;
    }

    public SubsetExpression getExpression()
    {
        return _expr;
    }

    public int hashCode()
    {
        int ret = _subset != null ? _subset.hashCode() : _expr.hashCode();
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

        if (_subset == null && other._subset != null)
            return false;
        if (_subset != null && !_subset.equals(other._subset))
            return false;

        if (_expr == null && other._expr != null)
            return false;
        if (_expr != null && !_expr.equals(other._expr))
            return false;

        return true;
    }

	private transient String _toString = null;
    private transient String _toEscapedString = null;

    public String toString(boolean escaped)
    {
        if (escaped)
        {
            if (_toEscapedString == null)
                _toEscapedString = _toString(true);
            return _toEscapedString;
        }
        else
        {
            if (_toString == null)
                _toString = _toString(false);
            return _toString;
        }
    }

    private String _toString(boolean escaped)
    {
        StringBuilder sb = new StringBuilder();
        if (_parent != null)
            sb.append(_parent.toString(escaped)).append("/");

        assert (_subset == null && _expr != null) ||  (_subset != null && _expr == null);
        if (_subset != null)
            sb.append(_subset.toString(escaped));
        else
            sb.append("(").append(_expr.toString(escaped)).append(")");

        return sb.toString();
    }

    // print in escaped form
    public String toString()
    {
        return toString(true);
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

    static public Comparator<SubsetSpec> COMPARATOR = new Comparator<SubsetSpec>()
    {
        public int compare(SubsetSpec spec1, SubsetSpec spec2)
        {
            if (spec1 == spec2)
                return 0;
            if (spec1 == null)
                return -1;
            if (spec2 == null)
                return 1;

            return spec1.toString().compareTo(spec2.toString());
        }
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
        return _expr != null;
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
