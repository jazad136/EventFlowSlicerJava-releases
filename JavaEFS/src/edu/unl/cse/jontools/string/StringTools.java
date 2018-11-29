package edu.unl.cse.jontools.string;

public class StringTools {
	/** If fewer than n = i characters were found, return -1, otherwise, 
	 * return the nth occurence of toFind in the string search.
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
