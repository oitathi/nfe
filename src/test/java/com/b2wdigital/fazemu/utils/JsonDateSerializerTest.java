package com.b2wdigital.fazemu.utils;

import static org.mockito.Mockito.verify;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 *
 * @author dailton.almeida
 */
public class JsonDateSerializerTest {
    @InjectMocks private JsonDateSerializer instance;
    @Mock private JsonGenerator jg;
    @Mock private SerializerProvider sp;
    
    public JsonDateSerializerTest() {
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testSerializeNull() throws Exception {
        instance.serialize(null, jg, sp);
        verify(jg).writeNull();
    }
    @Test
    public void testSerializeNotNull() throws Exception {
        Calendar cal = GregorianCalendar.getInstance(FazemuUtils.LOCALE);
        cal.clear();
        cal.set(1910, Calendar.SEPTEMBER, 1, 23, 59, 58);

        instance.serialize(cal.getTime(), jg, sp);

        verify(jg).writeString("01/09/1910 23:59:58");
    }
    
}
