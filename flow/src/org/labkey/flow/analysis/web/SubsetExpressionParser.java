package org.labkey.flow.analysis.web;

import java.util.Map;
import java.util.HashMap;
import org.labkey.flow.analysis.model.FlowException;

public class SubsetExpressionParser
{
    enum Tk
    {
        subset,
        opOr,
        opAnd,
        opNot,
        opLParen,
        opRParen,
        eof,
    }

    static Map<Character,Tk> s_mapCharTk = new HashMap();
    static
    {
        s_mapCharTk.put('|', Tk.opOr);
        s_mapCharTk.put('&', Tk.opAnd);
        s_mapCharTk.put('!', Tk.opNot);
        s_mapCharTk.put('(', Tk.opLParen);
        s_mapCharTk.put(')', Tk.opRParen);
    }

    static class Token
    {
        public Token(Tk tk, String text)
        {
            _tk = tk;
            _text = text;
        }
        Tk _tk;
        String _text;
    }

    int _ich;
    String _strExpr;
    Token _next;

    public SubsetExpressionParser(String expr)
    {
        _strExpr = expr;
    }

    public SubsetExpression parse()
    {
        SubsetExpression ret = parseOr();
        Token tk = next();
        if (tk._tk != Tk.eof)
        {
            throw error("Expected end of expression");
        }
        return ret;
    }

    private SubsetExpression parseOr()
    {
        SubsetExpression left = parseAnd();
        Token next = next();
        if (next._tk != Tk.opOr)
        {
            unget(next);
            return left;
        }
        SubsetExpression right = parseOr();
        return new SubsetExpression.OrTerm(left, right);
    }

    private SubsetExpression parseAnd()
    {
        SubsetExpression left = parseNot();
        Token next = next();
        if (next._tk != Tk.opAnd)
        {
            unget(next);
            return left;
        }
        SubsetExpression right = parseAnd();
        return new SubsetExpression.AndTerm(left, right);
    }

    private SubsetExpression parseNot()
    {
        Token next = next();
        if (next._tk != Tk.opNot)
        {
            unget(next);
            return parsePrimary();
        }
        SubsetExpression expr = parsePrimary();
        return new SubsetExpression.NotTerm(expr);
    }

    private SubsetExpression parsePrimary()
    {
        SubsetExpression ret;
        Token next = next();
        switch (next._tk)
        {
            case opLParen:
                ret = parseOr();
                Token nextCheck = next();
                if (nextCheck._tk != Tk.opRParen)
                {
                    throw error("Expected: ')'");
                }
                break;
            case subset:
                ret = new SubsetExpression.SubsetTerm(SubsetSpec.fromString(next._text));
                break;
            default:
                throw error("Unexpected: '" + next._text + "'");
        }
        return ret;
    }

    private void unget(Token tk)
    {
        _next = tk;
    }

    private Token next()
    {
        Token ret;
        if (_next != null)
        {
            ret = _next;
            _next = null;
            return ret;
        }
        if (_ich == _strExpr.length())
        {
            return new Token(Tk.eof, "");
        }
        char ch = _strExpr.charAt(_ich);
        Tk tk = s_mapCharTk.get(ch);
        if (tk != null)
        {
            ret = new Token(tk, _strExpr.substring(_ich, _ich + 1));
            _ich ++;
            return ret;
        }
        int ich;
        for (ich = _ich + 1; ich < _strExpr.length(); ich ++)
        {
            if (s_mapCharTk.get(_strExpr.charAt(ich)) != null)
                break;
        }
        ret = new Token(Tk.subset, _strExpr.substring(_ich, ich));
        _ich = ich;
        return ret;
    }

    private FlowException error(String message)
    {
        return new FlowException("Malformed subset expression '" + _strExpr + "' at character " + _ich + ": " + message);
    }
}
