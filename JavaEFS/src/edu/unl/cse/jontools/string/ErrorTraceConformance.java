package edu.unl.cse.jontools.string;

public class ErrorTraceConformance {
	
	public static final int STACK_TRACE_LIMIT = 7;
	
	public static String someOfStackTrace(Throwable e)
	{
		int lines = STACK_TRACE_LIMIT;
		String toReturn = "";
    	StackTraceElement[] st = e.getStackTrace();
    	String last = "";
    	int dupLines = 0;
    	for(int i = st.length-1; i >= 0 && i > st.length-lines; i--) {
    		if(last.equals(st[i].toString())) {
    			lines++;
    			dupLines++;
    		}
    		else {
    			if(dupLines > 0)
    				toReturn += dupLines + " more.\n";
    			last = st[i].toString();
    			
    			toReturn += st[i] + "\n";
    			dupLines = 0;
    		}
    	}
    	return toReturn;	
	}
	/**
	 * Print out a stack trace, each element delimited by line breaks, 
	 * of the throwable provided. A number of elements in the trace, up to the amount specified by lines,
	 * are returned. Repeated elements in the trace are truncated into a form implying their 
	 * repeated presence in the trace and the number of times they are repeated.
	 */
	public static String someOfStackTrace(Throwable e, int lines)
	{
    	String toReturn = "";
    	StackTraceElement[] st = e.getStackTrace();
    	String last = "";
    	int dupLines = 0;
    	for(int i = st.length-1; i >= 0 && i > st.length-lines; i--) {
    		if(last.equals(st[i].toString())) {
    			lines++;
    			dupLines++;
    		}
    		else {
    			if(dupLines > 0)
    				toReturn += dupLines + " more.\n";
    			last = st[i].toString();
    			
    			toReturn += st[i] + "\n";
    			dupLines = 0;
    		}
    	}
    	return toReturn;
	}
}
