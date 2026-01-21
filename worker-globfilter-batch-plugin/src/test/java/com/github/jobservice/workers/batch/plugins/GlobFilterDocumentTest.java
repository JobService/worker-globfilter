/*
 * Copyright 2017-2026 Open Text.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

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

        assertEquals(storageReference, globFilterDocument.getStorageReference(),
                "Storage Reference should be set correctly");

        assertEquals(fileName, globFilterDocument.getFileName(), "File Name should be set correctly");
    }
}
