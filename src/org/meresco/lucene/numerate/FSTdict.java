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

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.apache.lucene.util.fst.BytesRefFSTEnum.InputOutput;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;

public class FSTdict {

    private Builder<Long> fstbuilder = new Builder<>(FST.INPUT_TYPE.BYTE4,
            PositiveIntOutputs.getSingleton());
    private IntsRefBuilder scratch = new IntsRefBuilder();
    private BytesRefFSTEnum<Long> brfstenum;

    public void put(String uri, int ord) throws IOException {
        scratch.clear();
        this.fstbuilder.add(Util.toIntsRef(new BytesRef(uri), scratch), (long) ord);
    }

    public long get(String uri) throws IOException {
        if (this.fstbuilder.getTermCount() > 0L) {
            FST<Long> fst = this.fstbuilder.finish();
            if (fst != null)
                this.brfstenum = new BytesRefFSTEnum<Long>(fst);
        }
        InputOutput<Long> seekExact = this.brfstenum.seekExact(new BytesRef(uri));
        if (seekExact != null)
            return seekExact.output;
        return -1;
    }
}
