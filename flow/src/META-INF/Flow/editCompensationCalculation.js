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
