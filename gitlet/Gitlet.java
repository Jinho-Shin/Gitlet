package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

/** Class for Gitlet project.
 * @author Jinho Shin
 */
public class Gitlet {

    /** Current Working Directory. */
    private File _CWD = new File(System.getProperty("user.dir"));

    /** Gitlet directory. */
    private File _GITLET = new File(_CWD, ".gitlet");

    /** Persistence for current branch. */
    private File _HEAD = new File(_GITLET, "head");

    /** Directory of branches. */
    private File _BRANCHES = new File(_GITLET, ".branches");

    /** Directory of blobs. */
    private File _BLOBS = new File(_GITLET, ".blobs");

    /** Directory of commits. */
    private File _COMMITS = new File(_GITLET, ".commits");

    /** Persistence for tracked map. */
    private File _TRACKED = new File(_BLOBS, "tracked");

    /** Persistence for addition map. */
    private File _ADDITION = new File(_GITLET, "addition");

    /** Persistence for removal map. */
    private File _REMOVAL = new File(_GITLET, "removal");

    /** Persistence for remote branches. */
    private File _REMOTE = new File(_GITLET, ".remote");

    /** Check that the argument length is correct.
     * If command != init, check for existence of .gitlet repository.
     * @param args is the argument.
     * @param length is the required length.
     */
    public static void checkLength(String[] args, int length) {
        if (args.length != length) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (!args[0].equals("init") && !new File(".gitlet").isDirectory()) {
            System.out.println("Not in an initialized Gitlet Directory.");
            System.exit(0);
        }
    }

    /** Init method.
     * @param args has to be length of 1 */
    public void init(String[] args) throws IOException, ParseException {
        checkLength(args, 1);
        if (_GITLET.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }
        _GITLET.mkdir();
        _HEAD.createNewFile();
        _BRANCHES.mkdir();
        _BLOBS.mkdir();
        _COMMITS.mkdir();
        _REMOTE.mkdir();
        String start = "00:00:00";
        Date d = new SimpleDateFormat("HH:mm:ss").parse(start);
        Commit first = new Commit("initial commit",
                new GitMap(), null, null, d);
        String id = first.getSha();
        addBranch("master", id);
        setHead("master");
    }

    /** Add method.
     * @param args [1] the file to be added. */
    public void add(String[] args) throws IOException {
        checkLength(args, 2);
        String name = args[1];
        File f = new File(_CWD, name);
        if (!f.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        loadRemoval();
        if (_removal.contains(name)) {
            _removal.remove(name);
            saveRemoval();
            System.exit(0);
        }
        saveRemoval();
        String id = Utils.sha1(Utils.readContentsAsString(f) + name);
        File blob = new File(_BLOBS, id);
        loadAddition();
        if (!blob.exists()) {
            blob.createNewFile();
        } else {
            if (getHeadCommit().getBlobs().containsKey(name)) {
                String original = getHeadCommit().getBlobs().get(name);
                String content = getContentorEmpty(original);
                if (Utils.readContentsAsString(f).equals(content)) {
                    _addition.remove(name, id);
                    saveAddition();
                    System.exit(0);
                }
            }
        }
        Utils.writeContents(blob, Utils.readContents(f));
        _addition.put(name, id);
        saveAddition();
    }

    /** Commit method.
     * @param args [1] is the message for the commit. */
    public void commit(String[] args) throws IOException {
        checkLength(args, 2);
        String msg = args[1];
        if (msg.isBlank()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        loadAll();
        if (_addition.isEmpty() && _removal.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        for (String name : _removal) {
            _tracked.remove(name);
        }
        _tracked.putAll(_addition);
        String head = Utils.readContentsAsString(_HEAD);
        String parent = getHeadID();
        Commit newCommit = new Commit(msg, _tracked, parent, null, new Date());
        updateBranch(head, newCommit.getSha());
        _addition.clear();
        _removal.clear();
        saveAddition();
        saveRemoval();
        saveTracked();
    }

    /** Rm method.
     * @param args [1] is the name of the file to be removed. */
    public void rm(String[] args) throws IOException {
        checkLength(args, 2);
        String f = args[1];
        loadAll();
        if (!_tracked.containsKey(f) && !_addition.containsKey(f)
                && !_removal.contains(f)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        _addition.remove(f);
        if (_tracked.containsKey(f)) {
            _removal.add(f);
            File file = new File(f);
            Utils.restrictedDelete(file);
        }
        saveTracked();
        saveAddition();
        saveRemoval();
    }

    /** log method.
     * @param args needs to be length of 1 */
    public void log(String[] args) {
        checkLength(args, 1);
        String c = getHeadID();
        while (c != null) {
            File file = new File(_COMMITS, c);
            Commit com = Utils.readObject(file, Commit.class);
            printLog(com);
            c = com.getParent();
        }
    }

    /** global log method.
     * @param args needs to be length of 1 */
    public void globalLog(String[] args) {
        checkLength(args, 1);
        for (File f: _COMMITS.listFiles()) {
            Commit com = Utils.readObject(f, Commit.class);
            printLog(com);
        }
    }

    /** Print log.
     * @param com is the commit that is being logged.
     */
    public void printLog(Commit com) {
        System.out.println("===");
        System.out.println("commit " + com.getSha());
        System.out.println("Date: " + com.getTime());
        System.out.println(com.getMessage());
        System.out.println();
    }

    /** Find method.
     * @param args [1] is the message to match the commit with.
     */
    public void find(String[] args) {
        checkLength(args, 2);
        String msg = args[1];
        boolean found = false;
        for (File f: _COMMITS.listFiles()) {
            Commit com = Utils.readObject(f, Commit.class);
            if (com.getMessage().equals(msg)) {
                found = true;
                System.out.println(com.getSha());
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message");
        }
    }

    /** Status method.
     * @param args has to be length of 1.
     * @throws IOException
     */
    public void status(String[] args) throws IOException {
        checkLength(args, 1);
        System.out.println("=== Branches ===");
        String head = Utils.readContentsAsString(_HEAD);
        for (String s : Utils.plainFilenamesIn(_BRANCHES)) {
            if (head.equals(s)) {
                System.out.println("*" + s);
            } else {
                System.out.println(s);
            }
        }
        System.out.println();
        ArrayList<String> staged = new ArrayList<>();
        ArrayList<String> removed = new ArrayList<>();
        ArrayList<String> notStaged = new ArrayList<>();
        loadAll();
        _tracked = getHeadCommit().getBlobs();
        for (String s: _addition.keySet()) {
            File working = new File(s);
            if (!working.exists()) {
                notStaged.add(s + " (deleted)");
            } else {
                String s1 = Utils.readContentsAsString(working);
                File blob = new File(_BLOBS, _addition.get(s));
                String s2 = Utils.readContentsAsString(blob);
                if (!s1.equals(s2)) {
                    notStaged.add(s + " (modified)");
                } else {
                    staged.add(s);
                }
            }
        }
        for (String s: _tracked.keySet()) {
            File working = new File(s);
            if (!working.exists()) {
                if (!_removal.contains(s)) {
                    notStaged.add(s + " (deleted)");
                } else {
                    removed.add(s);
                }
            } else {
                String original = Utils.readContentsAsString(working);
                if (!getContentorEmpty(_tracked.get(s)).equals(original)) {
                    notStaged.add(s + " (modified)");
                }
            }
        }
        sortPrint("=== Staged Files ===", staged, true);
        sortPrint("=== Removed Files ===", removed, true);
        sortPrint("=== Modifications Not Staged For Commit ===",
                notStaged, true);
        sortPrint("=== Untracked Files ===", getUnTracked(), false);
    }

    /** Get untracked files.
     * @return ArrayList of untracked files.
     */
    public ArrayList<String> getUnTracked() {
        ArrayList<String> unTracked = new ArrayList<>();
        for (String s: Utils.plainFilenamesIn(_CWD)) {
            if (!_addition.containsKey(s) && !_tracked.containsKey(s)) {
                unTracked.add(s);
            }
        }
        return unTracked;
    }

    /** SortPrint the given list of files.
     * @param header is the category of the files.
     * @param list is the list of files.
     * @param blank whether or not last empty line is needed.
     */
    public void sortPrint(String header, ArrayList<String> list,
                          boolean blank) {
        System.out.println(header);
        Collections.sort(list);
        for (String s: list) {
            System.out.println(s);
        }
        if (blank) {
            System.out.println();
        }
    }

    /** Checkout method.
     * @param args determines which checkout method to be called.
     * @throws IOException
     */
    public void checkout(String[] args) throws IOException {
        if (args.length > 4 || args.length < 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (args.length == 2) {
            checkoutBranch(args[1]);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            checkoutID(args[1], args[3]);
        } else {
            checkoutFile(getHeadCommit(), args[2]);
        }
    }

    /** Checkout file.
     * @param c is the commit to look into.
     * @param name is the file to checkout.
     * @throws IOException
     */
    public void checkoutFile(Commit c, String name) throws IOException {
        GitMap blobs = c.getBlobs();
        if (!blobs.containsKey(name)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String id = blobs.get(name);
        File blob = new File(_BLOBS, id);
        File target = new File(_CWD, name);
        if (!target.exists()) {
            target.createNewFile();
        }
        Utils.writeContents(target, Utils.readContents(blob));
    }

    /** Checkout a file according to the ID.
     * @param id is the ID to find.
     * @param name is the file.
     * @throws IOException
     */
    public void checkoutID(String id, String name) throws IOException {
        if (id.length() < 10 * 4) {
            for (File file: _COMMITS.listFiles()) {
                if (file.getName().contains(id)) {
                    id = file.getName();
                    break;
                }
            }
        }
        File f = new File(_COMMITS, id);
        if (!f.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit com = Utils.readObject(f, Commit.class);
        checkoutFile(com, name);
    }

    /** Checkout according to branch.
     * @param branch is the name of the branch to find.
     * @throws IOException
     */
    public void checkoutBranch(String branch) throws IOException {
        File b = new File(_BRANCHES, branch);
        if (!b.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        String current = Utils.readContentsAsString(_HEAD);
        if (current.equals(branch)) {
            System.out.println("No need to check out the current branch.");
            System.exit(0);
        }
        loadAll();
        if (!getUnTracked().isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            System.exit(0);
        }
        String id = Utils.readContentsAsString(b);
        File c = new File(_COMMITS, id);
        Commit com = Utils.readObject(c, Commit.class);
        Set<String> checked = new HashSet<>();
        checked.addAll(com.getBlobs().keySet());
        for (String s: Utils.plainFilenamesIn(_CWD)) {
            if (!checked.contains(s)) {
                new File(s).delete();
            } else {
                File work = new File(s);
                File prev = new File(_BLOBS, com.getBlobs().get(s));
                Utils.writeContents(work, Utils.readContents(prev));
            }
            checked.remove(s);
        }
        for (String ch: checked) {
            File newFile = new File(ch);
            newFile.createNewFile();
            File prev = new File(_BLOBS, com.getBlobs().get(ch));
            Utils.writeContents(newFile, Utils.readContents(prev));
        }
        setHead(branch);
        _addition.clear();
        _removal.clear();
        saveAddition();
        saveRemoval();
        _tracked = com.getBlobs();
        saveTracked();
    }


    /** branch method.
     * @param args [1] is the name of the new branch.
     * @throws IOException
     */
    public void branch(String[] args) throws IOException {
        checkLength(args, 2);
        String newBranch = args[1];
        File b = new File(_BRANCHES, newBranch);
        if (b.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        addBranch(newBranch, getHeadID());
    }

    /** rmBranch method.
     * @param args [1] is the name of the branch.
     */
    public void rmBranch(String[] args) {
        checkLength(args, 2);
        String name = args[1];
        File branch = new File(_BRANCHES, name);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (name.equals(Utils.readContentsAsString(_HEAD))) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branch.delete();
    }

    /** reset method.
     * @param args [1] is the id of the commit.
     * @throws IOException
     */
    public void reset(String[] args) throws IOException {
        checkLength(args, 2);
        String id = args[1];
        File f = new File(_COMMITS, id);
        if (!f.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        String head = Utils.readContentsAsString(_HEAD);
        addBranch("temp", id);
        checkoutBranch("temp");
        setHead(head);
        updateBranch(head, id);
        rmBranch(new String[] {"rm", "temp"});
    }

    /** Check if merge is possible and return the split ID.
     * @param name the name of the branch.
     * @return the splitID.
     * @throws IOException
     */
    public String mergeCheck(String name) throws IOException {
        if (getBranchID(name) == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (name.equals((Utils.readContentsAsString(_HEAD)))) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        String splitID = findSplitPoint(getHeadID(), getBranchID(name));
        if (splitID.equals(getBranchID(name))) {
            System.out.println("Given branch is an ancestor of"
                    + " the current branch.");
            System.exit(0);
        }
        if (splitID.equals(getHeadID())) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(name);
            System.exit(0);
        }
        if (getBranchID(name).equals(getHeadID())) {
            System.out.println("No changes added to the commit");
            System.exit(0);
        }
        loadAll();
        if (!_addition.isEmpty() || !_removal.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!getUnTracked().isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            System.exit(0);
        }
        return splitID;
    }

    /** Make a merge commit.
     * @param name is the name of the branch merged into.
     */
    public void mergeCommit(String name) throws IOException {
        for (String r : _removal) {
            _tracked.remove(r);
        }
        _tracked.putAll(_addition);
        Commit mergeCommit = new Commit("Merged " + name + " into "
                + Utils.readContentsAsString(_HEAD) + ".", _tracked,
                getHeadID(), getBranchID(name), new Date());
        updateBranch(Utils.readContentsAsString(_HEAD), mergeCommit.getSha());
        _addition.clear();
        _removal.clear();
        saveAddition();
        saveRemoval();
        saveTracked();
    }

    /** Handling merge conflict.
     * @param conflictFiles is the list of files conficted.
     * @param current is the current branch.
     * @param branch is the branch to be merged into.
     * @throws IOException
     */
    public void mergeConflict(ArrayList<String> conflictFiles,
                              GitMap current,
                              GitMap branch)
            throws IOException {
        for (String s: conflictFiles) {
            String content = "<<<<<<< HEAD" + "\n"
                    + getContentorEmpty(current.get(s))
                    + "=======" + "\n"
                    + getContentorEmpty(branch.get(s))
                    + ">>>>>>>" + "\n";
            String id = Utils.sha1(content + s);
            File conflictBlob = new File(_BLOBS, id);
            conflictBlob.createNewFile();
            Utils.writeContents(conflictBlob, content);
            File conflictFile = new File(s);
            conflictFile.createNewFile();
            Utils.writeContents(conflictFile, content);
            _addition.put(s, id);
        }
        if (!conflictFiles.isEmpty()) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** merge method.
     * @param args [1] is the branch name to get merged into.
     * @throws IOException
     */
    public void merge(String[] args) throws IOException {
        checkLength(args, 2);
        String branchName = args[1];
        String splitID = mergeCheck(branchName);
        Commit splitcom = Utils.readObject(new File(_COMMITS, splitID),
                Commit.class);
        Commit curcom = Utils.readObject(new File(_COMMITS, getHeadID()),
                Commit.class);
        Commit branchcom = Utils.readObject(new File(_COMMITS,
                getBranchID(branchName)), Commit.class);
        _tracked = curcom.getBlobs();
        GitMap split = splitcom.getBlobs();
        GitMap current = curcom.getBlobs();
        GitMap branch = branchcom.getBlobs();
        ArrayList<String> conflictFiles = new ArrayList<>();
        for (String s: split.keySet()) {
            if (branch.containsKey(s)) {
                if (current.containsKey(s)) {
                    if (!contentEquals(split.get(s), branch.get(s))) {
                        if (!contentEquals(split.get(s), current.get(s))) {
                            if (!contentEquals(current.get(s), branch.get(s))) {
                                conflictFiles.add(s);
                            }
                        } else {
                            checkoutID(getBranchID(branchName), s);
                            _addition.put(s, branch.get(s));
                        }
                    }
                } else
                    if (!contentEquals(branch.get(s), split.get(s))) {
                        conflictFiles.add(s);
                    }
            } else
                if (current.containsKey(s)) {
                    if (!contentEquals(split.get(s), current.get(s))) {
                        conflictFiles.add(s);
                    } else {
                        new File(s).delete();
                        _tracked.remove(s);
                    }
                }
        }
        for (String s: branch.keySet()) {
            if (!split.containsKey(s)) {
                if (!current.containsKey(s)) {
                    checkoutID(getBranchID(branchName), s);
                    _addition.put(s, branch.get(s));
                } else if (!contentEquals(current.get(s), branch.get(s))) {
                    conflictFiles.add(s);
                }
            }
        }
        mergeConflict(conflictFiles, current, branch);
        mergeCommit(branchName);
    }

    /** Find the split point.
     * @param s1 is the current branch's commit id.
     * @param s2 is the target branch's commit id.
     * @return split point's commit id.
     */
    public String findSplitPoint(String s1, String s2) {
        HashMap<String, Integer> distances = getClosest(getParents(s1, 0), s2);
        String result = null;
        int min = _COMMITS.listFiles().length;
        for (String s: distances.keySet()) {
            if (distances.get(s) <= min) {
                min = distances.get(s);
                result = s;
            }
        }
        return result;
    }

    /** Get parents of a certain commit.
     * @param id is the commit's id.
     * @param distance is used to keep track of the distance from this commit.
     * @return the map of ids and distances.
     */
    public HashMap<String, Integer> getParents(String id, Integer distance) {
        HashMap<String, Integer> result = new HashMap<>();
        while (id != null) {
            result.put(id, distance);
            distance += 1;
            File f = new File(_COMMITS, id);
            Commit com = Utils.readObject(f, Commit.class);
            id = com.getParent();
            if (com.getParent2() != null) {
                HashMap<String, Integer> result2
                        = getParents(com.getParent2(), distance);
                result.putAll(result2);
            }
        }

        return result;
    }

    /** Get the closest parent(s).
     * could be multiple due to merge parents.
     * @param parents is the map of parents and distances from current branch.
     * @param id is the id of the target branch.
     * @return the map of possible split points and their distances
     * from the current branch.
     */
    public HashMap<String, Integer> getClosest(HashMap<String, Integer>
                                                        parents, String id) {
        HashMap<String, Integer> result = new HashMap<>();
        String closest = null;
        int min = _COMMITS.listFiles().length;
        while (id != null) {
            File f = new File(_COMMITS, id);
            Commit com = Utils.readObject(f, Commit.class);
            if (parents.containsKey(id)) {
                if (parents.get(id) < min) {
                    closest = id;
                    min = parents.get(id);
                } else {
                    break;
                }
            }
            if (com.getParent2() != null) {
                HashMap<String, Integer> result2
                        = getClosest(parents, com.getParent2());
                result.putAll(result2);
            }
            id = com.getParent();
        }
        result.put(closest, min);
        return result;
    }

    /** add-remote method.
     * @param args [1] is the name of the remote
     * args [2] is the directory to link the remote to.
     */
    public void addRemote(String[] args) throws IOException {
        checkLength(args, 3);
        String name = args[1];
        String directory = args[2];
        File newdir = new File(_REMOTE, name);
        if (newdir.exists()) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        newdir.createNewFile();
        Utils.writeContents(newdir, directory);
    }

    /** re-remote method.
     * @param args [1] is the name of the remote directory to remove.
     */
    public void rmRemote(String[] args) {
        checkLength(args, 2);
        String name = args[1];
        File dir = new File(_REMOTE, name);
        if (!dir.exists()) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        dir.delete();
    }

    /** push method.
     * @param args [1] is the name of the remote directory.
     * args [2] is the name of the remote branch.
     * @throws IOException
     */
    public void push(String[] args) throws IOException {
        checkLength(args, 3);
        String name = args[1];
        String branch = args[2];
        String remote = Utils.readContentsAsString(new File(_REMOTE, name));
        File rdir = new File(_CWD, remote);
        if (!rdir.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File rbranches = new File(rdir, ".branches");
        File bFile = new File(rbranches, branch);
        if (!bFile.exists()) {
            bFile.createNewFile();
            remoteaddCommits(getHeadID(), null, rdir);
        } else {
            String remoteID = Utils.readContentsAsString(bFile);
            if (!getParents(getHeadID(), 0).containsKey(remoteID)) {
                System.out.println("Please pull down remote "
                        + "changes before pushing.");
                System.exit(0);
            }
            remoteaddCommits(getHeadID(), remoteID, rdir);
        }
        Utils.writeContents(bFile, getHeadID());
    }

    /** add commits to the remote directory.
     * @param local is the id of the local head.
     * @param remote is the id of the remote head to track from.
     * @param dir is the remote directory.
     * @throws IOException
     */
    public void remoteaddCommits(String local, String remote, File dir)
            throws IOException {
        File rcommits = new File(dir, ".commits");
        File rblobs = new File(dir, ".blobs");
        while (local != null && !local.equals(remote)) {
            Commit com = Utils.readObject(new File(_COMMITS, local),
                    Commit.class);
            for (String blob: com.getBlobs().keySet()) {
                if (!new File(rblobs, blob).exists()) {
                    Files.copy(new File(_BLOBS,
                                    com.getBlobs().get(blob)).toPath(),
                            new File(rblobs, blob).toPath());
                }
            }
            Files.copy(new File(_COMMITS, com.getSha()).toPath(),
                 new File(rcommits, com.getSha()).toPath());
            local = com.getParent();
        }
    }

    /** fetch method.
     * @param args [1] is the name of the remote directory.
     * args [2] is the name of the remote branch.
     * @throws IOException
     */
    public void fetch(String[] args) throws IOException {
        checkLength(args, 3);
        String name = args[1];
        String branch = args[2];
        String remote = Utils.readContentsAsString(new File(_REMOTE, name));
        File rdir = new File(_CWD, remote);
        if (!rdir.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File rbranches = new File(rdir, ".branches");
        File rbranch = new File(rbranches, branch);
        if (!rbranch.exists()) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }
        for (File c: new File(rdir, ".commits").listFiles()) {
            if (!new File(_COMMITS, c.getName()).exists()) {
                Files.copy(c.toPath(), new File(_COMMITS,
                        c.getName()).toPath());
            }
        }
        for (File b: new File(rdir, ".blobs").listFiles()) {
            if (!new File(_BLOBS, b.getName()).exists()) {
                Files.copy(b.toPath(), new File(_BLOBS, b.getName()).toPath());
            }
        }
        String bname = name + "/" + branch;
        String id = Utils.readContentsAsString(rbranch);
        new File(_BRANCHES, name).mkdir();
        addBranch(bname, id);
    }

    /** pull method.
     * @param args [1] is the name of the remote directory.
     * args [2] is the name of the remote branch.
     * @throws IOException
     */
    public void pull(String[] args) throws IOException {
        checkLength(args, 3);
        String name = args[1];
        String branch = args[2];
        fetch(new String[] {"fetch", name, branch});
        merge(new String[] {"merge", name + "/" + branch});
    }

    /** Check if two blobs have the same content.
     * @param id1 is the first id.
     * @param id2 is the second id.
     * @return boolean of whether they have same content.
     */
    public boolean contentEquals(String id1, String id2) {
        File f1 = new File(_BLOBS, id1);
        File f2 = new File(_BLOBS, id2);
        String s1 = Utils.readContentsAsString(f1);
        String s2 = Utils.readContentsAsString(f2);
        return s1.equals(s2);
    }

    /** Add a branch.
     * @param name is the name of the branch.
     * @param id is the id of the commit to point to.
     * @throws IOException
     */
    public void addBranch(String name, String id) throws IOException {
        File newBranch = new File(_BRANCHES, name);
        if (!newBranch.exists()) {
            newBranch.createNewFile();
        }
        Utils.writeContents(newBranch, id);
    }

    /** Update a branch.
     * @param name is the name of the branch.
     * @param id is the id of the commit to point to.
     */
    public void updateBranch(String name, String id) {
        File branch = new File(_BRANCHES, name);
        Utils.writeContents(branch, id);
    }

    /** Get content (or empty string) of the blob.
     * @param id is the id of the blob.
     * @return the content as string.
     */
    public String getContentorEmpty(String id) {
        if (id == null) {
            return "";
        }
        File f = new File(_BLOBS, id);
        return Utils.readContentsAsString(f);
    }

    /** Get id of the branch.
     * @param name is the name of the branch.
     * @return the id.
     */
    public String getBranchID(String name) {
        File branch = new File(_BRANCHES, name);
        if (!branch.exists()) {
            return null;
        }
        return Utils.readContentsAsString(branch);
    }

    /** Set head to the branch given.
     * @param name is the name of the branch.
     */
    public void setHead(String name) {
        Utils.writeContents(_HEAD, name);
    }

    /** Get head commit.
     * @return the head commit.
     */
    public Commit getHeadCommit() {
        String id = getHeadID();
        File f = new File(_COMMITS, id);
        return Utils.readObject(f, Commit.class);
    }

    /** Returns the id of the head commit.
     * @return the id of the head commit.
     * */
    public String getHeadID() {
        String name = Utils.readContentsAsString(_HEAD);
        File f = new File(_BRANCHES, name);
        return Utils.readContentsAsString(f);
    }

    /** Save _addition for persistence.
     */
    public void saveAddition() throws IOException {
        if (!_ADDITION.exists()) {
            _ADDITION.createNewFile();
        }
        Utils.writeObject(_ADDITION, _addition);
    }

    /** Save _removal for persistence.
     */
    public void saveRemoval() throws IOException {
        if (!_REMOVAL.exists()) {
            _REMOVAL.createNewFile();
        }
        Utils.writeObject(_REMOVAL, _removal);
    }

    /** Save _tracked for persistence.
     */
    public void saveTracked() throws IOException {
        if (!_TRACKED.exists()) {
            _TRACKED.createNewFile();
        }
        Utils.writeObject(_TRACKED, _tracked);
    }

    /** Load _addition, _removal, and _tracked.
     * @throws IOException
     */
    public void loadAll() throws IOException {
        loadAddition();
        loadTracked();
        loadRemoval();
    }



    /** Load _addition for persistence.
     * @throws IOException
     */
    public void loadAddition() {
        if (!_ADDITION.exists()) {
            _addition = new GitMap();
        } else {
            _addition = Utils.readObject(_ADDITION, GitMap.class);
        }
    }

    /** Load _removal for persistence.
     * @throws IOException
     */
    public void loadRemoval() {
        if (!_REMOVAL.exists()) {
            _removal = new GitArray();
        } else {
            _removal = Utils.readObject(_REMOVAL, GitArray.class);
        }
    }

    /** Load _tracked for persistence.
     * @throws IOException
     */
    public void loadTracked() {
        if (!_TRACKED.exists()) {
            _tracked = new GitMap();
        } else {
            _tracked = Utils.readObject(_TRACKED, GitMap.class);

        }
    }

    /** GitMap of the addition staging area.
     * Map of the name of the files staged for addition
     * and the id of their blobs.
     */
    private GitMap _addition;

    /** GitArray of the removal staging area.
     */
    private GitArray _removal;

    /** GitMap of tracked files.
     * Map of the name of the files tracked
     * and the id of their blobs
     */
    private GitMap _tracked;





}
