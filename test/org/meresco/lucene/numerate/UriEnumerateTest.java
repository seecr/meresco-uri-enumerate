/* begin license *
 *
 * "Meresco Uri Enumerate" contains an http server which maps uris to integer numbers
 *
 * Copyright (C) 2016 Seecr (Seek You Too B.V.) http://seecr.nl
 *
 * This file is part of "Meresco Uri Enumerate"
 *
 * "Meresco Uri Enumerate" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * "Meresco Uri Enumerate" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "Meresco Uri Enumerate"; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * end license */

package org.meresco.lucene.numerate;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class UriEnumerateTest {
	private static final String TESTDIR = "urienumeratetest";
	private UriEnumerate enumerate;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		if (new File(TESTDIR).exists()) {
			FileUtils.deleteDirectory(new File(TESTDIR));
		}
		this.enumerate = new UriEnumerate(TESTDIR, 2);
	}

	@After
	public void tearDown() throws Exception {
		try {
			this.enumerate.close();
		} catch (Exception e) {
		}
		FileUtils.deleteDirectory(new File(TESTDIR));
	}

	@Test
	public void testUniqueSequentialNumbers() throws Exception {
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
		this.enumerate = new UriEnumerate(TESTDIR, 1);
		assertEquals(2, this.enumerate.put("uri:a:2"));
		assertEquals("uri:a:2", this.enumerate.get(2));
		assertEquals(1, this.enumerate.put("uri:a:1"));
		assertEquals("uri:a:1", this.enumerate.get(1));
		assertEquals(3, this.enumerate.put("uri:a:3"));
		assertEquals("uri:a:3", this.enumerate.get(3));
		assertEquals(3, this.enumerate.put("uri:a:3"));
		assertEquals("uri:a:1", this.enumerate.get(1));
		assertEquals(2, this.enumerate.put("uri:a:2"));
		assertEquals("uri:a:2", this.enumerate.get(2));
		System.out.println("-- Culprit");
		assertEquals(1, this.enumerate.put("uri:a:1"));
		System.out.println("-- tot zover");
		assertEquals("uri:a:1", this.enumerate.get(1));
		assertEquals(3, this.enumerate.put("uri:a:3"));
		assertEquals("uri:a:3", this.enumerate.get(3));
	}

	@Test
	public void testFlush() throws Exception {
		assertEquals(1, this.enumerate.put("uri:a:1"));
		assertEquals(1, this.enumerate.put("uri:a:1"));
		assertEquals(2, this.enumerate.put("uri:a:2"));
		assertEquals(2, this.enumerate.put("uri:a:2"));
		assertEquals(1, this.enumerate.put("uri:a:1"));
		// this.enumerate.reopen();
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
		assertEquals(15, dict.get("Aap"));  // moet gesorteerd zijn....
		assertEquals(21, dict.get("Mies"));
		assertEquals(42, dict.get("Noot"));

	}

	@Test
	public void testCache() throws Exception {
		assertEquals(0, this.enumerate.size());
		assertEquals(0, this.enumerate.cache.size());
		assertEquals(false, this.enumerate.cache.overflowed());
		this.enumerate.put("urn:a:1");
		assertEquals(1, this.enumerate.size());
		assertEquals(1, this.enumerate.cache.size());
		assertEquals(false, this.enumerate.cache.overflowed());
		assertEquals("{urn:a:1=1}", this.enumerate.cache.uri2ord.toString());
		this.enumerate.put("urn:a:2");
		assertEquals(2, this.enumerate.size());
		assertEquals(2, this.enumerate.cache.size());
		assertEquals(false, this.enumerate.cache.overflowed());
		assertEquals("{urn:a:1=1, urn:a:2=2}", this.enumerate.cache.uri2ord.toString());
		this.enumerate.put("urn:a:3");
		assertEquals(3, this.enumerate.size());
		assertEquals(2, this.enumerate.cache.size());
		assertEquals(false, this.enumerate.cache.overflowed());
		assertEquals("{urn:a:2=2, urn:a:3=3}", this.enumerate.cache.uri2ord.toString());
		assertEquals("urn:a:1", this.enumerate.get(1));
	}

	@Test
	public void testRecoverFromCrash() throws Exception {
		this.enumerate.close();
		int MAX_VALUE = 6;
		for (int i = 1; i < MAX_VALUE; i++) {
			this.enumerate = new UriEnumerate(TESTDIR, 2);
			for (int j = 1; j < i; j++) {
				assertEquals(j, this.enumerate.put("uri:a:" + j));
			}
			simulateCrash();
			System.out.println("Crashed");
			this.enumerate = new UriEnumerate(TESTDIR, 2);
			for (int j = i; j < MAX_VALUE; j++) {
				assertEquals(j, this.enumerate.put("uri:a:" + j));
			}
			System.out.println("Still there?");
			for (int j = 1; j < i; j++) {
				System.out.println("Knettergek");
				assertEquals(j, this.enumerate.put("uri:a:" + j));
			}
			this.enumerate.close();
			FileUtils.deleteDirectory(new File(TESTDIR));
		}
	}

	@Test
	public void testFailsToRecoverFromCrashWhenNoTransactionLog() throws Exception {
		this.enumerate.close();
		this.enumerate = new UriEnumerate(TESTDIR, 2, /* withTransactionLog */ false);
		assertEquals(1, this.enumerate.put("uri:a:1"));
		assertEquals(2, this.enumerate.put("uri:a:2"));
		simulateCrash();
		this.enumerate = new UriEnumerate(TESTDIR, 2, false);
		assertEquals(1, this.enumerate.put("uri:a:3"));  // undesirable, so that's where the transaction log comes in (but without is faster).
	}

	private void simulateCrash() {
		String tmpDir = TESTDIR + "_tmp";
		try {
			FileUtils.copyDirectory(new File(TESTDIR), new File(tmpDir));
			this.enumerate.close();
			this.enumerate = null;
			FileUtils.deleteDirectory(new File(TESTDIR));
			FileUtils.copyDirectory(new File(tmpDir), new File(TESTDIR));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (new File(tmpDir).exists()) {
				try {
					FileUtils.deleteDirectory(new File(tmpDir));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
