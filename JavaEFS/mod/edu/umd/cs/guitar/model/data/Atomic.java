

package edu.umd.cs.guitar.model.data;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Atomic complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Atomic">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Widget" type="{}AtomicGroup" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Atomic", propOrder = {
    "atomicGroup"
})
public class Atomic {

    @XmlElement(name = "AtomicGroup", required = true)
    protected List<AtomicGroup> atomicGroup;

    /**
     * Gets the value of the atomicGroup property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the orderGroup property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAtomicGroup().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AtomicGroup }
     *
     *
     */
    public List<AtomicGroup> getAtomicGroup()
    {
        if (atomicGroup == null)
            atomicGroup = new ArrayList<AtomicGroup>();

        return this.atomicGroup;
    }

    
    public static Atomic fromOrder(Order o, ObjectFactory fact)
    {
    	Atomic toReturn = fact.createAtomic();
    	for(int i = 0; i < o.getOrderGroup().size(); i++) {
    		AtomicGroup next = fact.createAtomicGroup();
    		OrderGroup nextO = o.getOrderGroup().get(i);
    		for(int j = 0; j < nextO.getWidget().size(); j++)
    			next.getWidget().add(nextO.getWidget().get(j));
    		toReturn.getAtomicGroup().add(next);
    	}
    	return toReturn;
    }
    public String toString()
    {
    	if(atomicGroup == null)
    		return "";

    	String toReturn = "";
    	for(int i = 0; i < atomicGroup.size(); i++)
    		toReturn += (i+1) + "-\n" + atomicGroup.get(i) + "\n";

    	return toReturn;
    }

}
