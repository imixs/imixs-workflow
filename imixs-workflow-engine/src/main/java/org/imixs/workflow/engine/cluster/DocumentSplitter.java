/****************************************************************************
 * Copyright (c) 2026 Imixs Software Solutions GmbH and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/
package org.imixs.workflow.engine.cluster;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DocumentSplitter implements Iterable<byte[]> {

    public int CHUNK_SIZE = 1048576; // 1mb

    private byte[] filedata = null;

    public DocumentSplitter(byte[] filedata) {
        super();
        this.filedata = filedata;
    }

    @Override
    public Iterator<byte[]> iterator() {

        return new ChunkIterator();

    }

    // Inner class to iterate the bytes in 2mb chunks
    private class ChunkIterator implements Iterator<byte[]> {
        private int cursor;
        private byte[] data;

        public ChunkIterator() {
            this.cursor = 0;
            // fetch the whole data in one array
            data = DocumentSplitter.this.filedata; // .getBytes();
        }

        public boolean hasNext() {
            return this.cursor < data.length;
        }

        public byte[] next() {
            if (this.hasNext()) {
                byte[] chunk;
                // check byte count from cursor...
                if (data.length > cursor + CHUNK_SIZE) {
                    chunk = Arrays.copyOfRange(data, cursor, cursor + CHUNK_SIZE);
                    cursor = cursor + CHUNK_SIZE;
                } else {
                    // read last junk
                    chunk = Arrays.copyOfRange(data, cursor, data.length);
                    cursor = data.length;
                }
                return chunk;
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
