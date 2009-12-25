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
import org.makumba.*;

public class intHandler extends FieldHandler 
{
  static final Object empty= new Integer(0);
  public Object getEmptyValue(){ return empty; }
  public String getDataType() { return "int"; }
  public Class getJavaType() { return java.lang.Integer.class; }

  public int getDefaultInt(){ return ((Integer)getDefaultValue()).intValue(); }
  
  public Object getNull() { return Pointer.NullInteger; }

  public Object checkValueImpl(Object value) { return normalCheck(value); }

  public boolean isAssignableFrom(FieldInfo fi){ return super.isAssignableFrom(fi) || fi.getType().equals("intEnum"); }

}