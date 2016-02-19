package org.meresco.lucene.numerate;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class UriUnumerateTest {

    private static final String TESTDIR = "urienumeratetest";
    private UriEnumerate enumerate;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        this.enumerate = new UriEnumerate(TESTDIR, 2);
    }

    @After
    public void tearDown() throws Exception {
        try {
            this.enumerate.close();
        } catch (Exception e) {
        }
        TestUtils.deleteDirectory(new File(TESTDIR));
    }

    @Test
    public void testUniqueSequantialNumbers() throws Exception {
        assertEquals(1, this.enumerate.put("uri:a:1"));
        assertEquals("uri:a:1", this.enumerate.get(1));
        assertEquals(1, this.enumerate.put("uri:a:1"));
        assertEquals("uri:a:1", this.enumerate.get(1));
        assertEquals(2, this.enumerate.put("uri:a:2"));
        assertEquals("uri:a:2", this.enumerate.get(2));
        assertEquals(2, this.enumerate.put("uri:a:2"));
        assertEquals("uri:a:2", this.enumerate.get(2));
        assertEquals(1, this.enumerate.put("uri:a:1"));
        assertEquals("uri:a:1", this.enumerate.get(1));
        this.enumerate.close();
        UriEnumerate e2 = new UriEnumerate(TESTDIR, 1);
        assertEquals(2, e2.put("uri:a:2"));
        assertEquals("uri:a:2", e2.get(2));
        assertEquals(1, e2.put("uri:a:1"));
        assertEquals("uri:a:1", e2.get(1));
        assertEquals(3, e2.put("uri:a:3"));
        assertEquals("uri:a:3", e2.get(3));
        assertEquals(3, e2.put("uri:a:3"));
        assertEquals("uri:a:1", e2.get(1));
        e2.close();
    }

    @Test
    public void testFlush() throws Exception {
        assertEquals(1, this.enumerate.put("uri:a:1"));
        assertEquals(1, this.enumerate.put("uri:a:1"));
        assertEquals(2, this.enumerate.put("uri:a:2"));
        assertEquals(2, this.enumerate.put("uri:a:2"));
        assertEquals(1, this.enumerate.put("uri:a:1"));
        //this.enumerate.reopen();
        assertEquals(2, this.enumerate.put("uri:a:2"));
        assertEquals(1, this.enumerate.put("uri:a:1"));
        assertEquals(3, this.enumerate.put("uri:a:3"));
    }

    @Test
    public void testAutoFlush() throws Exception {
        assertEquals(1, enumerate.put("uri:a:1"));
        assertEquals(1, enumerate.put("uri:a:1"));
        assertEquals(2, enumerate.put("uri:a:2"));
        assertEquals(2, enumerate.put("uri:a:2"));
        assertEquals(1, enumerate.put("uri:a:1"));
    }

    @Test
    public void testNotExistingUri() throws Exception {
        int ord = this.enumerate.get("uri:1");
        assertEquals(-1, ord);
        this.enumerate.put("uri:1");
        ord = this.enumerate.get("uri:1");
        assertEquals(1, ord);
    }

    @Test
    public void testNull() throws Exception {
        int ord = this.enumerate.get("");
        assertEquals(-1, ord);
        try {
            this.enumerate.get(null);
            throw new Exception("should throw NPE");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testFSTdict() throws IOException {
        FSTdict dict = new FSTdict();
        dict.put("Aap", 15);
        dict.put("Mies", 21);
        dict.put("Noot", 42);
        assertEquals(15, dict.get("Aap")); // moet gesorteerd zijn....
        assertEquals(21, dict.get("Mies"));
        assertEquals(42, dict.get("Noot"));

    }
}
