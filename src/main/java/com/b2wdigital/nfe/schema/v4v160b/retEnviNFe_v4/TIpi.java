//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.12.18 at 03:31:22 PM BRST 
//


package com.b2wdigital.nfe.schema.v4v160b.retEnviNFe_v4;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Tipo: Dados do IPI
 * 
 * <p>Java class for TIpi complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TIpi">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CNPJProd" type="{http://www.portalfiscal.inf.br/nfe}TCnpj" minOccurs="0"/>
 *         &lt;element name="cSelo" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.portalfiscal.inf.br/nfe}TString">
 *               &lt;minLength value="1"/>
 *               &lt;maxLength value="60"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="qSelo" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;whiteSpace value="preserve"/>
 *               &lt;pattern value="[0-9]{1,12}"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="cEnq">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.portalfiscal.inf.br/nfe}TString">
 *               &lt;minLength value="1"/>
 *               &lt;maxLength value="3"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;choice>
 *           &lt;element name="IPITrib">
 *             &lt;complexType>
 *               &lt;complexContent>
 *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                   &lt;sequence>
 *                     &lt;element name="CST">
 *                       &lt;simpleType>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                           &lt;whiteSpace value="preserve"/>
 *                           &lt;enumeration value="00"/>
 *                           &lt;enumeration value="49"/>
 *                           &lt;enumeration value="50"/>
 *                           &lt;enumeration value="99"/>
 *                         &lt;/restriction>
 *                       &lt;/simpleType>
 *                     &lt;/element>
 *                     &lt;choice>
 *                       &lt;sequence>
 *                         &lt;element name="vBC" type="{http://www.portalfiscal.inf.br/nfe}TDec_1302"/>
 *                         &lt;element name="pIPI" type="{http://www.portalfiscal.inf.br/nfe}TDec_0302a04"/>
 *                       &lt;/sequence>
 *                       &lt;sequence>
 *                         &lt;element name="qUnid" type="{http://www.portalfiscal.inf.br/nfe}TDec_1204v"/>
 *                         &lt;element name="vUnid" type="{http://www.portalfiscal.inf.br/nfe}TDec_1104"/>
 *                       &lt;/sequence>
 *                     &lt;/choice>
 *                     &lt;element name="vIPI" type="{http://www.portalfiscal.inf.br/nfe}TDec_1302"/>
 *                   &lt;/sequence>
 *                 &lt;/restriction>
 *               &lt;/complexContent>
 *             &lt;/complexType>
 *           &lt;/element>
 *           &lt;element name="IPINT">
 *             &lt;complexType>
 *               &lt;complexContent>
 *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                   &lt;sequence>
 *                     &lt;element name="CST">
 *                       &lt;simpleType>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                           &lt;whiteSpace value="preserve"/>
 *                           &lt;enumeration value="01"/>
 *                           &lt;enumeration value="02"/>
 *                           &lt;enumeration value="03"/>
 *                           &lt;enumeration value="04"/>
 *                           &lt;enumeration value="05"/>
 *                           &lt;enumeration value="51"/>
 *                           &lt;enumeration value="52"/>
 *                           &lt;enumeration value="53"/>
 *                           &lt;enumeration value="54"/>
 *                           &lt;enumeration value="55"/>
 *                         &lt;/restriction>
 *                       &lt;/simpleType>
 *                     &lt;/element>
 *                   &lt;/sequence>
 *                 &lt;/restriction>
 *               &lt;/complexContent>
 *             &lt;/complexType>
 *           &lt;/element>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TIpi", namespace = "http://www.portalfiscal.inf.br/nfe", propOrder = {
    "cnpjProd",
    "cSelo",
    "qSelo",
    "cEnq",
    "ipiTrib",
    "ipint"
})
public class TIpi {

    @XmlElement(name = "CNPJProd", namespace = "http://www.portalfiscal.inf.br/nfe")
    protected String cnpjProd;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe")
    protected String cSelo;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe")
    protected String qSelo;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String cEnq;
    @XmlElement(name = "IPITrib", namespace = "http://www.portalfiscal.inf.br/nfe")
    protected TIpi.IPITrib ipiTrib;
    @XmlElement(name = "IPINT", namespace = "http://www.portalfiscal.inf.br/nfe")
    protected TIpi.IPINT ipint;

    /**
     * Gets the value of the cnpjProd property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCNPJProd() {
        return cnpjProd;
    }

    /**
     * Sets the value of the cnpjProd property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCNPJProd(String value) {
        this.cnpjProd = value;
    }

    /**
     * Gets the value of the cSelo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCSelo() {
        return cSelo;
    }

    /**
     * Sets the value of the cSelo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCSelo(String value) {
        this.cSelo = value;
    }

    /**
     * Gets the value of the qSelo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getQSelo() {
        return qSelo;
    }

    /**
     * Sets the value of the qSelo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setQSelo(String value) {
        this.qSelo = value;
    }

    /**
     * Gets the value of the cEnq property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCEnq() {
        return cEnq;
    }

    /**
     * Sets the value of the cEnq property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCEnq(String value) {
        this.cEnq = value;
    }

    /**
     * Gets the value of the ipiTrib property.
     * 
     * @return
     *     possible object is
     *     {@link TIpi.IPITrib }
     *     
     */
    public TIpi.IPITrib getIPITrib() {
        return ipiTrib;
    }

    /**
     * Sets the value of the ipiTrib property.
     * 
     * @param value
     *     allowed object is
     *     {@link TIpi.IPITrib }
     *     
     */
    public void setIPITrib(TIpi.IPITrib value) {
        this.ipiTrib = value;
    }

    /**
     * Gets the value of the ipint property.
     * 
     * @return
     *     possible object is
     *     {@link TIpi.IPINT }
     *     
     */
    public TIpi.IPINT getIPINT() {
        return ipint;
    }

    /**
     * Sets the value of the ipint property.
     * 
     * @param value
     *     allowed object is
     *     {@link TIpi.IPINT }
     *     
     */
    public void setIPINT(TIpi.IPINT value) {
        this.ipint = value;
    }


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
     *         &lt;element name="CST">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;whiteSpace value="preserve"/>
     *               &lt;enumeration value="01"/>
     *               &lt;enumeration value="02"/>
     *               &lt;enumeration value="03"/>
     *               &lt;enumeration value="04"/>
     *               &lt;enumeration value="05"/>
     *               &lt;enumeration value="51"/>
     *               &lt;enumeration value="52"/>
     *               &lt;enumeration value="53"/>
     *               &lt;enumeration value="54"/>
     *               &lt;enumeration value="55"/>
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
    @XmlType(name = "", propOrder = {
        "cst"
    })
    public static class IPINT {

        @XmlElement(name = "CST", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
        protected String cst;

        /**
         * Gets the value of the cst property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCST() {
            return cst;
        }

        /**
         * Sets the value of the cst property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCST(String value) {
            this.cst = value;
        }

    }


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
     *         &lt;element name="CST">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;whiteSpace value="preserve"/>
     *               &lt;enumeration value="00"/>
     *               &lt;enumeration value="49"/>
     *               &lt;enumeration value="50"/>
     *               &lt;enumeration value="99"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *         &lt;choice>
     *           &lt;sequence>
     *             &lt;element name="vBC" type="{http://www.portalfiscal.inf.br/nfe}TDec_1302"/>
     *             &lt;element name="pIPI" type="{http://www.portalfiscal.inf.br/nfe}TDec_0302a04"/>
     *           &lt;/sequence>
     *           &lt;sequence>
     *             &lt;element name="qUnid" type="{http://www.portalfiscal.inf.br/nfe}TDec_1204v"/>
     *             &lt;element name="vUnid" type="{http://www.portalfiscal.inf.br/nfe}TDec_1104"/>
     *           &lt;/sequence>
     *         &lt;/choice>
     *         &lt;element name="vIPI" type="{http://www.portalfiscal.inf.br/nfe}TDec_1302"/>
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
        "cst",
        "vbc",
        "pipi",
        "qUnid",
        "vUnid",
        "vipi"
    })
    public static class IPITrib {

        @XmlElement(name = "CST", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
        protected String cst;
        @XmlElement(name = "vBC", namespace = "http://www.portalfiscal.inf.br/nfe")
        protected String vbc;
        @XmlElement(name = "pIPI", namespace = "http://www.portalfiscal.inf.br/nfe")
        protected String pipi;
        @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe")
        protected String qUnid;
        @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe")
        protected String vUnid;
        @XmlElement(name = "vIPI", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
        protected String vipi;

        /**
         * Gets the value of the cst property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCST() {
            return cst;
        }

        /**
         * Sets the value of the cst property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCST(String value) {
            this.cst = value;
        }

        /**
         * Gets the value of the vbc property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getVBC() {
            return vbc;
        }

        /**
         * Sets the value of the vbc property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setVBC(String value) {
            this.vbc = value;
        }

        /**
         * Gets the value of the pipi property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getPIPI() {
            return pipi;
        }

        /**
         * Sets the value of the pipi property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setPIPI(String value) {
            this.pipi = value;
        }

        /**
         * Gets the value of the qUnid property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getQUnid() {
            return qUnid;
        }

        /**
         * Sets the value of the qUnid property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setQUnid(String value) {
            this.qUnid = value;
        }

        /**
         * Gets the value of the vUnid property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getVUnid() {
            return vUnid;
        }

        /**
         * Sets the value of the vUnid property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setVUnid(String value) {
            this.vUnid = value;
        }

        /**
         * Gets the value of the vipi property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getVIPI() {
            return vipi;
        }

        /**
         * Sets the value of the vipi property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setVIPI(String value) {
            this.vipi = value;
        }

    }

}
