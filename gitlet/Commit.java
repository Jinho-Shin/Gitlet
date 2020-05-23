package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Commit class for gitlet.
 * @author Jinho Shin
 */
public class Commit implements Serializable {

    /** Create a commit.
     * @param message of the commit
     * @param blobs the files tracked
     * @param parent of the commit
     * @param parent2 for merges
     * @param time of the creation
     * */
    Commit(String message, GitMap blobs,
           String parent, String parent2, Date time) throws IOException {
        _message = message;
        _blobs = blobs;
        _parent = parent;
        _parent2 = parent2;
        String pattern = "EEE MMM d HH:mm:ss yyyy Z";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        _time = sdf.format(time);
        List<Object> all = new ArrayList<>();
        if (_message != null) {
            all.add(_message);
        }
        if (_blobs != null) {
            for (String s : _blobs.keySet()) {
                all.add(s);
            }
        }
        all.add(_time);
        _sha = Utils.sha1(all);
        saveCommit();
    }

    /** Save commit for persistence. */
    public void saveCommit() throws IOException {
        File newCommit = new File(new File(".gitlet", ".commits"), _sha);
        newCommit.createNewFile();
        Utils.writeObject(newCommit, this);
    }

    /** Get method for message.
     * @return the message */
    public String getMessage() {
        return _message;
    }

    /** Get method for time.
     * @return the time */
    public String getTime() {
        return _time;
    }

    /** Get method for hte ID.
     * @return the Sha */
    public String getSha() {
        return _sha;
    }

    /** Get method for the parent.
     * @return the parent */
    public String getParent() {
        return _parent;
    }

    /** Get method for the parent2.
     * @return parent2 (null if none) */
    public String getParent2() {
        return _parent2;
    }

    /** Get method for the blobs.
     * @return the blobs */
    public GitMap getBlobs() {
        return _blobs;
    }

    /** The message. */
    private String _message;

    /** The files/blobs linked to this commit. */
    private GitMap _blobs;

    /** The parent. */
    private String _parent;

    /** The second parent (for merges). */
    private String _parent2;

    /** The time. */
    private String _time;

    /** The ID. */
    private String _sha;

}
