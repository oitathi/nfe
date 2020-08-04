package com.b2wdigital.fazemu.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author dailton.almeida
 */
public class JsonDateDeserializerTest {
    @InjectMocks private JsonDateDeserializer instance;
    @Mock private JsonParser jp;
    @Mock private DeserializationContext dc;
    
    public JsonDateDeserializerTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testDeserializeNull() throws Exception {
        when(jp.getText()).thenReturn(null);

        Date result = instance.deserialize(jp, dc);

        verify(jp).getText();
        assertNull(result);
    }
    @Test
    public void testDeserializeBlank() throws Exception {
        when(jp.getText()).thenReturn("");

        Date result = instance.deserialize(jp, dc);

        verify(jp).getText();
        assertNull(result);
    }
    @Test
    public void testDeserializeDate() throws Exception {
        Calendar cal = GregorianCalendar.getInstance(FazemuUtils.LOCALE);
        cal.clear();
        cal.set(1910, Calendar.SEPTEMBER, 1, 23, 59, 58);

        when(jp.getText()).thenReturn("01/09/1910 23:59:58");

        Date expResult = cal.getTime();
        Date result = instance.deserialize(jp, dc);

        verify(jp).getText();
        assertEquals(expResult, result);
    }
    @Test
    public void testDeserializeParseException() throws Exception {
        when(jp.getText()).thenReturn("_not_parseable_string");

        try {
            instance.deserialize(jp, dc);
        } catch (JsonProcessingException e) {
            verify(jp).getText();
            assertTrue(e.getCause() instanceof ParseException);
            return;
        }

        fail();
    }
    
}
