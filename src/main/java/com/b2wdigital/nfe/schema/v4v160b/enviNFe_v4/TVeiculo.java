//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.12.18 at 03:31:10 PM BRST 
//


package com.b2wdigital.nfe.schema.v4v160b.enviNFe_v4;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Tipo Dados do Veículo
 * 
 * <p>Java class for TVeiculo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TVeiculo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="placa">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;whiteSpace value="preserve"/>
 *               &lt;pattern value="[A-Z]{2,3}[0-9]{4}|[A-Z]{3,4}[0-9]{3}|[A-Z0-9]{7}"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="UF" type="{http://www.portalfiscal.inf.br/nfe}TUf"/>
 *         &lt;element name="RNTC" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.portalfiscal.inf.br/nfe}TString">
 *               &lt;minLength value="1"/>
 *               &lt;maxLength value="20"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TVeiculo", namespace = "http://www.portalfiscal.inf.br/nfe", propOrder = {
    "placa",
    "uf",
    "rntc"
})
public class TVeiculo {

    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String placa;
    @XmlElement(name = "UF", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    @XmlSchemaType(name = "string")
    protected TUf uf;
    @XmlElement(name = "RNTC", namespace = "http://www.portalfiscal.inf.br/nfe")
    protected String rntc;

    /**
     * Gets the value of the placa property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlaca() {
        return placa;
    }

    /**
     * Sets the value of the placa property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlaca(String value) {
        this.placa = value;
    }

    /**
     * Gets the value of the uf property.
     * 
     * @return
     *     possible object is
     *     {@link TUf }
     *     
     */
    public TUf getUF() {
        return uf;
    }

    /**
     * Sets the value of the uf property.
     * 
     * @param value
     *     allowed object is
     *     {@link TUf }
     *     
     */
    public void setUF(TUf value) {
        this.uf = value;
    }

    /**
     * Gets the value of the rntc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRNTC() {
        return rntc;
    }

    /**
     * Sets the value of the rntc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRNTC(String value) {
        this.rntc = value;
    }

}
