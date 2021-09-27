/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class NewRAMDataAccessTest implements NewDataAccessTest {

    private String path;

    @BeforeEach
    void setup(@TempDir Path path) {
        this.path = path.resolve("test_ram_file").toAbsolutePath().toString();
    }

    @Override
    public NewDataAccess create(int bytesPerSegment) {
        return new NewRAMDataAccess(bytesPerSegment);
    }

    @Override
    public void flush(NewDataAccess da) {
        NewRAMDataAccess.flush((NewRAMDataAccess) da, path);
    }

    @Override
    public NewDataAccess load() {
        return NewRAMDataAccess.load(path);
    }
}
