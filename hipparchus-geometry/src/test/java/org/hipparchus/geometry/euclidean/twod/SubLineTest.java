/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This is not the original file distributed by the Apache Software Foundation
 * It has been modified by the Hipparchus project
 */
package org.hipparchus.geometry.euclidean.twod;

import org.hipparchus.geometry.euclidean.oned.Euclidean1D;
import org.hipparchus.geometry.euclidean.oned.IntervalsSet;
import org.hipparchus.geometry.euclidean.oned.Vector1D;
import org.hipparchus.geometry.partitioning.RegionFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubLineTest {

    @Test
    void testEndPoints() {
        Vector2D p1 = new Vector2D(-1, -7);
        Vector2D p2 = new Vector2D(7, -1);
        Segment segment = new Segment(p1, p2, new Line(p1, p2, 1.0e-10));
        SubLine sub = new SubLine(segment);
        List<Segment> segments = sub.getSegments();
        assertEquals(1, segments.size());
        assertEquals(0.0, new Vector2D(-1, -7).distance(segments.get(0).getStart()), 1.0e-10);
        assertEquals(0.0, new Vector2D( 7, -1).distance(segments.get(0).getEnd()), 1.0e-10);
    }

    @Test
    void testNoEndPoints() {
        SubLine wholeLine = new Line(new Vector2D(-1, 7), new Vector2D(7, 1), 1.0e-10).wholeHyperplane();
        List<Segment> segments = wholeLine.getSegments();
        assertEquals(1, segments.size());
        assertTrue(Double.isInfinite(segments.get(0).getStart().getX()) &&
                          segments.get(0).getStart().getX() < 0);
        assertTrue(Double.isInfinite(segments.get(0).getStart().getY()) &&
                          segments.get(0).getStart().getY() > 0);
        assertTrue(Double.isInfinite(segments.get(0).getEnd().getX()) &&
                          segments.get(0).getEnd().getX() > 0);
        assertTrue(Double.isInfinite(segments.get(0).getEnd().getY()) &&
                          segments.get(0).getEnd().getY() < 0);
    }

    @Test
    void testNoSegments() {
        SubLine empty = new SubLine(new Line(new Vector2D(-1, -7), new Vector2D(7, -1), 1.0e-10),
                                    new RegionFactory<Euclidean1D, Vector1D>().getComplement(new IntervalsSet(1.0e-10)));
        List<Segment> segments = empty.getSegments();
        assertEquals(0, segments.size());
    }

    @Test
    void testSeveralSegments() {
        SubLine twoSubs = new SubLine(new Line(new Vector2D(-1, -7), new Vector2D(7, -1), 1.0e-10),
                                    new RegionFactory<Euclidean1D, Vector1D>().
                                            union(new IntervalsSet(1, 2, 1.0e-10), new IntervalsSet(3, 4, 1.0e-10)));
        List<Segment> segments = twoSubs.getSegments();
        assertEquals(2, segments.size());
    }

    @Test
    void testHalfInfiniteNeg() {
        SubLine empty = new SubLine(new Line(new Vector2D(-1, -7), new Vector2D(7, -1), 1.0e-10),
                                    new IntervalsSet(Double.NEGATIVE_INFINITY, 0.0, 1.0e-10));
        List<Segment> segments = empty.getSegments();
        assertEquals(1, segments.size());
        assertTrue(Double.isInfinite(segments.get(0).getStart().getX()) &&
                          segments.get(0).getStart().getX() < 0);
        assertTrue(Double.isInfinite(segments.get(0).getStart().getY()) &&
                          segments.get(0).getStart().getY() < 0);
        assertEquals(0.0, new Vector2D(3, -4).distance(segments.get(0).getEnd()), 1.0e-10);
    }

    @Test
    void testHalfInfinitePos() {
        SubLine empty = new SubLine(new Line(new Vector2D(-1, -7), new Vector2D(7, -1), 1.0e-10),
                                    new IntervalsSet(0.0, Double.POSITIVE_INFINITY, 1.0e-10));
        List<Segment> segments = empty.getSegments();
        assertEquals(1, segments.size());
        assertEquals(0.0, new Vector2D(3, -4).distance(segments.get(0).getStart()), 1.0e-10);
        assertTrue(Double.isInfinite(segments.get(0).getEnd().getX()) &&
                          segments.get(0).getEnd().getX() > 0);
        assertTrue(Double.isInfinite(segments.get(0).getEnd().getY()) &&
                          segments.get(0).getEnd().getY() > 0);
    }

    @Test
    void testIntersectionInsideInside() {
        SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(3, 1), 1.0e-10);
        SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 2), 1.0e-10);
        assertEquals(0.0, new Vector2D(2, 1).distance(sub1.intersection(sub2, true)),  1.0e-12);
        assertEquals(0.0, new Vector2D(2, 1).distance(sub1.intersection(sub2, false)), 1.0e-12);
    }

    @Test
    void testIntersectionInsideBoundary() {
        SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(3, 1), 1.0e-10);
        SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 1), 1.0e-10);
        assertEquals(0.0, new Vector2D(2, 1).distance(sub1.intersection(sub2, true)),  1.0e-12);
        assertNull(sub1.intersection(sub2, false));
    }

    @Test
    void testIntersectionInsideOutside() {
        SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(3, 1), 1.0e-10);
        SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 0.5), 1.0e-10);
        assertNull(sub1.intersection(sub2, true));
        assertNull(sub1.intersection(sub2, false));
    }

    @Test
    void testIntersectionBoundaryBoundary() {
        SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(2, 1), 1.0e-10);
        SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 1), 1.0e-10);
        assertEquals(0.0, new Vector2D(2, 1).distance(sub1.intersection(sub2, true)),  1.0e-12);
        assertNull(sub1.intersection(sub2, false));
    }

    @Test
    void testIntersectionBoundaryOutside() {
        SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(2, 1), 1.0e-10);
        SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 0.5), 1.0e-10);
        assertNull(sub1.intersection(sub2, true));
        assertNull(sub1.intersection(sub2, false));
    }

    @Test
    void testIntersectionOutsideOutside() {
        SubLine sub1 = new SubLine(new Vector2D(1, 1), new Vector2D(1.5, 1), 1.0e-10);
        SubLine sub2 = new SubLine(new Vector2D(2, 0), new Vector2D(2, 0.5), 1.0e-10);
        assertNull(sub1.intersection(sub2, true));
        assertNull(sub1.intersection(sub2, false));
    }

    @Test
    void testIntersectionParallel() {
        final SubLine sub1 = new SubLine(new Vector2D(0, 1), new Vector2D(0, 2), 1.0e-10);
        final SubLine sub2 = new SubLine(new Vector2D(66, 3), new Vector2D(66, 4), 1.0e-10);
        assertNull(sub1.intersection(sub2, true));
        assertNull(sub1.intersection(sub2, false));
    }

}
