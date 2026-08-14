/*
 * This file is part of veraPDF wcag algorithms, a module of the veraPDF project.
 * Copyright (c) 2015-2026, veraPDF Consortium <info@verapdf.org>
 * All rights reserved.
 *
 * veraPDF wcag algorithms is free software: you can redistribute it and/or modify
 * it under the terms of either:
 *
 * The GNU General public license GPLv3+.
 * You should have received a copy of the GNU General Public License
 * along with veraPDF wcag algorithms as the LICENSE.GPL file in the root of the source
 * tree.  If not, see http://www.gnu.org/licenses/ or
 * https://www.gnu.org/licenses/gpl-3.0.en.html.
 *
 * The Mozilla Public License MPLv2+.
 * You should have received a copy of the Mozilla Public License along with
 * veraPDF wcag algorithms as the LICENSE.MPL file in the root of the source tree.
 * If a copy of the MPL was not distributed with this file, you can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.verapdf.wcag.algorithms.semanticalgorithms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.verapdf.wcag.algorithms.entities.content.LineChunk;
import org.verapdf.wcag.algorithms.entities.tables.TableBorderBuilder;
import org.verapdf.wcag.algorithms.entities.tables.tableBorders.TableBorder;
import org.verapdf.wcag.algorithms.semanticalgorithms.containers.StaticContainers;

public class TableBorderDanglingLineTest {

    private static final Integer PAGE_NUMBER = 1;

    @BeforeEach
    public void setUp() {
        StaticContainers.updateContainers(null);
    }

    /**
     * Verifies that a horizontal line which overlaps only the rightmost vertical
     * border and extends past it does not create an extra empty column.
     *
     * <p>Regression test for: page-header separator line absorbed into a table
     * border builder produces a spurious narrow rightmost column.</p>
     */
    @Test
    public void testDanglingHorizontalLineDoesNotCreateExtraColumn() {
        TableBorderBuilder builder = new TableBorderBuilder(
                new LineChunk(PAGE_NUMBER, 100.0, 400.0, 300.0, 400.0));

        // Two real columns: vertical lines at x=100, 200, 300.
        builder.addLine(new LineChunk(PAGE_NUMBER, 100.0, 100.0, 100.0, 400.0));
        builder.addLine(new LineChunk(PAGE_NUMBER, 200.0, 100.0, 200.0, 400.0));
        builder.addLine(new LineChunk(PAGE_NUMBER, 300.0, 100.0, 300.0, 400.0));

        // Bottom border spanning both columns.
        builder.addLine(new LineChunk(PAGE_NUMBER, 100.0, 100.0, 300.0, 100.0));

        // Dangling header line: overlaps the rightmost vertical line at x=300 and
        // continues 50 pt beyond it. Without filtering this creates a 3rd column.
        builder.addLine(new LineChunk(PAGE_NUMBER, 300.0, 400.0, 350.0, 400.0));

        TableBorder tableBorder = new TableBorder(builder);

        Assertions.assertEquals(2, tableBorder.getNumberOfColumns(),
                "Dangling horizontal line endpoint must not create an extra column");
        Assertions.assertEquals(99.5, tableBorder.getLeftX(), 0.01,
                "Table left edge should remain at the leftmost vertical line");
        Assertions.assertEquals(300.5, tableBorder.getRightX(), 0.01,
                "Table right edge should be trimmed to the rightmost vertical line");
    }

    /**
     * Verifies that legitimate short horizontal lines inside a table are not
     * affected by the dangling-line cleanup.
     */
    @Test
    public void testNormalTableKeepsAllColumns() {
        TableBorderBuilder builder = new TableBorderBuilder(
                new LineChunk(PAGE_NUMBER, 100.0, 400.0, 300.0, 400.0));

        builder.addLine(new LineChunk(PAGE_NUMBER, 100.0, 100.0, 100.0, 400.0));
        builder.addLine(new LineChunk(PAGE_NUMBER, 200.0, 100.0, 200.0, 400.0));
        builder.addLine(new LineChunk(PAGE_NUMBER, 300.0, 100.0, 300.0, 400.0));
        builder.addLine(new LineChunk(PAGE_NUMBER, 100.0, 100.0, 300.0, 100.0));

        // Internal horizontal line that spans only the first column.
        builder.addLine(new LineChunk(PAGE_NUMBER, 100.0, 250.0, 200.0, 250.0));

        TableBorder tableBorder = new TableBorder(builder);

        Assertions.assertEquals(2, tableBorder.getNumberOfColumns(),
                "Normal internal horizontal lines must not remove columns");
        Assertions.assertEquals(99.5, tableBorder.getLeftX(), 0.01);
        Assertions.assertEquals(300.5, tableBorder.getRightX(), 0.01);
    }

    /**
     * Verifies that a partial-border table whose left/right edges are defined only
     * by horizontal line endpoints is still recognized.
     */
    @Test
    public void testPartialBorderTableKeepsEdgeColumns() {
        TableBorderBuilder builder = new TableBorderBuilder(
                new LineChunk(PAGE_NUMBER, 100.0, 400.0, 300.0, 400.0));

        // Top and bottom horizontal borders define the left/right edges.
        builder.addLine(new LineChunk(PAGE_NUMBER, 100.0, 400.0, 300.0, 400.0));
        builder.addLine(new LineChunk(PAGE_NUMBER, 100.0, 100.0, 300.0, 100.0));

        // One internal vertical line splits the table into two columns,
        // but there are no left/right vertical borders.
        builder.addLine(new LineChunk(PAGE_NUMBER, 200.0, 100.0, 200.0, 400.0));

        TableBorder tableBorder = new TableBorder(builder);

        Assertions.assertEquals(2, tableBorder.getNumberOfColumns(),
                "Partial-border table must keep its left and right edge columns");
        Assertions.assertEquals(99.5, tableBorder.getLeftX(), 0.01,
                "Table left edge should come from horizontal line endpoints");
        Assertions.assertEquals(300.5, tableBorder.getRightX(), 0.01,
                "Table right edge should come from horizontal line endpoints");
    }

}
