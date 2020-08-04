package com.b2wdigital.fazemu.utils;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author dailton.almeida
 */
public class BlobInserter {

    public static void main(String[] args) throws Exception {
        System.out.println("Iniciando upload de informacoes... ");

        // 1. Selecionar a conexão
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//dbdocfisst.back.b2w:1521/SRV_DOCFIS", "DOCFIS", "DOCFIS");				//HML
//        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//dbdocfpr.back.b2w:1521/SRV_DOCFIS", "docfis", "docfispwd1z2c4b"); 		//PRD
        conn.setAutoCommit(true);

        // 2. Indicar o caminho do arquivo na máquina local
        FileInputStream fis = new FileInputStream("/home/thiagosanti/Área de Trabalho/Projetos/Fazemu/certificados/sellers/Loja_do_Mecanico (Gurgelmix)/GURGELMIX_MAQUINAS_E_FERRAMENTAS_S_A_29302348000115_1582818575449887000-2.pfx");
        //FileInputStream fis = new FileInputStream(args[0]);

//	PreparedStatement ps = conn.prepareStatement("insert into emissor_raiz_cdigital values(?, ?, ?, ?, ?, ?, ?)");
	PreparedStatement ps = conn.prepareStatement("update emissor_raiz_cdigital set edig_cdigital = ?, edig_datahora = sysdate where edig_id_cdigital = ?");
//	PreparedStatement ps = conn.prepareStatement("update parametros_infra_blob set paib_valor = ?, paib_datahora = sysdate where paib_id_parametro = ? and paib_tp_doc_fiscal = ? ");
//        PreparedStatement ps = conn.prepareStatement("update emissor_raiz_logo set elog_logo = ?, elog_datahora = sysdate where elog_id_emissor_raiz = ? and elog_id_logo = ?");

        ps.setBinaryStream(1, fis);
        //ps.setLong(2, Long.parseLong(args[1]));

        // 3. Indicar o emissor raiz
        //ps.setString(2, args[1]);
        //ps.setString(2, "5886614");
        //ps.setString(2, "776574");
        ps.setString(2, "366");
//        ps.setString(3, "NFSE");

        // 3. Indicar o logo
        //ps.setString(3, args[2]);        
        //ps.setString(3, "DIRECT");  
        //ps.setString(3, "SUBA"); 
//        ps.setString(3, "AMED");

        ResultSet rs = ps.executeQuery();

        rs.close();
        ps.close();

        System.out.println("Finalizando upload de informacoes.");
    }

}
