//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.06.02 at 10:12:18 AM PDT 
//


package org.apache.geronimo.tomcat.model;

import java.util.Map;
import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.namespace.QName;

import org.apache.catalina.Realm;
import org.apache.catalina.Executor;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Option;


/**
 * <p>Java class for RealmType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="RealmType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="className" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RealmType")
public class RealmType {

    @XmlAttribute
    protected String className;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the className property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the value of the className property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setClassName(String value) {
        this.className = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     *
     * <p>
     * the map is keyed by the name of the attribute and
     * the value is the string value of the attribute.
     *
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     *
     *
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

    public Realm getRealm(ClassLoader cl) throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();

        for (Map.Entry<QName, String> entry: otherAttributes.entrySet()) {
            String name = entry.getKey().getLocalPart();
            properties.put(name, entry.getValue());
        }
        ObjectRecipe recipe = new ObjectRecipe(className, properties);
        recipe.allow(Option.IGNORE_MISSING_PROPERTIES);
        Realm realm = (Realm) recipe.create(cl);
        return realm;
    }
}
