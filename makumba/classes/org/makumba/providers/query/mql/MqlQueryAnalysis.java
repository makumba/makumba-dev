package org.makumba.providers.query.mql;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.makumba.DataDefinition;
import org.makumba.OQLParseError;
import org.makumba.commons.NameResolver;
import org.makumba.commons.RegExpUtils;
import org.makumba.commons.RuntimeWrappedException;
import org.makumba.commons.NameResolver.TextList;
import org.makumba.providers.DataDefinitionProvider;
import org.makumba.providers.QueryAnalysis;
import org.makumba.providers.query.oql.QueryAST;

import antlr.RecognitionException;
import antlr.collections.AST;

/**
 * The hearth of the MQL query analysis and compilation. The query is first pre-processed and then the initial parsing takes place to produce the initial mql tree.
 * After this, the tree is transformed by the MqlSqlWalker for analysis and finally transformed again for sql query generation.
 * 
 * @author Cristian Bogdan
 * @version $Id: MqlQueryAnalysis.java,v 1.1 Apr 29, 2009 8:54:20 PM manu Exp $
 */
public class MqlQueryAnalysis implements QueryAnalysis {

    private static final String MAKUMBA_PARAM = "param";

    private String query;

    private DataDefinition insertIn;
    
    private List<String> parameterOrder = new ArrayList<String>();

    private DataDefinition proj;

    private boolean noFrom = false;

    private Hashtable<String, DataDefinition> labels;

    private Hashtable<String, String> aliases;

    private DataDefinition paramInfo;

    private TextList text;

    static String formatQueryAndInsert(String query, String insertIn) {
        if(insertIn!=null && insertIn.length()>0)
            return  "###"+insertIn+"###"+query;
        else return query;
    }
    
    public MqlQueryAnalysis(String queryAndInsert, boolean optimizeJoins, boolean autoLeftJoin){
        Date d = new Date();

        if(queryAndInsert.startsWith("###")){
            insertIn= DataDefinitionProvider.getInstance().getDataDefinition(queryAndInsert.substring(3, queryAndInsert.indexOf('#', 3)));
            query= queryAndInsert.substring(queryAndInsert.indexOf('#', 3)+3);
        }else
            query=queryAndInsert;

        query = preProcess(query);

        if (query.toLowerCase().indexOf("from") == -1) {
            noFrom = true;
            query += " FROM org.makumba.db.makumba.Catalog c";
        }

        HqlParser parser=null;
        try{
            parser = HqlParser.getInstance(query);
            parser.statement();
        }catch(Throwable t){
            doThrow(t, parser.getAST());
        }
        doThrow(parser.error, parser.getAST());

        transformOQLParameters(parser.getAST());
        transformOQL(parser.getAST());

        MqlSqlWalker mqlAnalyzer = new MqlSqlWalker(query, insertIn, optimizeJoins, autoLeftJoin);
        try {
            mqlAnalyzer.statement(parser.getAST());
        } catch (Throwable e) {
            doThrow(e, parser.getAST());
        }
        doThrow(mqlAnalyzer.error, parser.getAST());

        labels = mqlAnalyzer.rootContext.labels;
        aliases = mqlAnalyzer.rootContext.aliases;
        paramInfo = DataDefinitionProvider.getInstance().getVirtualDataDefinition("Parameters for " + query);
        proj = DataDefinitionProvider.getInstance().getVirtualDataDefinition("Projections for " + query);
        mqlAnalyzer.setProjectionTypes(proj);        
        
        for(int i=0; i<parameterOrder.size(); i++)
            paramInfo.addField(DataDefinitionProvider.getInstance().makeFieldWithName("param"+i, 
                   mqlAnalyzer.paramInfo.getFieldDefinition(parameterOrder.get(i))));
            
        // if(mqlAnalyzer.hasSubqueries)
        // System.out.println(mqlDebug);
        MqlSqlGenerator mg = new MqlSqlGenerator();
        try {
            mg.statement(mqlAnalyzer.getAST());
        } catch (Throwable e) {
            doThrow(e, mqlAnalyzer.getAST());
        }
        doThrow(mg.error, mqlAnalyzer.getAST());

        text = mg.text;
        
        long diff = new java.util.Date().getTime() - d.getTime();
        java.util.logging.Logger.getLogger("org.makumba.db.query.compilation").fine("MQL to SQL: " + diff + " ms: " + query);

    }

    private void doThrow(Throwable t, AST debugTree) {
        if (t == null)
            return;
        if (t instanceof RuntimeException) {
            t.printStackTrace();
            throw (RuntimeException) t;
        }
        String errorLocation = "";
        String errorLocationNumber="";
        if (t instanceof RecognitionException) {
            RecognitionException re = (RecognitionException) t;
            if (re.getColumn() > 0) {
                errorLocationNumber= " column "+re.getColumn()+" of ";
                StringBuffer sb = new StringBuffer();
                sb.append("\r\n");

                for (int i = 0; i < re.getColumn(); i++) {
                    sb.append(' ');
                }
                sb.append('^');
                errorLocation = sb.toString();
            }
        }
        throw new OQLParseError("\r\nin "+errorLocationNumber+" query:\r\n" + query + errorLocation+errorLocation+errorLocation, t);
    }

    public String writeInSQLQuery(NameResolver nr) {
        // TODO: we can cache these SQL results by the key of the NameResolver
        // still we should first check if this is needed, maybe the generated SQL (or processing of it)
        // is cached already somewhere else
        String sql = text.toString(nr);
        if (noFrom)
            return sql.substring(0, sql.toLowerCase().indexOf("from")).trim();
        return sql;
    }

    public String getQuery() {
        return query;
    }

    public DataDefinition getLabelType(String labelName) {
        String s1 = (String) aliases.get(labelName);
        if (s1 != null)
            labelName = s1;
        return (DataDefinition) labels.get(labelName);
    }

    public Map<String, DataDefinition> getLabelTypes() {
        return labels;
    }

    public DataDefinition getParameterTypes() {
        return paramInfo;
    }

    public DataDefinition getProjectionType() {
        return proj;
    }
    
    // FIXME this is ugly, there is for sure a way to get the FROM from the tree
    private String getFrom() {

        String[] splitAtFrom = query.split("\\s[f|F][r|R][o|O][m|M]\\s");
        String[] splitAtWhere = splitAtFrom[1].split("\\s[w|W][h|H][e|E][r|R][e|E]\\s");

        return splitAtWhere[0];
    }

    
    public String getFieldOfExpr(String expr) {
        if (expr.indexOf(".") > -1)
            return expr.substring(expr.lastIndexOf(".") + 1);
        else
            return expr;
    }

    public DataDefinition getTypeOfExprField(String expr) {

        if (expr.indexOf(".") == -1) {
            return getLabelType(expr);
        } else {
            DataDefinition result;
            int lastDot = expr.lastIndexOf(".");
            String beforeLastDot = expr.substring(0, lastDot);
            if (beforeLastDot.indexOf(".") == -1) {
                result = getLabelType(beforeLastDot);
            } else {
                // compute dummy query for determining pointed type

                // FIXME will this work if the FROM contains subqueries?
                String dummyQuery = "SELECT " + beforeLastDot + " AS projection FROM " + getFrom();
                try {
                    result = QueryAST.parseQueryFundamental(dummyQuery).getProjectionType().getFieldDefinition(
                        "projection").getPointedType();
                } catch (RecognitionException e) {
                    throw new RuntimeWrappedException(e);
                }
            }
            return result;

        }
    }
    
    public int parameterAt(int index) {
        String s = parameterOrder.get(index);
        if (!s.startsWith(MAKUMBA_PARAM))
            throw new IllegalArgumentException("parameter at " + index + " is not a $n parameter");
        s = s.substring(MAKUMBA_PARAM.length());
        return Integer.parseInt(s) + 1;
    }

    public int parameterNumber() {
        return parameterOrder.size();
    }
    
    /** Transform OQL $x into :parameters, and record the parameter order */
    void transformOQLParameters(AST a){
        if (a == null)
            return;
        if (a.getType() == HqlTokenTypes.IDENT && a.getText().startsWith("$")) {
            // replacement of $n with (: makumbaParam n)
            a.setType(HqlTokenTypes.COLON);
            AST para = new Node();
            para.setType(HqlTokenTypes.IDENT);
            try {
                para.setText(MAKUMBA_PARAM + (Integer.parseInt(a.getText().substring(1)) - 1));
            } catch (NumberFormatException e) {
                // we probably are in some query analysis, so we ignore
                para.setText(a.getText().substring(1));
            }
            parameterOrder.add(para.getText());
            a.setFirstChild(para);
            a.setText(":");
        }else if (a.getType() == HqlTokenTypes.COLON && a.getFirstChild() != null
                && a.getFirstChild().getType() == HqlTokenTypes.IDENT)
            // we also accept : params though we might not know what to do with them later
            parameterOrder.add(a.getFirstChild().getText());
        
        if(a.getType()==HqlTokenTypes.SELECT_FROM){
           // first the SELECT part
           transformOQLParameters(a.getFirstChild().getNextSibling());
           // then the FROM part
           transformOQLParameters(a.getFirstChild());           
           // then the rest
           transformOQLParameters(a.getNextSibling());            

        }else{
            transformOQLParameters(a.getFirstChild());
            // we make sure we don't do "SELECT" again
            if(a.getType()!=HqlTokenTypes.FROM)
                transformOQLParameters(a.getNextSibling());
       }
    }

    /** Transform the tree so that various OQL notations are still accepted
     * replacement of = or <> NIL with IS (NOT) NULL
     * OQL puts a 0.0+ in front of any AVG() expression 
     * 
     * This method also does various subquery transformations which are not OQL-specific, to support:
     * size(), elements(), firstElement()... 
     * */
    void transformOQL(AST a) {
        if (a == null)
            return;
        if (a.getType() == HqlTokenTypes.EQ || a.getType() == HqlTokenTypes.NE) {
            // replacement of = or <> NIL with IS (NOT) NULL
            if (MqlQueryAnalysis.isNil(a.getFirstChild())) {
                MqlQueryAnalysis.setNullTest(a);
                a.setFirstChild(a.getFirstChild().getNextSibling());
            } else if (MqlQueryAnalysis.isNil(a.getFirstChild().getNextSibling())) {
                MqlQueryAnalysis.setNullTest(a);
                a.getFirstChild().setNextSibling(null);
            }

        } else if (a.getType() == HqlTokenTypes.AGGREGATE && a.getText().toLowerCase().equals("avg")) {
            // OQL puts a 0.0+ in front of any AVG() expression probably to force the result to be floating point
            AST plus = new Node();
            plus.setType(HqlTokenTypes.PLUS);
            plus.setText("+");
            AST zero = new Node();
            zero.setType(HqlTokenTypes.NUM_DOUBLE);
            zero.setText("0.0");
            plus.setFirstChild(zero);
            zero.setNextSibling(a.getFirstChild());
            a.setFirstChild(plus);
        } 
        else if (a.getType() == HqlTokenTypes.ELEMENTS) {
            makeSubquery(a, a.getFirstChild());
        } else if (a.getType() == HqlTokenTypes.METHOD_CALL && a.getFirstChild().getText().toLowerCase().equals("size")) {
            makeSelect(a, HqlTokenTypes.COUNT, "count");
        } else if (a.getType() == HqlTokenTypes.METHOD_CALL
                && a.getFirstChild().getText().toLowerCase().endsWith("element")) {
            makeSelect(a, HqlTokenTypes.AGGREGATE, a.getFirstChild().getText().substring(0, 3));
        }

        transformOQL(a.getFirstChild());
        transformOQL(a.getNextSibling());
    }

    private void makeSelect(AST a, int type, String text) {
        makeSubquery(a, a.getFirstChild().getNextSibling().getFirstChild());
        AST from = a.getFirstChild().getFirstChild();
        from.setNextSibling(makeNode(HqlTokenTypes.SELECT, "select"));
        from.getNextSibling().setFirstChild(makeNode(type, text));
        from.getNextSibling().getFirstChild().setFirstChild(makeNode(HqlTokenTypes.IDENT, "makElementsLabel"));
    }

    private void makeSubquery(AST a, AST type) {
        a.setType(HqlTokenTypes.QUERY);
        a.setFirstChild(makeNode(HqlTokenTypes.SELECT_FROM, "select"));
        a.getFirstChild().setFirstChild(makeNode(HqlTokenTypes.FROM, "from"));
        a.getFirstChild().getFirstChild().setFirstChild(makeNode(HqlTokenTypes.RANGE, "range"));
        a.getFirstChild().getFirstChild().getFirstChild().setFirstChild(type);
        type.setNextSibling(makeNode(HqlTokenTypes.ALIAS, "makElementsLabel"));
    }

    private AST makeNode(int type, String string) {
        Node node = new Node();
        node.setType(type);
        node.setText(string);
        return node;
    }

    public static String showAst(AST ast) {
        return ast.toStringTree();
    }

    static boolean isNil(AST a) {
        return a.getType() == HqlTokenTypes.IDENT && a.getText().toUpperCase().equals("NIL");
    }

    static void setNullTest(AST a) {
        if (a.getType() == HqlTokenTypes.EQ) {
            a.setType(HqlTokenTypes.IS_NULL);
            a.setText("is null");
        } else {
            a.setType(HqlTokenTypes.IS_NOT_NULL);
            a.setText("is not null");
        }
    }

    public static final String regExpInSET = "in" + RegExpUtils.minOneWhitespace + "set" + RegExpUtils.whitespace
            + "\\(";

    public static final Pattern patternInSet = Pattern.compile(regExpInSET);

    public static String preProcess(String query) {
        // replace -> (subset separators) with __
        query = query.replaceAll("->", "__");

        // replace IN SET with IN.
        Matcher m = patternInSet.matcher(query.toLowerCase()); // find all occurrences of lower-case "in set"
        while (m.find()) {
            int start = m.start();
            int beginSet = m.group().indexOf("set"); // find location of "set" keyword
            // System.out.println(query);
            // composing query by concatenating the part before "set", 3 space and the part after "set"
            query = query.substring(0, start + beginSet) + "   " + query.substring(start + beginSet + 3);
            // System.out.println(query);
            // System.out.println();
        }
        query = query.replaceAll("IN SET", "IN    ");
        return query;
    }
}
