package metadata.abstr;
import java.util.*;
import java.io.File;

public final class RecordInfo 
{
  java.net.URL origin;
  String name;
  Vector templateArgumentNames;
  Properties templateValues;
  RecordInfo fromTemplate;
  
  Vector fieldOrder;
  String title;
  
  // nr of relations, 0= none, 1= 1:n, 2= m:n
  int relations= 0;
  
  Hashtable fields= new Hashtable();

  RecordInfo()
  {
    fieldOrder= new Vector();
    FieldInfo fi;
    
    fieldOrder.addElement("*dbsv:uid");
    fields.put("*dbsv:uid", fi= new FieldInfo("*dbsv:uid"));
    fi.type= "ptrIndex";
    fi.description= "unique index";
    fi.fixed= true;
    fi.notNull=true;
    
    fieldOrder.addElement("*creation");
    fields.put("*creation", fi= new FieldInfo("*creation"));
    fi.type= "dateCreate";
    fi.description= "creation date";
    fi.fixed= true;
    fi.notNull=true;
    
    fieldOrder.addElement("*modified");
    fields.put("*modified", fi= new FieldInfo("*modified"));
    fi.type= "dateModify";
    fi.notNull=true;
    fi.description= "last modification date";
    
  }
  
  RecordInfo(java.net.URL origin, String path) 
  {
    this();
    name= path;
    this.origin= origin;
  }
  
  
  // for subtables
  String subfield;
  String ptrSubfield="";
  String subfieldPtr="";
  RecordInfo papa;

  // template buidling
  RecordInfo(RecordInfo ri)
  {
    fromTemplate=ri;
    name= ri.name;
    title= ri.title;
    origin= ri.origin;
    templateArgumentNames= ri.templateArgumentNames;
    fieldOrder= ri.fieldOrder;
    templateValues= new Properties();

    subfield= ri.subfield;
    ptrSubfield= ri.ptrSubfield;
    subfieldPtr= ri.subfieldPtr;
    
    papa= ri.papa;

  }
  
  void matchArguments(Vector actual) throws TemplateArgumentsException
  {
    try{
    for(int i= actual.size()-1; i>=0; i--)
      templateValues.put(
         templateArgumentNames.elementAt(i),
         actual.elementAt(i));
    } catch (ArrayIndexOutOfBoundsException e)
      { throw new TemplateArgumentsException(this, actual); }
    
    for(int i= fieldOrder.size()-1; i>=0; i--)
    {
      String s= (String)fieldOrder.elementAt(i);
      fields.put(s, ((FieldInfo)fromTemplate.fields.get(s)).matchArguments(templateValues));
    }
   
   if(papa!=null)
     papa=papa.getInstance(actual);
  }
  
  Hashtable instances;
  
  synchronized RecordInfo getInstance(Vector actual) 
  throws TemplateArgumentsException
  {
    String s= actual.toString();
    RecordInfo ri= (RecordInfo)instances.get(s);
    if(ri== null)
    {
        instances.put(s, ri= new RecordInfo(this));
        ri.matchArguments(actual);
    }
    return ri;
  }
  
  RecordInfo(RecordInfo ri, String subfield)
  {
    this();
    name= ri.name;
    origin= ri.origin;
    this.subfield= subfield;
    this.papa= ri;
    this.templateArgumentNames= ri.templateArgumentNames;
    ptrSubfield= papa.ptrSubfield+"->"+subfield;
    subfieldPtr= papa.subfieldPtr+subfield+"->";
    if(isTemplate())
        instances= new Hashtable();
  }

  public boolean isSubtable(){ return papa!= null; }
  public RecordInfo getMainTable(){ return papa; }
  public String getFieldName(){ return subfield; }

  static Hashtable infos= new Hashtable();
  static Hashtable templateInfos= new Hashtable();
  
  /** returns the record info with the given absolute name and arguments 
   *  @throws MetadataNotFoundException if the name is not a valid record info name
   *  @throws ParseException if the syntax is wrong or a referred resource can't be found
   */
  public static RecordInfo getRecordInfo(String path, String args)  
     throws metadata.ConfigException
  {
    if(args.trim().length()==0)
      return getRecordInfo(path);
    return getTemplate(checkPath(path)).getInstance(RecordParser.commaString2Vector(args));
  }
  
  /** returns the record info with the given absolute name
   *  @throws MetadataNotFoundException if the name is not a valid record info name
   *  @throws ParseException if the syntax is wrong or a referred resource can't be found
   */
  public static RecordInfo getRecordInfo(String path)  
     throws metadata.ConfigException
  {
    RecordInfo ri= getTemplate(checkPath(path));
    if(ri.isTemplate())
      throw new TemplateArgumentsException(ri);
    return ri;
  }
  

  static String checkPath(String path) throws ParseException
  {
    path= path.trim();
    if(path.charAt(0)!= '/')
        path="/"+path;
    return path;
  }

  synchronized static RecordInfo getTemplate(String path)
      throws ParseException
  {
    java.net.URL u= ClassLoader.getSystemResource(path.substring(1));
    if(u== null)
        { throw new MetadataNotFoundException(path); }
        
    RecordInfo ri= (RecordInfo)infos.get(u);
    
    if(ri==null)
      {
        RecordParser rp= new RecordParser();
        rp.parse(ri= new RecordInfo(u, path));
        infos.put(u, ri);
        rp.parse();
      }
    else
        if(path.indexOf("./")==-1)
            ri.name= path;
    return ri;
  }
  
 
  public Vector getAllFieldNames(){ return (Vector)fieldOrder.clone(); }

  public Vector getFieldNames()
  { 
    Vector v= getAllFieldNames();
    v.removeElementAt(2);
    v.removeElementAt(1);
    v.removeElementAt(0);
    return v;
  }
  
  public FieldInfo getField(String nm){ return (FieldInfo)fields.get(nm); }
  
  public String getName(){ return name; }

  public String getTitleField(){ return title; }
  
  public String canonicalName()
    { return name+RecordParser.listArguments(templateSpecifier())+ptrSubfield; }
    
  Vector templateSpecifier()
  {
     if(templateValues==null)
        return templateArgumentNames;
     Vector v= new Vector();
     for(Enumeration e= templateArgumentNames.elements(); e.hasMoreElements(); )
         v.addElement("\""+templateValues.get(e.nextElement())+"\"");
     return v;
  }

  public String fieldPrefix(){ return subfieldPtr; }
  
  public boolean isTemplate()
    { return templateArgumentNames!= null && templateValues== null; }
  
  public boolean isTemplate(Vector v)
    { return isTemplate() && templateArgumentNames.size()== v.size(); }
  
  public Enumeration getTemplateArgumentNames()
    { return templateArgumentNames.elements(); }
   
  public String getTemplateArgument(String name) 
    { return templateValues.getProperty(name); }
    

}