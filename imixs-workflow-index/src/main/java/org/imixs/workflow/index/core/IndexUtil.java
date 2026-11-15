/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
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

package org.imixs.workflow.index.core;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.imixs.workflow.engine.index.DefaultOperator;
import org.imixs.workflow.exceptions.IndexException;

/**
 * The APIUtil provides static values
 * 
 * @author Ralph Soika
 * @version 1.0
 *
 */
public class IndexUtil {

    private static Logger logger = Logger.getLogger(IndexUtil.class.getName());

    /**
     * Verifies that an existing index can be read by the current Lucene version.
     * Lucene only supports indices created by the current or the previous major
     * version. An index created by an older version (e.g. Lucene 7) can not be
     * opened and must be rebuilt.
     * <p>
     * The method does not delete an outdated index automatically. Instead it logs
     * a warning and throws an IndexException, so an administrator can decide how
     * to handle the existing index (e.g. backup, move or delete).
     *
     * @param directory - the index directory to be verified
     * @param path      - the path of the directory, used for logging
     * @throws IOException
     */
    public static void verifyIndexFormat(Directory directory, String path) throws IOException {
        try {
            // reads only the latest commit point - a cheap operation
            SegmentInfos.readLatestCommit(directory);
        } catch (IndexFormatTooOldException e) {
            logger.log(Level.WARNING, "├── ⚠️ index directory ''{0}'' was created by an outdated Lucene version!",
                    path);
            logger.log(Level.WARNING, "│   ├── the index can not be read by Lucene {0}", Version.LATEST);
            logger.log(Level.WARNING, "│   ├── {0}", e.getMessage());
            logger.log(Level.WARNING, "│   └── 👉 remove or move the index directory ''{0}'' and the taxonomy"
                    + " directory - the index will be rebuilt automatically on next startup", path);
            throw new IndexException(IndexException.INVALID_INDEX,
                    "Index directory '" + path + "' was created by an outdated Lucene version", e);
        }
    }

    /**
     * Returns in instance of a QueyParser based on a KeywordAnalyzer. The method
     * set the lucene DefaultOperator to 'OR' if not specified otherwise.
     *
     * @see issue #28 - normalizeSearchTerm
     * @param defaultOperator
     * @return QueryParser
     */
    public static QueryParser createQueryParser(DefaultOperator defaultOperator) {
        // use the keywordAnalyzer for searching a search term.
        QueryParser parser = new QueryParser("content", new KeywordAnalyzer());
        if (defaultOperator == DefaultOperator.OR) {
            parser.setDefaultOperator(QueryParser.Operator.OR);
        } else {
            parser.setDefaultOperator(QueryParser.Operator.AND);
        }

        // set setSplitOnWhitespace (issue #438)
        // we do no longer support a config parameter here!
        parser.setSplitOnWhitespace(true);
        return parser;
    }
}