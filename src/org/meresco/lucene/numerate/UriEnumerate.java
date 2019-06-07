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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.bloom.BloomFilteringPostingsFormat;
import org.apache.lucene.codecs.lucene70.Lucene70Codec;
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
	private TransactionLog transactionLog;

	/**
	 *
	 * @param path
	 * @param max_cache_size
	 * @param withTransactionLog allows for crash recovery, but slows down UriNumerate considerably because of file system flush.
	 * @throws IOException
	 */
	public UriEnumerate(String path, int max_cache_size, boolean withTransactionLog) throws IOException {
		IndexWriterConfig config = new IndexWriterConfig(null);
		ConcurrentMergeScheduler ms = (ConcurrentMergeScheduler) config.getMergeScheduler();
		ms.setDefaultMaxMergesAndThreads(/* spins= */false);
		LogDocMergePolicy mp = new LogDocMergePolicy();
		mp.setMergeFactor(2);
		mp.setMinMergeDocs(max_cache_size);
		config.setMergePolicy(mp);
		config.setCodec(new Lucene70Codec() {
			@Override
			public PostingsFormat getPostingsFormatForField(String field) {
				return new BloomFilteringPostingsFormat(super.getPostingsFormatForField(field));
			}
		});
		config.setUseCompoundFile(false);
		this.writer = new IndexWriter(FSDirectory.open(FileSystems.getDefault().getPath(path)), config);
		this.next_ord = writer.numDocs() + 1;
		this.searcher = new SimpleSearcher(this.writer);
		this.cache = new Cache(max_cache_size, () -> this.commit());
		this.transactionLog = new TransactionLog(withTransactionLog ? path + "/transactionLog" : null);
		this.transactionLog.maybeRecover();
	}

	public UriEnumerate(String path, int cache_size) throws IOException {
		this(path, cache_size,  /* writeTransactionLog */ true);
	}

	public UriEnumerate(String path) throws IOException {
		this(path, CACHE_SIZE);
	}

	public UriEnumerate(Path path, int cache_size) throws IOException {
		this(path.toString(), cache_size);
	}


	public synchronized int put(String uri) throws Exception {
		int ord = this.get(uri);
		if (ord < 0) {
			ord = this.add(uri);
		}
		return ord;
	}

	public int get(String uri) throws Exception {
		Integer ord = this.cache.get(uri);
		if (ord != null) {
			return ord;
		}
		ord = this.searcher.search(uri);
		if (ord >= 0) {
			this.cache.put(uri, ord);
		}
		return ord;
	}

	public String get(int ord) throws Exception {
		// TODO there is no ord->uri cache yet, so we simple ALWAYS flush and
		// search with reOpen
		this.searcher.reOpen();
		Hit hit = this.searcher.search(ORD_INDEX_FIELD, OrdField.ord2bytes(ord));
		if (hit != null) {
			return hit.reader.document(hit.doc).get(URI_FIELD);
		}
		return null;
	}

	private int add(String uri) throws Exception {
		int ord = this.next_ord++;
		this.transactionLog.write(uri, ord);
		add(uri, ord);
		return ord;
	}

	void add(String uri, int ord) throws IOException {
		this.doc_proto.set(uri, ord);
		writer.addDocument(this.doc_proto.doc);
		if (this.cache.put(uri, ord) != null) {
			throw new RuntimeException("Why did this happen? No unit-test throws me!");
		}
	}

	public void close() throws IOException {
		this.commit();
		this.writer.close();
		this.writer = null;
		this.searcher.close();
		this.searcher = null;
		this.transactionLog.close();
		this.transactionLog = null;
	}

	public void commit() {
		try {
			this.writer.commit();
			this.transactionLog.erase();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.searcher.reOpen();
	}

	public int size() {
		return this.writer.numDocs();
	}

	public void updateNextOrd(int minimalNextOrd) {
		this.next_ord = Math.max(this.next_ord, minimalNextOrd);
	}


	class TransactionLog {
		String path;
		PrintWriter out;

		public TransactionLog(String transactionLogPath) throws IOException {
			this.path = transactionLogPath;
		}

		public void write(String uri, int ord) {
			if (this.path == null) {
				return;
			}
			if (this.out == null) {
				this.startNew();
			}
			this.out.println("" + ord + " " + uri);
			this.out.flush();
		}

		private void startNew() {
			try {
				this.out = new PrintWriter(
						new BufferedWriter(
								new OutputStreamWriter(
										new FileOutputStream(this.path), "UTF-8")));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void close() {
			if (this.out != null) {
				this.out.close();
				this.out = null;
			}
		}

		public void erase() {
			this.close();
			if (this.path != null && new File(this.path).exists()) {
				new File(this.path).delete();
			}
		}

		public void maybeRecover() throws IOException {
			if (this.path == null || !new File(this.path).exists()) {
				return;
			}
			int ord = 0;
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.path), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("\\s", 2);
				try {
					String uri = parts[1];
					ord = Integer.valueOf(parts[0]);
					UriEnumerate.this.add(uri, ord);
				} catch (ArrayIndexOutOfBoundsException e) {
					break;
				}
			}
			UriEnumerate.this.updateNextOrd(ord + 1);
			reader.close();
		}
	}
}
