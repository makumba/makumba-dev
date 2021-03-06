package org.makumba.test.util;

import org.makumba.Pointer;
import org.makumba.Text;
import org.makumba.Transaction;
import org.makumba.providers.TransactionProvider;

import java.util.*;

/**
 * Class that creates and deletes data in the database, used by the test set-up.
 * 
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @version $Id: MakumbaTestData.java,v 1.1 Jun 9, 2009 10:46:01 PM manu Exp $
 */
public class MakumbaTestData {

    public static final String namePersonIndivName_Bart = "bart";

    public static final String namePersonIndivSurname_Bart = "van Vandervanden";

    public static final String namePersonIndivName_John = "john";

    public static final String namePersonIndivSurname_John = "von Neumann";

    private static final String[][] languageData = { { "English", "en", "Germanic" }, { "French", "fr", "Romance" },
            { "German", "de", "Germanic" }, { "Italian", "it", "Romance" }, { "Spanish", "sp", "Romance" } };

    public static final Date birthdateJohn = new GregorianCalendar(1977, 2, 5, 0, 0, 0).getTime();

    public static final Date birthdateBart = new GregorianCalendar(1982, 5, 7, 0, 0, 0).getTime();

    public static final Date testDate = new GregorianCalendar(2008, 2, 9, 0, 0, 0).getTime();;

    public static final Integer uniqInt = new Integer(255);

    public static final String uniqChar = new String("testing \" character field");

    public final static String namePersonIndivName_AddToNew = "addToNewPerson";

    public final static String namePersonIndivName_AddToNewValidation = "addToNewPersonValidation";

    public static final String namePersonIndivName_FirstBrother = "firstBrother";

    public static final String namePersonIndivName_SecondBrother = "secondBrother";

    public static final String namePersonIndivName_StepBrother = "stepBrother";

    /** All names of individuals to be deleted. bart is referenced by john, so we delete him afterwards. */
    private static final String[] namesPersonIndivName = { namePersonIndivName_John, namePersonIndivName_Bart,
            namePersonIndivName_AddToNew, namePersonIndivName_SecondBrother, namePersonIndivName_FirstBrother,
            namePersonIndivName_StepBrother };

    public final ArrayList<Pointer> languages = new ArrayList<Pointer>();

    Pointer person;

    public Pointer brother;

    public void insertPerson(Transaction t) {
        Hashtable<String, Object> p = new Hashtable<String, Object>();

        p.put("indiv.name", namePersonIndivName_Bart);
        p.put("indiv.surname", namePersonIndivSurname_Bart);
        p.put("birthdate", birthdateBart);
        p.put("gender", new Integer(1));
        brother = t.insert("test.Person", p);
        t.commit();

        p.clear();
        p.put("indiv.name", namePersonIndivName_John);
        p.put("indiv.surname", namePersonIndivSurname_John);

        p.put("birthdate", birthdateJohn);

        p.put("uniqDate", birthdateJohn);
        p.put("gender", new Integer(1));
        p.put("uniqChar", uniqChar);

        p.put("age", 22);

        p.put("weight", new Double(85.7d));

        p.put("comment", new Text("This is a text field. It's a comment about this person."));

        p.put("uniqInt", uniqInt);

        Vector<Integer> intSet = new Vector<Integer>();
        intSet.addElement(new Integer(1));
        intSet.addElement(new Integer(0));
        p.put("intSet", intSet);

        p.put("brother", brother);
        p.put("uniqPtr", languages.get(0));
        person = t.insert("test.Person", p);

        p.clear();
        p.put("description", "");
        p.put("usagestart", birthdateJohn);
        p.put("email", "email1");
        t.insert(person, "address", p);

        // let's fill in the languages - we add them twice to have a meaningful test for distinct
        p.clear();
        Vector<Pointer> v = new Vector<Pointer>();
        for (Pointer l : languages) {
            v.add(l);
        }
        for (Pointer l : languages) {
            v.add(l);
        }
        p.put("speaks", v);
        t.update(brother, p);

        // let's add some toys
        p.clear();
        p.put("name", "car");
        t.insert(brother, "toys", p);
        p.clear();
        p.put("name", "doll");
        t.insert(brother, "toys", p);

    }

    public void deletePersonsAndIndividuals(Transaction t) {
        for (String element : namesPersonIndivName) {
            String query = "SELECT p AS p, p.indiv as i FROM test.Person p WHERE p.indiv.name=" + "$1";
            Vector<Dictionary<String, Object>> v = t.executeQuery(query, element);
            if (v.size() > 0) {

                Vector<Pointer> emptyPointerVector = new Vector<Pointer>();

                // delete the languages
                Dictionary<String, Object> speaksDic = new Hashtable<String, Object>();
                speaksDic.put("speaks", emptyPointerVector);
                t.update((Pointer) v.firstElement().get("p"), speaksDic);

                // delete the address
                Dictionary<String, Object> addressDic = new Hashtable<String, Object>();
                speaksDic.put("address", emptyPointerVector);
                t.update((Pointer) v.firstElement().get("p"), addressDic);

                // delete the toys
                Dictionary<String, Object> toysDic = new Hashtable<String, Object>();
                speaksDic.put("toys", emptyPointerVector);
                t.update((Pointer) v.firstElement().get("p"), toysDic);

                t.delete((Pointer) v.firstElement().get("p"));
                t.delete((Pointer) v.firstElement().get("i"));
            }
        }
    }

    public void insertLanguages(Transaction t) {
        languages.clear();
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        for (String[] element : languageData) {
            p.put("name", element[0]);
            p.put("isoCode", element[1]);
            p.put("family", element[2]);
            languages.add(t.insert("test.Language", p));
        }
    }

    public void deleteLanguages(Transaction t) {
        String query = "SELECT " + (t.getTransactionProvider().getQueryLanguage().equals("oql") ? "l" : "l.id")
                + " AS l FROM test.Language l";
        Vector<Dictionary<String, Object>> v = t.executeQuery(query, new Object[] {});
        if (v.size() > 0) {
            for (Dictionary<String, Object> dictionary : v) {
                t.delete((Pointer) dictionary.get("l"));
            }
        }
    }

    public static void main(String[] args) {
        MakumbaTestData testData = new MakumbaTestData();
        Transaction t = TransactionProvider.getInstance().getConnectionTo(
            TransactionProvider.getInstance().getDefaultDataSourceName());
        if (args == null || args.length == 0 || args[0].equals("create")) {
            testData.insertLanguages(t);
            testData.insertPerson(t);
        } else if (args[0].equals("delete")) {
            testData.deletePersonsAndIndividuals(t);
            testData.deleteLanguages(t);
        }
        t.close();

        // also close data source completely
        t.getTransactionProvider().closeDataSource(t.getDataSource());
    }

}
