package com.b2wdigital.fazemu.form;

import com.b2wdigital.nfe.schema.v4v160b.consReciNFe_v4.TNfeProc;
import com.b2wdigital.nfe.schema.v4v160b.distdfe.resNFe_v1.ResNFe;

import lombok.Data;

@Data
public class NfeCompletaOuResumoForm {
	
	private TNfeProc completa;
	private ResNFe resumida;
	private String tipoNota;
	
	

}
