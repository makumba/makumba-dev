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

package org.makumba.view;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.makumba.abstr.FieldHandler;

public class FieldFormatter extends FieldHandler
{
  static String[] params={ "default", "empty" };
  static String[][] paramValues={ null, null };
  public String[] getAcceptedParams(){ return params; }
  public String[][] getAcceptedValue(){ return paramValues; }

  static Object dummy=new Object();

  String expr;

  public String getExpr() 
  { 
    if(expr!=null)
      return expr;
    return getName();
  }

  public void initExpr(String s) { expr=s; }

  Hashtable validParams= new Hashtable(13);

  public FieldFormatter()
  {
    for(int i=0; i<getAcceptedParams().length; i++)
      {
	Hashtable h=new Hashtable(13);
	if(getAcceptedValue()[i]!=null)
	  for(int j=0; j<getAcceptedValue()[i].length; j++)
	    h.put(getAcceptedValue()[i][j], dummy);
	validParams.put(getAcceptedParams()[i], h);
      }
  }

  public void checkParams(Dictionary formatParams)
  {
    for(Enumeration e=formatParams.keys(); e.hasMoreElements(); )
      {
	String s=(String)e.nextElement();
	if(s.startsWith("org.makumba"))
	  continue;
	checkParam(s, ((String)formatParams.get(s)).toLowerCase());
      }
  }

  public void checkParam(String name, String val)
  {
    Hashtable h=(Hashtable)validParams.get(name);
    if(h==null)
      throw new InvalidValueException(this, "invalid format parameter \'"+name+"\'");
    if(h.size()==0)
      return;
    if(h.get(val)==null)
      throw new InvalidValueException(this, "invalid value for format parameter \'"+name+"\': <"+val+">");
  }

  /** 
   * Format the object to pure text. If text-format is blank, try the "empty" replacer value.
   */
  public String format(Object o, Dictionary formatParams)
  {
    String formatted;
    if( o==null || o.equals(getNull()) ) {
       formatted = formatNull(formatParams);
    } else {
       formatted = formatNotNull(o, formatParams);
    }
    if ( "".equals(formatted) ) {
      return getEmptyValueFormat(formatParams);
    }
    return formatted;
  }

  /** 
   * Format the null-object to pure text. Try the "default" format parameter.
   */
  public String formatNull(Dictionary formatParams) { 
      return getDefaultValueFormat(formatParams);
  }
  
  /** 
   * Format the not-null-object to pure text. To be over-ridden by subclasses.
   */
  public String formatNotNull(Object o, Dictionary formatParams) {return o.toString(); }

  public int getIntParam(Dictionary formatParams, String name)
  {
    String s=(String)formatParams.get(name);
    if(s==null)
      return -1;
    try{
      return Integer.parseInt(s);
    }catch(NumberFormatException e) { throw new InvalidValueException(this, "invalid integer for "+name+": "+s); }
  }

  public String getIntParamString(Dictionary formatParams, String name)
  {
    int n=getIntParam(formatParams, name);
    if(n==-1)
      return "";
    return name+"=\""+n+"\" ";
  }
 
 
  // FIXME? these 2 might get more complicated, if {default, empty} are OQL expressions.
  
  /** Gets the formatted default value, used if real value is null. Returns blank if not set.*/
  public String getDefaultValueFormat(Dictionary formatParams) {
      String s = (String) formatParams.get("default");
      return (s == null)? "" : s ;
  }
  
  /** Gets the formatted empty value, used if real value is empty. Returns blank if not set. */
  public String getEmptyValueFormat(Dictionary formatParams) {
      String s = (String) formatParams.get("empty");
      return (s == null)? "" : s ;
  }

  /** Chooses between the real (formatted) value and possible replacements (default, empty). */
  public String resetValueFormat( String s, Dictionary formatParams){
     if (s == null) {
         s = getDefaultValueFormat(formatParams);
     }
     if ( "".equals(s) ) {
         return getEmptyValueFormat(formatParams);
     }     
     return s;     
  }
}
// end class