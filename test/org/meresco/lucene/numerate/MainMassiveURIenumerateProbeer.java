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

import java.util.Random;


public class MainMassiveURIenumerateProbeer {
	private static final int CACHE_SIZE = 1000 * 1000;
	private static final double N = 1000 * 1000 * 1000;
	private static final long INTERVAL = 100 * 1000;

	public static void main(String[] args) throws Exception {
		UriEnumerate uris = new UriEnumerate("uri_enumerate_probeer", CACHE_SIZE);

		Random random = new Random();
		long t0 = System.currentTimeMillis();
		long t1 = t0;
		for (int i = 1; i < N; i++) {
			if (i % INTERVAL == 0) {
				long t2 = System.currentTimeMillis();
				long ops = INTERVAL / (t2 - t1);
				float ops_avg = i / ((float) (t2 - t0));
				t1 = t2;
				System.out.println(i + "  speed: " + ops + "k/s  avg: " + ops_avg + "k/s");
				String uri = uris.get(random.nextInt(i / 10)); // random check
				assert uri.startsWith("http://");
			}
			int ord = uris.put("http://" + random.nextInt((int) (N / 10)));
			//String uri = uris.get(ord);
		}
		uris.close();
	}
}
