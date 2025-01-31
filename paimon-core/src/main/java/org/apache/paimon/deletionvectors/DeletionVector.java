/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.deletionvectors;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * The DeletionVector can efficiently record the positions of rows that are deleted in a file, which
 * can then be used to filter out deleted rows when processing the file.
 */
public interface DeletionVector {

    /**
     * Marks the row at the specified position as deleted.
     *
     * @param position The position of the row to be marked as deleted.
     */
    void delete(long position);

    /**
     * Marks the row at the specified position as deleted.
     *
     * @param position The position of the row to be marked as deleted.
     * @return true if the added position wasn't already deleted. False otherwise.
     */
    default boolean checkedDelete(long position) {
        if (isDeleted(position)) {
            return false;
        } else {
            delete(position);
            return true;
        }
    }

    /**
     * Checks if the row at the specified position is marked as deleted.
     *
     * @param position The position of the row to check.
     * @return true if the row is marked as deleted, false otherwise.
     */
    boolean isDeleted(long position);

    /**
     * Determines if the deletion vector is empty, indicating no deletions.
     *
     * @return true if the deletion vector is empty, false if it contains deletions.
     */
    boolean isEmpty();

    /**
     * Serializes the deletion vector to a byte array for storage or transmission.
     *
     * @return A byte array representing the serialized deletion vector.
     */
    byte[] serializeToBytes();

    /**
     * Deserializes a deletion vector from a byte array.
     *
     * @param bytes The byte array containing the serialized deletion vector.
     * @return A DeletionVector instance that represents the deserialized data.
     */
    static DeletionVector deserializeFromBytes(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                DataInputStream dis = new DataInputStream(bis)) {
            int magicNum = dis.readInt();
            if (magicNum == BitmapDeletionVector.MAGIC_NUMBER) {
                return BitmapDeletionVector.deserializeFromDataInput(dis);
            } else {
                throw new RuntimeException("Invalid magic number: " + magicNum);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to deserialize deletion vector", e);
        }
    }
}
