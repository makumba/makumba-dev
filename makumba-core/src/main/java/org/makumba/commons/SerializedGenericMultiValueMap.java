///////////////////////////////
//  Makumba, Makumba tag library
//  Copyright (C) 2000-2003  http://www.makumba.org
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
//  -------------
//  $Id$
//  $Name$
/////////////////////////////////////

package org.makumba.commons;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.collections4.map.MultiValueMap;

/**
 * This class provides a serialized version of {@link MultiValueMap}, and adds generics support.
 * 
 * @author Rudolf Mayer
 * @version $Id$
 */
public class SerializedGenericMultiValueMap<T> extends MultiValueMap implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    public Collection<T> values() {
        return super.values();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<T> get(Object key) {
        return super.getCollection(key);
    }
}
