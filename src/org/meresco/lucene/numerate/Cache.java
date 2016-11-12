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

import org.apache.lucene.facet.taxonomy.LRUHashMap;

public class Cache {
	int max_cache_size;
	private LRUHashMap<String, Integer> uri2ord;
	private int adds;

	Cache(int max_cache_size) {
		this.max_cache_size = max_cache_size;
		this.uri2ord = new LRUHashMap<String, Integer>(max_cache_size);
		this.adds = 0;
	}

	public Integer get(String uri) {
		return this.uri2ord.get(uri);
	}

	public Object put(String uri, Integer ord) {
		this.adds++;
		return this.uri2ord.put(uri, ord);
	}

	public int size() {
		return this.uri2ord.size();
	}

	public boolean overflow() {
		return this.adds > this.max_cache_size;
	}

	public void reset() {
		this.adds = 0;
	}

}
