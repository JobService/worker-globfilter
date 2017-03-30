## CAF_16002 - Generate messages from an input folder and Glob filter matches on partial filename ##

Verify that the Glob Filter worker will take a glob filter for partial filename, and with an environment variable, generate a message for file matched

**Test Steps**

1. Point the Glob Filter Worker at a folder of files to create task messages for with environment variable
2. Provide the Glob Filter Worker with a batch definition that takes the form of a glob filter for partial filename
3. Verify that the files output are those that match the glob filter within the input folder

**Test Data**

An input folder containing files and sub directories with files that match the glob filter

**Expected Result**

The files are processed and the correct number of messages are output to the queue for each item matching the glob filter within the input folder.

**JIRA Link** - [CAF-2329](https://jira.autonomy.com/browse/CAF-2329)

