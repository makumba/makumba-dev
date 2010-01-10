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

package org.makumba.view.jsptaglib;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.JspException;

import org.makumba.util.MultipleKey;
import org.makumba.FieldDefinition;
import org.makumba.abstr.FieldInfo;

import org.makumba.controller.jsp.PageAttributes;

import org.makumba.LogicException;
import org.makumba.ProgrammerError;

public class InputTag extends MakumbaTag
{
  String name;
  String valueExprOriginal;
  String dataType;
  String display;
  String expr;

  public String toString() { return "INPUT name="+name+" value="+valueExprOriginal+" dataType="+dataType; }
  

  public void setField(String field)  { setName(field);}
  public void setName(String field) {   this.name=field.trim(); }
  public void setValue(String value) {   this.valueExprOriginal=value.trim(); }
  public void setDataType(String dt) {   this.dataType=dt.trim();  }
  public void setDisplay(String d) {   this.display=d; }
  public void setType(String s) 
  {
    super.setType(s);
    if(s.equals("file"))
      getForm().setMultipart();
  }  

  FormTagBase getForm() 
  { return (FormTagBase)TagSupport.findAncestorWithClass(this, FormTagBase.class); }

  boolean isValue()
  {
    return expr!=null && !expr.startsWith("$");
  }

  boolean isAttribute()
  {
    return expr!=null && expr.startsWith("$");
  }
  /** Set tagKey to uniquely identify this tag. Called at analysis time before doStartAnalyze() and at runtime before doMakumbaStartTag() */
  public void setTagKey() 
  {
    expr=valueExprOriginal;
    if(expr==null)
      expr=getForm().getDefaultExpr(name);
    Object[] keyComponents= {name, getForm().tagKey};
    tagKey= new MultipleKey(keyComponents);
  }

  /** determine the ValueComputer and associate it with the tagKey */
  public void doStartAnalyze()
  {
    if(name==null)
      throw new ProgrammerError("name attribute is required");

    if(isValue())
      pageCache.valueComputers.put(tagKey, ValueComputer.getValueComputer(this, expr));
  }

  /** tell the ValueComputer to finish analysis, and set the types for var and printVar */
  public void doEndAnalyze()
  {
    FieldInfo dataTypeInfo=null;  
    FieldInfo type=null;

    if(dataType!=null)
      dataTypeInfo=FieldInfo.getFieldInfo(name, dataType, true);

    if(isValue())
      {
	ValueComputer vc= (ValueComputer)pageCache.valueComputers.get(tagKey);
	vc.doEndAnalyze(this);
	type= (FieldInfo)vc.type;
      }
    if(isAttribute())
      type=(FieldInfo)pageCache.types.get(expr.substring(1));
    if(type==null)
      {
	type= (FieldInfo)getForm().getDefaultType(name);
	if(type==null && getForm().canComputeTypeFromEnclosingQuery())
	  type=(FieldInfo)getForm().computeTypeFromEnclosingQuery(name);
      }

    if(type!=null && dataTypeInfo!=null && !dataTypeInfo.compatible(type))
      throw new ProgrammerError("computed type for INPUT is different from the indicated dataType: "+this+" has dataType indicated to "+ dataType+ " type computed is "+type);
    
    
    if(type==null)
      type=dataTypeInfo;
    
    if(type==null)
      throw new ProgrammerError("cannot determine input type: "+this+" . Please specify the type using dataType=...");
    pageCache.inputTypes.put(tagKey, type);
  }

  public int doMakumbaStartTag() throws JspException, LogicException
  {
    Object val=null;

    if(isValue())
      val=((ValueComputer)getPageCache(pageContext).valueComputers.get(tagKey)).getValue(this);
    if(isAttribute())
      val=PageAttributes.getAttributes(pageContext).getAttribute(expr.substring(1));

    FieldInfo type= (FieldInfo)pageCache.inputTypes.get(tagKey);

    if(val!=null)
      val=((FieldInfo)type).checkValue(val);
    
    String formatted=getForm().responder.format(name, type, val, params);

    if(display==null ||! display.equals("false"))
      {
	try{
	  pageContext.getOut().print(formatted);
	}catch(java.io.IOException e)	  {throw new JspException(e.toString());}
      }
    return EVAL_BODY_INCLUDE;
  } 
}