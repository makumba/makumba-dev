package org.makumba.abstr.translator;
import org.makumba.abstr.*;

/** printer handler family's field handler */
public class FieldPrinter extends FieldHandler
{
  /** all the information retrieved from the field info */
  public String valueToString(){ return getDataType(); }

  /** writes the field name, attributes, then calls valueToString(), then writes the description */
  public String toString()
    {
        return getName()+": "
            +(isFixed()?"fixed ":"")
            +(isNotNull()?"notnull ":"")
            +valueToString()
            +(hasDescription()?" ; "+getDescription():"")
            +"\n";
    }
}
