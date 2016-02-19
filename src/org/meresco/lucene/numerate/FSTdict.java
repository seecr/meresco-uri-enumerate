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
