package org.meresco.lucene.numerate;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;

class UriMapDocument {
	private final StringField uri_index = new StringField(UriEnumerate.URI_FIELD, "", Store.YES);
	private final NumericDocValuesField ord_value = new NumericDocValuesField(UriEnumerate.ORD_VALUE_FIELD, 0);
	private final OrdField ord_index = new OrdField(UriEnumerate.ORD_INDEX_FIELD, Store.NO);
	final Document doc = new Document();
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