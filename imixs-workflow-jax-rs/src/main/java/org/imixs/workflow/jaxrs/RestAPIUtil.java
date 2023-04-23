package org.imixs.workflow.jaxrs;

import java.util.List;
import java.util.Vector;

import com.ibm.icu.util.StringTokenizer;

/**
 * This Utility class provides methods to convert Item Values in Rest API calls.
 */
public class RestAPIUtil {
    
     /**
     * This method returns a List object from a given comma separated string. The
     * method returns null if no elements are found. The provided parameter looks
     * typical like this: <code>
     *   txtWorkflowStatus,numProcessID,txtName
     * </code>
     * 
     * @param items
     * @return
     */
    public static List<String> getItemList(String items) {
        if (items == null || "".equals(items))
            return null;
        Vector<String> v = new Vector<String>();
        StringTokenizer st = new StringTokenizer(items, ",");
        while (st.hasMoreTokens())
            v.add(st.nextToken());
        return v;
    }
}
