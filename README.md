# worker-globfilter

The GlobFilter Worker is a Batch Worker which takes in a glob filter as the batch definition, filters a given directory for matches and creates items of work from those matches.

## Input

The following example is an example input JSON for the Glob Filter Worker:

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

- `batchType` The plugin to use for processing of the batchDefinition. Currently the only supported batch type is `GlobPattern`. Other batch types may be added in the future as required.

- `batchDefinition` The glob filter to match - in this case `input-sub-folder/**.txt`.

- `taskMessageType` The type of TaskMessage that should be output from the worker. This must be set to "DocumentMessage" (different types may become configurable in the future).

- `taskMessageParams`:
    - `pi:datastorePartialReference` The DataStore service partial reference to store file binaries against.
    - `field:binaryFile` The name of the field that will hold the reference to the content.
    - `field:fileName` The name of the field that will hold the name of the file.
    - `field:binaryFileReference` The name of the field that will hold the storage reference of the file.
    - `newField:aNewField` A new field and value to be added to output Documents' taskData fields.
    - `cd:aCustomDataField` A field and value to add to output Documents' taskData custom data.

- `targetPipe` The queue that generated TaskMessages should be output to.

## Environment Variables

- `CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER` The input folder to be scanned for matches e.g: `/mnt/caf-datastore-root/sample-files`.