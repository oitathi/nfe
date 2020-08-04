package com.b2wdigital.fazemu.service.impl;

import com.b2wdigital.fazemu.business.repository.DocumentoFiscalRepository;
import com.b2wdigital.fazemu.domain.ResumoLote;
import com.b2wdigital.fazemu.enumeration.ServicosEnum;

import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author dailton.almeida
 */
public class FecharEnviarLoteServiceImplTest {
    @InjectMocks private FecharEnviarLoteServiceImpl instance;
    @Mock private DocumentoFiscalRepository documentoFiscalRepository;
    private final Long idLote = 2L;
    private final String versao = "_versao";
    private static final long idDocFiscal0 = 23L;
    private static final long idDocFiscal1 = 29L;
    
    public FecharEnviarLoteServiceImplTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testMontarEnviNFeXML() {
        ResumoLote lote = ResumoLote.build(idLote);
        lote.setVersao(versao);
        lote.setIdDocFiscalList(Arrays.asList(idDocFiscal0, idDocFiscal1));
        lote.setServico(ServicosEnum.AUTORIZACAO_NFE.name());

        when(documentoFiscalRepository.getXmlByIdLoteAndIdDocFiscal(idLote, idDocFiscal0)).thenReturn("<?xml version=\"1.0\" encoding=\"UTF-8\"?><t>pqr</t>");
        when(documentoFiscalRepository.getXmlByIdLoteAndIdDocFiscal(idLote, idDocFiscal1)).thenReturn("<t>xyz</t>");

        String expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><enviNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"_versao\"><idLote>2</idLote><indSinc>0</indSinc><t>pqr</t><t>xyz</t></enviNFe>";
        
        //call
        String result = instance.montarEnvelopeXML(lote);

        verify(documentoFiscalRepository).getXmlByIdLoteAndIdDocFiscal(idLote, idDocFiscal0);
        verify(documentoFiscalRepository).getXmlByIdLoteAndIdDocFiscal(idLote, idDocFiscal1);
        verifyNoMoreInteractions(documentoFiscalRepository);
        assertEquals(expResult, result);
    }
    
}
