/*******************************************************************************
 *    Copyright (c) 2018 Jonathan A. Saddler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    Contributors:
 *     Jonathan A. Saddler - initial API and implementation
 *******************************************************************************/
package edu.unl.cse.efs.view.ft;

import java.awt.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import edu.umd.cs.guitar.model.data.Widget;

public class DisplayingWidgetsModel extends AbstractListModel<DisplayIcon> implements Iterable<DisplayIcon>
{
	public LinkedList<DisplayIcon> inDisplay;
	private LinkedList<PairUpdate> newListAdditions;
	private LinkedList<PairUpdate> newListRemovals;
	public static DisplayIcon noneIcon;
	static {	
		noneIcon = new DisplayIcon("none");
	}
	private class PairUpdate implements Comparable<PairUpdate>{
		final DisplayIcon up; final int index; 
		PairUpdate(DisplayIcon up, int index){this.up = up; this.index = index;}
		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {final int prime = 31;int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((up == null) ? 0 : up.hashCode()); return result;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true; if (obj == null) return false; if (!(obj instanceof PairUpdate)) return false;
			PairUpdate other = (PairUpdate) obj;
			if (!getOuterType().equals(other.getOuterType())) return false;
			if (up == null) {
				if (other.up != null) return false; } 
			else if (!up.equals(other.up)) return false;
			return true;
		}
		private DisplayingWidgetsModel getOuterType() { return DisplayingWidgetsModel.this; }
		public String toString(){return "" + up + ": " + index;}
		@Override
		public int compareTo(PairUpdate o) {
			return Integer.compare(index, o.index);
		}
	}
	
	
	public DisplayingWidgetsModel()
	{
		inDisplay = new LinkedList<DisplayIcon>();
		newListAdditions = new LinkedList<>();
		newListRemovals = new LinkedList<>();
	}
	
	/**
	 * This method should 
	 * @param initial
	 */
	public DisplayingWidgetsModel(Collection<Widget> initial)
	{
		this();
		
		
		for(Widget w : initial) {
			DisplayIcon newDI = new DisplayIcon(w);
			for(int idx = 0; idx < inDisplay.size(); idx++) {
				if(inDisplay.get(idx).toDisplay.equals(newDI.toDisplay)) {
					// change the display of the old icon.
					DisplayIcon first = inDisplay.get(idx);
					first.setPrimed();
					inDisplay.set(idx, first);
					// change the display of the new icon.
					newDI.setPrimed();
					break;
				}
			}
			inDisplay.add(newDI);
		}
	}
	
	public DisplayingWidgetsModel(ListModel<DisplayIcon> otherModel)
	{
		this();
		for(int i = 0; i < otherModel.getSize(); i++) {
			DisplayIcon newDI = otherModel.getElementAt(i);
			if(!inDisplay.contains(newDI))
				inDisplay.add(newDI);
		}
	}
	public void clear()
	{
		for(int i = 0; i < inDisplay.size(); i++)
			remove(i);
		fireChanges();
	}
	
	public boolean add(DisplayIcon newDI) {
		// add a new update. 
		if(!inDisplay.contains(newDI)) {
			int newIndex = (newListAdditions.isEmpty() ? getSize() : newListAdditions.peek().index+1);
			PairUpdate newP = new PairUpdate(newDI, newIndex);
			if(!newListAdditions.contains(newP)) {
				newListAdditions.add(new PairUpdate(newDI, newIndex));
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Immediately add newDI to the display and sort the display, without firing any 
	 * listSelectionIntervalChanged events.
	 * 
	 * Preconditions: No additions or removals should be in effect before calling
	 * this method.   
	 * @param newDI
	 * @return
	 */
	public boolean addImmediateNoFire(DisplayIcon newDI)
	{
		if(!(newListAdditions.isEmpty() && newListRemovals.isEmpty()))
			throw new RuntimeException(
					"addImmediateNoFire was called when there were list "
					+ "additions and/or removals to be made.");
		if(!inDisplay.contains(newDI)) {
			inDisplay.add(newDI);
			Collections.sort(inDisplay);
			return true;
		}
		return false;
	}
	
	public boolean add(Widget w) {
		/*
		 * Use this formula when it's time to fix this problem
		 * DisplayIcon newDI = new DisplayIcon(w);
			for(int idx = 0; idx < inDisplay.size(); idx++) {
				if(inDisplay.get(idx).toDisplay.equals(newDI.toDisplay)) {
					// change the display of the old icon.
					DisplayIcon first = inDisplay.get(idx);
					first.setPrimed();
					inDisplay.set(idx, first);
					// change the display of the new icon.
					newDI.setPrimed();
					break;
				}
			}
			inDisplay.add(newDI);
		 */
	// add a new update. 
		DisplayIcon newDI = new DisplayIcon(w);
		boolean isACopy = false;
		if(inDisplay.contains(newDI)) { 
			// change the display of the old icon.
			int idx = inDisplay.indexOf(newDI);
			DisplayIcon first = remove(newDI);
			first.setPrimed();
			inDisplay.set(idx, first);
			// change the display of the new icon.
			newDI.setPrimed();
			isACopy = true;
		}
		int newIndex = (newListAdditions.isEmpty() ? getSize() : newListAdditions.peek().index+1);
		newListAdditions.add(new PairUpdate(newDI, newIndex));
		return isACopy;
		
//		if(!inDisplay.contains(newDW)) {
//			int newIndex = (newListAdditions.isEmpty() ? getSize() : newListAdditions.peek().index+1);
//			newListAdditions.add(new PairUpdate(newDW, newIndex));
//			return true;
//		}
	}
	
	public int findWidget(Widget w)
	{
		return findIcon(new DisplayIcon(w));
	}
	
	public int findIcon(DisplayIcon di)
	{
		for(int i = 0; i < inDisplay.size(); i++) 
			if(inDisplay.get(i).equals(di))
				return i;
		return -1;
	}
	
	public DisplayIcon remove(DisplayIcon toRemove)
	{
		int rIndex = findIcon(toRemove);
		if(rIndex == -1)
			return new DisplayIcon();
		return remove(rIndex);
	}
	
	public DisplayIcon remove(int index)		  
	{
		if(index >= inDisplay.size() || index < 0)
			return new DisplayIcon();
		else { 
			newListRemovals.add(new PairUpdate(inDisplay.get(index), index));
			return inDisplay.get(index);
		}
	}
	
	public void fireChanges()
	{
		// handle removals 
		Collections.sort(newListRemovals);
		Iterator<PairUpdate> removalIt = newListRemovals.descendingIterator();
		while(removalIt.hasNext()) {
			PairUpdate u = removalIt.next();
			inDisplay.remove(u.index);
			fireIntervalRemoved(this, u.index, u.index);
		}
		int allRemoved = newListRemovals.size();
		// handle additions
		if(!newListAdditions.isEmpty()) {
			int firstIndex = newListAdditions.peekFirst().index;
			int lastIndex = newListAdditions.peekLast().index;
			for(PairUpdate u : newListAdditions) 
				inDisplay.add(u.up);
			fireIntervalAdded(this, firstIndex-allRemoved, lastIndex-allRemoved);
		}
		Collections.sort(inDisplay);
		newListRemovals.clear();
		newListAdditions.clear();
	}
	public Iterator<DisplayIcon> iterator()    	{return inDisplay.iterator();}
	public int getSize() 				  	   	{return inDisplay.size();}
	
	public DisplayIcon getElementAt(int index) 	{return inDisplay.get(index);}
	public String toString()			  		{return printSpacedListing(0);}
	
	/**
	 * This method returns true if the current display contains no content.
	 * 
	 * Preconditions: 	
	 * Postconditions: 	True is returned if the display currently contains no content,
	 * 					or a single widget specifying that there is no content. 
	 * @return
	 */
	public boolean isDisplayEmpty() 
	{
		if(getSize() == 0)
			return true;
		else if(getSize() == 1 && inDisplay.get(0).equals(noneIcon))
			return true;
		return false;
	}

	public String printSpacedListing(int numSpaces)
	{
		String toReturn = "\n";
		for(DisplayIcon w : this)
			toReturn += sp(numSpaces) + w + "\n";
		return toReturn;
	}
	
	
	private static String sp(int num)
	{
		if(num == 0)
			return "";
		else return " " + sp(num-1);
	}
	
	public class Renderer extends JLabel implements ListCellRenderer<Widget>
	{
		final ImageIcon longIcon = new ImageIcon("long.gif");
	    final ImageIcon shortIcon = new ImageIcon("short.gif");
		@Override
		public Component getListCellRendererComponent(JList<? extends Widget> list, Widget value,            
		       int index, boolean isSelected, boolean cellHasFocus)    
		{
	         String s = value.toString();
	         setText(s);
	         setIcon((s.length() > 10) ? longIcon : shortIcon);
	         if (isSelected) {
	             setBackground(list.getSelectionBackground());
	             setForeground(list.getSelectionForeground());
	         } else {
	             setBackground(list.getBackground());
	             setForeground(list.getForeground());
	         }
	         setEnabled(list.isEnabled());
	         setFont(list.getFont());
	         setOpaque(true);
	         return this;
	     }
	}
}
