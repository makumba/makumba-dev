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

package org.makumba.controller.html;
import org.makumba.*;
import org.makumba.abstr.*;
import org.makumba.view.*;
import java.util.*;
import javax.servlet.http.*;

public class RecordEditor extends RecordFormatter
{
  String database;

  public RecordEditor(RecordInfo ri, Hashtable h, String database)   
  { 
    super(ri, h); 
    this.database= database;
  }  

  public Dictionary readFrom(HttpServletRequest req, String suffix)
  {
    Dictionary data= new Hashtable();
    for(Enumeration e=handlerOrder.elements(); e.hasMoreElements(); )
      {
	FieldEditor fe= (FieldEditor)e.nextElement();
	if(fe.getInputName(suffix)==null)
	  continue;
	Object o= fe.readFrom(org.makumba.controller.http.RequestAttributes.getParameters(req), suffix);
	if(o!=null)
	  o=fe.getFieldInfo().checkValue(o);
	else
	  o=fe.getNull();

	org.makumba.controller.http.RequestAttributes.setAttribute(req, fe.getInputName(suffix)+"_type", fe.getFieldInfo());

	// FIXME: semantics of EDIT might be wrong here
	if(o!=null)
	  data.put(fe.getInputName(suffix), o);
	org.makumba.controller.http.RequestAttributes.setAttribute(req, fe.getInputName(suffix), o);
      }
    return data;
  }

  public void config()
  {
    Object a[]= { this} ;
    try{
      callAll(getHandlerMethod("onStartup"), a);
    }catch(java.lang.reflect.InvocationTargetException e)
      {
	throw new org.makumba.MakumbaError(e.getTargetException());
      }
  }

}