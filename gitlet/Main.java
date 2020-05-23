package gitlet;

import java.io.IOException;
import java.text.ParseException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Jinho Shin
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException, ParseException {
        Gitlet G = new Gitlet();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        } else if (args[0].equals("init")) {
            G.init(args);
        } else if (args[0].equals("add")) {
            G.add(args);
        } else if (args[0].equals("commit")) {
            G.commit(args);
        } else if (args[0].equals("rm")) {
            G.rm(args);
        } else if (args[0].equals("log")) {
            G.log(args);
        } else if (args[0].equals("global-log")) {
            G.globalLog(args);
        } else if (args[0].equals("find")) {
            G.find(args);
        } else if (args[0].equals("checkout")) {
            G.checkout(args);
        } else if (args[0].equals("status")) {
            G.status(args);
        } else if (args[0].equals("branch")) {
            G.branch(args);
        } else if (args[0].equals("rm-branch")) {
            G.rmBranch(args);
        } else if (args[0].equals("reset")) {
            G.reset(args);
        } else if (args[0].equals("merge")) {
            G.merge(args);
        } else if (args[0].equals("add-remote")) {
            G.addRemote(args);
        } else if (args[0].equals("rm-remote")) {
            G.rmRemote(args);
        } else if (args[0].equals("push")) {
            G.push(args);
        } else if (args[0].equals("fetch")) {
            G.fetch(args);
        } else if (args[0].equals("pull")) {
            G.pull(args);
        } else {
            System.out.println("No command with that name exists.");
        }
        System.exit(0);
    }
}


