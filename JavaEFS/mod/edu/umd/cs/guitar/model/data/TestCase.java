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
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}GUIStructure" minOccurs="0"/>
 *         &lt;element name="Step" type="{}StepType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "guiStructure",
    "step"
})
@XmlRootElement(name = "TestCase")
public class TestCase {

    @XmlElement(name = "GUIStructure")
    protected GUIStructure guiStructure;
    @XmlElement(name = "Step", required = true)
    protected List<StepType> step;

    /**
     * Gets the value of the guiStructure property.
     * 
     * @return
     *     possible object is
     *     {@link GUIStructure }
     *     
     */
    public GUIStructure getGUIStructure() {
        return guiStructure;
    }

    /**
     * Sets the value of the guiStructure property.
     * 
     * @param value
     *     allowed object is
     *     {@link GUIStructure }
     *     
     */
    public void setGUIStructure(GUIStructure value) {
        this.guiStructure = value;
    }

    /**
     * Gets the value of the step property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the step property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStep().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StepType }
     * 
     * 
     */
    public List<StepType> getStep() {
        if (step == null) {
            step = new ArrayList<StepType>();
        }
        return this.step;
    }

    /**
     * Sets the value of the step property.
     * 
     * @param step
     *     allowed object is
     *     {@link StepType }
     *     
     */
    public void setStep(List<StepType> step) {
        this.step = step;
    }

    public String toString()
    {
    	String toReturn = "";
    	if(step.size() > 0) {
    		toReturn += 0 + ": " + step.get(0).getEventId();
	    	for(int i = 1; i < step.size(); i++) {
	    		String stepString = step.get(i).getEventId();
	    		toReturn += "\n|\n" + i + ": " + stepString;
	    	}
    	}
    	return toReturn;
    }
}
