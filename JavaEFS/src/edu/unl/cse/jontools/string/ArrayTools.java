package edu.unl.cse.jontools.string;

import java.util.Arrays;

/**
 * Tools to help with recognizing and identifying inclusion arrays.
 * 
 * @author Jonathan Saddler
 *
 */
public class ArrayTools {
	public static void main(String[] args)
	{
		try(java.util.Scanner j = new java.util.Scanner(System.in)) {
			while(j.hasNextLine()) {
				String in = j.nextLine();
				System.out.println(bibleNotationType(in, false));
			}		
		}
	}
	public static boolean[][] extendRangeDepth(int newSize, boolean[][] currentRanges)
	{
		boolean[][] newArr = new boolean[newSize][];
		for(int i = 0; i < currentRanges.length; i++)  
			newArr[i] = Arrays.copyOf(currentRanges[i], currentRanges[i].length); // for safety.
		for(int i = currentRanges.length; i < newSize; i++) 
			newArr[i] = new boolean[0];
		return newArr;
	}
	
	/**
	 * Extend the first dimension of currentRanges to be the size newSize,
	 * allowing the arrays added to have zero dimension. 
	 */
	public static int[][] extendRangeDepthInt(int newSize, int[][] currentRanges)
	{
		int[][] newArr = new int[newSize][];
		for(int i = 0; i < currentRanges.length; i++)  
			newArr[i] = Arrays.copyOf(currentRanges[i], currentRanges[i].length); // for safety.
		for(int i = currentRanges.length; i < newSize; i++) 
			newArr[i] = new int[0];
		return newArr;
	}
	public static void assignRange(int minReal, int maxReal, boolean[] currentRange)
	{
		Arrays.fill(currentRange, minReal-1, maxReal, true);
	}
	
	public static boolean[] extendRange(int newSize, boolean[] currentRange)
	{
		boolean[] newArr = new boolean[newSize];
		for(int i = 0; i < currentRange.length; i++) 
			newArr[i] = currentRange[i];
		return newArr;
	}
	
	public static int[] extendRangeInt(int newSize, int[] currentRange)
	{
		int[] newArr = new int[newSize];
		for(int i = 0; i < currentRange.length; i++) 
			newArr[i] = currentRange[i];
		return newArr;
	}
	
	/**
	 * Returns a code indicating what kind of selector notation that arg is in
	 * when using the bibleNotationType language. There are three codes:<br> 
	 * 0 indicates only notation indicating a single numeric argument.<br>
	 * 1 indicates notation including multiple numeric arguments separated by comma delimiters,
	 * or multiple spans (two numeric arguments separated by dashes) separated by comma delimiters<br>
	 * 2 indicates notation including 1 or more type-1 arguments preceded by a single numeric
	 * argument followed by a colon. <br>These three types of arguments have no meaning until brought
	 * under the context of some program who needs to decipher the types returned for further use. 
	 * These notations have been used to create special arrays 
	 * using other methods in the ArrayTools class.    
	 * 
	 * For an arg that doesn't follow any three of these definintions, -1 is returned. 
	 * If accept0 is false, then if arg contains a 0, return -1.     
	 * As a guide, here are some templates:<br>
	 * <pre>
	 * 11 (type-0)<br>
	 * 11,11 (type-1) 
	 * 11-11 (type-1)<br>
	 * 11:11-11 (type-2) 
	 * 2:3-4,5:5-5 (type-2)
	 * @author Jonathan Saddler (jsaddler) 
	 * </pre>
	 */
	public static int bibleNotationType(String arg, boolean accept0)	
	{
		if(!accept0)
			return bibleNotationTypeNoZero(arg);
		
		if(arg.matches("^\\d+$")) return 0; // a single
		if(arg.matches("(\\d+(-\\d+)?)(,(\\d+(-\\d+)?))*")) return 1; // ranges, multiple singles, and multiple ranges
		if(arg.matches("(\\d+:\\d+(-\\d+)?)(,\\d+:\\d+(-\\d+)?)*")) return 2; // books of ranges and singles. 
		return -1;
	}
	public static int bibleNotationTypeNoZero(String arg)	
	{
		if(arg.matches("^[1-9][0-9]*$")) return 0; // a single
		if(arg.matches("([1-9][0-9]*(-[1-9][0-9]*)?)(,([1-9][0-9]*+(-[1-9][0-9]*)?))*")) return 1; // ranges, multiple singles, and multiple ranges
		if(arg.matches("([1-9]+:[1-9]+(-[1-9]+)?)(,[1-9]+:[1-9]+(-[1-9]+)?)*")) return 2; // books of ranges and singles. 
		return -1;
	}
	/**
	 * Returns an inclusion array, where selArg specifies (in 1-based notation) what cells in an array
	 * should be returned as true. 
	 * Depending on the notation: 
	 * (digits only)<br>
	 * the number represents the cell in the (first) array within the 2d result to be set to true. 
	 * 
	 * (digits-comma-digits/digits-dash-digits)<br>
	 * the numbers or ranges, represent the cells to be set to true in the aforementioned array
	 * 
	 * digits-colon-digits-dash-digit (+ underscore-more-digit-colon-digit)...<br>
	 * number behind colon represents the array within the result, numbers after the colon
	 * represent the ranges to be set.
	 * 
	 * We do our best to get a filled inclusion array result for the argument specified, 
	 * even if selArg contains a 0.
	 */
	public static boolean[][] getFilledArrayFor(String selArg)
	{
		boolean[][] toReturn = new boolean[0][0];
		switch(bibleNotationType(selArg, true)) {
			case -1: return toReturn;	
			case 0: {
				int soleNum = Integer.parseInt(selArg);
				toReturn = new boolean[1][soleNum];
				if(soleNum > 0)
					toReturn[0][soleNum-1] = true;
				return toReturn;
			} 
			case 1: {
				boolean[] newRange = new boolean[0];
				String[] parts = selArg.split(",");
				int min, max;
				
				for(String part : parts) {
					min = max = -1;
					// assign points 1 and 2 integers
					String[] range = part.split("-");
					min = Integer.parseInt(range[0]);
					if(range.length == 2) 
						max = Integer.parseInt(range[1]);
					
					// parse the points 
					if(max < min)
						max = min;
					
					// if the new range is too small to fit the range specified, extend it
					if(max > 0) {
						if(newRange.length < max) 
							newRange = extendRange(max, newRange);
						assignRange(min, max, newRange); // assign the range specified to the range we call for.
					}
				}
				toReturn = new boolean[1][];
				toReturn[0] = newRange;
				return toReturn;
			} 
			default: {
				boolean[][] newRanges = new boolean[0][0];
				String[] parts = selArg.split(",");
				int book, min, max;
				for(String part : parts) {
					// setup 
					String[] range = part.split("[\\:\\-]");
					book = Integer.parseInt(range[0]);
					// if the old array does not exist, create it. 
					if(newRanges.length < book) 
						newRanges = extendRangeDepth(book, newRanges);
					
					min = max = -1;
					
					// find the min and max
					min = Integer.parseInt(range[1]);
					if(range.length == 3)
						max = Integer.parseInt(range[2]);
					// parse the min and max
					if(max < min)
						max = min;
					
					// if the new range is too small to fit the range specified, extend the old one.
					if(book > 0 && max > 0) {
						if(newRanges[book-1].length < max) 
							newRanges[book-1] = extendRange(max, newRanges[book-1]);
						// assign new values to the array
						assignRange(min, max, newRanges[book-1]); // assign the range specified to the range we call for.
					}
					
				}
				return newRanges;					
			}
		}
	}
}
