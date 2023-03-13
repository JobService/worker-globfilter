/*
 * Copyright 2023 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.batch.plugins;

import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.messagebuilder.TaskMessage;
import com.hpe.caf.worker.batch.BatchDefinitionException;
import com.hpe.caf.worker.batch.BatchWorkerPlugin;
import com.hpe.caf.worker.batch.BatchWorkerServices;
import com.hpe.caf.worker.batch.plugins.messagebuilder.GlobFilterWorkerTaskMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Plugin that expects a batch definition that is in the form of a glob filter. The plugin will scan the input folder
 * (set via environment variable) and create TaskMessages for each file matching the glob filter pattern
 */
public class GlobPattern implements BatchWorkerPlugin {
    private static final Logger logger = LoggerFactory.getLogger(GlobPattern.class);

    private DataStore dataStore = null;

    /**
     * Process the input directory specified via environment variable against the globFilter (passed as the
     * batchDefinition) to find matches. Create TaskMessages for each file filtered and register them with
     * BatchWorkerServices.
     *
     * @param batchWorkerServices used to retrieve the DataStore service and register constructed TaskMessages with
     * @param batchDefinition the glob filter used to find file matches
     * @param taskMessageType this must be set to "DocumentMessage" (different types may become configurable in the
     *                        future)
     * @param taskMessageParams a Map of TaskMessage Parameters containing the required input folder and DataStore
     *                          partial reference values
     * @throws BatchDefinitionException thrown when failure to validate input or generation of TaskMessages occurs
     */
    @Override
    public void processBatch(final BatchWorkerServices batchWorkerServices, final String batchDefinition,
                             final String taskMessageType,
                             final Map<String, String> taskMessageParams) throws BatchDefinitionException {
        logger.info("GloberFilterBatchPlugin processing batch");
        // Get the dataStore from the BatchWorkerServices
        this.dataStore = batchWorkerServices.getService(DataStore.class);

        // Validate that the glob filter has been passed
        final String globFilter = batchDefinition;
        if (globFilter == null || globFilter.isEmpty()) {
            throw new BatchDefinitionException("No valid glob filter passed in the batch definition.");
        }

        // Validate taskMessageType has been set
        if (taskMessageType == null || !taskMessageType.equals("DocumentMessage")) {
            throw new BatchDefinitionException("Task message type null or not set to DocumentMessage.");
        }

        final Map<String, Map<String, String>> namespaceParams = retrieveNamespacedParamsFromTaskMessageParams(
                taskMessageParams);

        // Construct a list of TaskMessage(s) to send
        final List<TaskMessage> taskMessagesToSend = constructTaskMessages(globFilter, namespaceParams);

        // Register the list of constructed TaskMessage(s) with BatchWorkerServices
        registerMessages(batchWorkerServices, taskMessagesToSend);
    }

    private static String getNamespace(final String namespacedKey) {
        String[] namespaceAndKey = namespacedKey.split(":", 2);
        // The first element is the namespace
        return namespaceAndKey[0];
    }

    private static String getKeyFromNamespacedKey(final String namespacedKey) {
        String[] namespaceAndKey = namespacedKey.split(":", 2);
        // If we do not have two elements then we do not have a namespaced key
        if (namespaceAndKey.length != 2) {
            return null;
        }
        // The second element is the key
        return namespaceAndKey[1];
    }

    /**
     * Retrieve a map of namespaced parameters from task message parameters.
     *
     * @param taskMessageParams map of task message parameters containing keys prefixed with namespaces
     * @return map of namespace parameters where the key is the namespace and the values are string to string key pairs
     * belonging to that namespace
     */
    private static Map<String, Map<String, String>> retrieveNamespacedParamsFromTaskMessageParams(
            final Map<String, String> taskMessageParams) {

        Map<String, Map<String, String>> namespaceParams =
                taskMessageParams.entrySet().stream().collect(Collectors.groupingBy(
                        entry -> getNamespace(entry.getKey()),
                        Collectors.toMap(entry -> getKeyFromNamespacedKey(entry.getKey()), Map.Entry::getValue)));

        // If there are no processing instructions set pi to an empty hash map
        namespaceParams.putIfAbsent("pi", new HashMap<>());

        return namespaceParams;
    }

    /**
     * Construct and return a list of TaskMessage(s) for each file that matches the glob filter when applied against
     * the input folder.
     *
     * @param globFilter the glob filter to use for file matching against the input folder
     * @param namespaceParams a Map of Namespace Parameters containing processing instructions and fields
     * @return a List of TaskMessage(s) constructed from each file that matched from applying the glob filter against
     * the input folder
     * @throws BatchDefinitionException if unable to get document matches or unable to store the document in storage
     */
    private List<TaskMessage> constructTaskMessages(final String globFilter,
                                                    final Map<String, Map<String, String>> namespaceParams)
            throws BatchDefinitionException {

        logger.info("Constructing list of TaskMessages from glob filter and input folder");
        final List<Path> filteredDocumentPaths = getDocumentMatches(globFilter);

        final List<TaskMessage> taskMessagesToSend = new ArrayList<>();
        // For each filtered document attempt to store it, construct a TaskMessage from it and add it to the list of
        // messages to send.
        for (Path filteredDocumentPath : filteredDocumentPaths) {
            // Store the document
            final String storageReference = storeDocument(filteredDocumentPath, namespaceParams);

            // Create a GlobFilterDocument containing the storage reference and the name of the file stored
            final GlobFilterDocument document = new GlobFilterDocument(storageReference,
                    filteredDocumentPath.toFile().getName());
            final GlobFilterWorkerTaskMessageBuilder taskMessageBuilder = new GlobFilterWorkerTaskMessageBuilder();
            taskMessagesToSend.add(taskMessageBuilder.buildMessage(document, namespaceParams));
        }

        return taskMessagesToSend;
    }

    /**
     * With a glob filter string try to filter documents from the input folder and return a list of paths for each match
     *
     * @param globFilter glob filter with or without a sub directory prefix
     * @return a list of paths for each file that matched the glob filter
     * @throws BatchDefinitionException if failed to create a batch of items matching globFilter supplied
     */
    public List<Path> getDocumentMatches(final String globFilter) throws BatchDefinitionException {
        final String inputFolderEnvVar = System.getProperty("CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER",
                System.getenv("CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER"));
        final Path inputFolder;
        if (inputFolderEnvVar != null) {
            // Get the path of the input folder from system variables
            inputFolder = new File(inputFolderEnvVar).toPath();
        } else {
            // Throw if the environment variable has not been set
            throw new BatchDefinitionException("CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER system property or " +
                    "environment variable needs set");
        }

        // If the inputFolder is a directory then get a list of file paths from each file that matches the glob filter
        // within the input directory and all if it's subsequent directories. Else return an empty List.
        final List<Path> filteredDocumentPaths;
        try {
            filteredDocumentPaths = Files.isDirectory(inputFolder) ?
                    listFiles(inputFolderEnvVar, globFilter) : new ArrayList<>();
        } catch (IOException ex) {
            logger.error("Failed to enumerate property files", ex);
            throw new BatchDefinitionException("Failed to create a batch of items matching filter supplied", ex);
        }
        return filteredDocumentPaths;
    }

    /**
     * Store the document within the DataStore and return the storage reference.
     *
     * @param documentPath the path of the document to store
     * @param namespaceParams the namespace parameters map with processing instructions containing the DataStore partial
     *                        reference key
     * @return the storage reference of the document
     * @throws BatchDefinitionException if the storage reference returned from the DataStore is null
     */
    private String storeDocument(final Path documentPath, final Map<String, Map<String, String>> namespaceParams)
            throws BatchDefinitionException {
        final String storageReference;
        try {
            final Map<String, String> processingInstructions = namespaceParams.get("pi");
            final String datastorePartialReference =
                    processingInstructions.get(GlobFilterWorkerConstants.DATASTORE_PARTIAL_REFERENCE_KEY);
            storageReference = dataStore.store(documentPath, datastorePartialReference);
        } catch (DataStoreException e) {
            logger.error("Unable to store file in storage", e);
            throw new BatchDefinitionException("Unable to store file in storage", e);
        }
        // The storage reference should not be null
        if (storageReference == null || storageReference.isEmpty()) {
            throw new BatchDefinitionException("A storage reference was not returned from the data store.");
        }
        return storageReference;
    }

    /**
     * Register a List of TaskMessages to send with BatchWorkerServices
     *
     * @param batchWorkerServices the BatchWorkerServices to register messages as item subtasks with
     * @param taskMessagesToSend a List of TaskMessages for BatchWorkerServices registration
     */
    private static void registerMessages(final BatchWorkerServices batchWorkerServices,
                                  final List<TaskMessage> taskMessagesToSend) {
        logger.info("Registering TaskMessages to send with BatchWorkerServices");
        // Register each message as an item subtask with BatchWorkerServices
        for (TaskMessage message : taskMessagesToSend) {
            batchWorkerServices.registerItemSubtask(message.getTaskClassifier(), message.getTaskApiVersion(),
                    message.getTaskData());
        }
    }

    /**
     * Returns a list of file paths for each file that matches the glob filter within the input directory and all of
     * it's subsequent directories.
     *
     * @param inputDir the input folder to apply glob filter file matching
     * @param globFilter the glob filter used to perform file matching
     * @return a List of paths belonging to files matched within the input folder and it's subsequent directories
     * @throws IOException if an I/O error is thrown by one of the SimpleFileVisitor methods
     */
    private static List<Path> listFiles(final String inputDir, final String globFilter) throws IOException {
        final List<Path> fileList = new ArrayList<>();
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + inputDir + "/" + globFilter);
        Files.walkFileTree(Paths.get(inputDir), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                if (matcher.matches(file)) {
                    fileList.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        return fileList;
    }
}
