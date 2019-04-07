//
/*	
 *  Copyright (c) 2009-@year@. The GUITAR group at the University of Maryland. Names of owners of this group may
 *  be obtained by sending an e-mail to atif@cs.umd.edu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *  documentation files (the "Software"), to deal in the Software without restriction, including without 
 *  limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *	the Software, and to permit persons to whom the Software is furnished to do so, subject to the following 
 *	conditions:
 * 
 *	The above copyright notice and this permission notice shall be included in all copies or substantial 
 *	portions of the Software.
 *
 *	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 *	LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO 
 *	EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *	IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *	THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 */

package edu.umd.cs.guitar.model.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AttributesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AttributesType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Property" type="{}PropertyType" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AttributesType", propOrder = {
    "property"
})
@XmlRootElement(name = "Attributes")
public class AttributesType implements java.io.Serializable {

    @XmlElement(name = "Property", required = true)
    protected List<PropertyType> property;

    /**
     * Gets the value of the property property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the property property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PropertyType }
     * 
     * 
     */
    public List<PropertyType> getProperty() {
        if (property == null) {
            property = new ArrayList<PropertyType>();
        }
        return this.property;
    }

    /**
     * Sets the value of the property property.
     * 
     * @param property
     *     allowed object is
     *     {@link PropertyType }
     *     
     */
    public void setProperty(List<PropertyType> property) {
        this.property = property;
    }
    
    public String toString()
    {
    	if(property == null)
    		return "";
    	String toReturn = "";
    	
    	if(property.isEmpty())
    		return toReturn;
    	toReturn = printProperty(property.get(0));
    	for(int i = 1; i < property.size(); i++)
    		toReturn += "\n" + printProperty(property.get(i));
    			
//    	for(PropertyType p : property) { 
//    		
////    		toReturn += "Name: " + p.getName() + " Value: [" + p.getValue() + "]";  
//    		toReturn += "Name: " + p.getName() + " Value: [";
//    		if(p.getValue().size() != 0) {
//    			toReturn += p.getValue().get(0);
//    			for(int i = 0; i < p.getValue().size(); i++) 
//    				toReturn += ", " + p.getValue().get(i);
//    		}
//    		else 
//    			toReturn += "";
//    		toReturn += "]\n";
//    	}
//    	toReturn += "";
    	return toReturn;
    }
    
    private String printProperty(PropertyType p)
    {
    	String toReturn = "Name: " + p.getName() + " Value: [";
		if(p.getValue().size() != 0) {
			toReturn += p.getValue().get(0);
			for(int i = 0; i < p.getValue().size(); i++) 
				toReturn += ", " + p.getValue().get(i);
		}
		else 
			toReturn += "";
		toReturn += "]";
		return toReturn;
    }
}
