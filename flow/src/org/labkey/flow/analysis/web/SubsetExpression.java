package org.labkey.flow.analysis.web;

import org.labkey.flow.analysis.model.*;

import java.util.BitSet;

abstract public class SubsetExpression
{
    /**
     * Expr -> (SubsetExpr)
     * SubsetExpr -> SubsetExpr + SubsetTerm | SubsetTerm
     * SubsetTerm -> SubsetTerm * SubsetFactor | SubsetFactor
     * SubsetFactor ->
     */

    static public SubsetExpression fromString(String expression)
    {
        SubsetExpressionParser parser = new SubsetExpressionParser(expression);
        return parser.parse();
    }

    abstract public BitSet apply(Subset subset, PopulationSet populationSet);

    abstract static public class BinaryTerm extends SubsetExpression
    {
        protected SubsetExpression _left;
        protected SubsetExpression _right;
        public BinaryTerm(SubsetExpression left, SubsetExpression right)
        {
            _left = left;
            _right = right;
        }
    }
    static public class OrTerm extends BinaryTerm
    {
        public OrTerm(SubsetExpression left, SubsetExpression right)
        {
            super(left, right);
        }

        public BitSet apply(Subset subset, PopulationSet populationSet)
        {
            BitSet left = _left.apply(subset, populationSet);
            BitSet right = _right.apply(subset, populationSet);
            left.or(right);
            return left;
        }
    }
    static public class AndTerm extends BinaryTerm
    {
        public AndTerm(SubsetExpression left, SubsetExpression right)
        {
            super(left, right);
        }
        public BitSet apply(Subset subset, PopulationSet populationSet)
        {
            BitSet left = _left.apply(subset, populationSet);
            BitSet right = _right.apply(subset, populationSet);
            left.and(right);
            return left;
        }
    }
    static public class NotTerm extends SubsetExpression
    {
        SubsetExpression _term;
        public NotTerm(SubsetExpression term)
        {
            _term = term;
        }
        public BitSet apply(Subset subset, PopulationSet populationSet)
        {
            BitSet set = _term.apply(subset, populationSet);
            set.flip(0, subset.getDataFrame().getRowCount());
            return set;
        }
    }
    static public class SubsetTerm extends SubsetExpression
    {
        SubsetSpec _spec;
        public SubsetTerm(SubsetSpec spec)
        {
            _spec = spec;
        }

        public BitSet apply(Subset subset, PopulationSet populationSet)
        {
            String[] terms = _spec.getSubsets();
            BitSet ret = new BitSet();
            ret.flip(0, subset.getDataFrame().getRowCount());
            for (int i = 0; i < terms.length; i ++)
            {
                String term = terms[i];
                if (SubsetSpec.isExpression(term))
                {
                    assert i == terms.length - 1;
                    SubsetExpression expr = SubsetExpression.fromString(term);
                    BitSet bits = expr.apply(subset, populationSet);
                    ret.and(bits);
                    return ret;
                }
                Population pop = populationSet.getPopulation(term);

                if (pop == null)
                {
                    throw new FlowException("Could not find subset '" + _spec + "'");
                }
                for (Gate gate : pop.getGates())
                {
                    BitSet bits = gate.apply(subset.getDataFrame());
                    ret.and(bits);
                }
                populationSet = pop;
            }
            return ret;
        }
    }
}
