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
import java.util.Vector;
import org.makumba.*;

public class setHandler extends ptrHandler implements subtableHandler
{   
  public String getDataType() { return "set"; }
  public Class getJavaType() { return java.util.Vector.class; }
  public Object getNull() { return Pointer.NullSet; }

  public RecordInfo getSubtable() { return (RecordInfo)fi.extra1; }

  public boolean isAssignableFrom(FieldInfo fi)
  { 
    return "nil".equals(fi.getType())||  
      getType().equals(fi.getType()) && 
      getForeignTable().getName().equals(fi.getForeignTable().getName()); 
  }
  
  FieldInfo pointerToForeign()
  {
    return (FieldInfo)getSubtable().fields.get(getSubtable().fieldOrder.elementAt(4));
  }

  public RecordInfo getForeignTable()
  {
    if(fi.extra3==null)  // automatic set
      return pointerToForeign().getForeignTable();
    else return (RecordInfo)fi.extra3; // manually made
  }  

  public Object checkValueImpl(Object value)
  {
    try{
      // may be just a pointer
      Object o= super.checkValueImpl(value);
      Vector v= new Vector();
      if(o!=null && o instanceof Pointer)
	v.addElement(o);
      return v;
    }catch(org.makumba.InvalidValueException ive){}
    
    normalCheck(value);

    Vector v=(Vector)value;

    FieldInfo ptr= getForeignTable().getField(getForeignTable().getIndexName());
    
    for(int i=0; i<v.size(); i++)
      {
	if(v.elementAt(i)==null || v.elementAt(i).equals(org.makumba.Pointer.Null))
	  throw new org.makumba.InvalidValueException(getFieldInfo(), "set members cannot be null");
	try{
	  v.setElementAt(ptr.checkValue(v.elementAt(i)), i);
	}catch(org.makumba.InvalidValueException e)
	  { 
	    throw new org.makumba.InvalidValueException(getFieldInfo(), "the set member <"+v.elementAt(i)+"> is not assignable to pointers of type "+getForeignTable().getName());
	  }
      }
    return v;
  }

}
