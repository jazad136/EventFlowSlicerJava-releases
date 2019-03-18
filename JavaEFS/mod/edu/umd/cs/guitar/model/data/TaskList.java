/*******************************************************************************
 *    Copyright (c) 2019 Jonathan A. Saddler
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
 *         &lt;element name="Widget" type="{}Widget" maxOccurs="unbounded"/>
 *         &lt;element name="Exclusion" type="{}Exclusion" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Atomic" type="{}Atomic" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Order" type="{}Order" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Required" type="{}Required" minOccurs="0"/>
 *         &lt;element name="Repeat" type="{}Repeat" minOccurs="0"/>
 *         &lt;element name="MinInteractions" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "widget",
    "required",
    "exclusion",
    "order",
    "repeat",
    "stop",
    "atomic",
    "minInteractions"
})
@XmlRootElement(name = "TaskList")
public class TaskList {

    @XmlElement(name = "Widget", required = true)
    protected List<Widget> widget;
    @XmlElement(name = "Required")
    protected List<Required> required;
    @XmlElement(name = "Exclusion")
    protected List<Exclusion> exclusion;
    @XmlElement(name = "Order")
    protected List<Order> order;
    @XmlElement(name = "Repeat")
    protected List<Repeat> repeat;
    @XmlElement(name = "Atomic")
    protected List<Atomic> atomic;
    @XmlElement(name = "Stop")
    protected List<Stop> stop;
    @XmlElement(name = "MinInteractions")
    protected String minInteractions;

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
    public List<Widget> getWidget() {
        if (widget == null) {
            widget = new ArrayList<Widget>();
        }
        return this.widget;
    }

    /**
     * Gets the value of the exclusion property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the exclusion property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExclusion().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Exclusion }
     *
     *
     */
    public List<Exclusion> getExclusion() {
        if (exclusion == null) {
            exclusion = new ArrayList<Exclusion>();
        }
        return this.exclusion;
    }

    /**
     * Gets the value of the atomic property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the atomic property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAtomic().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Atomic }
     *
     *
     */
    public List<Atomic> getAtomic() {
        if (atomic == null) {
            atomic = new ArrayList<Atomic>();
        }
        return this.atomic;
    }

    /**
     * Gets the value of the order property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the order property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOrder().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Order }
     *
     *
     */
    public List<Order> getOrder() {
        if (order == null) {
            order = new ArrayList<Order>();
        }
        return this.order;
    }

    /**
     * Gets the value of the required property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the order property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRequired().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Required }
     *
     */
    public List<Required> getRequired()
    {
    	if (required == null) {
            required = new ArrayList<Required>();
        }
        return this.required;
    }

    /**
     * Gets the value of the repeat property.
     */
    public List<Repeat> getRepeat() {
    	if(repeat == null) {
    		repeat = new ArrayList<Repeat>();
    	}
    	return this.repeat;
    }

    public List<Stop> getStop() {
    	if(stop == null)
    		stop = new ArrayList<Stop>();
    	return this.stop;
    }
    /**
     * Gets the value of the minInteractions property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getMinInteractions() {
        return minInteractions;
    }

    /**
     * Sets the value of the minInteractions property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setMinInteractions(String value) {
        this.minInteractions = value;
    }


    public String toString()
    {
    	String toReturn = "";
    	toReturn += "WIDGETS" + "\n";
    	if(widget != null) toReturn += widget + "\n";
    	toReturn += "REQUIRED" + "\n";
    	if(required != null) toReturn += required + "\n";
    	toReturn += "EXCLUSION" + "\n";
    	if(exclusion != null) toReturn += exclusion + "\n";
    	toReturn += "ORDER" + "\n";
    	if(order != null) toReturn += order + "\n";
    	toReturn += "ATOMIC" + "\n";
    	if(atomic != null) toReturn += atomic + "\n";
    	toReturn += "REPEAT" + "\n";
    	if(repeat != null) toReturn += repeat + "\n";
    	toReturn += "STOP" + "\n";
    	if(stop != null) toReturn += stop;
    	return toReturn;
    }
}
