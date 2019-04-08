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
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="OpenWindow" type="{}ComponentListType"/&gt;
 *         &lt;element name="CloseWindow" type="{}ComponentListType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "openWindow",
    "closeWindow"
})
@XmlRootElement(name = "LogWidget")
public class LogWidget {

    @XmlElement(name = "OpenWindow", required = true)
    protected ComponentListType openWindow;
    @XmlElement(name = "CloseWindow", required = true)
    protected ComponentListType closeWindow;

    /**
     * Gets the value of the openWindow property.
     * 
     * @return
     *     possible object is
     *     {@link ComponentListType }
     *     
     */
    public ComponentListType getOpenWindow() {
        return openWindow;
    }

    /**
     * Sets the value of the openWindow property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComponentListType }
     *     
     */
    public void setOpenWindow(ComponentListType value) {
        this.openWindow = value;
    }

    /**
     * Gets the value of the closeWindow property.
     * 
     * @return
     *     possible object is
     *     {@link ComponentListType }
     *     
     */
    public ComponentListType getCloseWindow() {
        return closeWindow;
    }

    /**
     * Sets the value of the closeWindow property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComponentListType }
     *     
     */
    public void setCloseWindow(ComponentListType value) {
        this.closeWindow = value;
    }

}
