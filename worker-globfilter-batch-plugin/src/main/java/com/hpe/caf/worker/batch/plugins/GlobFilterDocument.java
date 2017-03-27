package com.hpe.caf.worker.batch.plugins;

public final class GlobFilterDocument {

    private final String storageReference;
    private final String fileName;

    public GlobFilterDocument(final String storageReference, final String fileName){
        this.storageReference = storageReference;
        this.fileName = fileName;
    }

    public String getStorageReference(){
        return this.storageReference;
    }

    public String getFileName() {
        return this.fileName;
    }
}
