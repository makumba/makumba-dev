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

package org.makumba.db.makumba;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Dictionary;

import org.makumba.Attributes;
import org.makumba.Pointer;
import org.makumba.providers.TransactionProvider;

/**
 * A wrapper for db connections, used to provide a temporary that holds a reference to a permanent DBConnection
 * 
 * @author Cristian Bogdan
 * @author Stefan Baebler
 * @author Manuel Bernhardt <manuel@makumba.org>
 */
public class DBConnectionWrapper extends DBConnection {
    DBConnection wrapped;

    // uncomment this if you want to know where the unclosed connections are created
    // maybe this can become a devel feature?
    Throwable t;

    Date created;

    public DBConnection getWrapped() {
        return wrapped;
    }

    DBConnectionWrapper(TransactionProvider tp) {
        super(tp);
    }

    DBConnectionWrapper(DBConnection wrapped, String dataSource, TransactionProvider tp) {
        this(tp);
        t = new Throwable();
        created = new Date();
        this.wrapped = wrapped;
        this.dataSource = dataSource;
    }

    @Override
    public String getName() {
        return getWrapped().getName();
    }

    @Override
    public Database getHostDatabase() {
        return getWrapped().getHostDatabase();
    }

    @Override
    public java.util.Dictionary<String, Object> read(Pointer ptr, Object fields) {
        return getWrapped().read(ptr, fields);
    }

    @Override
    public java.util.Vector<Dictionary<String, Object>> executeQuery(String OQL, Object parameterValues, int offset,
            int limit) {
        return getWrapped().executeQuery(OQL, parameterValues, offset, limit);
    }

    @Override
    public Pointer insert(String type, java.util.Dictionary<String, Object> data) {
        return getWrapped().insert(type, data);
    }

    @Override
    public Pointer insert(Pointer host, String subsetField, java.util.Dictionary<String, Object> data) {
        return getWrapped().insert(host, subsetField, data);
    }

    @Override
    public int insertFromQuery(String type, String OQL, Object parameterValues) {
        return getWrapped().insertFromQuery(type, OQL, parameterValues);
    }

    @Override
    public int update(Pointer ptr, java.util.Dictionary<String, Object> fieldsToChange) {
        return getWrapped().update(ptr, fieldsToChange);
    }

    @Override
    public int update(String from, String set, String where, Object parameters) {
        return getWrapped().update(from, set, where, parameters);
    }

    @Override
    public void delete(Pointer ptr) {
        getWrapped().delete(ptr);
    }

    @Override
    public int delete(String from, String where, Object parameters) {
        return getWrapped().delete(from, where, parameters);
    }

    @Override
    public void commit() {
        getWrapped().unlockAll();
        getWrapped().commit();
    }

    @Override
    public void rollback() {
        getWrapped().unlockAll();
        getWrapped().rollback();
    }

    @Override
    public void lock(String symbol) {
        getWrapped().lock(symbol);
    }

    @Override
    public void unlock(String symbol) {
        getWrapped().lock(symbol);
    }

    @Override
    public synchronized void close() {
        try {
            getWrapped().setContext(null);
            commit();
            // we close the connection here - in fact since it's a connection wrapped by the c3p0 pool it will not be
            // closed but returned
            // to the pool
            getWrapped().close();
        } finally {
            wrapped = ClosedDBConnection.getInstance();
        }
    }

    @Override
    protected synchronized void finalize() {
        if (wrapped != ClosedDBConnection.getInstance()) {
            java.util.logging.Logger.getLogger("org.makumba.db").severe(
                "Makumba connection " + getName() + " not closed\n" + getCreationStack());
            close();
        }
    }

    @Override
    public void setContext(Attributes a) {
        getWrapped().setContext(a);
    }

    public String getCreationStack() {
        StringWriter sbw = new StringWriter();
        PrintWriter output = new PrintWriter(sbw);
        output.print("connection created on" + created + " with stacktrace: ");
        t.printStackTrace(output);
        return sbw.toString();
    }

    @Override
    public String toString() {

        return getWrapped() + " " + getCreationStack();
    }

}

class ClosedDBConnection extends DBConnectionWrapper {
    private static final class SingletonHolder {
        static DBConnection singleton = new ClosedDBConnection(null);
    }

    private ClosedDBConnection(TransactionProvider tp) {
        super(tp);
    }

    public static DBConnection getInstance() {
        return SingletonHolder.singleton;
    }

    @Override
    public DBConnection getWrapped() {
        throw new IllegalStateException("connection already closed");
    }
}
