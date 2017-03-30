# worker-globfilter

The GlobFilter Worker is a Batch Worker which takes in a glob filter as the batch definition, filters a given directory for matches and creates items of work from those matches.

## Input

The following is an example input JSON for the Glob Filter Worker:

<pre><code>
{
   "batchDefinition":"input-sub-folder/**.txt",
   "batchType":"GlobPattern",
   "taskMessageType":"DocumentMessage",
   "taskMessageParams":{
      "pi:datastorePartialReference":"74c98be740b44d64b5b7a4e224555917",
      "field:binaryFile":"CONTENT",
      "field:fileName":"FILE_NAME",
      "field:binaryFileReference":"STORAGE_REFERENCE",
      "newField:aNewField":"aNewFieldValue",
      "cd:aCustomDataField":"aCustomDataFieldValue"
   },
   "targetPipe":"langdetect-in"
}
</code></pre>

### Input JSON fields

- `batchType` The plugin to use for processing of the batchDefinition. Currently the only supported batch type is `GlobPattern`. Other batch types may be added in the future as required.

- `batchDefinition` The glob filter to match - in this case `input-sub-folder/**.txt`.

- `taskMessageType` The type of TaskMessage that should be output from the worker. This must be set to "DocumentMessage" (different types may become configurable in the future).

- `taskMessageParams` A list of namespace Task Message parameters that the worker uses to build Task Messages (`pi` and `field` namespaces are described under [Task Message Parameters Namespaces](#Task-Message-Parameters-Namespaces):
    - `pi:datastorePartialReference` The DataStore service partial reference to store file binaries against.
    - `field:binaryFile` The name of the field that will hold the reference to the content.
    - `field:fileName` The name of the field that will hold the name of the file.
    - `field:binaryFileReference` The name of the field that will hold the storage reference of the file.
    - `newField:aNewField` A new field and value to be added to output Documents' taskData fields. Given the example above, TaskMessages output taskData fields will contain a field with a key called `aNewField` with a value of `aNewFieldValue`.
    - `cd:aCustomDataField` A field and value to add to output Documents' taskData custom data. Given the example above, TaskMessages output taskData customData will contain a field with a key called `aCustomDataField` with a value of `aCustomDataFieldValue`.

- `targetPipe` The queue that generated TaskMessages should be output to.

#### Task Message Parameters Namespaces

The following tables describe the Glob Filter Worker processing instruction and field namespace parameters:

##### Processing instructions

The processing instructions namespace, denoted with `pi`, are parameters that are used by worker operations.

The following table lists the processing instructions that are used by the Glob Filter worker:

| pi                        | Description                                                             |
|---------------------------|-------------------------------------------------------------------------|
| datastorePartialReference | The DataStore service partial reference to store file binaries against. |

##### Field

The fields namespace, denoted with `field`, are parameters that provide the name of the fields as to which information on the glob matched file will be stored.

The following table lists the fields that will be added to TaskMessage taskData `fields` output from the Glob Filter Worker when specified:

| field               | Description                                                             |
|---------------------|-------------------------------------------------------------------------|
| binaryFile          | The name of the field that will hold the reference to the content.      |
| binaryFileReference | The name of the field that will hold the storage reference of the file. |
| fileName            | The name of the field that will hold the name of the file.              |

Note that if a name is not specified for any of the above fields then that field will not be added to the taskData.

## Output

The following is an example TaskMessage taskData JSON returned from the Glob Filter Worker for a file that matched the glob operation:

<pre><code>
{
   "fields":{
      "aNewField":[
         {
            "data":"aNewFieldValue"
         }
      ],
      "CONTENT":[
         {
            "data":"74c98be740b44d64b5b7a4e224555917/adb4cdce-62f1-4ada-a7e7-463e9abd4b95",
            "encoding":"storage_ref"
         }
      ],
      "FILE_NAME":[
         {
            "data":"ATextFileThatMatchedTheGlobFilterOutput.txt"
         }
      ],
      "STORAGE_REFERENCE":[
         {
            "data":"74c98be740b44d64b5b7a4e224555917/adb4cdce-62f1-4ada-a7e7-463e9abd4b95"
         }
      ]
   },
   "customData":{
      "aCustomDataField":"aCustomDataFieldValue"
   }
}
</code></pre>

## Environment Variables

- `CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER` The input folder to be scanned for matches e.g: `/mnt/caf-datastore-root/sample-files`.