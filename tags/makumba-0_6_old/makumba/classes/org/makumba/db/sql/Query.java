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

package org.makumba.db.sql;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.makumba.DBError;
import org.makumba.DataDefinition;
import org.makumba.InvalidValueException;
import org.makumba.MakumbaSystem;
import org.makumba.OQLAnalyzer;
import org.makumba.db.DBConnection;
import org.makumba.db.sql.oql.QueryAST;

/** SQL implementation of a OQL query */
public class Query implements org.makumba.db.Query
{
  String query;
  TableManager resultHandler;
  String command;
  ParameterAssigner assigner;
  String limitSyntax;
  boolean offsetFirst;
  boolean supportsLimitInQuery;

  public String getCommand(){ return command; }

  public Query(org.makumba.db.Database db, String OQLQuery){ this(db, MakumbaSystem.getOQLAnalyzer(OQLQuery)); }
  public Query(org.makumba.db.Database db, OQLAnalyzer tree) 
  {
    command= ((QueryAST)tree).writeInSQLQuery(db);

    resultHandler= (TableManager)db.makePseudoTable((DataDefinition)tree.getProjectionType());
    assigner= new ParameterAssigner(db, tree);
	limitSyntax=((org.makumba.db.sql.Database)db).getLimitSyntax();
	offsetFirst=((org.makumba.db.sql.Database)db).isLimitOffsetFirst();
	supportsLimitInQuery=((org.makumba.db.sql.Database)db).supportsLimitInQuery();
  }

  public Vector execute(Object [] args, DBConnection dbc, int offset, int limit)
  {
	String com = command;  
	if(supportsLimitInQuery)
		com += " " + limitSyntax; //TODO: it might happen that it should be in other places than at the end.
    PreparedStatement ps=((SQLDBConnection)dbc).getPreparedStatement(com);
    
    try{
      String s=assigner.assignParameters(ps, args);
	  
      if(supportsLimitInQuery)
      {
		  int limit1=limit==-1?Integer.MAX_VALUE:limit;
		  
		  if(offsetFirst){
			  ps.setInt(assigner.tree.parameterNumber()+1, offset);
			  ps.setInt(assigner.tree.parameterNumber()+2, limit1);
		  }else{
			  ps.setInt(assigner.tree.parameterNumber()+1, limit1);
			  ps.setInt(assigner.tree.parameterNumber()+2, offset);
		  }
      }

      if(s!=null)
    	throw new InvalidValueException("Errors while trying to assign arguments to query:\n"+com+"\n"+s);

      MakumbaSystem.getMakumbaLogger("db.query.execution").fine(""+ps);
      java.util.Date d= new java.util.Date();
      ResultSet rs= null; 
      try{
    	  rs= ps.executeQuery();
      }catch(SQLException se)
	{ 
	  org.makumba.db.sql.Database.logException(se, dbc);
	  throw new DBError(se, com);
      }
      long diff = new java.util.Date().getTime()-d.getTime();
      MakumbaSystem.getMakumbaLogger("db.query.performance").fine(""+ diff +" ms "+com);
      return goThru(rs, resultHandler);
    }
    catch(SQLException e){ throw new org.makumba.DBError(e); }  
  }

  Vector goThru(ResultSet rs, TableManager rm) 
  {
	//TODO: if(!supportsLimitInQuery) { do slower limit & offset in java}
    int size= rm.keyIndex.size();

    Vector ret=new Vector(100, 100);
    Object []dt;
    try{ 
      while(rs.next())
	{
	  rm.fillResult(rs, dt=new Object[size]);
	  ret.addElement(new org.makumba.util.ArrayMap(rm.keyIndex, dt));
	}
      rs.close();
    }catch(SQLException e){ throw new org.makumba.DBError(e, rm.getDataDefinition().getName()); }
    return ret;
  }
   


}
