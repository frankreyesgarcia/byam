package com.premiumminds.billy.portugal.services.export.saftpt.v1_03_01.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.lang.DefaultToStringStrategy;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "References", propOrder = {
    "reference",
    "reason"
})
public class References implements ToString2
{

    @XmlElement(name = "Reference")
    protected String reference;
    @XmlElement(name = "Reason")
    protected String reason;

    public String getReference() {
        return reference;
    }

    public void setReference(String value) {
        this.reference = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new DefaultToStringStrategy();
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
            String theReference;
            theReference = this.getReference();
            strategy.appendField(locator, this, "reference", buffer, theReference, (this.reference!= null));
        }
        {
            String theReason;
            theReason = this.getReason();
            strategy.appendField(locator, this, "reason", buffer, theReason, (this.reason!= null));
        }
        return buffer;
    }

}