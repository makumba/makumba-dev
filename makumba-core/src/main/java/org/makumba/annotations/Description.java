package org.makumba.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for field descriptions
 * 
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @version $Id: Description.java,v 1.1 Jun 21, 2010 6:32:24 PM manu Exp $
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.FIELD })
public @interface Description {
    String value();
}
