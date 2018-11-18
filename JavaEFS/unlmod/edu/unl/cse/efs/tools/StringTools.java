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

/**
 * 
 * Tools to help parse parts of character strings
 * @author Jonathan A. Saddler
 *
 */
public class StringTools {
	/** If fewer than n = i characters were found, return -1, otherwise, 
	 * return the nth occurrence of toFind in the string search.
	 */  
	public static int findNthCharIn(String search, char toFind, int n)
    {
    	int found = -1;
    	for(int i = 0; i < n; i++) {
    		found = search.indexOf(toFind, found+1);
    		if(found == -1)
    			return found;
    	}
    	return found;
    }
	
	/**
	 * Returns
	 * @param search
	 * @return
	 */
	public static int charactersIn(String search, char toFind)
	{
		int count = 0;
		for(char c : search.toCharArray()) 
			if(c == toFind)
				count++;
		return count;
	}
	
	
	/**
	 * Returns the indices in the string search where toFind was found
	 * up to n indices are returned in the returned int array. If fewer
	 * than n = i characters were found, return i cells containing the positions where the i characters were
	 * found, and return n-i cells containing '-1' in the positions following. Always returns an n-size array.  
	 */
	public static int[] findNCharactersIn(String search, char toFind, int n)
	{
		int[] foundIndices = new int[n];
		int found = -1;
		for(int i = 0; i < n; i++) {
    		found = search.indexOf(toFind, found+1);
    		foundIndices[i] = found;
    		if(foundIndices[i] == -1)
    			while(i+1 < n) {
    				i++;
    				foundIndices[i] = -1;
    			}
    	}
		return foundIndices;
	}
}
