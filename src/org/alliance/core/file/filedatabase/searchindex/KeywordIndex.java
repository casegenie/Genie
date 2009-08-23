package org.alliance.core.file.filedatabase.searchindex;

import org.alliance.core.T;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.filedatabase.FileType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 20:36:10
 * To change this template use File | Settings | File Templates.
 */

public class KeywordIndex {
    private final static byte VERSION=1;

    private HashMap<String, CompressedBitSet> index = new HashMap<String, CompressedBitSet>();
    private HashMap<FileType, BitSet> fileTypeFilters = new HashMap<FileType, BitSet>();

    public KeywordIndex() {
        for(FileType ft : FileType.values()) fileTypeFilters.put(ft, new BitSet());
    }

    public synchronized void add(int index, FileDescriptor fd) {
        for(FileType ft : FileType.values()) {
            if (ft.fileTypeIdentifier().matches(fd)) fileTypeFilters.get(ft).set(index);
        }

        StringTokenizer st = splitIntoKeywords(fd.getBasePath()+' '+fd.getSubpath());
        while(st.hasMoreTokens()) {
            registerForKeyword(new String(st.nextToken()), index);
        }
    }

    /**
     * Searches through all files in this index
     * @param query
     * @return array of indices into FileDescriptorAllocationTable
     */
    public synchronized int[] search(String query, int maxSearchHits, FileType ft) {
        BitSet result = (BitSet)fileTypeFilters.get(ft).clone();

        if (query.trim().length() > 0) {
            StringTokenizer st = splitIntoKeywords(query);
            ArrayList<BitSet> al = new ArrayList<BitSet>();
            while(st.hasMoreTokens()) {
                String s = st.nextToken();
                BitSet totalForKeyword = new BitSet();
                for(String keyword : index.keySet()) { //find all indexed keywords that contain the string s
                    if (keyword.indexOf(s) != -1) {
                        CompressedBitSet b = index.get(keyword);
                        if (b!=null) totalForKeyword.or(b.decompress());
                    }
                }
                if (totalForKeyword.cardinality() == 0) return new int[0]; //no hits for one of the keywords - no need to go on.
                al.add(totalForKeyword);
            }

            if (al.size() == 0) return new int[0];

            for(int i=0;i<al.size();i++) result.and(al.get(i));
        }

        int len = result.cardinality();
        if (len > maxSearchHits) len = maxSearchHits;
        int[] intResult = new int[len];

        int j=0;
        for(int i=result.length();i>=0&&j<intResult.length;i--) {
            if (result.get(i)) {
                intResult[j++] = i;
            }
        }

        return intResult;
    }

    private void registerForKeyword(String keyword, int idx) {
        keyword = keyword.toLowerCase();
        CompressedBitSet b;
        if (index.containsKey(keyword)) {
            b = index.get(keyword);
        } else {
            b = new CompressedBitSet();
            index.put(keyword, b);
        }
        b.set(idx);
    }

    public synchronized void save(ObjectOutputStream out) throws IOException {
        out.writeByte(VERSION);
        out.writeObject(index);

        out.writeInt(fileTypeFilters.keySet().size());
        for(FileType ft : fileTypeFilters.keySet()) {
            out.writeInt(ft.id());
            out.writeObject(fileTypeFilters.get(ft));
        }
    }

    public synchronized void load(ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (in.readByte() != VERSION) throw new IOException("Incorrect version for keyword index!");
        index = (HashMap<String, CompressedBitSet>)in.readObject();
        fileTypeFilters = new HashMap<FileType, BitSet>();

        int size = in.readInt();
        for(int i=0;i<size;i++) {
            FileType ft = FileType.getFileTypeById(in.readInt());
            BitSet bs = (BitSet)in.readObject();
            if(T.t)T.info("Loaded bitset for "+ft+", hits: "+bs.cardinality());
            fileTypeFilters.put(ft, bs);
        }

        //add new file types if they're been added since last version
        for(FileType ft : FileType.values()) if (!fileTypeFilters.containsKey(ft)) fileTypeFilters.put(ft, new BitSet());
    }

    public static StringTokenizer splitIntoKeywords(String s) {
        return new StringTokenizer(s.toLowerCase(), ".,;:\\/ \t-[]()!\"#\u20ac%&=?+'\u017d`*_|<>");
    }

    public void remove(int index) {
        if(T.t)T.info("Removing index "+index+" from keywordindex.");
        for(CompressedBitSet b : this.index.values()) b.clear(index);
    }

    public int getNumbefOfKeywords() {
        return index.size();
    }
}
