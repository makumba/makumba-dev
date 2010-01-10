package org.makumba.abstr;
import java.util.*;
import java.io.File;
import org.makumba.util.*;
import org.makumba.*;

/** This is the internal representation of the org.makumba. One can make
 * RecordHandlers based on an instance of this class and do useful things
 * with it (generate sql tables, html code, etc)
 */
public class RecordInfo implements java.io.Serializable, DataDefinition
{
  java.net.URL origin;
  String name;
  Properties templateValues;
  //  Vector templateArgumentNames;

  Vector fieldOrder= new Vector();
  String title;
  String indexName;
  static final String createName= "TS_create";
  static final String modifyName= "TS_modify";

  // for set and setComplex subtables
  String mainPtr;

  // for set tables, also used for setintEnum and setcharEnum to store the name of the int or char field
  String foreignPtr;

  // nr of relations, 0= none, 1= 1:n, 2= m:n
  int relations= 0;

  Hashtable fields= new Hashtable();
  Hashtable fieldIndexes=null;

  // for subtables
  String subfield;
  String ptrSubfield="";
  String subfieldPtr="";
  RecordInfo papa;

  void addStandardFields(String name)
  {
    FieldInfo fi;

    indexName=name;
    
    fi= new FieldInfo(this, indexName);
    fi.type= "ptrIndex";
    fi.description= "Unique index";
    fi.fixed= true;
    fi.notNull=true;
    addField1(fi);

    fi= new FieldInfo(this, modifyName);
    fi.type= "dateModify";
    fi.notNull=true;
    fi.description= "Last modification date";
    addField1(fi);

    fi=new FieldInfo(this, createName);
    fi.type= "dateCreate";
    fi.description= "Creation date";
    fi.fixed= true;
    fi.notNull=true;
    addField1(fi);
  }

  public Dictionary getKeyIndex() 
  { 
    if(fieldIndexes==null)
      {
	fieldIndexes=new Hashtable();
	int maxIndex=0;
	for(Enumeration e= fieldOrder.elements(); e.hasMoreElements(); )
	  {
	    FieldInfo fi= (FieldInfo)fields.get(e.nextElement());
	    if(!fi.type.startsWith("set"))
	      fieldIndexes.put(fi.name, new Integer(maxIndex++));
	  }
      }
    return fieldIndexes; 
  }

  public java.net.URL getOrigin(){ return origin; }

  public boolean isTemporary()
  {
    return origin==null;
  }

  /** make a temporary recordInfo that is only used for query results*/
  public RecordInfo()
  {
    name= "temp"+hashCode();
    origin=null;
  }
  
  protected void addField1(FieldInfo fi)
  {
    fieldOrder.addElement(fi.name);
    fields.put(fi.name, fi);
    fi.ri=this;
  }

  /** only meant for building of temporary types */
  public void addField(FieldInfo fi)
  {
    if(!isTemporary())
      throw new RuntimeException("can't add field to non-temporary type");
    addField1(fi);
    //  the field cannot be of set type...
    if(fieldIndexes==null)
      fieldIndexes=new Hashtable();
    fieldIndexes.put(fi.name, new Integer(fieldIndexes.size()));
  }

  public int getDataSize(){ return fieldIndexes.size(); }

  RecordInfo(java.net.URL origin, String path)
  {
    name= path;
    this.origin= origin;
    //    templateArgumentNames= new Vector(0);
  }

  RecordInfo(RecordInfo ri, String subfield)
  {
    //    initStandardFields(subfield);
    name= ri.name;
    origin= ri.origin;
    this.subfield= subfield;
    this.papa= ri;
    //    this.templateArgumentNames= ri.templateArgumentNames;
    ptrSubfield= papa.ptrSubfield+"->"+subfield;
    subfieldPtr= papa.subfieldPtr+subfield+"->";
  }

  /** Tells if this class is a set or one-to-one pointer subtable */
  public boolean isSubtable(){ return papa!= null; }

  /** If this is a subtable, what is the main table */
  public RecordInfo getMainTable(){ return papa; }

  /** If this is a subtable, what field in the main table it represents */
  public String getFieldName(){ return subfield; }

  static int infos= NamedResources.makeStaticCache
  ("Data definitions parsed",
   new NamedResourceFactory()
   {
     protected Object getHashObject(Object name)
       {
	 java.net.URL u= RecordParser.findDataDefinition((String)name, "mdd");
	 if(u== null)
	   { throw new DataDefinitionNotFoundError((String)name); }
	 return u;
       }

     protected Object makeResource(Object name, Object hashName) 
       {
	 String nm=(String)name;
	 if(nm.indexOf('/')!=-1)
	   nm=nm.replace('/', '.').substring(1);
	 return new RecordInfo((java.net.URL)hashName, nm);
      }
     protected void configureResource(Object name, Object hashName, Object resource)
       {
	 new RecordParser().parse((RecordInfo)resource);
       }
   }
    );

/** returns the record info with the given absolute name
   *  @throws org.makumba.DataDefinitionNotFoundError if the name is not a valid record info name
   *  @throws org.makumba.DataDefinitionParseError if the syntax is wrong or a referred resource can't be found
   */
  public static RecordInfo getRecordInfo(String name)	
  {
    int n= name.indexOf("->");
    if (n==-1)
      {
	try{
	  return getSimpleRecordInfo(name);
	}catch(DataDefinitionNotFoundError e)
	  {
	    n= name.lastIndexOf(".");
	    if(n==-1)
	      throw e;
	    try{
	      return getRecordInfo(name.substring(0, n)+"->"+name.substring(n+1));
	      }
	    catch(DataDefinitionParseError f){ throw e; }
	  }
      }
    
    RecordInfo ri= getRecordInfo(name.substring(0, n));
    while(true)
      {
	name= name.substring(n+2);
	n= name.indexOf("->");
	if(n==-1)
	  break;
	ri= ri.getField(name.substring(0, n)).getSubtable();
      }
    ri=ri.getField(name).getSubtable();
    return ri;
  }
  
  
  public static synchronized RecordInfo getSimpleRecordInfo(String path)
  {
    // this is to avoid a stupid error if path is "..."
    boolean dot=false;
    for(int i=0; i<path.length(); i++)
      if(path.charAt(i)=='.')
	{
	  if(dot)
	    throw new DataDefinitionParseError("two consecutive dots not allowed in type name");
	  dot=true;
	}
      else dot=false;
	
    if(path.indexOf('/')!=-1){
      path=path.replace('/', '.');
      if(path.charAt(0)=='.')
	path=path.substring(1);
    }
    
    RecordInfo ri=null;
    try{
      ri= (RecordInfo)NamedResources.getStaticCache(infos).getResource(path);
    }catch(RuntimeWrappedException e)
      {
	if(e.getReason() instanceof DataDefinitionParseError)
	  throw (DataDefinitionParseError)e.getReason();
	if(e.getReason() instanceof MakumbaError)
	  throw (MakumbaError)e.getReason();
	throw e;
      }
    if(path.indexOf("./")==-1)
      ri.name= path;
    else
      MakumbaSystem.getMakumbaLogger("debug.abstr").severe("shit happens: "+path);
    return ri;
  }
  
  /** returns all the field names */
  public Vector getFieldNames(){ return (Vector)fieldOrder.clone(); }
  
  /** returns the names of the declared fields, i.e. fields that are not default, like index or creation date, modification date */
  public Enumeration getDeclaredFields() 
  {
    Enumeration e= fieldOrder.elements();
    e.nextElement();
    e.nextElement();
    e.nextElement();
    return e;
  }

  /** returns the field info associated with a name */
  public FieldInfo getField(String nm){ return (FieldInfo)fields.get(nm); }

  /** returns the field info associated with a name */
  public FieldDefinition getFieldDefinition(String nm){ return getField(nm); }

  /** the field with the respective index, null if such a field doesn't exist */
  public FieldDefinition getFieldDefinition(int n)
  {
    if(n<0 ||n>= fieldOrder.size())
      return null;
    return getField((String)fieldOrder.elementAt(n));
  }

  /** returns the path-like abstract-level name of this record info */
  public String getName(){ return name+ptrSubfield; }

  /** returns the path-like abstract-level name of this record info */
  public String getBaseName(){ return name; }

  /** returns the title field */
  public String getTitleField(){ return title; }

  /** returns the canonical name, which is name(templatearg1, templatearg2, ...) */
  //  public String canonicalName()
  //  { return name+/*RecordParser.listArguments(templateSpecifier())+*/ptrSubfield; }

  /*  Vector templateSpecifier()
  {
     Vector v= new Vector();
     for(Enumeration e= templateArgumentNames.elements(); e.hasMoreElements(); )
       v.addElement("\""+templateValues.get(e.nextElement())+"\"");
     return v;
  }
  */

  /** if this is a subtable, the field prefix is maintable-> */
  public String fieldPrefix(){ return subfieldPtr; }

  /** the number of relational pointers. 1 for 1-to-n, 2 for m-to-n */
  public int getRelations(){return relations; }

  RecordInfo makeSubtable(String name){ return new RecordInfo(this, name); }

  /** which is the name of the index field, if any? */
  public String getIndexName(){ return indexName; }

  /** which is the name of the creation timestamp field, if any? */
  public String getCreateName(){ return createName; }

  /** which is the name of the modification timestamp field, if any? */
  public String getModifyName(){ return modifyName; }

  /** which is the name of the pointer to the main table, for set and internal set subtables */
  public String getMainTablePointerName(){ return mainPtr; }

  /** which is the name of the pointer to the foreign table, for set subtables */
  public String getForeignTablePointerName(){ return foreignPtr; }

  //-----------
  /** the title field indicated, or the default one */
  public String getTitleFieldName(){ return getTitleField(); }

  /** which is the name of the index field (primary key), if any? */
  public String getIndexPointerFieldName(){return getIndexName(); }

  /** which is the name of the creation timestamp field, if any? */
  public String getCreationDateFieldName(){ return getCreateName(); }

  /** which is the name of the modification timestamp field, if any? */
  public String getLastModificationDateFieldName(){ return getModifyName(); }

  /** If this type is the data pointed by a 1-1 pointer or subset, return the type of the main record, otherwise return null */
  public FieldDefinition getParentField(){
    RecordInfo ri= getMainTable();
    if(ri==null)
      return null;
    return ri.getField(getFieldName()); 
  }

  /** which is the name of the set member (Pointer, Character or Integer), for set subtables */
  public String getSetMemberFieldName(){ return foreignPtr ; }

  public String checkFieldName(Object o)
  {
    if(!(o instanceof String))
      throw new org.makumba.NoSuchFieldException(this, "Dictionaries passed to makumba DB operations should have String keys. Key <"+o+"> is of type "+o.getClass()+getName());
    if(getField((String)o)==null)
      throw new org.makumba.NoSuchFieldException(this, (String)o);
    return (String)o;
  }

  public void checkFieldNames(Dictionary d)
  {
    for(Enumeration e=d.keys(); e.hasMoreElements(); )
      checkFieldName(e.nextElement());
  }

}