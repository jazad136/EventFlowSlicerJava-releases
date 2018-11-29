package edu.unl.cse.efs.tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

/**
 * Plans: 
 * I'm using a hashmap to make understanding of mapping characters to digits easier. 
 * @author jsaddle
 */
public class AlphabetIterator implements Iterator<String>{
	private final Map<Integer, Character> charMap;
	int size;
	boolean small;
	public static char firstLarge = 'A', lastLarge = 'Z', firstSmall = 'a', lastSmall = 'z';
	
	public static void main(String[] args)
	{
		AlphabetIterator myIt = new AlphabetIterator(true);
		try (Scanner myScanner = new Scanner(System.in)) {
			while(myScanner.hasNext()) {
				String next = myScanner.nextLine();
				if(next.length() > 0) {
					System.out.print(myIt.next());
					for(int i = 1; i < next.length(); i++) 
						System.out.print("\n" + myIt.next());
				}
				System.out.println();
			}
		}
	}
	
	/**
	 * Creates a new instance of alphabet iterator that generates capital letters.
	 */
	public AlphabetIterator()
	{
		this(false);
	}
	
	/**
	 * Creates a new instance of alphabet iterator. If smallLetters is true,
	 * this iterator returns small letters in response to a call to next,
	 * if false, this iterator returns capital letters.
	 * @param smallLetters
	 */
	public AlphabetIterator(boolean smallLetters)
	{
		small = smallLetters;
		size = 0;
		charMap = new HashMap<Integer, Character>(); // contains size letters.
	}
	
	@Override
	/**
	 * Always returns true, as there is always a next letter.
	 */
	public boolean hasNext()   
	{
		return true;
	}
	
	@Override
	public String next()
	{
		if(small) {
			int flip;
			for(flip = 1; flip <= size; flip++)   
				if(charMap.get(flip) == lastSmall) 
					charMap.put(flip, firstSmall);
				else {
					charMap.put(flip, (char)(charMap.get(flip)+1));
					return printCharMap();
				}
			size++;
			charMap.put(flip, firstSmall);
			return printCharMap();	
		}
		else {
			int flip;
			for(flip = 1; flip <= size; flip++)   
				if(charMap.get(flip) == lastLarge) 
					charMap.put(flip, firstLarge);
				else {
					charMap.put(flip, (char)(charMap.get(flip)+1));
					return printCharMap();
				}
			// we enter the loop if the 1.2.3.n <= size.
			// don't enter if flip > size
			// if the bit to flip is greater than our size
			// having flipped the rest of the Z's to a's, we add a new 
			// highest order alphit then flip it to an A.
			
			// at the start of the loop we have looked at all bits mapped to indices in the char map <= size, and have
			// flipped them if they need to be flipped. 
			
			// at the start we look at the lowest order alphit to see if it needs flipped. 
			// then we look at higher order alphits as necessary, only if the last former alphit has flipped. 
			// by the end, we have flipped all alphits that need to be flipped <= the size alphit, incremented the carry bit, 
			// and have not flipped all alphits beyond the carry bit.
			
			// if we must flip the (size+1th) alphit, we do it after the loop. 
			// this works if size = 0, when we need to flip the first alphit to an A
			// this works if size > 0, when flip > size, and actually flip is set to size+1, when we need to flip 
			// the size+1'th bit to an A and increase the size. 
			
			size++;
			charMap.put(flip, firstLarge);
		}
		return printCharMap();
	}
	
	public String printCharMap()
	{
		StringBuilder sb = new StringBuilder();
		for(int i = size ; i >= 1; i--) 
			sb.append(charMap.get(i));
		return sb.toString();
	}
	
	public String nextOld() 
	{
		StringBuilder sb = new StringBuilder();
		int nextAlphit = 0;
		while(true) {
			if(nextAlphit == size) {
				size++;
				charMap.put(nextAlphit, 'A');
				sb.insert(0,'A');
				break;
			}
			// increment the last letter.
			char gotten = charMap.get(nextAlphit);
			if(gotten != 'Z') {
				gotten++;
				charMap.put(nextAlphit, gotten);
				sb.insert(0 , gotten);
				break;
			}
			else {
				// flip all characters from 0 to size to 0 (A) until we get to a nonzero bit.
				
				int flip;
				for(flip = 0; flip < size; flip++) {
					charMap.put(flip, 'A');
					if(charMap.get(flip+1) != 'Z')
						break;	
				}
				flip++;
				if(flip > size)
					charMap.put(flip, 'A');
				else {
					char c = charMap.get(flip);
					c++;
					charMap.put(flip, c);
				}
			}
		}
		return sb.toString();
	}

	@Override
	public void remove() 
	{
		
	}
	
}
