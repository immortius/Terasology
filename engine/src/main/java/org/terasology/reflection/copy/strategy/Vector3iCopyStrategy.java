/*
 * Copyright 2013 MovingBlocks
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
package org.terasology.reflection.copy.strategy;

import org.terasology.math.geom.Vector3i;
import org.terasology.reflection.copy.CopyStrategy;
import org.terasology.reflection.copy.RegisterCopyStrategy;

/**
 * @author Immortius
 */
@RegisterCopyStrategy
public class Vector3iCopyStrategy implements CopyStrategy<Vector3i> {

    @Override
    public Vector3i copy(Vector3i value) {
        if (value != null) {
            return new Vector3i(value);
        }
        return null;
    }
}
