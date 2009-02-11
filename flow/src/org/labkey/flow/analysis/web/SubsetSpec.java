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

package org.labkey.flow.analysis.web;

import org.labkey.flow.analysis.model.FlowException;

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
    final String _subset;

    public SubsetSpec(SubsetSpec parent, String subset)
    {
        _parent = parent;
        _subset = subset;
        if (_parent != null && isExpression(_parent.getSubset()))
        {
            throw new SubsetFormatException(toString(), "Subset expressions must be last");
        }
    }

    static public SubsetSpec fromString(String strSubset)
    {
        if (strSubset == null || strSubset.length() == 0)
        {
            return null;
        }
        int ichSlash;
        if (strSubset.endsWith(")"))
        {
            int cRParen = 1;
            int ich;
            for (ich = strSubset.length() - 2; ich >= 0 && cRParen != 0; ich --)
            {
                switch (strSubset.charAt(ich))
                {
                    case ')':
                        cRParen ++;
                        break;
                    case '(':
                        cRParen --;
                        break;
                }
            }
            if (cRParen > 0)
                throw new SubsetFormatException(strSubset, "Too many ')'");
            if (ich < 0)
            {
                return new SubsetSpec(null, strSubset);
            }
            if (strSubset.charAt(ich) != '/')
            {
                throw new SubsetFormatException(strSubset, "Expected '/' at character " + ich);
            }
            ichSlash = ich;
        }
        else
        {
            ichSlash = strSubset.lastIndexOf("/");
        }
        if (ichSlash < 0)
        {
            return new SubsetSpec(null, strSubset);
        }
        return new SubsetSpec(SubsetSpec.fromString(strSubset.substring(0, ichSlash)), strSubset.substring(ichSlash + 1));
    }

    public SubsetSpec getParent()
    {
        return _parent;
    }

    public String[] getSubsets()
    {
        if (_parent == null)
            return new String[]{_subset};
        String[] parents = _parent.getSubsets();
        String[] ret = new String[parents.length + 1];
        System.arraycopy(parents, 0, ret, 0, parents.length);
        ret[parents.length] = _subset;
        return ret;
    }

    public String getSubset()
    {
        return _subset;
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
	
    public String toString()
    {
		if (_toString == null)
			_toString = (_parent == null) ? _subset : _parent.toString() + "/" + _subset;
		return _toString;
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

    static public boolean isExpression(String str)
    {
        return str.startsWith("(") && str.endsWith(")");
    }

    public boolean isExpression()
    {
        return isExpression(_subset);
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
