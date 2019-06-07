package org.meresco.lucene.numerate;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

public class SimpleSearcher {

	public static final class Hit {

		public final LeafReader reader;
		public final int doc;

		public Hit(LeafReader reader, int doc) {
			this.reader = reader;
			this.doc = doc;
		}
	};

	private DirectoryReader reader = null;
	private IndexWriter writer;
	//private Map<Object, NumericDocValues> ordscache = new WeakHashMap<Object, NumericDocValues>();
	private NumericDocValuesRandomAccess keyValues;

	public SimpleSearcher(IndexWriter writer) throws IOException {
		this.writer = writer;
		this.reader = DirectoryReader.open(writer, true, true);
		//this.keyValues = new NumericDocValuesRandomAccess(reader, UriEnumerate.ORD_VALUE_FIELD);
	}

	public Hit search(String field, BytesRef text) throws Exception {
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

	public int search(String uri) throws Exception {
		Hit hit = this.search(UriEnumerate.URI_FIELD, new BytesRef(uri));
		if (hit != null) {
			if (this.keyValues == null) {
				this.keyValues = new NumericDocValuesRandomAccess(hit.reader, UriEnumerate.ORD_VALUE_FIELD);
			}
			return (int) this.keyValues.get(hit.doc);
		}
		return -1;
	}

	/*private NumericDocValues getOrds(LeafReader reader) throws IOException {
		Object key = reader.getCoreCacheKey();
		NumericDocValues ords = this.ordscache.get(key);
		if (ords == null) {
			ords = reader.getNumericDocValues(UriEnumerate.ORD_VALUE_FIELD);
			this.ordscache.put(key, ords);
		}
		return ords;
	}*/

	void reOpen() {
		try {
			DirectoryReader reader = DirectoryReader.openIfChanged(this.reader, this.writer, true);
			if (reader != null) {
				System.out.println("Reopened");
				DirectoryReader oldReader = this.reader;
				this.reader = reader;
				oldReader.close();
			} else {
				System.out.println("Bitch ain't here");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() throws IOException {
		this.reader.close();
		this.writer = null;
	}

}
