package org.alliance.core.file.filedatabase;

import com.stendahls.util.TextUtils;

import java.io.Serializable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.alliance.core.T;

/**
 * Can keep track of a lot of filepaths in a efficient (memory usage wise) way.
 * Used in the filedatabase to keep track of what files are indexed.
 *
 * Uhm. It's not efficient memory wise right now. There's room for optimiziation here.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jul-12
 * Time: 14:04:19
 */

public class CompressedPathCollection implements Serializable {
    private static final long serialVersionUID = 7234254693355857212L;   //@todo: MAKE SURE THIS IS BACKWARDS COMPATIBLE
    private HashSet<String> paths = new HashSet<String>();

    public synchronized void addPath(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        paths.add(path);
    }

    public synchronized void removePath(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        paths.remove(path);
    }

    public int getNmberOfPaths() {
        return paths.size();
    }

    /**
     * @param path
     * @return true if there is a possibility that this path is contained in this collections
     */
    public boolean contains(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        return paths.contains(path);
    }

    // this one is synchronized because two threads where getting in here - one invoking this and another invoking
    // add or remove path causing a ConcurrentModificationError
    public synchronized String[] getDirectoryListing(String path) {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        if (!path.endsWith("/")) path = path+'/';

        HashSet<String> hs = new HashSet<String>();
        for (java.util.Iterator it = paths.iterator(); it.hasNext();) {
            String s = (String) it.next();
            if (s.startsWith(path)) {
                //add filename without path
                s = s.substring(path.length());
                if (s.indexOf('/') != -1) {
                    //show only files and folders that are in this directory, not in subdirectories
                    s = s.substring(0, s.indexOf('/') + 1);
                }
                if (!new File(path+s).exists()) {
                    if(T.t)T.info("Ehm. Found file in path collection that does not exist. Recovering by ignoring it and removing it from path collection.");
                    it.remove();
                } else {
                    hs.add(s);
                }
            }
        }

        ArrayList<String> dirs = new ArrayList<String>();
        ArrayList<String> files = new ArrayList<String>();
        for(String s : hs) {
            if (s.endsWith("/"))
                dirs.add(s);
            else
                files.add(s);
        }
        Collections.sort(dirs);
        Collections.sort(files);

        String[] sa = new String[dirs.size()+files.size()];
        int i=0;
        for(String s : dirs) sa[i++] = s;
        for(String s : files) sa[i++] = s;
        return sa;
    }
}
