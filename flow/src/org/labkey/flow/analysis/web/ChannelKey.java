package org.labkey.flow.analysis.web;

class ChannelKey
{
    public ChannelKey(CompSign sign, String name)
    {
        _sign = sign;
        _channel = name;
    }
    final CompSign _sign;
    final String  _channel;
    public boolean equals(Object o)
    {
        if (o == null || o.getClass() != getClass())
            return false;
        ChannelKey that = (ChannelKey) o;
        return that._sign == this._sign && that._channel.equals(this._channel);
    }
    public int hashCode()
    {
        return _sign.hashCode() ^ _channel.hashCode();
    }

    public CompSign getSign()
    {
        return _sign;
    }

    public String getName()
    {
        return _channel;
    }
}
