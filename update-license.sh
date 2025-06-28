#!/bin/bash

# Define the new license header
read -r -d '' NEW_HEADER <<'EOF'
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
EOF

# Find all Java files in the project
find . -name "*.java" | while read -r file; do
    echo "Processing: $file"

    # Read the first line
    first_line=$(head -n 1 "$file")

    # Case 1: existing comment block at the very top
    if [[ "$first_line" =~ ^[[:space:]]*/\* ]]; then
        echo " -> Found header comment at line 1. updating it."
        # Remove the first comment block (header) only
        awk '
            BEGIN {in_header=1}
            NR==1 && $0 !~ /^\/\*/ {in_header=0}
            in_header && $0 ~ /\*\// {getline; in_header=0}
            !in_header {print}
        ' "$file" > "$file.tmp"

        {
            echo "$NEW_HEADER"
            cat "$file.tmp"
        } > "$file"

        rm "$file.tmp"

    # Case 2: no header, file starts with package or import
    elif [[ "$first_line" =~ ^[[:space:]]*(package|import) ]]; then
        echo " -> No license header found. Inserting new header."

        mv "$file" "$file.tmp"
        {
            echo "$NEW_HEADER"
            echo ""
            cat "$file.tmp"
        } > "$file"
        rm "$file.tmp"

    # Case 3: some other structure; skip for safety
    else
        echo " -> No recognizable header structure. Skipping."
    fi
done

echo "Done."
