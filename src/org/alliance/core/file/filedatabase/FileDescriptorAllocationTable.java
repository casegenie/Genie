package org.alliance.core.file.filedatabase;

import org.alliance.core.file.share.T;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 17:11:26
 * To change this template use File | Settings | File Templates.
 */
public class FileDescriptorAllocationTable {
    private static final int ARRAY_INCREMENT_SIZE = 128;

    private int offsets[] = new int[ARRAY_INCREMENT_SIZE];
    private int numberOfFiles = 0;
    private static final int MAGIC = 0xbabe;

    public int getOffset(int fdIndex) {
        if (fdIndex >= numberOfFiles) throw new ArrayIndexOutOfBoundsException("No such fd index "+fdIndex);
        return offsets[fdIndex];
    }

    /**
     *
     * @param offset
     * @return index at wich this offset ended up at
     */
    public int addOffset(int offset) {
        numberOfFiles++;
        if (numberOfFiles > offsets.length) {
            if(T.t)T.info("Expanding FDAT with "+ARRAY_INCREMENT_SIZE+" new size: "+(offsets.length+ARRAY_INCREMENT_SIZE)+" nof : "+numberOfFiles);
            int newArray[] = new int[offsets.length+ARRAY_INCREMENT_SIZE];
            System.arraycopy(offsets, 0, newArray, 0, numberOfFiles-1); //-1 because we added one at start of method
            offsets = newArray;
        }
        offsets[numberOfFiles-1] = offset;
        return numberOfFiles-1;
    }

    public int getNumberOfFiles() {
        return numberOfFiles;
    }

    public void save(ObjectOutputStream out) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(numberOfFiles);
        for(int i=0;i<numberOfFiles;i++) out.writeInt(getOffset(i));
    }

    public void load(ObjectInputStream in) throws IOException {
        if (in.readInt() != MAGIC) throw new IOException("Index file is probably corrupt! Magic number incorrect.");
        numberOfFiles = in.readInt();
        offsets = new int[numberOfFiles];
        for(int i=0;i<numberOfFiles;i++) offsets[i] = in.readInt();
    }
}
