package org.alliance.core.file.filedatabase.searchindex;

import java.io.Serializable;
import java.util.BitSet;

/**
 * User: maciek
 * Date: 2006-jan-07
 * Time: 11:40:27
 * To change this template use File | Settings | File Templates.
 */
public class CompressedBitSet implements Serializable {
    private static final long serialVersionUID = -8235012528149791357l;
    protected BitSet bitSet;
    protected int zeroIndexIsActually;

    public CompressedBitSet(BitSet bitSet) {
        this.bitSet = bitSet;
    }

    public CompressedBitSet() {
    }

    public BitSet decompress() {
        BitSet b = new BitSet();
        for(int i=bitSet.nextSetBit(0); i>=0; i=bitSet.nextSetBit(i+1)) {
            b.set(i+zeroIndexIsActually);
        }
        return b;
    }

    public void set(int index) {
        checkCapacity(index);
        bitSet.set(index-zeroIndexIsActually);
    }

    public void clear(int index) {
        if (index < zeroIndexIsActually) return;
        checkCapacity(index);
        bitSet.set(index-zeroIndexIsActually, false);
    }

    public boolean get(int index) {
        if (index < zeroIndexIsActually) return false;
        return bitSet.get(index-zeroIndexIsActually);
    }

    private void checkCapacity(int index) {
        if (bitSet == null) {
            bitSet = new BitSet();
            zeroIndexIsActually = index;
        } else if (index < zeroIndexIsActually) {
            //"index" will be new "zeroIndexIsActually" after this
            BitSet b = new BitSet();
            for(int i=bitSet.nextSetBit(0); i>=0; i=bitSet.nextSetBit(i+1)) {
                b.set((i+zeroIndexIsActually)-index);
            }
            bitSet = b;
            zeroIndexIsActually = index;
        }
    }
}
