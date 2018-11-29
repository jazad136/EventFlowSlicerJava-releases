package edu.unl.cse.efs.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.Slice;
import edu.umd.cs.guitar.model.data.Widget;

public class Splicer
{

	static ObjectFactory fact = new ObjectFactory();
	ArrayList<ArrayList<Slice>> storage;
	ArrayList<ArrayList<Integer>> edges;
	int next;
	int dequeueState;
	List<EventType> allEvents;
	public Splicer(List<EventType> allEvents)
	{
		this.allEvents = allEvents;
		storage = new ArrayList<>();
		edges = new ArrayList<>();
		dequeueState = 0;
	}

	public void advance() { dequeue(); }
	public boolean hasMoreSlices() { return storage.size() != 0;}
	public int currentN() { return storage.get(0).size(); }
	public void abbySpliceConstruction(List<DirectionalPack> dp, EventType openE, EventType closeE)
	{
		// assume there are an odd number of splices beginning with the first
		// as search, the second as directional.
		int next = 2;
		// advance the head of the queue forward one to skip the search pack.
		dequeueState++;
		copyFirst();
		for(DirectionalPack p : dp) {

			// add an extra copy of the slice
			set(next, orderBy(next, next-1));
			if(p.allHoversFirst) {
				add(append(next, next+1, next+2));
				dequeue(3);
			}
			else {
				add(equiJoin(next, next+1, next+2));
				dequeue(3);
			}
			Slice openS = fact.createSlice(openE);
			Slice closeS = fact.createSlice(closeE);
			LinkedList<Slice> openL = new LinkedList<>();
			openL.add(openS);
			LinkedList<Slice> closeL = new LinkedList<>();
			closeL.add(closeS);
			add(append(openL, storage.get(next-1), closeL));
			dequeue();
			next++;
		}
		removeFirst();
		formEdges(false, true);
	}
	public void timSpliceConstruction(List<DirectionalPack> dp)
	{
		// assume there are an even number of splices beginning with
		// the first as directional.
		int next = 1;
		for(DirectionalPack p : dp) {
			if(p.allHoversFirst) {
				add(append(next, next+1));
				dequeue(); dequeue();
				next++;
			}
			else {
				add(equiJoin(next, next+1));
				dequeue(); dequeue();
				next++;
			}
		}
		formEdges(false);
	}
	public void formEdges(boolean formEmpties)
	{
		for(int i = 1; i <= storage.size(); i++)
			formEdges(formEmpties, i, false);
	}
	public void formEdges(boolean formEmpties, boolean connectToEnd)
	{
		for(int i = 1; i <= storage.size(); i++)
			formEdges(formEmpties, i, connectToEnd);
	}

	public void formEdges(boolean formEmpties, int scenario, boolean connectToEnd)
	{
		int k = storage.get(scenario-1).size();
		for(int i = 0; i < k-1; i++) {
			edges.add(new ArrayList<Integer>());
			edges.get(i).add(i+1);
			if(formEmpties)
				edges.get(i).add(i+k);
			if(connectToEnd && i != k-1 && i+1 != k-1)
				edges.get(i).add(k-1);
		}
	}


	public List<Slice> orderBy(List<Slice> one, List<Slice> ordered)
	{
		List<Slice> toOrder = new LinkedList<>(one);
		List<Slice> toReturn = new ArrayList<Slice>();
		for(int i = 0; i < ordered.size(); i++) {
			boolean similar = false;
			int j = 0;
			for(j = 0; j < toOrder.size(); j++) {
				if(toOrder.get(j).getMainEvent().equals(ordered.get(i).getMainEvent())) {
					similar = true;
					break;
				}
			}
			if(similar)
				toReturn.add(toOrder.remove(j));
		}
		return toReturn;
	}
	public List<Slice> orderBy(int s1, int s2)
	{
		return orderBy(storage.get(s1), storage.get(s2));
	}

	public ArrayList<Integer> newRow(int size)
	{
		ArrayList<Integer> newRow = new ArrayList<>();
		for(int i = 0; i < edges.size(); i++)
			newRow.add(0);
		return newRow;

	}
	public int copyFirst()
	{
		storage.add(0, storage.get(0));
		edges.add(0, new ArrayList<Integer>());
		return 0;
	}
	public void removeFirst()
	{
		storage.remove(0);
		edges.remove(0);
		dequeueState--;
	}
	public int add(List<Slice> slices)
	{
		storage.add(new ArrayList<Slice>(slices));
		edges.add(new ArrayList<Integer>());
		return storage.size();
	}
	public void set(int index, List<Slice> slices)
	{
		storage.set(index, new ArrayList<>(slices));
	}
	public void resetDequeueState()
	{
		dequeueState = storage.size();
	}
	public void dequeue()
	{
		storage.remove(dequeueState);
		edges.remove(dequeueState);
	}
	public void dequeue(int repeats)
	{
		for(int i = 0; i < repeats; i++) {
			storage.remove(dequeueState);
			edges.remove(dequeueState);
		}
	}

	public void addAll(Collection<List<Slice>> slices)
	{
		for(List<Slice> sl : slices)
			add(sl);
	}

	/**
	 * According to the index in the respective slice, pick from all slices
	 * at the same index level in the slice lists in storage referenced by s1 and s2,
	 * and return the corresponding result list of slices, equijoined together
	 * <br><br>
	 * This method converts 1-indexed parameters into 0-indexed list indices.
	 */
	public List<Slice> equiJoin(int s1, int s2)
	{
		return equiJoin(storage.get(s1-1), storage.get(s2-1));
	}

	/**
	 * According to the index in the respective slice, pick from all slices
	 * at the same index level in the slice lists in storage referenced by s1, s2, and s3,
	 * and return the corresponding result list of slices, equijoined together
	 * <br><br>
	 * This method converts 1-indexed parameters into 0-indexed list indices.
	 */
	public List<Slice> equiJoin(int s1, int s2, int s3)
	{
		return equiJoin(storage.get(s1-1), storage.get(s2-1), storage.get(s3-1));
	}
	/**
	 * Append the list in storage referenced by s2 to the list in storage referenced
	 * by s1.
	 * <br><br>
	 * This method converts 1-indexed parameters into 0-indexed list indices.
	 * @param s1
	 * @param s2
	 * @return
	 */
	public List<Slice> append(int s1, int s2)
	{
		return append(storage.get(s1-1), storage.get(s2-1));
	}
	/**
	 *
	 * Append the list in storage referenced by s2 to the list in storage referenced
	 * by s1, and then append the list referenced by s3 to that result.
	 * This method converts 1-indexed parameters into 0-indexed list indices.
	 * @param s1
	 * @param s2
	 * @param s3
	 * @return
	 */
	public List<Slice> append(int s1, int s2, int s3)
	{
		return append(storage.get(s1-1), storage.get(s2-1), storage.get(s3-1));
	}

	public List<Slice> equiJoin(List<Slice> one, List<Slice> two)
	{
		LinkedList<Slice> newS = new LinkedList<Slice>();
		for(int i = 0; i < one.size(); i++) {
			for(int j = 0; j < 2; j++) {
				if(j == 0)
					newS.add(one.get(i));
				else
					newS.add(two.get(i));
			}
		}
		return newS;
	}
	public List<Slice> equiJoin(List<Slice> one, List<Slice> two, List<Slice> three)
	{
		LinkedList<Slice> newS = new LinkedList<Slice>();
		for(int i = 0; i < one.size(); i++) {
			for(int j = 0; j < 3; j++) {
				if(j == 0)
					newS.add(one.get(i));
				else if(j == 1)
					newS.add(two.get(i));
				else
					newS.add(three.get(i));
			}
		}
		return newS;
	}

	public List<Slice> append(List<Slice> one, List<Slice> two)
	{
		ArrayList<Slice> newS = new ArrayList<Slice>(one.size() + two.size());
		newS.addAll(one);
		newS.addAll(two);
		return newS;
	}
	public List<Slice> append(List<Slice> one, List<Slice> two, List<Slice> three)
	{
		ArrayList<Slice> newS = new ArrayList<Slice>(one.size() + two.size() + three.size());
		newS.addAll(one);
		newS.addAll(two);
		newS.addAll(three);
		return newS;
	}

	public int findSlice(Widget w)
	{
		int e = findEvent(w);
		if(e == -1)
			return -1;
		EventType ev = allEvents.get(e);
		return findSlice(ev);
	}
	public int findSlice(EventType ev)
	{
		for(int i = 0; i < storage.get(0).size(); i++)
			if(storage.get(0).get(i).contains(ev))
				return i;
		return -1;
	}
	public int findSliceByEvent(int eI)
	{
		for(int i = 0; i < storage.get(0).size(); i++) {
			if(storage.get(0).get(i).equals(allEvents.get(eI)))
				return i;
		}
		return -1;
	}
	public int findEvent(Widget w)
	{
		for(int i = 0; i < allEvents.size(); i++)
			if(w.getEventID().equals(allEvents.get(i).getEventId())) {
				if(TestCaseGenerate.parameterMatch(allEvents.get(i), w))
					return i;
			}
		return -1;
	}
	public String toString()
	{
		return storage.toString();
	}
}
