package edu.unl.cse.jontools.widget;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


public class HyperList<K> implements List<K>
{
	private int current;
	private LinkedList<LinkedList<K>> model;
	
	public HyperList()
	{
		model = new LinkedList<LinkedList<K>>();
		model.add(new LinkedList<K>());
		current = 0;
	}
	
	public HyperList(Collection<K> initial)
	{
		this();
		addAll(initial);
	}
	
	public Iterator<K> iterator(){return model.get(current).iterator();}
	
	/**
	 * Add a new list to the current number of lists available. 
	 * @param c
	 */
	public void addNewList(Collection<? extends K> c)
	{
		 model.add(new LinkedList<K>(c));	 
		 setCurrent(lists()-1);
	}
	public void addNewList()
	{
		model.add(new LinkedList<K>());
		setCurrent(lists()-1);
	}
	
	public void startNewListWith(K c){
		LinkedList<K> newList = new LinkedList<K>();
		newList.add(c);
		model.add(newList);
		setCurrent(model.size()-1);
	}
	public int getCurrent()
	{
		return current;
	}
	
	public void setCurrent(int i)		
	{
		if(i >= 0 && i < lists()) 
			current = i;
	}
	
	public void setList(int index, Collection<K> toSet)
	{
		model.set(index, new LinkedList<K>(toSet));
	}
	
	public LinkedList<K> list(int index)
	{
		return model.get(index);
	}
	
	public LinkedList<K> removeList(int index)
	{
		return model.remove(index);
	}
	/**
	 * Returns an iterator that will allow for this HyperList's lists to be traversable
	 * @return
	 */
	public Iterable<LinkedList<K>> getListsIterable()
	{
		return new Iterable<LinkedList<K>>(){
			public ListIterator<LinkedList<K>> iterator(){return model.listIterator();
		}};
	}
	/**
	 * Returns an iterator over the many lists this HyperList contains. 
	 * @return
	 */
	public ListIterator<LinkedList<K>> iteratorOfLists()
	{
		return model.listIterator();
	}
	
	public K get(int index) 
	{
		return model.get(current).get(index);
	}
	
	/**
	 * Empty only the list that is set to be the current list. 
	 */
	public void clear()	
	{
		for(int i = 0; i < model.size(); i++)
			model.get(i).clear();
//		model.get(current).clear();
		
	}
	
	/**
	 * 	Empty all the lists in this HyperList
	 */
	public void clearAll()
	{
		model.clear();
		model.add(new LinkedList<K>());
	}
	
	/**
	 * Get the number of models stored within this list
	 * @return
	 */
	public int lists()
	{
		return model.size();
	}
	public boolean isDepthEmpty()
	{
		return model.size() == 1 && isEmpty();
	}
	
	
	public int size()				  {return model.get(current).size();};
	public boolean isEmpty() 		  {return model.get(current).isEmpty();}
	public boolean contains(Object o) {return model.get(current).contains(o);}
	public Object[] toArray() {return model.get(current).toArray();}
	public <T> T[] toArray(T[] a) {return model.get(current).toArray(a);}
	public boolean add(K e) 				   {return model.get(current).add(e);}
	public boolean addAll(Collection<? extends K> c) {return model.get(current).addAll(c);}
	public boolean addAll(int index, Collection<? extends K> c){return model.get(current).addAll(index, c);}
	public boolean remove(Object o) 		   	{return model.get(current).remove(o);}
	public boolean containsAll(Collection<?> c)	{return model.get(current).containsAll(c);}
	public boolean removeAll(Collection<?> c) 	{return model.get(current).removeAll(c);}
	public boolean retainAll(Collection<?> c) 	{return model.get(current).retainAll(c);}
	public K set(int index, K element) 			{return model.get(current).set(index, element);}
	public void add(int index, K element) 		{model.get(current).add(index, element);}
	public K remove(int index) 					{return model.get(current).remove(index);}
	public int indexOf(Object o) 				{return model.get(current).indexOf(o);}
	public int lastIndexOf(Object o) 			{return model.get(current).lastIndexOf(o);}
	public ListIterator<K> listIterator() 		{return model.get(current).listIterator();}
	public ListIterator<K> listIterator(int index) {return model.get(current).listIterator(index);}
	public List<K> subList(int fromIndex, int toIndex) {return model.get(current).subList(fromIndex, toIndex);}
	public String toString()
	{
		String toReturn = "";
		for(int i = 0; i < model.size(); i++) 
			toReturn += "(" + i + ") " + model.get(i) + " | "; 
		toReturn += "\n";
		return toReturn;
	}
}

