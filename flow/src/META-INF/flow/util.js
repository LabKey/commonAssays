///////////////////////////////////////////////////////////////
// StringSubst
//
// Performs a simple string substitution

function StringSubst(str, o)
	{
	var rg = str.split('|');
	for (var i = 1; i < rg.length; i += 2)
		{
		if (rg[i])
			{
			rg[i] = o[rg[i]];
			}
		else
			{
			rg[i] = '|';
			}
		}
	return rg.join('');
	}

///////////////////////////////////////////////////////////////
// _StringSubst
//
// Called as a method, invokes the simple string substitution
// with the this.str as the string to be substituted.

function _StringSubst()
	{
	return StringSubst(this.str, this);
	}

///////////////////////////////////////////////////////////////
// StringArray
// Just like a normal array, but overloads "join" so that it
// doesn't put in commas.

function StringArrayToString()
	{
	return this.join('');
	}

function StringArray()
	{
	var ret = new Array;
	ret.toString = StringArrayToString;
	return ret;
	}

function BaseObj()
{
    return {
        toString : _StringSubst
    };
}

/*function EventProlog(event, w)
  {
  if (!w)
    w = window;
  if (w.event)
    {
    event = w.event;
    }
  else
    {
    event.srcElement = event.target;

	if (event.ctrlKey == undefined)
		event.ctrlKey = Boolean(event.modifiers & Event.CONTROL_MASK);
	if (event.altKey == undefined)
		event.altKey = Boolean(event.modifiers & Event.ALT_MASK);
	if (event.shiftKey == undefined)
		event.shiftKey = Boolean(event.modifiers & Event.SHIFT_MASK);
    switch (event.type)
      {
      case "click":
      case "mousedown":
      case "mouseup":
		if (event.button == undefined)
			event.button = event.which;
        break;
      case "keypress":
      case "keydown":
      case "keyup":
		if (event.keyCode == undefined)
			event.keyCode = event.which;
        break;
      }
    }
  return event;
  }*/

function toRc(rect)
{
    return { x : rect.left,
            y : rect.top,
            width: rect.right - rect.left,
            height: rect.bottom - rect.top };
}

function urlEncode(str)
{
    var ret = window.escape(str);
    return ret.replace(/\+/g, "%2B");
}

function getValue(el)
{
    if (el.options)
        return el.options[el.selectedIndex].value;
    return el.value;
}

function setValue(el, value)
{
    if (el.options)
    {
        for (var i = 0; i < el.options.length; i ++)
        {
            if (el.options[i].value == value)
            {
                el.selectedIndex = i;
                return;
            }
        }
        return;
    }
    el.value = value;
}

function getBoundingRc(el)
{
    if (el.getBoundingClientRect)
       {
       var rect;
       try
       {
           rect = el.getBoundingClientRect();
       }
       catch(e)
       {

       }
       /*if (rect)
        {
        var ptScroll = GetScrollPosition();
        return {left: rc.left + ptScroll.x, top: rc.top + ptScroll.y, right: rc.right + ptScroll.x, bottom: rc.bottom + ptScroll.y};
        }*/
      return toRc(rect);
      }
    var dx = el.offsetWidth;
    var dy = el.offsetHeight;
    var x = el.offsetLeft;
    var y = el.offsetTop;
    var fFirst = true;
    while (true)
      {
      el = GetOffsetParent(el);
      if (!el)
        break;
      x += el.offsetLeft;
      y += el.offsetTop;
      fFirst = false;
      }

    return {left: x, top: y, right: x + dx, bottom: y + dy};
}

function h(str)
{
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/"/g, "&quot;");
}

function q(str)
{
    return "'" + str.replace(/\\/g, "\\\\").replace(/'/g, "\\'") + "'";
}

function hq(str)
{
    return h(q(str));
}

function EscapeXmlText(str)
  {
  if (!str.match(/[&<>\240]/))
    return str;
  }
