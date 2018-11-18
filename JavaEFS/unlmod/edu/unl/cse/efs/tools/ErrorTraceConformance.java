/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.unl.cse.efs.tools;

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
