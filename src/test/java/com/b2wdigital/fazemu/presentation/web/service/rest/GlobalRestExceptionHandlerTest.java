package com.b2wdigital.fazemu.presentation.web.service.rest;

import com.b2wdigital.fazemu.exception.NotFoundException;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dailton.almeida
 */
public class GlobalRestExceptionHandlerTest {
    private GlobalRestExceptionHandler instance;
    private final String message = "_msg";
    
    public GlobalRestExceptionHandlerTest() {
    }
    
    @Before
    public void setUp() {
        instance = new GlobalRestExceptionHandler();
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testHandleNotFoundException() {
        NotFoundException e = new NotFoundException(message);

        //call
        Map<String, String> result = instance.handleNotFoundException(e);

        assertEquals(2, result.size());
        assertEquals("404", result.get("errorCode"));
        assertEquals(message, result.get("message"));
    }
    
}
