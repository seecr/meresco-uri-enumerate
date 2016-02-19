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
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.bloom.BloomFilteringPostingsFormat;
import org.apache.lucene.codecs.lucene54.Lucene54Codec;
import org.apache.lucene.codecs.memory.MemoryPostingsFormat;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class UriEnumerate {

	private static final int CACHE_SIZE = 1 * 1000;
	private static final String URI_FIELD = "uri_index";
	private static final String ORD_INDEX_FIELD = "ord_index";
	private static final String ORD_VALUE_FIELD = "ord_value";

	private static class UriMapDocument {
		private final StringField uri_index = new StringField(URI_FIELD, "", Store.YES);
		private final NumericDocValuesField ord_value = new NumericDocValuesField(ORD_VALUE_FIELD, 0);
		private final OrdField ord_index = new OrdField(ORD_INDEX_FIELD, Store.NO);
		private final Document doc = new Document();
		{
			this.doc.add(this.uri_index);
			this.doc.add(this.ord_value);
			this.doc.add(this.ord_index);
		}

		void set(String uri, int ord) {
			this.uri_index.setStringValue(uri);
			this.ord_value.setLongValue(ord);
			this.ord_index.setIntValue(ord);
		}
	}

	private static final class Hit {

		private final LeafReader reader;
		private final int doc;

		public Hit(LeafReader reader, int doc) {
			this.reader = reader;
			this.doc = doc;
		}
	}

	private IndexWriter writer;
	private UriMapDocument doc_proto = new UriMapDocument();
	private Map<Object, NumericDocValues> ordscache = new WeakHashMap<Object, NumericDocValues>();
	private Cache cache;
	private int next_ord;
	private DirectoryReader reader;

	public UriEnumerate(Path path, int max_cache_size) throws IOException {
		IndexWriterConfig config = new IndexWriterConfig(null);
		/*
		 * The idea behind this merge config is to avoid useless background merging and instead synchronize merging with the reopen calls. For now, all merging happens when openIfChanged is called,
		 * but this may be done in a carefully sync'ed thread.
		 */
		config.setMergeScheduler(new SerialMergeScheduler());
		LogDocMergePolicy mp = new LogDocMergePolicy();
		mp.setMergeFactor(2);
		mp.setMinMergeDocs(100000);
		config.setMergePolicy(mp);
		config.setRAMBufferSizeMB(Integer.MAX_VALUE); // no background flush
		config.setMaxBufferedDocs(IndexWriterConfig.DISABLE_AUTO_FLUSH);
		/*
		 * The idea behind this codec is to have the whole dictionary in memory, compressed as an FST. The Bloom filter gives a slight advantage in speed and costs only little memory and disk space.
		 */
		config.setCodec(new Lucene54Codec() {
			@Override
			public PostingsFormat getPostingsFormatForField(String field) {
				return new BloomFilteringPostingsFormat(new MemoryPostingsFormat());
			}
		});
		config.setUseCompoundFile(false);
		this.writer = new IndexWriter(FSDirectory.open(path), config);
		writer.commit();
		this.reader = DirectoryReader.open(this.writer, false);
		this.cache = new Cache(max_cache_size);
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

	public int put(String uri) throws Exception {
		int ord = this.get(uri);
		if (ord < 0)
			ord = this.add(uri);
		return ord;
	}

	public int get(String uri) throws Exception {
		Integer ord = this.cache.get(uri);
		if (ord != null)
			return ord;
		this.maybeReopen();
		ord = this.search(uri);
		if (ord >= 0)
			this.cache.put(uri, ord);
		return ord;
	}

	public String get(int ord) throws Exception {
		this.reOpen(); // #TODO we ignore get's influencing LRU for now
		Hit hit = this.search(ORD_INDEX_FIELD, OrdField.ord2bytes(ord));
		if (hit != null)
			return hit.reader.document(hit.doc).get(URI_FIELD);
		return null;
	}

	private int search(String uri) throws Exception {
		Hit hit = this.search(URI_FIELD, new BytesRef(uri));
		if (hit != null)
			return (int) this.getOrds(hit.reader).get(hit.doc);
		return -1;

	}

	private Hit search(String field, BytesRef text) throws Exception {
		PostingsEnum postings = null;
		for (LeafReaderContext leaf : this.reader.leaves()) {
			LeafReader leafreader = leaf.reader();
			TermsEnum termsenum = leafreader.terms(field).iterator();
			if (!termsenum.seekExact(text))
				continue;
			postings = termsenum.postings(postings, PostingsEnum.NONE);
			return new Hit(leafreader, postings.nextDoc());
		}
		return null;
	}

	private int add(String uri) throws Exception {
		int ord = this.next_ord++;
		this.doc_proto.set(uri, ord);
		writer.addDocument(this.doc_proto.doc);
		if (this.cache.put(uri, ord) != null)
			throw new RuntimeException("Why did this happen? No unit-test throws me!");
		this.maybeReopen();
		return ord;
	}

	private void maybeReopen() {
		if (this.cache.overflow()) { // all cache might be invalid
			this.reOpen();
			this.cache.reset();
		}
	}

	private void reOpen() {
		try {
			DirectoryReader reader = DirectoryReader.openIfChanged(this.reader, this.writer, false);
			if (reader != null)
				this.reader = reader;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private NumericDocValues getOrds(LeafReader reader) throws IOException {
		Object key = reader.getCoreCacheKey();
		NumericDocValues ords = this.ordscache.get(key);
		if (ords == null) {
			ords = reader.getNumericDocValues(ORD_VALUE_FIELD);
			this.ordscache.put(key, ords);
		}
		return ords;
	}

	public void close() throws IOException {
		this.writer.commit();
		this.writer.close();
		this.writer = null;
	}

	public void commit() throws IOException {
		this.writer.commit();
	}

	public int size() {
		return this.writer.numDocs();
	}
}
