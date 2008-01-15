package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 1:06:34 PM
 */
public class ParamsValidatorFactory
{
    private static BooleanParamsValidator _boolean;
    private static NaturalNumberParamsValidator _naturalNumber;
    private static PositiveDoubleParamsValidator _positiveDouble;
    private static RealNumberParamsValidator _realNumber;
    private static PositiveIntegerParamsValidator _positiveInteger;
    private static ListParamsValidator _listParamsValidator;


    public static IParamsValidator getBooleanParamsValidator()
    {
        if (_boolean == null)
        {
            _boolean = new BooleanParamsValidator();
        }
        return _boolean;
    }

    public static IParamsValidator getNaturalNumberParamsValidator()
    {
        if (_naturalNumber == null)
        {
            _naturalNumber = new NaturalNumberParamsValidator();
        }
        return _naturalNumber;
    }

    public static IParamsValidator getPositiveDoubleParamsValidator()
    {
        if (_positiveDouble == null)
        {
            _positiveDouble = new PositiveDoubleParamsValidator();
        }
        return _positiveDouble;
    }

    public static IParamsValidator getRealNumberParamsValidator()
    {
        if (_realNumber == null)
        {
            _realNumber = new RealNumberParamsValidator();
        }
        return _realNumber;
    }

    public static IParamsValidator getPositiveIntegerParamsValidator()
    {
        if (_positiveInteger == null)
        {
            _positiveInteger = new PositiveIntegerParamsValidator();
        }
        return _positiveInteger;
    }

    public static ListParamsValidator getListParamsValidator(String[] list)
    {
        if(_listParamsValidator == null)
        {
            _listParamsValidator = new ListParamsValidator(list);
        }
        return _listParamsValidator;
    }
}
