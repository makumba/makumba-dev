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
import java.util.*;
import org.makumba.util.*;
import javax.servlet.http.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.*;
import org.makumba.*;

public class RootData
{
  //  static final String attrNameAttr="org.makumba.attributeName"; 
  String header, footer, db;
  MakumbaTag rootTag;
  Dictionary subtagData= new Hashtable();
  long stamp;
  int ntags=0;
  PageContext pageContext;

  public RootData(MakumbaTag t, PageContext pageContext){
    this.rootTag=t; 
    subtagData=new Hashtable();
    MakumbaSystem.getMakumbaLogger("taglib.performance").fine("---- tag start ---");
    stamp= new Date().getTime();
    this.pageContext=pageContext;
  }

  public void setStrategy(Object key, MakumbaTag tag) throws LogicException
  {
    ntags++;
    tag.strategy= (TagStrategy)subtagData.get(key);
    if(tag.strategy==null)
      {
	tag.strategy=tag.makeStrategy(key);
	if(!(tag.strategy instanceof QueryTagStrategy))
	  // a non-query
	  return;
	subtagData.put(key, tag.strategy);
	tag.strategy.init(rootTag, tag, key);
	if(!tag.template)
	  ((RootTagStrategy)rootTag.strategy).onInit(tag.strategy);
      }
    else
      // mostly for QueryStrategy
      if(tag.strategy instanceof TagStrategySupport)
	((TagStrategySupport)tag.strategy).tag=tag;
    tag.strategy.loop();
  }

  /** release resources kept by all the tags */
  protected void close()
  {
    if(subtagData==null)
      return;
    MakumbaSystem.getMakumbaLogger("taglib.performance").fine("tag time: "+(new Date().getTime()-stamp)+" ms "+ntags+" query tags");
    for(Enumeration e= subtagData.elements(); e.hasMoreElements();)
      ((TagStrategy)e.nextElement()).rootClose();
    subtagData=null;
  }
}