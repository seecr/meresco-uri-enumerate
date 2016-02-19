package org.meresco.lucene.numerate;

import org.apache.lucene.facet.taxonomy.LRUHashMap;

public class Cache {
	private LRUHashMap<String, Integer> uri2ord;
	private int adds;

	Cache(int max_cache_size) {
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
		return this.adds > this.uri2ord.size();
	}

	public void reset() {
		this.adds = 0;
	}

}
