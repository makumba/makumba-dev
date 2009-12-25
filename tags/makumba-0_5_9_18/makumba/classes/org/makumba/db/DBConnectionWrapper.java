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

package org.makumba.db;
import org.makumba.*;

/** a wrapper for dbconnections, used to provide a temporary that holds a reference to a permanent DBConnection */
public class DBConnectionWrapper extends DBConnection
{
    DBConnection wrapped;
    public DBConnection getWrapped(){ return wrapped; }

    DBConnectionWrapper(){}
    DBConnectionWrapper(DBConnection wrapped){this.wrapped=wrapped; }

    public String getName(){ return getWrapped().getName(); }

    public Database getHostDatabase(){ return getWrapped().getHostDatabase(); }

    public java.util.Dictionary read(Pointer ptr, Object fields)
    { return getWrapped().read(ptr, fields); }
    
    public java.util.Vector executeQuery(String OQL, Object parameterValues)
    { return getWrapped().executeQuery(OQL, parameterValues); }
    
    public Pointer insert(String type, java.util.Dictionary data)
    { return getWrapped().insert(type, data); }
    
    public Pointer insert(Pointer host, String subsetField, java.util.Dictionary data)
    {return getWrapped().insert(host, subsetField, data); }

    public void update(Pointer ptr, java.util.Dictionary fieldsToChange)
    { getWrapped().update(ptr, fieldsToChange); }
    
    public int update(String from, String set, String where, Object parameters)
    { return getWrapped().update(from, set, where, parameters); }
    
    public void delete(Pointer ptr)
    { getWrapped().delete(ptr); }
    
    public int delete(String from, String where, Object parameters)
    { return getWrapped().delete(from, where, parameters); }
    
    public synchronized void close(){ 
	getHostDatabase().connections.put(getWrapped()); 
	wrapped=ClosedDBConnection.singleton;
    }	
    protected synchronized void finalize(){ 
	if(wrapped!=ClosedDBConnection.singleton)
	    close();
    }
}

class ClosedDBConnection extends DBConnectionWrapper
{
    static DBConnection singleton= new ClosedDBConnection();
    public DBConnection getWrapped() { throw new IllegalStateException("connection already closed"); }
}
