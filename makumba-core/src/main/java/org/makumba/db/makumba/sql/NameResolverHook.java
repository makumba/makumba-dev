package org.makumba.db.makumba.sql;

import org.makumba.DataDefinition;
import org.makumba.commons.NameResolver;
import org.makumba.db.makumba.Database;

/**
 * This is a decorator for {@link NameResolver} which makes sure that a table gets opened (i.e. configured and
 * eventually created) on access.
 * 
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @author Cristian Bogdan
 * @version $Id: NameResolverHook.java,v 1.1 13.11.2007 12:23:58 Manuel Exp $
 */
public class NameResolverHook extends NameResolver {
    private final Database db1;

    private NameResolver delegate;

    NameResolverHook(Database db1) {
        this.db1 = db1;
        this.delegate = new NameResolver(db1.getConfigurationProperties());
    }

    @Override
    public String resolveTypeName(String typeName) {
        db1.getTable(typeName);
        return delegate.resolveTypeName(typeName);
    }

    @Override
    public String resolveFieldName(DataDefinition dd, String field) {
        return delegate.resolveFieldName(dd, field);
    }
}