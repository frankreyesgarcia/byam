//
// This file was generated by the Eclipse Implementation of JAXB, v2.3.6 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.08.18 at 08:13:47 AM GMT 
//

package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

/**
 * <p>Java class for MovementTax complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MovementTax"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="TaxType" type="{urn:OECD:StandardAuditFile-Tax:PT_1.02_01}SAFTPTMovementTaxType"/&gt;
 *         &lt;element ref="{urn:OECD:StandardAuditFile-Tax:PT_1.02_01}TaxCountryRegion"/&gt;
 *         &lt;element name="TaxCode" type="{urn:OECD:StandardAuditFile-Tax:PT_1.02_01}SAFTPTMovementTaxCode"/&gt;
 *         &lt;element ref="{urn:OECD:StandardAuditFile-Tax:PT_1.02_01}TaxPercentage"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MovementTax", propOrder = {
    "taxType",
    "taxCountryRegion",
    "taxCode",
    "taxPercentage"
})
public class MovementTax implements ToString2
{

    @XmlElement(name = "TaxType", required = true)
    @XmlSchemaType(name = "string")
    protected SAFTPTMovementTaxType taxType;
    @XmlElement(name = "TaxCountryRegion", required = true)
    protected String taxCountryRegion;
    @XmlElement(name = "TaxCode", required = true)
    protected String taxCode;
    @XmlElement(name = "TaxPercentage", required = true)
    protected BigDecimal taxPercentage;

    /**
     * Gets the value of the taxType property.
     * 
     * @return
     *     possible object is
     *     {@link SAFTPTMovementTaxType }
     *     
     */
    public SAFTPTMovementTaxType getTaxType() {
        return taxType;
    }

    /**
     * Sets the value of the taxType property.
     * 
     * @param value
     *     allowed object is
     *     {@link SAFTPTMovementTaxType }
     *     
     */
    public void setTaxType(SAFTPTMovementTaxType value) {
        this.taxType = value;
    }

    /**
     * Gets the value of the taxCountryRegion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTaxCountryRegion() {
        return taxCountryRegion;
    }

    /**
     * Sets the value of the taxCountryRegion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTaxCountryRegion(String value) {
        this.taxCountryRegion = value;
    }

    /**
     * Gets the value of the taxCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTaxCode() {
        return taxCode;
    }

    /**
     * Sets the value of the taxCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTaxCode(String value) {
        this.taxCode = value;
    }

    /**
     * Gets the value of the taxPercentage property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getTaxPercentage() {
        return taxPercentage;
    }

    /**
     * Sets the value of the taxPercentage property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setTaxPercentage(BigDecimal value) {
        this.taxPercentage = value;
    }

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = JAXBToStringStrategy.INSTANCE;
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, strategy);
        return buffer.toString();
    }

    @Override
    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        strategy.appendStart(locator, this, buffer);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, this, buffer);
        return buffer;
    }

    @Override
    public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        {
            SAFTPTMovementTaxType theTaxType;
            theTaxType = this.getTaxType();
            strategy.appendField(locator, this, "taxType", buffer, theTaxType, (this.taxType != null));
        }
        {
            String theTaxCountryRegion;
            theTaxCountryRegion = this.getTaxCountryRegion();
            strategy.appendField(locator, this, "taxCountryRegion", buffer, theTaxCountryRegion, (this.taxCountryRegion != null));
        }
        {
            String theTaxCode;
            theTaxCode = this.getTaxCode();
            strategy.appendField(locator, this, "taxCode", buffer, theTaxCode, (this.taxCode != null));
        }
        {
            BigDecimal theTaxPercentage;
            theTaxPercentage = this.getTaxPercentage();
            strategy.appendField(locator, this, "taxPercentage", buffer, theTaxPercentage, (this.taxPercentage != null));
        }
        return buffer;
    }

}