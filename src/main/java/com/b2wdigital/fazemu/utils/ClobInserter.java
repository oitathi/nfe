package com.b2wdigital.fazemu.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.io.IOUtils;

public class ClobInserter {

    public static void main(String[] args) throws Exception {
        System.out.println("Iniciando upload de informacoes... ");

        // 1. Selecionar a conexão
//        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//dbdocfisst.back.b2w:1521/SRV_DOCFIS", "DOCFIS", "DOCFIS");				//HML
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//dbdocfpr.back.b2w:1521/SRV_DOCFIS", "docfis", "docfispwd1z2c4b"); 		//PRD
        conn.setAutoCommit(true);

        // 2. Indicar o caminho do arquivo na máquina local
        String pathXml = "/home/thiagosanti/Downloads/NFe35200600776574002280550010088859561111368220.xml";

        InputStream isFromFile = new FileInputStream(new File(pathXml));
        String xml = IOUtils.toString(isFromFile, StandardCharsets.UTF_8.name());
        
        String sql = "update documento_clob set docl_clob = ?, docl_datahora = sysdate where docl_id_clob = ?";
        
	PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, xml);
        ps.setString(2, "57541308");

        ResultSet rs = ps.executeQuery();

        rs.close();
        ps.close();

        System.out.println("Finalizando upload de informacoes.");
    }

}
