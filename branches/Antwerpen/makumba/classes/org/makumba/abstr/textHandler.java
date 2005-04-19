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

package org.makumba.abstr;
import org.makumba.InvalidValueException;
import org.makumba.Pointer;
import org.makumba.Text;

public class textHandler extends FieldHandler implements stringType
{
  public Object getEmptyValue(){ return ""; }

  public String getDataType() { return "text"; }
  public Class getJavaType() { return org.makumba.Text.class; }

  public String getDefaultString(){ return (String)getDefaultValue(); }

  public Object getNull() { return Pointer.NullText; }

  public Object checkValueImpl(Object value) 
  { 
    try{
      return Text.getText(value);
    }catch(InvalidValueException e) {throw new InvalidValueException(getFieldDefinition(), e.getMessage()); }
    
  }
}
