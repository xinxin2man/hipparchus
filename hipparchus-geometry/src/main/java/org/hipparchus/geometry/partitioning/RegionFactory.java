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
package org.hipparchus.geometry.partitioning;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.LocalizedGeometryFormats;
import org.hipparchus.geometry.Point;
import org.hipparchus.geometry.Space;
import org.hipparchus.geometry.partitioning.BSPTree.VanishingCutHandler;
import org.hipparchus.geometry.partitioning.Region.Location;
import org.hipparchus.geometry.partitioning.SubHyperplane.SplitSubHyperplane;

/** This class is a factory for {@link Region}.

 * @param <S> Type of the space.
 * @param <P> Type of the points in space.

 */
public class RegionFactory<S extends Space, P extends Point<S>> {

    /** Visitor removing internal nodes attributes. */
    private final NodesCleaner nodeCleaner;

    /** Simple constructor.
     */
    public RegionFactory() {
        nodeCleaner = new NodesCleaner();
    }

    /** Build a convex region from a collection of bounding hyperplanes.
     * @param hyperplanes collection of bounding hyperplanes
     * @return a new convex region, or null if the collection is empty
     */
    @SafeVarargs
    public final Region<S, P> buildConvex(final Hyperplane<S, P> ... hyperplanes) {
        if ((hyperplanes == null) || (hyperplanes.length == 0)) {
            return null;
        }

        // use the first hyperplane to build the right class
        final Region<S, P> region = hyperplanes[0].wholeSpace();

        // chop off parts of the space
        BSPTree<S, P> node = region.getTree(false);
        node.setAttribute(Boolean.TRUE);
        for (final Hyperplane<S, P> hyperplane : hyperplanes) {
            if (node.insertCut(hyperplane)) {
                node.setAttribute(null);
                node.getPlus().setAttribute(Boolean.FALSE);
                node = node.getMinus();
                node.setAttribute(Boolean.TRUE);
            } else {
                // the hyperplane could not be inserted in the current leaf node
                // either it is completely outside (which means the input hyperplanes
                // are wrong), or it is parallel to a previous hyperplane
                SubHyperplane<S, P> s = hyperplane.wholeHyperplane();
                for (BSPTree<S, P> tree = node; tree.getParent() != null && s != null; tree = tree.getParent()) {
                    final Hyperplane<S, P>         other = tree.getParent().getCut().getHyperplane();
                    final SplitSubHyperplane<S, P> split = s.split(other);
                    switch (split.getSide()) {
                        case HYPER :
                            // the hyperplane is parallel to a previous hyperplane
                            if (!hyperplane.sameOrientationAs(other)) {
                                // this hyperplane is opposite to the other one,
                                // the region is thinner than the tolerance, we consider it empty
                                return getComplement(hyperplanes[0].wholeSpace());
                            }
                            // the hyperplane is an extension of an already known hyperplane, we just ignore it
                            break;
                        case PLUS :
                        // the hyperplane is outside of the current convex zone,
                        // the input hyperplanes are inconsistent
                        throw new MathIllegalArgumentException(LocalizedGeometryFormats.NOT_CONVEX_HYPERPLANES);
                        default :
                            s = split.getMinus();
                    }
                }
            }
        }

        return region;

    }

    /** Compute the union of two regions.
     * @param region1 first region (will be unusable after the operation as
     * parts of it will be reused in the new region)
     * @param region2 second region (will be unusable after the operation as
     * parts of it will be reused in the new region)
     * @return a new region, result of {@code region1 union region2}
     */
    public Region<S, P> union(final Region<S, P> region1, final Region<S, P> region2) {
        final BSPTree<S, P> tree =
            region1.getTree(false).merge(region2.getTree(false), new UnionMerger());
        tree.visit(nodeCleaner);
        return region1.buildNew(tree);
    }

    /** Compute the intersection of two regions.
     * @param region1 first region (will be unusable after the operation as
     * parts of it will be reused in the new region)
     * @param region2 second region (will be unusable after the operation as
     * parts of it will be reused in the new region)
     * @return a new region, result of {@code region1 intersection region2}
     */
    public Region<S, P> intersection(final Region<S, P> region1, final Region<S, P> region2) {
        final BSPTree<S, P> tree =
            region1.getTree(false).merge(region2.getTree(false), new IntersectionMerger(region1, region2));
        tree.visit(nodeCleaner);
        return region1.buildNew(tree);
    }

    /** Compute the symmetric difference (exclusive or) of two regions.
     * @param region1 first region (will be unusable after the operation as
     * parts of it will be reused in the new region)
     * @param region2 second region (will be unusable after the operation as
     * parts of it will be reused in the new region)
     * @return a new region, result of {@code region1 xor region2}
     */
    public Region<S, P> xor(final Region<S, P> region1, final Region<S, P> region2) {
        final BSPTree<S, P> tree =
            region1.getTree(false).merge(region2.getTree(false), new XorMerger());
        tree.visit(nodeCleaner);
        return region1.buildNew(tree);
    }

    /** Compute the difference of two regions.
     * @param region1 first region (will be unusable after the operation as
     * parts of it will be reused in the new region)
     * @param region2 second region (will be unusable after the operation as
     * parts of it will be reused in the new region)
     * @return a new region, result of {@code region1 minus region2}
     */
    public Region<S, P> difference(final Region<S, P> region1, final Region<S, P> region2) {
        final BSPTree<S, P> tree =
            region1.getTree(false).merge(region2.getTree(false), new DifferenceMerger(region1, region2));
        tree.visit(nodeCleaner);
        return region1.buildNew(tree);
    }

     /** Get the complement of the region (exchanged interior/exterior).
     * @param region region to complement, it will not be modified, a new
     * region independent region will be built
     * @return a new region, complement of the specified one
     */
    public Region<S, P> getComplement(final Region<S, P> region) {
        return region.buildNew(recurseComplement(region.getTree(false)));
    }

    /** Recursively build the complement of a BSP tree.
     * @param node current node of the original tree
     * @return new tree, complement of the node
     */
    private BSPTree<S, P> recurseComplement(final BSPTree<S, P> node) {

        // transform the tree, except for boundary attribute splitters
        final Map<BSPTree<S, P>, BSPTree<S, P>> map = new HashMap<>();
        final BSPTree<S, P> transformedTree = recurseComplement(node, map);

        // set up the boundary attributes splitters
        for (final Map.Entry<BSPTree<S, P>, BSPTree<S, P>> entry : map.entrySet()) {
            if (entry.getKey().getCut() != null) {
                @SuppressWarnings("unchecked")
                BoundaryAttribute<S, P> original = (BoundaryAttribute<S, P>) entry.getKey().getAttribute();
                if (original != null) {
                    @SuppressWarnings("unchecked")
                    BoundaryAttribute<S, P> transformed = (BoundaryAttribute<S, P>) entry.getValue().getAttribute();
                    for (final BSPTree<S, P> splitter : original.getSplitters()) {
                        transformed.getSplitters().add(map.get(splitter));
                    }
                }
            }
        }

        return transformedTree;

    }

    /** Recursively build the complement of a BSP tree.
     * @param node current node of the original tree
     * @param map transformed nodes map
     * @return new tree, complement of the node
     */
    private BSPTree<S, P> recurseComplement(final BSPTree<S, P> node,
                                         final Map<BSPTree<S, P>, BSPTree<S, P>> map) {

        final BSPTree<S, P> transformedNode;
        if (node.getCut() == null) {
            transformedNode = new BSPTree<>(((Boolean) node.getAttribute()) ? Boolean.FALSE : Boolean.TRUE);
        } else {

            @SuppressWarnings("unchecked")
            BoundaryAttribute<S, P> attribute = (BoundaryAttribute<S, P>) node.getAttribute();
            if (attribute != null) {
                final SubHyperplane<S, P> plusOutside =
                        (attribute.getPlusInside() == null) ? null : attribute.getPlusInside().copySelf();
                final SubHyperplane<S, P> plusInside  =
                        (attribute.getPlusOutside() == null) ? null : attribute.getPlusOutside().copySelf();
                // we start with an empty list of splitters, it will be filled in out of recursion
                attribute = new BoundaryAttribute<>(plusOutside, plusInside, new NodesSet<>());
            }

            transformedNode = new BSPTree<>(node.getCut().copySelf(),
                                            recurseComplement(node.getPlus(),  map),
                                            recurseComplement(node.getMinus(), map),
                                            attribute);
        }

        map.put(node, transformedNode);
        return transformedNode;

    }

    /** BSP tree leaf merger computing intersection of two regions. */
    private abstract class FixingMerger implements BSPTree.LeafMerger<S, P>, VanishingCutHandler<S, P> {

        /** First region. */
        private final Region<S, P> region1;

        /** Second region. */
        private final Region<S, P> region2;

        /** Simple constructor.
         * @param region1 first region
         * @param region2 second region
         */
        protected FixingMerger(final Region<S, P> region1, final Region<S, P> region2) {
            this.region1 = region1.copySelf();
            this.region2 = region2.copySelf();
        }

        /** {@inheritDoc} */
        @Override
        public BSPTree<S, P> fixNode(final BSPTree<S, P> node) {
            // get a representative point in the degenerate cell
            final BSPTree<S, P> cell = node.pruneAroundConvexCell(Boolean.TRUE, Boolean.FALSE, null);
            final Region<S, P> r = region1.buildNew(cell);
            final P p = r.getBarycenter();
            return new BSPTree<>(shouldBeInside(region1.checkPoint(p), region2.checkPoint(p)));
        }

        /**
         * Check if node should be an inside or outside node.
         * @param location1 location of representative point in region1
         * @param location2 location of representative point in region2
         * @return true if node should be an inside node
         */
        protected abstract boolean shouldBeInside(final Location location1, final Location location2);

    }

    /** BSP tree leaf merger computing union of two regions. */
    private class UnionMerger implements BSPTree.LeafMerger<S, P> {
        /** {@inheritDoc} */
        @Override
        public BSPTree<S, P> merge(final BSPTree<S, P> leaf, final BSPTree<S, P> tree,
                                final BSPTree<S, P> parentTree,
                                final boolean isPlusChild, final boolean leafFromInstance) {
            if ((Boolean) leaf.getAttribute()) {
                // the leaf node represents an inside cell
                leaf.insertInTree(parentTree, isPlusChild, new VanishingToLeaf(true));
                return leaf;
            }
            // the leaf node represents an outside cell
            tree.insertInTree(parentTree, isPlusChild, new VanishingToLeaf(false));
            return tree;
        }
    }

    /** BSP tree leaf merger computing intersection of two regions. */
    private class IntersectionMerger extends FixingMerger {

        /** Simple constructor.
         * @param region1 first region
         * @param region2 second region
         */
        IntersectionMerger(final Region<S, P> region1, final Region<S, P> region2) {
            super(region1, region2);
        }

        /** {@inheritDoc} */
        @Override
        public BSPTree<S, P> merge(final BSPTree<S, P> leaf, final BSPTree<S, P> tree,
                                final BSPTree<S, P> parentTree,
                                final boolean isPlusChild, final boolean leafFromInstance) {
            if ((Boolean) leaf.getAttribute()) {
                // the leaf node represents an inside cell
                tree.insertInTree(parentTree, isPlusChild, this);
                return tree;
            }
            // the leaf node represents an outside cell
            leaf.insertInTree(parentTree, isPlusChild, this);
            return leaf;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean shouldBeInside(final Location location1, final Location location2)
        {
            return !(location1.equals(Location.OUTSIDE) || location2.equals(Location.OUTSIDE));
        }

    }

    /** BSP tree leaf merger computing symmetric difference (exclusive or) of two regions. */
    private class XorMerger implements BSPTree.LeafMerger<S, P> {
        /** {@inheritDoc} */
        @Override
        public BSPTree<S, P> merge(final BSPTree<S, P> leaf, final BSPTree<S, P> tree,
                                final BSPTree<S, P> parentTree, final boolean isPlusChild,
                                final boolean leafFromInstance) {
            BSPTree<S, P> t = tree;
            if ((Boolean) leaf.getAttribute()) {
                // the leaf node represents an inside cell
                t = recurseComplement(t);
            }
            t.insertInTree(parentTree, isPlusChild, new VanishingToLeaf(true));
            return t;
        }
    }

    /** BSP tree leaf merger computing difference of two regions. */
    private class DifferenceMerger extends FixingMerger {

        /** Simple constructor.
         * @param region1 region to subtract from
         * @param region2 region to subtract
         */
        DifferenceMerger(final Region<S, P> region1, final Region<S, P> region2) {
            super(region1, region2);
        }

        /** {@inheritDoc} */
        @Override
        public BSPTree<S, P> merge(final BSPTree<S, P> leaf, final BSPTree<S, P> tree,
                                final BSPTree<S, P> parentTree, final boolean isPlusChild,
                                final boolean leafFromInstance) {
            if ((Boolean) leaf.getAttribute()) {
                // the leaf node represents an inside cell
                final BSPTree<S, P> argTree =
                    recurseComplement(leafFromInstance ? tree : leaf);
                argTree.insertInTree(parentTree, isPlusChild, this);
                return argTree;
            }
            // the leaf node represents an outside cell
            final BSPTree<S, P> instanceTree =
                leafFromInstance ? leaf : tree;
            instanceTree.insertInTree(parentTree, isPlusChild, this);
            return instanceTree;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean shouldBeInside(final Location location1, final Location location2) {
            return location1 == Location.INSIDE && location2 == Location.OUTSIDE;
        }

    }

    /** Visitor removing internal nodes attributes. */
    private class NodesCleaner implements  BSPTreeVisitor<S, P> {

        /** {@inheritDoc} */
        @Override
        public Order visitOrder(final BSPTree<S, P> node) {
            return Order.PLUS_SUB_MINUS;
        }

        /** {@inheritDoc} */
        @Override
        public void visitInternalNode(final BSPTree<S, P> node) {
            node.setAttribute(null);
        }

        /** {@inheritDoc} */
        @Override
        public void visitLeafNode(final BSPTree<S, P> node) {
        }

    }

    /** Handler replacing nodes with vanishing cuts with leaf nodes. */
    private class VanishingToLeaf implements VanishingCutHandler<S, P> {

        /** Inside/outside indicator to use for ambiguous nodes. */
        private final boolean inside;

        /** Simple constructor.
         * @param inside inside/outside indicator to use for ambiguous nodes
         */
        VanishingToLeaf(final boolean inside) {
            this.inside = inside;
        }

        /** {@inheritDoc} */
        @Override
        public BSPTree<S, P> fixNode(final BSPTree<S, P> node) {
            if (node.getPlus().getAttribute().equals(node.getMinus().getAttribute())) {
                // no ambiguity
                return new BSPTree<>(node.getPlus().getAttribute());
            } else {
                // ambiguous node
                return new BSPTree<>(inside);
            }
        }

    }

}
