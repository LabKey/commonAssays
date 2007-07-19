function getValue(el)
{
    if (el.options)
        return el.options[el.selectedIndex].value;
    return el.value;
}

function appendLine(el, text)
{
    value = el.value;
    if (value && value.charAt(value.length - 1) != '\n')
    {
        value = value + '\n';
    }
    value += text;
    el.value = value;
}

