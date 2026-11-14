/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
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

package org.imixs.workflow.index.vectors;

/**
 * A single result of a vector search.
 *
 * @param uniqueId   - the $uniqueid of the business document
 * @param similarity - the cosine similarity of the best matching chunk of the
 *                   document, in the range -1 to 1 (1 = identical direction)
 */
public record VectorMatch(String uniqueId, float similarity) {
}