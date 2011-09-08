/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

import org.labkey.flow.analysis.model.*;

import java.util.BitSet;

abstract public class SubsetExpression implements SubsetPart
{
    /**
     * Parses a string into a SubsetSpec.
     * 
     * @param subsetAndExpression The subset and expression (eg., "X/Y/(A&B)" or "(A&B)")
     * @return
     */
    static public SubsetSpec subset(String subsetAndExpression)
    {
        SubsetParser parser = new SubsetParser(subsetAndExpression);
        return parser.parseFullSubset();
    }

    /**
     * Parses a string into a SubsetExpression.
     *
     * @param expression The expression portion of a subset (eg., "(A&B)")
     * @return
     */
    static public SubsetExpression expression(String expression)
    {
        SubsetParser parser = new SubsetParser(expression);
        return parser.parseFullExpression();
    }

    // Grouped flag is used for display purposes only
    protected boolean _grouped = false;

    public SubsetExpression setGrouped(boolean grouped) { _grouped = grouped; return this; }
    public boolean isGrouped() { return _grouped; }

    @Override
    public String toString()
    {
        return toString(true, false);
    }

    public String toString(boolean escape, boolean withinExpression)
    {
        assert !withinExpression : "Nested boolean expression not supported";
        return toString(null, escape);
    }

    public String toString(SubsetSpec parent, boolean escaped)
    {
        if (_grouped)
            return "(" + _toString(parent, escaped) + ")";
        return _toString(parent, escaped);
    }

    abstract protected String _toString(SubsetSpec parent, boolean escaped);

    abstract public BitSet apply(Subset subset, PopulationSet populationSet);

    abstract public <U> U reduce(Transform<U> transformer);

    public interface Transform<U>
    {
        U and(SubsetExpression.AndTerm term, U leftResult, U rightResult);
        U or(SubsetExpression.OrTerm term, U leftResult, U rightResult);
        U not(SubsetExpression.NotTerm term, U notResult);
        U subset(SubsetExpression.SubsetTerm term);
    }

    abstract static public class BinaryTerm extends SubsetExpression
    {
        protected SubsetExpression _left;
        protected SubsetExpression _right;
        BinaryTerm(SubsetExpression left, SubsetExpression right, boolean grouped)
        {
            _left = left;
            _right = right;
            _grouped = grouped;
        }

        @Override
        public <U> U reduce(Transform<U> transformer)
        {
            return _reduce(transformer, _left.reduce(transformer), _right.reduce(transformer));
        }

        protected abstract <U> U _reduce(Transform<U> transformer, U left, U right);

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BinaryTerm that = (BinaryTerm) o;

            if (_left != null ? !_left.equals(that._left) : that._left != null) return false;
            if (_right != null ? !_right.equals(that._right) : that._right != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = _left != null ? _left.hashCode() : 0;
            result = 31 * result + (_right != null ? _right.hashCode() : 0);
            return result;
        }
    }

    static public class OrTerm extends BinaryTerm
    {
        public OrTerm(SubsetExpression left, SubsetExpression right)
        {
            this(left, right, false);
        }

        public OrTerm(SubsetExpression left, SubsetExpression right, boolean grouped)
        {
            super(left, right, grouped);
        }

        public BitSet apply(Subset subset, PopulationSet populationSet)
        {
            BitSet left = _left.apply(subset, populationSet);
            BitSet right = _right.apply(subset, populationSet);
            left.or(right);
            return left;
        }

        @Override
        public <U> U _reduce(Transform<U> transformer, U left, U right)
        {
            return transformer.or(this, left, right);
        }

        @Override
        protected String _toString(SubsetSpec parent, boolean escaped)
        {
            return _left.toString(parent, escaped) + "|" + _right.toString(parent, escaped);
        }
    }
    
    static public class AndTerm extends BinaryTerm
    {
        public AndTerm(SubsetExpression left, SubsetExpression right)
        {
            this(left, right, false);
        }

        public AndTerm(SubsetExpression left, SubsetExpression right, boolean grouped)
        {
            super(left, right, grouped);
        }

        public BitSet apply(Subset subset, PopulationSet populationSet)
        {
            BitSet left = _left.apply(subset, populationSet);
            BitSet right = _right.apply(subset, populationSet);
            left.and(right);
            return left;
        }

        @Override
        public <U> U _reduce(Transform<U> transformer, U left, U right)
        {
            return transformer.and(this, left, right);
        }

        @Override
        protected String _toString(SubsetSpec parent, boolean escaped)
        {
            return _left.toString(parent, escaped) + "&" + _right.toString(parent, escaped);
        }
    }

    static public class NotTerm extends SubsetExpression
    {
        SubsetExpression _term;
        public NotTerm(SubsetExpression term)
        {
            this(term, false);
        }

        public NotTerm(SubsetExpression term, boolean grouped)
        {
            _term = term;
            _grouped = grouped;
        }
        public BitSet apply(Subset subset, PopulationSet populationSet)
        {
            BitSet set = _term.apply(subset, populationSet);
            set.flip(0, subset.getDataFrame().getRowCount());
            return set;
        }

        @Override
        public <U> U reduce(Transform<U> transformer)
        {
            return transformer.not(this, _term.reduce(transformer));
        }

        @Override
        protected String _toString(SubsetSpec parent, boolean escaped)
        {
            return "!" + _term.toString(parent, escaped);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NotTerm notTerm = (NotTerm) o;

            if (_term != null ? !_term.equals(notTerm._term) : notTerm._term != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            return _term != null ? _term.hashCode() : 0;
        }
    }

    static public class SubsetTerm extends SubsetExpression
    {
        SubsetSpec _spec;

        public SubsetTerm(SubsetSpec spec)
        {
            this(spec, false);
        }

        public SubsetTerm(SubsetSpec spec, boolean grouped)
        {
            _spec = spec;
            _grouped = grouped;
        }

        public SubsetSpec getSpec()
        {
            return _spec;
        }

        public void setSpec(SubsetSpec spec)
        {
            _spec = spec;
        }

        @Override
        public <U> U reduce(Transform<U> transformer)
        {
            return transformer.subset(this);
        }

        public BitSet apply(Subset subset, PopulationSet populationSet)
        {
            SubsetPart[] terms = _spec.getSubsets();
            BitSet ret = new BitSet();
            ret.flip(0, subset.getDataFrame().getRowCount());
            for (int i = 0; i < terms.length; i ++)
            {
                Object term = terms[i];
                // UNDONE: allow populations below boolean expressions
                if (term instanceof SubsetExpression)
                {
                    assert i == terms.length - 1;
                    SubsetExpression expr = (SubsetExpression)term;
                    BitSet bits = expr.apply(subset, populationSet);
                    ret.and(bits);
                    return ret;
                }
                else if (term instanceof PopulationName)
                {
                    Population pop = populationSet.getPopulation((PopulationName)term);

                    if (pop == null)
                    {
                        throw new FlowException("Could not find subset '" + _spec + "'");
                    }
                    for (Gate gate : pop.getGates())
                    {
                        BitSet bits = gate.apply(null, subset.getDataFrame());
                        ret.and(bits);
                    }
                    populationSet = pop;
                }
                else
                {
                    throw new FlowException("Unexpected subset term: " + term);
                }
            }
            return ret;
        }

        @Override
        protected String _toString(SubsetSpec parent, boolean escaped)
        {
            // Emit just the last subset part if this spec matches the passed in parent.
            // UNDONE: support relative paths other than just parent (e.g, using backslashes for parents like FlowJo xml)
            if (parent != null && parent.equals(_spec.getParent()))
                return _spec.getSubset().toString(escaped, true);

            return _spec.toString(escaped, true);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SubsetTerm that = (SubsetTerm) o;

            if (_spec != null ? !_spec.equals(that._spec) : that._spec != null) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            return _spec != null ? _spec.hashCode() : 0;
        }

    }

    // For testing
    static SubsetExpression And(SubsetExpression left, SubsetExpression right)
    {
        return new SubsetExpression.AndTerm(left, right);
    }

    // For testing
    static SubsetExpression Or(SubsetExpression left, SubsetExpression right)
    {
        return new SubsetExpression.OrTerm(left, right);
    }

    // For testing
    static SubsetExpression Not(SubsetExpression term)
    {
        return new SubsetExpression.NotTerm(term);
    }

    // For testing
    static SubsetExpression Group(SubsetExpression term)
    {
        return term.setGrouped(true);
    }

    static SubsetSpec Parents(String... names)
    {
        SubsetSpec spec = null;
        for (String name : names)
            spec = new SubsetSpec(spec, PopulationName.fromString(name));
        return spec;
    }

    // For testing
    static SubsetExpression Subset(String name, String... names)
    {
        SubsetSpec spec = null;
        if (name != null)
            spec = new SubsetSpec(spec, PopulationName.fromString(name));

        for (String n : names)
            spec = new SubsetSpec(spec, PopulationName.fromString(n));
        return new SubsetExpression.SubsetTerm(spec);
    }

    // For testing
    static SubsetExpression Subset(String name, Object... parts)
    {
        SubsetSpec spec = null;
        if (name != null)
            spec = new SubsetSpec(spec, PopulationName.fromString(name));

        for (Object part : parts)
            if (part instanceof String)
                spec = new SubsetSpec(spec, PopulationName.fromString((String)part));
            else if (part instanceof SubsetExpression)
                spec = new SubsetSpec(spec, (SubsetExpression)part);
        return new SubsetExpression.SubsetTerm(spec);
    }

}
