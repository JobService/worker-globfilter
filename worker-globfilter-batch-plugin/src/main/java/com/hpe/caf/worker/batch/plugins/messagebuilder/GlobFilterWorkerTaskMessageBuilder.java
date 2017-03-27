package com.hpe.caf.worker.batch.plugins.messagebuilder;

import com.hpe.caf.messagebuilder.TaskMessage;
import com.hpe.caf.worker.batch.plugins.GlobFilterDocument;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Builds a TaskMessage that represents a output for the GlobFilterWorker
 */
public final class GlobFilterWorkerTaskMessageBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobFilterWorkerTaskMessageBuilder.class);

    private String fileNameFieldName = null;
    private String binaryFileFieldName = null;
    private String binaryFileReferenceFieldName = null;

    /**
     * Build a GlobFilterWorker TaskMessage
     *
     * @param document GlobFilterDocument that contains the storage reference and filename of the filtered document
     * @param namespaceParams Glob Filter Worker task parameters that will be added to the TaskMessage customFields
     * @return TaskMessage containing details on the Worker and taskData representing the filtered document
     */
    public TaskMessage buildMessage(final GlobFilterDocument document,
                                    final Map<String, Map<String, String>> namespaceParams) {

        // Verify that required props passed are not null
        final String fileName = document.getFileName();
        Objects.requireNonNull(fileName, "Document file name must not be null.");
        final String storageReference = document.getStorageReference();
        Objects.requireNonNull(storageReference, "storageReference on document must not be null.");
        Objects.requireNonNull(namespaceParams, "namespaceParams must not be null.");

        logger.info("Building TaskMessage for file: {%s}  storageRef: {%s}", fileName, storageReference);

        extractFieldNamesFromNamespaceParams(namespaceParams);

        // Construct a DocumentWorkerTask that will be added to TaskMessage
        final DocumentWorkerTask taskData = constructDocumentWorkerTaskData(namespaceParams, fileName,
                storageReference);

        // Create a TaskMessage from the taskData containing fields and customData
        final TaskMessage taskMessage = new TaskMessage();
        taskMessage.setTaskData(taskData);
        taskMessage.setTaskApiVersion(DocumentWorkerConstants.WORKER_API_VER);
        taskMessage.setTaskClassifier(DocumentWorkerConstants.WORKER_NAME);

        logger.info("Constructed message successfully");

        // Return the constructed TaskMessage representing the filtered file
        return taskMessage;
    }

    /**
     * Extract field names to use from namespace params
     *
     * @param namespaceParams containing field names to be mapped to the taskData
     */
    private void extractFieldNamesFromNamespaceParams(final Map<String, Map<String, String>> namespaceParams) {
        // Get a list of field names that values will be stored to from the namespaced parameters
        final Map<String, String> fields = namespaceParams.get("field");
        if (fields != null) {
            // Get the values from the keys we that contain the names of the field to use
            fileNameFieldName = fields.get("fileName");
            binaryFileFieldName = fields.get("binaryFile");
            binaryFileReferenceFieldName = fields.get("binaryFileReference");
        }
    }

    /**
     * Construct DocumentWorkerTask object from the namespaceParams, fileName and storageReference of the document.
     *
     * @param namespaceParams map containing namespaced parameters that are added to the DocumentWorkerTask as
     *                        separate fieldss
     * @param fileName the filename of the filtered document which is added to fields if required
     * @param storageReference the storage reference of the filtered document which is added to fields if required
     * @return DocumentWorkerTask containing fields and customData representing the filtered document
     */
    private DocumentWorkerTask constructDocumentWorkerTaskData(final Map<String, Map<String, String>> namespaceParams,
                                                               final String fileName, final String storageReference) {
        final DocumentWorkerTask taskData = new DocumentWorkerTask();

        taskData.fields = new HashMap<>();

        // If a binaryFileFieldName was set, construct and add a binary reference field and value to taskData fields
        if (binaryFileFieldName != null) {
            final DocumentWorkerFieldValue binaryReferenceFieldValue = new DocumentWorkerFieldValue();
            binaryReferenceFieldValue.data = storageReference;
            binaryReferenceFieldValue.encoding = DocumentWorkerFieldEncoding.storage_ref;
            taskData.fields.put(binaryFileFieldName, Arrays.asList(binaryReferenceFieldValue));
        }

        // If a fileNameFieldName was set, construct and add a document filename field and value value to taskData
        // fields
        if (fileNameFieldName != null) {
            // Construct and add a document filename field value to taskData FILE_NAME field
            final DocumentWorkerFieldValue fileNameField = new DocumentWorkerFieldValue();
            fileNameField.data = fileName;
            taskData.fields.put(fileNameFieldName, Arrays.asList(fileNameField));
        }

        // If a binaryFileReferenceFieldName was set, construct and add a storage reference field and value to taskData
        // fields
        if(binaryFileReferenceFieldName != null) {
            final DocumentWorkerFieldValue binaryFileReferenceField = new DocumentWorkerFieldValue();
            binaryFileReferenceField.data = storageReference;
            taskData.fields.put(binaryFileReferenceFieldName, Arrays.asList(binaryFileReferenceField));
        }

        // Add new fields to the taskData
        extractNewFieldsFromNamespaceParams(taskData, namespaceParams);
        // Add custom data fields to the taskData
        taskData.customData = namespaceParams.get("cd");

        // Uninitialise taskData.fields if they are empty
        if (taskData.fields.isEmpty()) {
            taskData.fields = null;
        }

        return taskData;
    }

    /**
     * Map new fields from the Namespace Parameters onto the Task Data
     *
     * @param taskData document task data to map fields to
     * @param namespaceParams containing new fields to be mapped to the task
     */
    private void extractNewFieldsFromNamespaceParams(final DocumentWorkerTask taskData,
                                                     final Map<String, Map<String, String>> namespaceParams) {
        // Get a list of new fields from the namespace params
        final Map<String, String> listOfNewFields = namespaceParams.get("newField");

        if (listOfNewFields != null) {
            // For each new field add it to the taskData fields
            for (final Map.Entry<String, String> newField : listOfNewFields.entrySet()) {
                final DocumentWorkerFieldValue fileNameField = new DocumentWorkerFieldValue();
                fileNameField.data = newField.getValue();
                taskData.fields.put(newField.getKey(), Arrays.asList(fileNameField));
            }
        }
    }
}
