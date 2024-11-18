/*
 * Copyright 2017-2024 Open Text.
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
package com.github.jobservice.workers.batch.plugins;

import com.github.cafdataprocessing.workers.document.DocumentWorkerFieldEncoding;
import com.github.cafdataprocessing.workers.document.DocumentWorkerFieldValue;
import com.github.cafdataprocessing.workers.document.DocumentWorkerTask;
import com.github.jobservice.messagebuilder.TaskMessage;
import com.github.jobservice.workers.batch.plugins.messagebuilder.GlobFilterWorkerTaskMessageBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

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
        assertEquals(newFieldValue, resultNewField1.get(0).data,
                "New field 1 should be mapped to the result task data fields");
        final List<DocumentWorkerFieldValue> resultNewField2 = resultTaskDataFields.get(newFieldKey2);
        assertEquals(newFieldValue2, resultNewField2.get(0).data,
                "New field 2 should be mapped to the result task data fields");

        // Validate expected fields
        final List<DocumentWorkerFieldValue> resultFileNameField = resultTaskDataFields.get(fileNameFieldName);
        assertEquals(documentFileName, resultFileNameField.get(0).data,
                "File name should be mapped to the result task data fields");
        final List<DocumentWorkerFieldValue> resultBinaryFileReferenceField =
                resultTaskDataFields.get(binaryFileReferenceFieldName);
        assertEquals(storageReference, resultBinaryFileReferenceField.get(0).data,
                "Binary File Reference should be mapped to the result task data fields");
        final List<DocumentWorkerFieldValue> resultBinaryFileField =
                resultTaskDataFields.get(binaryFileFieldName);
        assertEquals(storageReference, resultBinaryFileField.get(0).data,
                "Binary File should be mapped to the result task data fields with data");
        assertEquals(DocumentWorkerFieldEncoding.storage_ref, resultBinaryFileField.get(0).encoding,
                "Binary File should be mapped to the result task data fields with encoding");

        // Validate expected custom data
        final Map<String, String> resultTaskDataCustomData = ((DocumentWorkerTask) result.getTaskData()).customData;
        assertEquals(customDataValue, resultTaskDataCustomData.get(customDataKey),
                "Custom data field 1 should be mapped to the result task data custom data");
        assertEquals(customDataValue2, resultTaskDataCustomData.get(customDataKey2),
                "Custom data field 2 should be mapped to the result task data custom data");
    }
}
