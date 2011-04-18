/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
