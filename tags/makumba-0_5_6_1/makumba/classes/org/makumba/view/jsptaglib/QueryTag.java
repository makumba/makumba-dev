package org.makumba.view.jsptaglib;
import org.makumba.*;
import org.makumba.view.*;
import org.makumba.util.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.io.*;
import java.util.*;

/** Display of OQL query results in nested loops. The Query FROM, WHERE, GROUPBY and ORDERBY are indicated in the head of the tag. The query projections are indicated by Value tags in the body of the tag. The tag can learn more projections during the various threads of execution. The sub-tags will generate subqueries of their enclosing tag queries (i.e. their WHERE, GROUPBY and ORDERBY are concatenated). Attributes of the environment can be passed as $attrName to the query */
public class QueryTag extends MakumbaBodyTag 
{
  protected Class getParentClass(){ return QueryTag.class; }


  MultipleKey getBasicKey()
  {
    MultipleKey mk= new MultipleKey(queryProps().length+2);
    for(int i=0; i<queryProps().length; i++)
      mk.setAt(queryProps()[i], i);
    mk.setAt(getKeyDifference(), queryProps().length);
    return mk;
  }

  public Object getRootRegistrationKey()
  {
    MultipleKey mk= getBasicKey();
    mk.setAt(new Integer(System.identityHashCode
			 (pageContext.getPage().getClass())), 
	     queryProps().length+1);
    //    System.out.println(mk);
    return mk;
  }

  public Object getRegistrationKey()
  {
    MultipleKey mk= getBasicKey();
    mk.setAt(getParentQueryStrategy().getKey(), queryProps().length+1);
    //    System.out.println(mk);
    return mk;
  }

  public Object getKeyDifference(){ return "VIEW"; }

  public RootTagStrategy makeRootStrategy(Object key)
  { return new RootQueryStrategy((QueryStrategy)makeNonRootStrategy(key)); }

  public TagStrategy makeNonRootStrategy(Object key)
  { return new QueryStrategy(); }

  /** make common data for all queries to be stored in the root data */
  public Object makeBuffer() { return new RootQueryBuffer(); }

  /** return true */
  protected boolean canBeRoot() {return true; }

  /** get the strategy of the root */
  protected RootQueryBuffer getRootQueryBuffer() 
  { 
    return (RootQueryBuffer)getRootData().buffer;
  }

  public String[] queryProps(){ return getRootQueryBuffer().bufferQueryProps; }

  public void setFrom(String s) { queryProps()[ComposedQuery.FROM]=s; }
  public void setWhere(String s){ queryProps()[ComposedQuery.WHERE]=s; }
  public void setOrderBy(String s){ queryProps()[ComposedQuery.ORDERBY]=s; }
  public void setGroupBy(String s){ queryProps()[ComposedQuery.GROUPBY]=s; }
  public void setSeparator(String s){ getRootQueryBuffer().bufferSeparator=s; }
  public void setCountVar(String s){ getRootQueryBuffer().bufferCountVar=s; }
  public void setMaxCountVar(String s){ getRootQueryBuffer().bufferMaxCountVar=s; }

}
