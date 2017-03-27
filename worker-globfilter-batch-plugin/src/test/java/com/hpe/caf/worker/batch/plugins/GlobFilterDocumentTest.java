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
