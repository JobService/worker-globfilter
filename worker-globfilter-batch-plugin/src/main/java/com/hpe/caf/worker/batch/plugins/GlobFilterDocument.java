/*
 * Copyright 2017-2020 Micro Focus or one of its affiliates.
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
