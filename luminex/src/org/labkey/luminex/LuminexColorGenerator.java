package org.labkey.luminex;

import java.util.Map;
import java.util.HashMap;
import java.awt.*;

public class LuminexColorGenerator
{
    private static final int[] SUBCOLOR_STRINGS = new int[]{0x66, 0xAA, 0xEE, 0x88, 0xCC};
    private int _r = 0;
    private int _g = 1;
    private int _b = 2;

    private Map<String, Color> _cache = new HashMap<String, Color>();

    public LuminexColorGenerator()
    {
    }

    public Color next(String specimenId)
    {
        Color result = _cache.get(specimenId);
        if (result != null)
        {
            return result;
        }
        advance();
        result = new Color(SUBCOLOR_STRINGS[_r % SUBCOLOR_STRINGS.length],
               SUBCOLOR_STRINGS[_g % SUBCOLOR_STRINGS.length],
               SUBCOLOR_STRINGS[_b % SUBCOLOR_STRINGS.length]);
        _cache.put(specimenId, result);
        return result;
    }

    private void advance()
    {
        do
        {
            _r = (_r + 1) % SUBCOLOR_STRINGS.length;
            if (_r % 2 == 0)
            {
                _g = (_g + 1) % SUBCOLOR_STRINGS.length;
                if (_g % 3 == 0)
                    _b = (_b + 1) % SUBCOLOR_STRINGS.length;
            }
        }
        while (_r == _g && _g == _b);
    }
}
