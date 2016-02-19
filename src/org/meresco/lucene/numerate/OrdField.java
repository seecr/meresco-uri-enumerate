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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.BytesTermAttribute;
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

	private static final class BinaryTokenStream extends TokenStream {
	    private final BytesTermAttribute bytesAtt = addAttribute(BytesTermAttribute.class);
	    private boolean used = true;
	    private BytesRef value;

	    /** Creates a new TokenStream that returns a BytesRef as single token.
	     * <p>Warning: Does not initialize the value, you must call
	     * {@link #setValue(BytesRef)} afterwards!
	     */
	    BinaryTokenStream() {
	    }

	    public void setValue(BytesRef value) {
	      this.value = value;
	    }

	    @Override
	    public boolean incrementToken() {
	      if (used) {
	        return false;
	      }
	      clearAttributes();
	      bytesAtt.setBytesRef(value);
	      used = true;
	      return true;
	    }

	    @Override
	    public void reset() {
	      used = false;
	    }

	    @Override
	    public void close() {
	      value = null;
	    }
	}
}