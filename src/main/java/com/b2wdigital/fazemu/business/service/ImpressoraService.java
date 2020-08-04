package com.b2wdigital.fazemu.business.service;

import java.util.List;

import com.b2wdigital.fazemu.domain.Impressora;

public interface ImpressoraService {
	List<Impressora> listByFiltros(String nome, String local, String ip, String marca, String modelo, String situacao);
	void insert(Impressora impressora);
	void update(Impressora impressora);
}
