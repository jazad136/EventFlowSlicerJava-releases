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
package edu.umd.cs.guitar.model.data;

import java.util.Comparator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Widget complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Widget">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Type" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Window" type="{http://www.w3.org/2001/XMLSchema}string minOccurs="0"/>
 *         &lt;element name="Action" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Parent" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Parameter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="EventID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Widget", propOrder = {
	"eventID",
	"name",
    "type",
    "window",
    "action",
    "parent",
    "parameter"
})
public class Widget implements Comparable<Widget>{

    @XmlElement(name = "Name", required = true)
    protected String name;
    @XmlElement(name = "Type")
    protected String type;
    @XmlElement(name = "Window")
    protected String window;
    @XmlElement(name = "Action")
    protected String action;
    @XmlElement(name = "Parent")
    protected String parent;
    @XmlElement(name = "Parameter")
    protected String parameter;
    @XmlElement(name = "EventID")
    protected String eventID;

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the type property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the window property.
     *
     */
    public String getWindow() {
        return window;
    }

    /**
     * Sets the value of the window property.
     *
     */
    public void setWindow(String value) {
        this.window = value;
    }

    /**
     * Gets the value of the action property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setAction(String value) {
        this.action = value;
    }

    /**
     * Gets the value of the parent property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getParent() {
        return parent;
    }

    /**
     * Sets the value of the parent property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setParent(String value) {
        this.parent = value;
    }

    /**
     * Gets the value of the parameter property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * Sets the value of the parameter property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setParameter(String value) {
        this.parameter = value;
    }

    /**
     * Gets the value of the eventID property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getEventID() {
        return eventID;
    }

    /**
     * Sets the value of the eventID property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setEventID(String value) {
        this.eventID = value;
    }

    public String toString()
    {
    	String toReturn = name;
    	if(eventID != null && !eventID.isEmpty())
    		toReturn += " (" + eventID + ")";
    	if(parameter != null && !parameter.isEmpty()) {
    		toReturn += ": \"" + parameter + "\"";
    	}
    	return toReturn;
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((eventID == null) ? 0 : eventID.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parameter == null) ? 0 : parameter.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((window == null) ? 0 : window.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Widget))
			return false;
		Widget other = (Widget) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		if (eventID == null) {
			if (other.eventID != null)
				return false;
		} else if (!eventID.equals(other.eventID))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parameter == null || parameter.isEmpty()) {
			if (other.parameter != null && !other.parameter.isEmpty())
				return false;
		} else if (!parameter.equals(other.parameter))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (window == null) {
			if (other.window != null)
				return false;
		} else if (!window.equals(other.window))
			return false;
		return true;
	}

	@Override
	public int compareTo(Widget o)
	{
		return eventID.compareTo(o.eventID);
	}

	public Widget copyOf(ObjectFactory fact)
	{
		Widget copy = fact.createWidget();
		copy.setAction(action);
		copy.setEventID(eventID);
		copy.setName(name);
		copy.setParameter(parameter);
		if(parent != null)
			copy.setParent(parent);
		copy.setType(type);
		copy.setWindow(window);
		return copy;
	}

	public static class IDComparator implements Comparator<Widget>
	{
		@Override
		public int compare(Widget o1, Widget o2)
		{
			if(o1 == null || o2 == null)
				return 0;
			else if(o1.getEventID() == null || o2.getEventID() == null)
				return 0;
			return o1.getEventID().compareTo(o2.getEventID());
		}
	}
}

