package com.b2wdigital.fazemu.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

public class TxtReader {
	
	
	/** 
	 * Monta lista de chaves de acesso baseado no arquivo carregado
	 * 
	 * @param bais
	 * @return
	 * @throws IOException
	 */
	public static Set<String> getListaChaveAcesso(ByteArrayInputStream bais) throws IOException {
		Set<String> listaChavesAcesso = new HashSet<String>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(bais))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (NumberUtils.isCreatable(line))
					listaChavesAcesso.add(line);
			}
		}

		return listaChavesAcesso;
	}
	
	
}
