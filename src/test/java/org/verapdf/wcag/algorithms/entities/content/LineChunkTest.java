/*
 * This file is part of veraPDF wcag algorithms, a module of the veraPDF project.
 * Copyright (c) 2015-2026, veraPDF Consortium <info@verapdf.org>
 * All rights reserved.
 *
 * veraPDF wcag algorithms is free software: you can redistribute it and/or modify
 * it under the terms of either:
 *
 * The GNU General public license GPLv3+.
 * The Mozilla Public License MPLv2+.
 */
package org.verapdf.wcag.algorithms.entities.content;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LineChunkTest {

    @Test
    void preservesRgbStrokeColor() {
        LineChunk line = new LineChunk(1, 0, 0, 10, 0, 1, new double[]{0.1, 0.2, 0.3});

        assertThat(line.getStrokeColor()).containsExactly(0.1, 0.2, 0.3);
        assertThat(line.toString()).contains("strokeColor=[0.1, 0.2, 0.3]");
    }

    @Test
    void treatsNonRgbStrokeColorAsUnknown() {
        LineChunk line = new LineChunk(1, 0, 0, 10, 0, 1, new double[]{0.5});

        assertThat(line.getStrokeColor()).isNull();
        assertThat(line.toString()).contains("strokeColor=null");
    }

    @Test
    void supportsUnknownStrokeColor() {
        LineChunk line = new LineChunk(1, 0, 0, 10, 0, 1, null);

        assertThat(line.getStrokeColor()).isNull();
        assertThat(line.toString()).contains("strokeColor=null");
    }
}
