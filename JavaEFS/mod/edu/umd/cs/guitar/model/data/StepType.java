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
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for StepType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StepType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="EventId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ReachingStep" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="Parameter" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Optional" type="{}AttributesType" minOccurs="0"/>
 *         &lt;element ref="{}GUIStructure" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * 
 */


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StepType", propOrder = {
    "eventId",
    "windowId",
    "reachingStep",
    "action",
    "parameter",
    "optional",
    "guiStructure"
})
public class StepType {

    @XmlElement(name = "EventId", required = true)
    protected String eventId;
    @XmlElement(name = "ReachingStep")
    protected boolean reachingStep;
    @XmlElement(name = "Parameter")
    protected List<String> parameter;
    @XmlElement(name = "Optional")
    protected AttributesType optional;
    @XmlElement(name = "GUIStructure")
    protected GUIStructure guiStructure;
    @XmlElement(name = "WindowID")
    protected String windowId;
    @XmlElement(name = "Action")
    protected String action;
    
    /**
     * Gets the value of the eventId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the value of the eventId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEventId(String value) {
        this.eventId = value;
    }

    /**
     * Gets the value of the reachingStep property.
     * 
     */
    public boolean isReachingStep() {
        return reachingStep;
    }

    /**
     * Sets the value of the reachingStep property.
     */
    public void setReachingStep(boolean value) {
        this.reachingStep = value;
    }
    
    /*
     * @XmlElement(name = "WindowID")
    protected String windowId;
    @XmlElement(name = "Action")
    protected String action;
     */
    /**
     * Gets the value of the windowId property.
     */
    public String getWindowID()
    {
    	return windowId;
    }
    
    /**
     * Sets the value of the windowId property.
     * 
     */
    public void setWindowID(String value)
    {
    	this.windowId = value;
    }
    
    /**
     * Gets the value of the action property.
     */
    public String getAction()
    {
    	return action;
    }
    /**
     * Sets the value of the action property.
     */
    public void setAction(String value)
    {
    	this.action = value;
    }
    
    /**
     * Gets the value of the parameter property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parameter property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParameter().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getParameter() {
        if (parameter == null) {
            parameter = new ArrayList<String>();
        }
        return this.parameter;
    }

    /**
     * Gets the value of the optional property.
     * 
     * @return
     *     possible object is
     *     {@link AttributesType }
     *     
     */
    public AttributesType getOptional() {
        return optional;
    }

    /**
     * Sets the value of the optional property.
     * 
     * @param value
     *     allowed object is
     *     {@link AttributesType }
     *     
     */
    public void setOptional(AttributesType value) {
        this.optional = value;
    }

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
     * Sets the value of the parameter property.
     * 
     * @param parameter
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setParameter(List<String> parameter) {
        this.parameter = parameter;
    }

}
