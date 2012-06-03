/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function populateSelect(el, values)
{
    var cur;
    cur = getValue(el);
    el.options.length = 0;
    if (values)
    {
        for (var index = 0; index < values.length; index ++)
        {
            var value = values[index];
            var optionTag = new Option(value, value);
            el.options[el.options.length] = optionTag;
            if (value == cur)
            {
                el.selectedIndex = el.options.length - 1;
            }
        }
    }
}

function makeArrayFromPropNames(o)
{
    var ret = [];
    for (p in o)
    {
        ret.push(p);
    }
    return ret;
}

function getValue(sel)
{
    if (sel.selectedIndex < 0)
        return null;
    return sel.options[sel.selectedIndex].value;
}

function setValue(sel, value)
{
    for (var i = 0; i < sel.length; i++)
    {
        var option = sel.options[i];
        if (option.value == value)
        {
            sel.selectedIndex = i;
            return true;
        }
    }
    return false;
}

function selectKeywordName(sign, index)
{
    return document.getElementsByName(sign + "KeywordName[" + index + "]")[0];
}
function selectKeywordValue(sign, index)
{
    return document.getElementsByName(sign + "KeywordValue[" + index + "]")[0];
}
function selectSubset(sign, index)
{
    return document.getElementsByName(sign + "Subset[" + index + "]")[0];
}

function selectFilterField(index)
{
    return document.getElementsByName("ff_filter_field")[index];
}
function selectFilterOp(index)
{
    return document.getElementsByName("ff_filter_op")[index];
}
function selectFilterValue(index)
{
    return document.getElementsByName("ff_filter_value")[index];
}

function populateKeywordValues(sign, index)
{
    var elKeywordName = selectKeywordName(sign, index);
    var keyword = getValue(elKeywordName);
    var oValues = [];
    if (keyword)
    {
        oValues = keywordValueSubsetListMap[keyword];
    }
    var elKeywordValue = selectKeywordValue(sign, index);
    populateSelect(elKeywordValue, makeArrayFromPropNames(oValues))
    populateSubsets(sign, index);
}

function populateSubsets(sign, index)
{
    var elKeywordName = selectKeywordName(sign, index);
    var keyword = getValue(elKeywordName);
    var values = [''];
    if (keyword)
    {
        var elKeywordValue = selectKeywordValue(sign, index);
        var value = getValue(elKeywordValue);
        values = values.concat(keywordValueSubsetListMap[keyword][value]);
    }
    var elSubset = selectSubset(sign, index);
    populateSelect(elSubset, values);
    elSubset.options[0].text = 'Ungated';
}

function clearFilters()
{
    for (var i = 1; ; i++)
    {
        var elFilterField = selectFilterField(i);
        if (!elFilterField)
            return;

        setValue(elFilterField, "");
        setValue(selectFilterOp(i), "eq");
        var elFilterValue = selectFilterValue(i);
        elFilterValue.value = "";
    }
}

function populateAutoComp(selectAutoCompScript)
{
    var scriptName = getValue(selectAutoCompScript);
    if (!scriptName)
        return;

    var script = AutoComp[scriptName];
    if (!script)
        return;

    if (script.criteria)
    {
        var secondaryKeyword = script.criteria[1];
        var secondaryValue = script.criteria[2];
        if (!setValue(selectFilterField(0), secondaryKeyword))
            setValue(selectFilterField(0), "Keyword/" + secondaryKeyword);
        setValue(selectFilterOp(0, "eq"));
        var elFilterValue = selectFilterValue(0);
        elFilterValue.value = secondaryValue;
    
        clearFilters();
    }

    for (var paramIndex = 0; paramIndex < parameters.length; paramIndex++)
    {
        var paramName = parameters[paramIndex];
        var param = script.params[paramName];
        if (!param)
        {
            clearParameter("positive", paramIndex);
            clearParameter("negative", paramIndex);
        }
        else
        {
            populateAutoCompParameter(param, paramName, paramIndex);
        }
    }

    var universalNegative = script.params["Universal Negative"];
    if (universalNegative)
    {
        populateUniversalNegative(universalNegative);
    }
}

function populateUniversalNegative(param)
{
    var keyword = param[0];
    var value = param[1];
    var negativeSubset = param[3];

    populateAutoCompValues("negative", 0, keyword, value, negativeSubset);
    universalNegative();
}

function populateAutoCompParameter(param, paramName, paramIndex)
{
    var keyword = param[0];
    var value = param[1];
    var positiveSubset = param[2];
    var negativeSubset = param[3];

    if (positiveSubset)
    {
        populateAutoCompValues("positive", paramIndex, keyword, value, positiveSubset);
    }

    if (negativeSubset)
    {
        populateAutoCompValues("negative", paramIndex, keyword, value, negativeSubset);
    }
}

function populateAutoCompValues(sign, index, keyword, value, subset)
{
    var elKeywordName = selectKeywordName(sign, index);
    setValue(elKeywordName, keyword);
    populateKeywordValues(sign, index);

    var elKeywordValue = selectKeywordValue(sign, index);
    setValue(elKeywordValue, value);
    populateSubsets(sign, index);

    var elSubset = selectSubset(sign, index);
    setValue(elSubset, subset);
}

function clearParameter(sign, index)
{
    var elKeywordName = selectKeywordName(sign, index);
    setValue(elKeywordName, "");
    populateKeywordValues(sign, index);
}

function copyOptions(elSrc, elDest, i)
{
    elDest.options.length = 0;
    var idxSel = elSrc.selectedIndex;
    for (var i = 0; i < elSrc.options.length; i ++)
    {
        var src = elSrc.options[i];
        var dest = new Option(src.value, src.text, i == idxSel);
        elDest.options[elDest.options.length] = dest;
    }
    elDest.selectedIndex = elSrc.selectedIndex;
}

function universalNegative()
{
    var idxName = selectKeywordName("negative", 0).selectedIndex;
    var elValue = selectKeywordValue("negative", 0);
    var elSubset = selectSubset("negative", 0);
    for (var i = 1; ; i ++)
    {
        var elKeyword = selectKeywordName("negative", i);
        if (!elKeyword)
            return;
        elKeyword.selectedIndex = idxName;
        copyOptions(elValue, selectKeywordValue("negative", i));
        copyOptions(elSubset, selectSubset("negative", i));
    }
}
