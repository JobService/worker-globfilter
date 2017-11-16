/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class GlobFilterDocumentTest {

    /**
     * Testing that the GlobFilterDocument performs as expected
     */
    @Test
    public void testGlobFilterDocument() {
        final String storageReference = UUID.randomUUID().toString();
        final String fileName = "testMockFileName.txt";

        final GlobFilterDocument globFilterDocument = new GlobFilterDocument(storageReference, fileName);

        Assert.assertEquals("Storage Reference should be set correctly", storageReference,
                globFilterDocument.getStorageReference());

        Assert.assertEquals("File Name should be set correctly", fileName, globFilterDocument.getFileName());
    }
}
