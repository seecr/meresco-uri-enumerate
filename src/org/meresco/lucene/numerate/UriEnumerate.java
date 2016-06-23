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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.bloom.BloomFilteringPostingsFormat;
import org.apache.lucene.codecs.lucene60.Lucene60Codec;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.store.FSDirectory;
import org.meresco.lucene.numerate.SimpleSearcher.Hit;

public class UriEnumerate {

	private static final int CACHE_SIZE = 1 * 1000;
	static final String URI_FIELD = "uri_index";
	static final String ORD_INDEX_FIELD = "ord_index";
	static final String ORD_VALUE_FIELD = "ord_value";

	private SimpleSearcher searcher;
	private IndexWriter writer;
	private UriMapDocument doc_proto = new UriMapDocument();
	Cache cache;
	private int next_ord;

	public UriEnumerate(Path path, int max_cache_size) throws IOException {
		IndexWriterConfig config = new IndexWriterConfig(null);
		ConcurrentMergeScheduler ms = (ConcurrentMergeScheduler) config.getMergeScheduler();
		ms.setDefaultMaxMergesAndThreads(/* spins= */false);
		LogDocMergePolicy mp = new LogDocMergePolicy();
		mp.setMergeFactor(2);
		mp.setMinMergeDocs(max_cache_size);
		config.setMergePolicy(mp);

		config.setCodec(new Lucene60Codec() {
			@Override
			public PostingsFormat getPostingsFormatForField(String field) {
				return new BloomFilteringPostingsFormat(super.getPostingsFormatForField(field));
			}
		});

		config.setUseCompoundFile(false);
		this.writer = new IndexWriter(FSDirectory.open(path), config);
		writer.commit();
		this.searcher = new SimpleSearcher(this.writer);
		this.cache = new Cache(max_cache_size, () -> this.searcher.reOpen());
		this.next_ord = writer.numDocs() + 1;
	}

	public UriEnumerate(String string, int cache_size) throws IOException {
		this(FileSystems.getDefault().getPath(string), cache_size);
	}

	public UriEnumerate(String path) throws IOException {
		this(path, CACHE_SIZE);
	}

	public UriEnumerate(Path dictPath) throws IOException {
		this(dictPath, CACHE_SIZE);
	}

	public synchronized int put(String uri) throws Exception {
		int ord = this.get(uri);
		if (ord < 0)
			ord = this.add(uri);
		return ord;
	}

	public int get(String uri) throws Exception {
		Integer ord = this.cache.get(uri);
		if (ord != null)
			return ord;
		ord = this.searcher.search(uri);
		if (ord >= 0)
			this.cache.put(uri, ord);
		return ord;
	}

	public String get(int ord) throws Exception {
		// TODO there is no ord->uri cache yet, so we simple ALWAYS flush and search with reOpen
		this.searcher.reOpen();
		Hit hit = this.searcher.search(ORD_INDEX_FIELD, OrdField.ord2bytes(ord));
		if (hit != null)
			return hit.reader.document(hit.doc).get(URI_FIELD);
		return null;
	}

	private int add(String uri) throws Exception {
		int ord = this.next_ord++;
		this.doc_proto.set(uri, ord);
		writer.addDocument(this.doc_proto.doc);
		if (this.cache.put(uri, ord) != null)
			throw new RuntimeException("Why did this happen? No unit-test throws me!");
		return ord;
	}

	public void close() throws IOException {
		this.writer.commit();
		this.writer.close();
		this.writer = null;
		this.searcher.close();
	}

	public void commit() throws IOException {
		this.writer.commit();
	}

	public int size() {
		return this.writer.numDocs();
	}
}
