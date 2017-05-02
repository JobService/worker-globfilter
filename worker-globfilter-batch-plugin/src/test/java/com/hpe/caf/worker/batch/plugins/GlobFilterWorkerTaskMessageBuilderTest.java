/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

import com.hpe.caf.messagebuilder.TaskMessage;
import com.hpe.caf.worker.batch.plugins.messagebuilder.GlobFilterWorkerTaskMessageBuilder;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GlobFilterWorkerTaskMessageBuilderTest {

    /**
     * Testing that the GlobFilterWorkerTaskMessageBuilder performs as expected
     */
    @Test
    public void testGlobFilterWorkerTaskMessageBuilder() {

        final GlobFilterWorkerTaskMessageBuilder globFilterWorkerTaskMessageBuilder =
                new GlobFilterWorkerTaskMessageBuilder();

        final Map<String, Map<String, String>> namespaceParams = new HashMap<>();

        final Map<String, String> processingInstructions = new HashMap<>();
        final String dsPartialRefKey = "test-part-ref";
        processingInstructions.put(GlobFilterWorkerConstants.DATASTORE_PARTIAL_REFERENCE_KEY, dsPartialRefKey);
        namespaceParams.put("pi", processingInstructions);

        final Map<String, String> customDatas = new HashMap<>();
        final String customDataKey = "aCustomDataField";
        final String customDataValue = "aCustomDataFieldValue";
        customDatas.put(customDataKey, customDataValue);
        final String customDataKey2 = "aCustomDataField2";
        final String customDataValue2 = "aCustomDataFieldValue2";
        customDatas.put(customDataKey2, customDataValue2);
        namespaceParams.put("cd", customDatas);

        final Map<String, String> newFields = new HashMap<>();
        final String newFieldKey = "aNewField";
        final String newFieldValue = "aNewFieldValue";
        newFields.put(newFieldKey, newFieldValue);
        final String newFieldKey2 = "aNewField2";
        final String newFieldValue2 = "aNewFieldValue2";
        newFields.put(newFieldKey2, newFieldValue2);
        namespaceParams.put("newField", newFields);

        final Map<String, String> fields = new HashMap<>();
        final String fileName = "fileName";
        final String fileNameFieldName = "CUSTOM_FILE_NAME";
        fields.put(fileName, fileNameFieldName);
        final String binaryFile = "binaryFile";
        final String binaryFileFieldName = "CUSTOM_CONTENT";
        fields.put(binaryFile, binaryFileFieldName);
        final String binaryFileReference = "binaryFileReference";
        final String binaryFileReferenceFieldName = "CUSTOM_STORAGE_REFERENCE";
        fields.put(binaryFileReference, binaryFileReferenceFieldName);
        namespaceParams.put("field", fields);

        final String storageReference = UUID.randomUUID().toString();
        final String documentFileName = "testMockFileName.txt";

        final GlobFilterDocument globFilterDocument = new GlobFilterDocument(storageReference, documentFileName);

        final TaskMessage result = globFilterWorkerTaskMessageBuilder.buildMessage(globFilterDocument, namespaceParams);

        final Map<String, List<DocumentWorkerFieldValue>> resultTaskDataFields =
                ((DocumentWorkerTask) result.getTaskData()).fields;

        // Validate new fields
        final List<DocumentWorkerFieldValue> resultNewField1 = resultTaskDataFields.get(newFieldKey);
        Assert.assertEquals("New field 1 should be mapped to the result task data fields", newFieldValue,
                resultNewField1.get(0).data);
        final List<DocumentWorkerFieldValue> resultNewField2 = resultTaskDataFields.get(newFieldKey2);
        Assert.assertEquals("New field 2 should be mapped to the result task data fields", newFieldValue2,
                resultNewField2.get(0).data);

        // Validate expected fields
        final List<DocumentWorkerFieldValue> resultFileNameField = resultTaskDataFields.get(fileNameFieldName);
        Assert.assertEquals("File name should be mapped to the result task data fields", documentFileName,
                resultFileNameField.get(0).data);
        final List<DocumentWorkerFieldValue> resultBinaryFileReferenceField =
                resultTaskDataFields.get(binaryFileReferenceFieldName);
        Assert.assertEquals("Binary File Reference should be mapped to the result task data fields", storageReference,
                resultBinaryFileReferenceField.get(0).data);
        final List<DocumentWorkerFieldValue> resultBinaryFileField =
                resultTaskDataFields.get(binaryFileFieldName);
        Assert.assertEquals("Binary File should be mapped to the result task data fields with data", storageReference,
                resultBinaryFileField.get(0).data);
        Assert.assertEquals("Binary File should be mapped to the result task data fields with encoding",
                DocumentWorkerFieldEncoding.storage_ref, resultBinaryFileField.get(0).encoding);

        // Validate expected custom data
        final Map<String, String> resultTaskDataCustomData = ((DocumentWorkerTask) result.getTaskData()).customData;
        Assert.assertEquals("Custom data field 1 should be mapped to the result task data custom data", customDataValue,
                resultTaskDataCustomData.get(customDataKey));
        Assert.assertEquals("Custom data field 2 should be mapped to the result task data custom data",
                customDataValue2, resultTaskDataCustomData.get(customDataKey2));
    }
}
