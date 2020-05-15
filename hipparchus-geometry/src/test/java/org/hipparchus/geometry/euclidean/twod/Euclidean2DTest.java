/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.hipparchus.UnitTestUtils;
import org.hipparchus.geometry.Space;
import org.hipparchus.geometry.euclidean.oned.Euclidean1D;
import org.junit.Assert;
import org.junit.Test;

public class Euclidean2DTest {

    @Test
    public void testDimension() {
        Assert.assertEquals(2, Euclidean2D.getInstance().getDimension());
    }

    @Test
    public void testSubSpace() {
        Assert.assertTrue(Euclidean1D.getInstance() == Euclidean2D.getInstance().getSubSpace());
    }

    @Test
    public void testSerialization() {
        Space e2 = Euclidean2D.getInstance();
        Space deserialized = (Space) UnitTestUtils.serializeAndRecover(e2);
        Assert.assertTrue(e2 == deserialized);
    }

}
