# Gitlet Design Document

**Name**: Jinho Shin

## Classes and Data Structures

1. Blob
    - Comparable
    - Readable
    - Serializable
    - SHA-1
2. Tree
    - Do we need this?
3. Commit
   - Timestamp
   - Log message
   - Parent
   - Second parent
   - Names to blob references (SHA-1)
   - SHA-1

## Algorithms
1. init
    - Initial commit
    - Check that there is no system already.
2. add
    - Make a copy of the file in the staging area.
    - Check that the file exists. 
3. commit
    - Make a snapshot of the staging area.
    - Create a new instance of a Commit.
    - Check that there are files in the staging area.
    - Check for a message.
4. rm
    - Unstage the file or stage it for removal.
    - Delete the file from the staging area or put it in the
    staging area for removal. 
    - Check that the file is tracked or staged. 
5. log
    - Print out the logs starting from the first commit.
    - Climb up the tree starting with the current branch
    (Recursion)
6. global-log
    - Print out all messages.
    - Climb up and down starting with the current branch.
    (Recursion). 
    - For all branches of the parent, print log. 
7. find
    - Print out id of all commits with the message.
    - Iterate through the tree. 
8. status
    - Print out current branch.
    - Print out current staged files.
    - Print out current removed files.
    - Print out current modified files not staged.
    - Print out current untracked files. 
    - All of those information should be already handled in 
    persistence section.
9. checkout
    - Checkout a file: overwrite the current file with the 
    version at the head commit. (Recursion to the parent)
    - Checkout a id and file name: overwrite the current file
    with the version at the id. (Recursion to find the id)
    - Checkout a branch name: overwrite all files in current
     branch with the ones in the target branch. Delete all the
     tracked files that are not in the target branch. Change
     the current branch to the target branch.
10. branch
    - Create a new branch and point it at the current head node.
    - Add information to persistence.
    - Check that the name is not repeated.
11. rm-branch
    - Remove branch.
    - Delete information from persistence.
    - Check that the name exists. 
    - Check that current branch != branch.
12. reset
    - Checkout all files given the id.
    - Run checkout [file] for all files in the id.
    - Check for untracked files (message!)
13. merge
    - Find the split point between current branch and target.
    - 


## Persistence

What do we have to persist?

1. The History of Commits
    - As long as the commits themselves exist in the folder, 
    the history is saved by the parent & second parent information?
2. The branches
3. Current branch
4. Staging Area
5. Tracked files
6. Current Parent & Second parent?

 

