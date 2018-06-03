package com.atherys.rpg.character.tree;

import com.atherys.rpg.api.character.Mutator;
import com.atherys.rpg.api.character.player.ProgressionTree;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PlayerProgressionTree implements ProgressionTree<ProgressionTree.Node> {

    public static class Node implements ProgressionTree.Node {

        private String id;
        private String name;

        private List<Mutator> mutators;

        private Node parent;
        private List<ProgressionTree.Node> children;

        @Override
        public Collection<Mutator> getMutators() {
            return mutators;
        }

        @Override
        public Node getParent() {
            return parent;
        }

        @Override
        public List<ProgressionTree.Node> getChildren() {
            return children;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private String id;
    private String name;

    private Node root;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ProgressionTree.Node getRoot() {
        return root;
    }

    @Override
    public Optional<ProgressionTree.Node> getNodeById(String id) {
        return getNodeById(root, id);
    }

    private Optional<ProgressionTree.Node> getNodeById(ProgressionTree.Node startingPoint, String id) {
        if ( startingPoint.getId().equals(id) ) return Optional.of(startingPoint);
        else {
            for ( ProgressionTree.Node node : startingPoint.getChildren() ) {
                Optional<ProgressionTree.Node> nodeById = getNodeById(node, id);
                if ( nodeById.isPresent() ) return nodeById;
            }
            return Optional.empty();
        }
    }

}
