package com.memdiag.core.heap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GcRootPath {
    private final ObjectId targetObjectId;
    private final List<PathNode> pathNodes;
    private final int depth;

    private GcRootPath(ObjectId targetObjectId, List<PathNode> pathNodes) {
        this.targetObjectId = targetObjectId;
        this.pathNodes = Collections.unmodifiableList(new ArrayList<>(pathNodes));
        this.depth = pathNodes.size();
    }

    public static GcRootPath create(ObjectId targetObjectId, List<PathNode> pathNodes) {
        return new GcRootPath(targetObjectId, pathNodes);
    }

    public ObjectId getTargetObjectId() {
        return targetObjectId;
    }

    public List<PathNode> getPathNodes() {
        return pathNodes;
    }

    public int getDepth() {
        return depth;
    }

    public PathNode getRoot() {
        return pathNodes.isEmpty() ? null : pathNodes.get(0);
    }

    public GcRootType getRootType() {
        PathNode root = getRoot();
        return root != null ? root.getNodeType() : GcRootType.OTHER;
    }

    @Override
    public String toString() {
        return "GcRootPath{" +
            "target=" + targetObjectId +
            ", depth=" + depth +
            ", rootType=" + getRootType() +
            '}';
    }

    public static class PathNode {
        private final ObjectId objectId;
        private final String className;
        private final String fieldName;
        private final GcRootType nodeType;

        public PathNode(ObjectId objectId, String className, String fieldName, GcRootType nodeType) {
            this.objectId = objectId;
            this.className = className;
            this.fieldName = fieldName;
            this.nodeType = nodeType;
        }

        public ObjectId getObjectId() {
            return objectId;
        }

        public String getClassName() {
            return className;
        }

        public String getFieldName() {
            return fieldName;
        }

        public GcRootType getNodeType() {
            return nodeType;
        }

        @Override
        public String toString() {
            return "PathNode{" +
                "objectId=" + objectId +
                ", className='" + className + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", nodeType=" + nodeType +
                '}';
        }
    }
}
