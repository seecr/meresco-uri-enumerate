package org.meresco.lucenejena;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;

public class OrdField extends Field {
	BinaryTokenStream token = new BinaryTokenStream(); // #TODO #FIXME made public in Lucene
	public static final FieldType ORDFIELD_STORED = new FieldType();
	public static final FieldType ORDFIELD_INDEXED = new FieldType();
	static {
		// ORDFIELD_STORED.setIndexed(true);
		ORDFIELD_STORED.setTokenized(true); // data is never tokenized
		ORDFIELD_STORED.setOmitNorms(true);
		ORDFIELD_STORED.setIndexOptions(IndexOptions.DOCS);
		ORDFIELD_STORED.setStored(true);
		ORDFIELD_STORED.freeze();

		// /ORDFIELD_INDEXED.setIndexed(true);
		ORDFIELD_INDEXED.setTokenized(true); // data is never tokenized
		ORDFIELD_INDEXED.setOmitNorms(true);
		ORDFIELD_INDEXED.setIndexOptions(IndexOptions.DOCS);
		ORDFIELD_INDEXED.setStored(false);
		ORDFIELD_INDEXED.freeze();
	}

	public OrdField(String name, Store store) {
		super(name, store == Store.NO ? ORDFIELD_INDEXED : ORDFIELD_STORED);
		super.setTokenStream(this.token);
	}

	public OrdField(String ordIndexField, int ord, Store store) {
		this(ordIndexField, store);
		this.setIntValue(ord);
	}

	@Override
	public void setIntValue(int ord) {
		this.token.setValue(ord2bytes(ord));
	}

	public static BytesRef ord2bytes(int ord) {
		return new BytesRef(new byte[] { (byte) (ord >> 24), (byte) (ord >> 16), (byte) (ord >> 8), (byte) ord });
	}
}