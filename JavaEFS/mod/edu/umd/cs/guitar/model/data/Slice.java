
package edu.umd.cs.guitar.model.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for Repeat complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Slice">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Widget" type="{}Widget" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Slice", propOrder = {
    "event"
})
public class Slice implements Iterable<EventType> {

	public Iterator<EventType> iterator() { return getEvents().iterator(); }

    @XmlElement(name = "event", required = true)
    protected List<EventType> event;
    @XmlTransient
    protected EventType mainEvent;

    public EventType getMainEvent() { return mainEvent;}
    public boolean contains(EventType e)
    {
    	if(event == null)
    		return false;
    	else
    		return event.contains(e);
    }
    /**
     * Gets the value of the widget property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the widget property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWidget().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Widget }
     *
     *
     */
    public List<EventType> getEvents() {
        if (event == null) {
            event = new ArrayList<EventType>();
        }
        return this.event;
    }

    public void setNextEvent(EventType event)
    {
    	this.mainEvent = event;
    	getEvents().add(event);
    }
    public void setMain(EventType mainEvent)
    {
    	this.mainEvent = mainEvent;
    }

    /**
     * Return a string representation of this Slice.
     */
    public String toString()
    {
    	if(event == null || event.isEmpty())
    		return "";

    	String toReturn = "";
    	for(EventType w : event)
    		toReturn += "" + w + "\n";
    	return toReturn;
    }
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + ((mainEvent == null) ? 0 : mainEvent.hashCode());
		return result;
	}
	
	public void setEvent(int idx, EventType newEvent)
	{
		event.set(idx, newEvent);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Slice other = (Slice) obj;
		if (event == null) {
			if (other.event != null)
				return false;
		} else if (!event.equals(other.event))
			return false;
		if (mainEvent == null) {
			if (other.mainEvent != null)
				return false;
		} else if (!mainEvent.equals(other.mainEvent))
			return false;
		return true;
	}
}
