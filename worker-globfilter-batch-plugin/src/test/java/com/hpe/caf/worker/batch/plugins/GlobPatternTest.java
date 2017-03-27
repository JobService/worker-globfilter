package com.hpe.caf.worker.batch.plugins;

import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.messagebuilder.TaskMessage;
import com.hpe.caf.worker.batch.BatchDefinitionException;
import com.hpe.caf.worker.batch.BatchWorkerServices;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class GlobPatternTest {
    BatchWorkerServices testWorkerServices = null;
    Map<String, String> testTaskMessageParams = null;
    int subBatchCount = 0;

    @Test
    public void testGlobPatternGetDocs() throws BatchDefinitionException {
        setBinaryDataInputFolderAsIfInContainer();

        final String globTaskFilter = "sub-dir/**.txt";

        // Get the documents from test resources that match the glob filter
        final List<Path> documentMatches = new GlobPattern().getDocumentMatches(globTaskFilter);
        if (documentMatches.size() != 2) {
            Assert.fail("Expected number of document matches not returned");
        }
    }

    /**
     * Testing that a simple glob filter batch definition of 'sub-dir/**.txt' constructs 2 tasks, registers each with
     * BatchWorkerServices and that each task has been constructed as expected.
     */
    @Test
    public void testGlobFilterBatchPlugin() throws BatchDefinitionException, DataStoreException {
        setBinaryDataInputFolderAsIfInContainer();

        final String globTaskFilter = "sub-dir/**.txt";

        // Get the documents from test resources that match the glob filter
        final List<Path> documentMatches = new GlobPattern().getDocumentMatches(globTaskFilter);
        if (documentMatches.size() != 2) {
            Assert.fail("Expected number of document matches not returned");
        }

        final String dataStorePartialReferenceValue = UUID.randomUUID().toString().replace("-", "");

        // Initialize a collection for containing TaskMessages
        final Collection<TaskMessage> constructedTaskMessages = new ArrayList<>();
        // Create an implementation of BatchWorkerServices for the purposes of testing. When registerItemSubTask is
        // called it will add the TaskMessage to the constructedTaskMessages collection.
        testWorkerServices = createTestBatchWorkerServices(constructedTaskMessages, dataStorePartialReferenceValue);

        final String customFileName = "CUSTOM_FILE_NAME";
        final String customBinaryFileFieldName = "CUSTOM_CONTENT";
        final String customBinaryFileReferenceFieldName = "CUSTOM_STORAGE_REFERENCE";

        final String customDataField = "aCustomDataField";
        final String customDataFieldValue = "aCustomDataFieldValue";

        final AbstractMap.SimpleEntry<String, String> taskMessageParamsCustomDataEntry =
                new AbstractMap.SimpleEntry<>("cd:" + customDataField, customDataFieldValue);

        // Create Glob filter plugin task message params for testing
        testTaskMessageParams = createTaskMessageParams(
                new AbstractMap.SimpleEntry<>("pi:" + GlobFilterWorkerConstants.DATASTORE_PARTIAL_REFERENCE_KEY,
                        dataStorePartialReferenceValue),
                taskMessageParamsCustomDataEntry,
                new AbstractMap.SimpleEntry<>("newField:aNewField", "aNewFieldValue"),
                new AbstractMap.SimpleEntry<>("aFieldWithNoNamespace", "aFieldWithNoNamespaceValue"),
                new AbstractMap.SimpleEntry<>("field:fileName", customFileName),
                new AbstractMap.SimpleEntry<>("field:binaryFile", customBinaryFileFieldName),
                new AbstractMap.SimpleEntry<>("field:binaryFileReference",
                        customBinaryFileReferenceFieldName));

        // Create an instance of the glob filter plugin and process the batch
        final GlobPattern plugin = new GlobPattern();
        plugin.processBatch(testWorkerServices, globTaskFilter, "DocumentMessage", testTaskMessageParams);

        // Verify that expected number of sub batches were registered
        Assert.assertEquals("Expecting sub-batches to be created.", 0, subBatchCount);

        // Verify that expected number of messages were registered
        Assert.assertEquals("Expecting same number of task messages generated as references we had on batch" +
                " definition.", documentMatches.size(), constructedTaskMessages.size());

        // For each task message created by the plugin assert that the fields are set as expected
        for (final TaskMessage returnedMessage : constructedTaskMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerTask taskData = (DocumentWorkerTask) returnedMessage.getTaskData();
            Assert.assertNotNull("Expecting task data returned to not be null.", taskData);

            // Verify that the customData has been set correctly
            final String taskDataCustomDataFieldValue = taskData.customData.get(customDataField);
            Assert.assertEquals("Expecting customData to be returned as expected", customDataFieldValue,
                    taskDataCustomDataFieldValue);

            // Verify that the CONTENT field data is correct
            final String expectedContentKeyValue = dataStorePartialReferenceValue + "/" + "mockRefId";
            Assert.assertEquals(expectedContentKeyValue,
                    taskData.fields.get(customBinaryFileFieldName).get(0).data);
            // Verify that the CONTENT field encoding is correct
            final DocumentWorkerFieldEncoding expectedDocumentWorkerFieldEncoding =
                    DocumentWorkerFieldEncoding.storage_ref;
            Assert.assertEquals(expectedDocumentWorkerFieldEncoding,
                    taskData.fields.get(customBinaryFileFieldName).get(0).encoding);

            // Verify that the STORAGE_REFERENCE field data is correct
            Assert.assertEquals(expectedContentKeyValue,
                    taskData.fields.get(customBinaryFileReferenceFieldName).get(0).data);

            // Verify that the FILE_NAME field contains an expected file name
            final String fileName = taskData.fields.get(customFileName).get(0).data;

            Assert.assertTrue("FILE_NAME field should contain file name that we expect",
                    documentMatches.stream().anyMatch(path -> path.toFile().getName().equals(fileName)));
            for (int i = 0; i < documentMatches.size(); i++) {
                if (documentMatches.get(i).toFile().getName().endsWith(fileName)) {
                    documentMatches.remove(i);
                }
            }
        }
    }

    private void setBinaryDataInputFolderAsIfInContainer() {
        // Use the example-files directory within the test resources that contains files that will match filtering
        String pathToTestItems = new File(getClass().getClassLoader().getResource(
                "example-files").getFile()).getPath();

        // if happens to be on windows, turn to valid glob filter which is linux type c:/xyz/*.txt.
        pathToTestItems = pathToTestItems.replace("\\", "/");

        // Set the system prop that Glob Filter plugin uses to find the input folder
        System.setProperty("CAF_GLOB_WORKER_BINARY_DATA_INPUT_FOLDER", pathToTestItems);
    }

    @After
    public void reset(){
        testWorkerServices = null;
        testTaskMessageParams = null;
        subBatchCount = 0;
    }

    private static void checkClassifierAndApiVersion(final TaskMessage returnedMessage) {
        Assert.assertEquals("Expecting task api version to be that defined in Test builder.",
                            DocumentWorkerConstants.WORKER_API_VER, returnedMessage.getTaskApiVersion());
        Assert.assertEquals("Expecting task classifier to be that defined in Test builder.",
                            DocumentWorkerConstants.WORKER_NAME, returnedMessage.getTaskClassifier());
    }

    private static Map<String, String> createTaskMessageParams(final Map.Entry<String, String>... entries) {
        final Map<String, String> testTaskMessageParams = new HashMap<>();
        for (final Map.Entry<String, String> entry : entries) {
            testTaskMessageParams.put(entry.getKey(), entry.getValue());
        }
        return testTaskMessageParams;
    }

    private static BatchWorkerServices createTestBatchWorkerServices(
            final Collection<TaskMessage> constructedTaskMessages,
            final String datastorePartialReferenceValue)
            throws DataStoreException {
        return new BatchWorkerServices() {
            private final Map<Class<DataStore>, Object> serviceMap = createServiceMap();

            private Map<Class<DataStore>, Object> createServiceMap() throws DataStoreException {
                // Mock DataStore
                final String mockRefId = "mockRefId";
                final DataStore mockDataStore = mock(DataStore.class);
                when(mockDataStore.store(any(Path.class), any(String.class))).thenReturn(datastorePartialReferenceValue
                        + "/" + mockRefId);
                // Add the mocked data store service to the service map and return that map
                final Map<Class<DataStore>, Object> serviceMap = new HashMap<>();
                serviceMap.put(DataStore.class, mockDataStore);
                return serviceMap;
            };

            @Override
            public void registerBatchSubtask(final String batchDefinition) {
                // Unused method
            }

            @Override
            public void registerItemSubtask(final String taskClassifier, final int taskApiVersion,
                                            final Object taskData) {
                //store this as a task message so we can refer to it when verifying results
                final TaskMessage message = new TaskMessage();
                message.setTaskApiVersion(taskApiVersion);
                message.setTaskClassifier(taskClassifier);
                message.setTaskData(taskData);
                constructedTaskMessages.add(message);
            }

            @Override
            public <S> S getService(final Class<S> service) {
                return (S) serviceMap.get(service);
            }
        };
    }
}
