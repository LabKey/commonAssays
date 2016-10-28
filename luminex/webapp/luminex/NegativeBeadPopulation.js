/*
 * Copyright (c) 2014-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.onReady(function(){
    attachAnalyteNegativeControlListeners();
    updateAnalyteNegativeBeadInputs();
});

function attachAnalyteNegativeControlListeners()
{
    var negBeadEls = document.querySelectorAll(".negative-bead-input");
    for (var i = 0; i < negBeadEls.length; i++)
    {
        var negControlEl = Ext4.get(getAnalyteNegativeControlEl(negBeadEls[i].name));
        negControlEl.on('change', updateAnalyteNegativeBeadInputs);
    }
}

function getAnalyteSelectedNegativeControls()
{
    var negControlAnalytes = [];
    var negBeadEls = document.querySelectorAll(".negative-bead-input");
    for (var i = 0; i < negBeadEls.length; i++)
    {
        var negControlEl = getAnalyteNegativeControlEl(negBeadEls[i].name);
        if (negControlEl != null && negControlEl.checked)
            negControlAnalytes.push(negBeadEls[i].getAttribute("analytename"));
    }
    return negControlAnalytes.sort();
}

function getAnalyteNegativeControlEl(negBeadElName)
{
    var negControlElName = negBeadElName.replace("_NegativeBead", "_NegativeControl");
    var els = document.getElementsByName(negControlElName);
    if (els.length == 1)
        return els[0];
    else
        return null;
}

function updateAnalyteNegativeBeadInputs()
{
    /*
     * When a change is made to a selection/de-selection of a Negative Control checkbox, get the updated set of
     * Negative Bead options and repopulate the dropdowns accordingly.
     */

    var clearSelectOptions = function(select) {
        while(select.options.length > 0)
            select.remove(0);
    };

    var addSelectOptions = function(select, optionVals, prevVal) {
        select.options.add(new Option("", ""));
        for (var i = 0; i < optionVals.length; i++)
        {
            select.options.add(new Option(optionVals[i], optionVals[i], false, optionVals[i] == prevVal));
        }
    };

    var negControlAnalytes = getAnalyteSelectedNegativeControls();

    var negBeadEls = document.querySelectorAll(".negative-bead-input");
    for (var i = 0; i < negBeadEls.length; i++)
    {
        var negBeadEl = negBeadEls[i];
        var prevValue = negBeadEl.value;
        var analyteName = negBeadEl.getAttribute("analytename");
        var hidden = negControlAnalytes.indexOf(analyteName) > -1;

        negBeadEl.value = null;
        clearSelectOptions(negBeadEl);
        negBeadEl.style.display = hidden ? 'none' : 'inline-block';
        if (!hidden)
        {
            addSelectOptions(negBeadEl, negControlAnalytes, prevValue);
        }
    }
}