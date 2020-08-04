//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.02.11 at 11:51:27 AM BRST 
//


package com.b2wdigital.nfe.schema.v4v160b.epec.e110140_v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
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
 *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}descEvento"/>
 *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}cOrgaoAutor"/>
 *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}tpAutor"/>
 *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}verAplic"/>
 *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}dhEmi"/>
 *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}tpNF"/>
 *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}IE"/>
 *         &lt;element name="dest">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{http://www.portalfiscal.inf.br/nfe}UF"/>
 *                   &lt;choice>
 *                     &lt;element name="CNPJ" type="{http://www.portalfiscal.inf.br/nfe}TCnpj"/>
 *                     &lt;element name="CPF" type="{http://www.portalfiscal.inf.br/nfe}TCpf"/>
 *                     &lt;element name="idEstrangeiro">
 *                       &lt;simpleType>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                           &lt;whiteSpace value="preserve"/>
 *                           &lt;pattern value="([!-ÿ]{0}|[!-ÿ]{5,20})?"/>
 *                         &lt;/restriction>
 *                       &lt;/simpleType>
 *                     &lt;/element>
 *                   &lt;/choice>
 *                   &lt;element ref="{http://www.portalfiscal.inf.br/nfe}IE" minOccurs="0"/>
 *                   &lt;element ref="{http://www.portalfiscal.inf.br/nfe}vNF"/>
 *                   &lt;element ref="{http://www.portalfiscal.inf.br/nfe}vICMS"/>
 *                   &lt;element ref="{http://www.portalfiscal.inf.br/nfe}vST"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="versao" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;whiteSpace value="preserve"/>
 *             &lt;enumeration value="1.00"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "descEvento",
    "cOrgaoAutor",
    "tpAutor",
    "verAplic",
    "dhEmi",
    "tpNF",
    "ie",
    "dest"
})
@XmlRootElement(name = "detEvento", namespace = "http://www.portalfiscal.inf.br/nfe")
public class DetEvento {

    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String descEvento;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String cOrgaoAutor;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String tpAutor;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String verAplic;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String dhEmi;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String tpNF;
    @XmlElement(name = "IE", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected String ie;
    @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
    protected DetEvento.Dest dest;
    @XmlAttribute(name = "versao", required = true)
    protected String versao;

    /**
     * Gets the value of the descEvento property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescEvento() {
        return descEvento;
    }

    /**
     * Sets the value of the descEvento property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescEvento(String value) {
        this.descEvento = value;
    }

    /**
     * Gets the value of the cOrgaoAutor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCOrgaoAutor() {
        return cOrgaoAutor;
    }

    /**
     * Sets the value of the cOrgaoAutor property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCOrgaoAutor(String value) {
        this.cOrgaoAutor = value;
    }

    /**
     * Gets the value of the tpAutor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTpAutor() {
        return tpAutor;
    }

    /**
     * Sets the value of the tpAutor property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTpAutor(String value) {
        this.tpAutor = value;
    }

    /**
     * Gets the value of the verAplic property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVerAplic() {
        return verAplic;
    }

    /**
     * Sets the value of the verAplic property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVerAplic(String value) {
        this.verAplic = value;
    }

    /**
     * Gets the value of the dhEmi property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDhEmi() {
        return dhEmi;
    }

    /**
     * Sets the value of the dhEmi property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDhEmi(String value) {
        this.dhEmi = value;
    }

    /**
     * Gets the value of the tpNF property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTpNF() {
        return tpNF;
    }

    /**
     * Sets the value of the tpNF property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTpNF(String value) {
        this.tpNF = value;
    }

    /**
     * Gets the value of the ie property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIE() {
        return ie;
    }

    /**
     * Sets the value of the ie property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIE(String value) {
        this.ie = value;
    }

    /**
     * Gets the value of the dest property.
     * 
     * @return
     *     possible object is
     *     {@link DetEvento.Dest }
     *     
     */
    public DetEvento.Dest getDest() {
        return dest;
    }

    /**
     * Sets the value of the dest property.
     * 
     * @param value
     *     allowed object is
     *     {@link DetEvento.Dest }
     *     
     */
    public void setDest(DetEvento.Dest value) {
        this.dest = value;
    }

    /**
     * Gets the value of the versao property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersao() {
        return versao;
    }

    /**
     * Sets the value of the versao property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersao(String value) {
        this.versao = value;
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
     *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}UF"/>
     *         &lt;choice>
     *           &lt;element name="CNPJ" type="{http://www.portalfiscal.inf.br/nfe}TCnpj"/>
     *           &lt;element name="CPF" type="{http://www.portalfiscal.inf.br/nfe}TCpf"/>
     *           &lt;element name="idEstrangeiro">
     *             &lt;simpleType>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *                 &lt;whiteSpace value="preserve"/>
     *                 &lt;pattern value="([!-ÿ]{0}|[!-ÿ]{5,20})?"/>
     *               &lt;/restriction>
     *             &lt;/simpleType>
     *           &lt;/element>
     *         &lt;/choice>
     *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}IE" minOccurs="0"/>
     *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}vNF"/>
     *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}vICMS"/>
     *         &lt;element ref="{http://www.portalfiscal.inf.br/nfe}vST"/>
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
        "uf",
        "cnpj",
        "cpf",
        "idEstrangeiro",
        "ie",
        "vnf",
        "vicms",
        "vst"
    })
    public static class Dest {

        @XmlElement(name = "UF", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
        @XmlSchemaType(name = "string")
        protected TUf uf;
        @XmlElement(name = "CNPJ", namespace = "http://www.portalfiscal.inf.br/nfe")
        protected String cnpj;
        @XmlElement(name = "CPF", namespace = "http://www.portalfiscal.inf.br/nfe")
        protected String cpf;
        @XmlElement(namespace = "http://www.portalfiscal.inf.br/nfe")
        protected String idEstrangeiro;
        @XmlElement(name = "IE", namespace = "http://www.portalfiscal.inf.br/nfe")
        protected String ie;
        @XmlElement(name = "vNF", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
        protected String vnf;
        @XmlElement(name = "vICMS", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
        protected String vicms;
        @XmlElement(name = "vST", namespace = "http://www.portalfiscal.inf.br/nfe", required = true)
        protected String vst;

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
         * Gets the value of the cnpj property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCNPJ() {
            return cnpj;
        }

        /**
         * Sets the value of the cnpj property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCNPJ(String value) {
            this.cnpj = value;
        }

        /**
         * Gets the value of the cpf property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCPF() {
            return cpf;
        }

        /**
         * Sets the value of the cpf property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCPF(String value) {
            this.cpf = value;
        }

        /**
         * Gets the value of the idEstrangeiro property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getIdEstrangeiro() {
            return idEstrangeiro;
        }

        /**
         * Sets the value of the idEstrangeiro property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setIdEstrangeiro(String value) {
            this.idEstrangeiro = value;
        }

        /**
         * Gets the value of the ie property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getIE() {
            return ie;
        }

        /**
         * Sets the value of the ie property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setIE(String value) {
            this.ie = value;
        }

        /**
         * Gets the value of the vnf property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getVNF() {
            return vnf;
        }

        /**
         * Sets the value of the vnf property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setVNF(String value) {
            this.vnf = value;
        }

        /**
         * Gets the value of the vicms property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getVICMS() {
            return vicms;
        }

        /**
         * Sets the value of the vicms property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setVICMS(String value) {
            this.vicms = value;
        }

        /**
         * Gets the value of the vst property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getVST() {
            return vst;
        }

        /**
         * Sets the value of the vst property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setVST(String value) {
            this.vst = value;
        }

    }

}
