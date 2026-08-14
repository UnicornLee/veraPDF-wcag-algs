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
package org.verapdf.wcag.algorithms.semanticalgorithms.consumers;

import org.verapdf.wcag.algorithms.entities.content.LineChunk;
import org.verapdf.wcag.algorithms.entities.geometry.Vertex;
import org.verapdf.wcag.algorithms.entities.tables.TableBorderBuilder;
import org.verapdf.wcag.algorithms.entities.tables.TableBordersCollection;
import org.verapdf.wcag.algorithms.semanticalgorithms.containers.StaticContainers;
import org.verapdf.wcag.algorithms.semanticalgorithms.utils.WCAGProgressStatus;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LinesPreprocessingConsumer extends WCAGConsumer {

    private static final Logger LOGGER = Logger.getLogger(LinesPreprocessingConsumer.class.getCanonicalName());

    private static final double MAX_LINE_WIDTH = 5.0;
    private static final int MAX_NUMBER_OF_LINES_FOR_PAGE = 5000;

    private List<List<TableBorderBuilder>> tableBorders;

    @Override
    public boolean run() {
        if (!StaticContainers.isHuman()) {
            return false;
        }
        if (!startStep()) {
            return true;
        }
        findTableBorders();
        StaticContainers.setTableBordersCollection(new TableBordersCollection(getTableBorders()));
        return false;
    }

    public List<List<TableBorderBuilder>> getTableBorders() {
        if (tableBorders == null) {
            findTableBorders();
        }
        return tableBorders;
    }

    public void findTableBorders() {
        tableBorders = new LinkedList<>();
        for (int pageNumber = 0; pageNumber < StaticContainers.getDocument().getNumberOfPages(); pageNumber++) {
            tableBorders.add(findTableBorders(pageNumber));
        }
    }

    private List<TableBorderBuilder> findTableBorders(Integer pageNumber) {
        List<TableBorderBuilder> tableBorders = new ArrayList<>();
        if (StaticContainers.getLinesCollection().getTotalNumberOfLines(pageNumber) > MAX_NUMBER_OF_LINES_FOR_PAGE) {
            LOGGER.log(Level.WARNING, "There are over {0} lines on the page {1}. Table border finding will be skipped",
                    new Object[]{MAX_NUMBER_OF_LINES_FOR_PAGE, pageNumber + 1});
            return tableBorders;
        }
        Set<LineChunk> set = new HashSet<>(StaticContainers.getLinesCollection().getHorizontalLines(pageNumber));
        set.addAll(StaticContainers.getLinesCollection().getVerticalLines(pageNumber));
        set.addAll(StaticContainers.getLinesCollection().getSquares(pageNumber));
        for (LineChunk line : set) {
            if (line.getWidth() > MAX_LINE_WIDTH) {
                continue;
            }
            boolean separateTableBorder = true;
            for (TableBorderBuilder border : tableBorders) {
                boolean isCross = false;
                if (line.isHorizontalLine()) {
                    for (LineChunk verticalLine : border.getVerticalLines()) {
                        Vertex vv = LineChunk.getIntersectionVertex(line, verticalLine);
                        if (vv != null) {
                            border.addVertex(vv);
                            isCross = true;
                        }
                    }
                } else if (line.isVerticalLine()) {
                    for (LineChunk horizontalLine : border.getHorizontalLines()) {
                        Vertex vv = LineChunk.getIntersectionVertex(horizontalLine, line);
                        if (vv != null) {
                            border.addVertex(vv);
                            isCross = true;
                        }
                    }
                } else if (line.isSquare()) {
                    for (Vertex vertex : border.getVertexes()) {
                        if (Vertex.areCloseVertexes(vertex, line.getStart())) {
                            isCross = true;
                            break;
                        }
                    }
                }
                if (isCross) {
                    border.addLine(line);
                    separateTableBorder = false;
                    break;
                }
            }
            if (separateTableBorder) {
                TableBorderBuilder border = new TableBorderBuilder(line);
                tableBorders.add(border);
            }
        }
        mergeTableBorders(tableBorders);
        for (int i = 0; i < tableBorders.size();) {
            TableBorderBuilder border = tableBorders.get(i);
            if (border.getVertexesNumber() <= 2 || border.getHorizontalLinesNumber() == 0 ||
                    border.getVerticalLinesNumber() == 0) {
                tableBorders.remove(i);
            } else {
                i++;
            }
        }
        for (int i = 0; i < tableBorders.size();) {
            TableBorderBuilder border = tableBorders.get(i);
            if ((border.getHorizontalLinesNumber() <= 2 && border.getVerticalLinesNumber() <= 1) ||
                    (border.getHorizontalLinesNumber() <= 1 && border.getVerticalLinesNumber() <= 2)) {
                tableBorders.remove(i);
            } else {
                i++;
            }
        }
        List<TableBorderBuilder> filterTableBorders = new ArrayList<>();
        for (TableBorderBuilder border : tableBorders) {
            List<LineChunk> verticalLines = border.getVerticalLines().stream().collect(Collectors.toList());
            verticalLines.sort(Comparator.comparingDouble(item -> item.getLeftX()));
            List<LineChunk> horizontalLines = border.getHorizontalLines().stream().collect(Collectors.toList());
            horizontalLines.sort(Comparator.comparingDouble(item -> item.getTopY()));
            boolean isAllNotBlack = true;
            for (LineChunk verticalLine : verticalLines) {
                double[] strokeColor = verticalLine.getStrokeColor();
                if (strokeColor == null || (strokeColor.length == 3 &&
                        strokeColor[0] == 0.0 && strokeColor[1] == 0.0 && strokeColor[2] == 0.0)) {
                    isAllNotBlack = false;
                    break;
                }
            }
            if (!isAllNotBlack) {
                for (LineChunk horizontalLine : horizontalLines) {
                    double[] strokeColor = horizontalLine.getStrokeColor();
                    if (strokeColor == null || (strokeColor.length == 3 &&
                            strokeColor[0] == 0.0 && strokeColor[1] == 0.0 && strokeColor[2] == 0.0)) {
                        isAllNotBlack = false;
                        break;
                    }
                }
            }
            List<List<LineChunk>> groupVerticalLines = new ArrayList<>();
            List<LineChunk> group = new ArrayList<>();
            for (int i = 0; i < verticalLines.size(); i++) {
                LineChunk lineChunk = verticalLines.get(i);
                if (i == 0) {
                    group.add(lineChunk);
                }
                if (Math.abs(lineChunk.getLeftX() - group.get(0).getLeftX()) < 2) {
                    group.add(lineChunk);
                } else {
                    groupVerticalLines.add(group);
                    group = new ArrayList<>();
                    group.add(lineChunk);
                }
            }
            if (group.size() > 0) {
                groupVerticalLines.add(group);
            }
            List<List<LineChunk>> groupHorizontalLines = new ArrayList<>();
            group = new ArrayList<>();
            for (int i = 0; i < horizontalLines.size(); i++) {
                LineChunk lineChunk = horizontalLines.get(i);
                if (i == 0) {
                    group.add(lineChunk);
                }
                if (Math.abs(lineChunk.getTopY() - group.get(0).getTopY()) < 2) {
                    group.add(lineChunk);
                } else {
                    groupHorizontalLines.add(group);
                    group = new ArrayList<>();
                    group.add(lineChunk);
                }
            }
            if (group.size() > 0) {
                groupHorizontalLines.add(group);
            }

            if (!isAllNotBlack || groupVerticalLines.size() > 2 || groupHorizontalLines.size() > 2) {
                filterTableBorders.add(border);
            }
            StaticContainers.getLinesCollection().getVerticalLines(pageNumber).removeAll(border.getVerticalLines());
            StaticContainers.getLinesCollection().getHorizontalLines(pageNumber).removeAll(border.getHorizontalLines());

        }
        return filterTableBorders;
    }

    private void mergeTableBorders(List<TableBorderBuilder> tableBorders) {
        for (int i = tableBorders.size() - 2; i >= 0; i--) {
            TableBorderBuilder border = tableBorders.get(i);
            List<Integer> indexes = new LinkedList<>();
            for (int j = tableBorders.size() - 1; j > i; j--) {
                TableBorderBuilder border2 = tableBorders.get(j);
                if (border.isConnectedBorder(border2)) {
                    indexes.add(j);
                }
            }
            for (Integer index : indexes) {
                border.mergeBorder(tableBorders.get(index));
                tableBorders.remove((int)index);
            }
        }
    }

    @Override
    public WCAGProgressStatus getWCAGProgressStatus() {
        return WCAGProgressStatus.LINES_PREPROCESSING;
    }
}
