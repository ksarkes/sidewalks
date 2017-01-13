package com.usachev;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

import java.io.File;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javafx.util.Pair;


/**
 * Created by Andrey on 23.12.2016.
 */
@SuppressWarnings("WeakerAccess")
public class SidewalkProcessor {

    private ArrayList<BoundContainer> bounds = new ArrayList<>();
    private ArrayList<NodeContainer> nodes = new ArrayList<>();
    private ArrayList<WayContainer> ways = new ArrayList<>();
    private ArrayList<RelationContainer> relations = new ArrayList<>();
    private ArrayList<WayContainer> sidewalks = new ArrayList<>();

    // For getting node by its id (e.g. in WayNode)
    private HashMap<Long, NodeContainer> nodesMap = new HashMap<>();
    // For getting way by its id
    private HashMap<Long, WayContainer> waysMap = new HashMap<>();
    // For getting ways related to specified node id
    private HashMap<Long, ArrayList<Long>> waysNodesMap = new HashMap<>();
    // For getting node`s adjacent nodes
    private HashMap<Long, ArrayList<Long>> adjacentNodes = new HashMap<>();

    private HashMap<Long, Long> currentNodeWayMap = new HashMap<>();

    private ArrayList<Long> processedNodes = new ArrayList<>();

    public SidewalkProcessor() {
    }

    public void addBound(BoundContainer boundContainer) {
        bounds.add(boundContainer);
    }

    public void addNode(NodeContainer nodeContainer) {
        nodes.add(nodeContainer);
        long id = nodeContainer.getEntity().getId();
        nodesMap.put(id, nodeContainer);
    }

    public void addWay(WayContainer wayContainer) {
        ways.add(wayContainer);
        long id = wayContainer.getEntity().getId();
        waysMap.put(id, wayContainer);

        List<WayNode> wayNodes = wayContainer.getEntity().getWayNodes();
        for (int i = 0; i < wayNodes.size(); i++) {
            WayNode wayNode = wayNodes.get(i);
            long nodeId = wayNode.getNodeId();
            if (waysNodesMap.get(nodeId) == null) {
                ArrayList<Long> waysArray = new ArrayList<>();
                waysArray.add(wayContainer.getEntity().getId());
                waysNodesMap.put(nodeId, waysArray);
//                ArrayList<Long> adjacents = new ArrayList<>();
//                adjacents.add(id);
//                adjacentNodes.put(id, adjacents);
            } else {
                waysNodesMap.get(nodeId).add(wayContainer.getEntity().getId());
//                if (i > 0) {
//                    long prevAdj = wayNodes.get(i - 1).getNodeId();
//                    adjacentNodes.get(nodeId).add(prevAdj);
//                }
//                if (i < wayNodes.size() - 2) {
//                    long nextAdj = wayNodes.get(i + 1).getNodeId();
//                    adjacentNodes.get(nodeId).add(nextAdj);
//                }
            }
        }
    }

    public void addRelation(RelationContainer relationContainer) {
        relations.add(relationContainer);
    }

    public void process() throws UnexpectedSidewalkTypeException {
/*        log(GeoUtil.movePoint(new LatLng(61.7849636, 34.352475), 10, 145.63481572463607).toString());
        Pair<LatLng,LatLng> res = GeoUtil.moveLine(new LatLng(61.786135, 34.350503), new LatLng(61.785438, 34.351479), GeoUtil.LEFT);
        log(res.getKey() + " " +  res.getValue());
        log(GeoUtil.distance(new Point(0.5995363601285268, 1.3810594211706333), new Point(0.599563794959039, 1.3810197393070807)) + "");
        log(GeoUtil.distance(new Point(0.5995910587472845, 1.3810385888498842), new Point(0.5995804925239929, 1.3810538724928147)) + "");
        log(180 + (GeoUtil.angle(new LatLng( 61.7849636, 34.352475), new LatLng(61.7860385, 34.3509031))/ Math.PI * 180));*/

        ArrayList<WayContainer> containers = new ArrayList<>();
         sidewalks = new ArrayList<>();
        for (WayContainer way : ways) {
            Long id = way.getEntity().getWayNodes().get(0).getNodeId();
            boolean hasFootway = false;
            boolean hasSidewalk = false;
            for (Tag tag : way.getEntity().getTags()) {
                if (tag.getKey().equals("highway") && tag.getValue().equals("footway")) {
                    hasFootway = true;
                }
                if (tag.getKey().equals("sidewalk") && !tag.getValue().equals("none") && !tag.getValue().equals("no")) {
//                if (tag.getKey().equals("sidewalk") && (tag.getValue().equals("right") || tag.getValue().equals("left"))) {
//                    log(tag.getValue());
                    hasSidewalk = true;
                }
            }
            if (!hasFootway)
                containers.add(way);
            if (hasSidewalk) {
                sidewalks.add(way);
            }
        }
        ways = containers;

        for (WayContainer way : sidewalks) {
            ArrayList<WayNode> wayNodes = (ArrayList<WayNode>) way.getEntity().getWayNodes();

            ArrayList<NodeContainer> newNodesLeft = new ArrayList<>();
            ArrayList<NodeContainer> newNodesRight = new ArrayList<>();
            ArrayList<WayNode> wayNodesLeft = new ArrayList<>();
            ArrayList<WayNode> wayNodesRight = new ArrayList<>();

            for (int i = 0; i < wayNodes.size(); i++) {
                NodeContainer node = nodesMap.get(wayNodes.get(i).getNodeId());
                if (isCrossroad(node)) {
                    if (processedNodes.contains(node.getEntity().getId())) {

                        continue;
                    }

                    processedNodes.add(node.getEntity().getId());

                    Long prevNodeId = null, nextNodeId = null;
                    Double initialAngle;
                    if (i > 0) {
                        prevNodeId = wayNodes.get(i - 1).getNodeId();
                        NodeContainer prevNode = nodesMap.get(prevNodeId);
                        initialAngle = GeoUtil.angle(node, prevNode);
                    } else {
                        // i always > 2, so we can do that
                        nextNodeId = wayNodes.get(i + 1).getNodeId();
                        NodeContainer nextNode = nodesMap.get(nextNodeId);
                        initialAngle = GeoUtil.angle(node, nextNode);
                    }

                    Pair<Long, Long> nearests;
                    NodeContainer nearestLeft, nearestRight;
                    // TODO: связать найденные nodes с ways
                    if (prevNodeId != null) {
                        nearests = findNearestNodes(node, initialAngle, prevNodeId);
                        nearestLeft = nodesMap.get(nearests.getKey());
                        nearestRight = nodesMap.get(nearests.getValue());
                    }
                    else {
                        // Swap left and right if use next node to find nearest (reverse vector direction)
                        nearests = findNearestNodes(node, initialAngle, nextNodeId);
                        nearestRight = nodesMap.get(nearests.getKey());
                        nearestLeft = nodesMap.get(nearests.getValue());
                    }

//                    if (nearestLeft == nearestRight && nearestLeft != null)
//                        log("\n" + node.getEntity().getId() + "\nprev " + prevNodeId + "\nnext " + nextNodeId +
//                        "\nleft" + nearestLeft.getEntity().getId() + "\nright" + nearestRight.getEntity().getId());
                    //TODO: check if right==left
                    String rightSidewalkType = determineSidewalk(nearestRight);
                    String leftSidewalkType = determineSidewalk(nearestLeft);
                    String initSidewalkType = determineSidewalk(way);

                    if (rightSidewalkType != null) {
                        if (rightSidewalkType.equals("both")) {

                        } else if (rightSidewalkType.equals("left")) {

                        } else if (rightSidewalkType.equals("right")) {

                        }
                    } else {
                        if (initSidewalkType == null)
                            throw new UnexpectedSidewalkTypeException();

                        if (initSidewalkType.equals("both") || initSidewalkType.equals("right")) {

                        }
                    }

                    // clear hashmap
                    currentNodeWayMap = new HashMap<>();
                } else {
                    String sidewalkDirection = determineSidewalk(way);
                    if (sidewalkDirection == null)
                        throw new UnexpectedSidewalkTypeException();

                    if (sidewalkDirection.equals("both") || sidewalkDirection.equals("left")) {
                        NodeContainer prevNode = null, nextNode = null;
                        if (i > 0)
                            prevNode = nodesMap.get(wayNodes.get(i - 1).getNodeId());
                        if (i < wayNodes.size() - 1)
                            nextNode = nodesMap.get(wayNodes.get(i + 1).getNodeId());
                        NodeContainer newNode = GeoUtil.moveNode(node, prevNode, nextNode, GeoUtil.LEFT);
                        newNodesLeft.add(newNode);
                        wayNodesLeft.add(new WayNode(newNode.getEntity().getId()));
                        newWritableNodes.add(newNode);
/*                        Pair<NodeContainer, NodeContainer> newNodes =
                                GeoUtil.movePath(nodesMap.get(wayNodes.get(i - 1).getNodeId()), node, GeoUtil.LEFT);
                        newNodesLeft.add(newNodes.getKey());
                        newNodesLeft.add(newNodes.getValue());
                        wayNodesLeft.add(new WayNode(newNodes.getKey().getEntity().getId()));
                        wayNodesLeft.add(new WayNode(newNodes.getValue().getEntity().getId()));
                        newWritableNodes.add(newNodes.getKey());
                        newWritableNodes.add(newNodes.getValue());*/
                    }
                    if (sidewalkDirection.equals("both") || sidewalkDirection.equals("right")) {
                        NodeContainer prevNode = null, nextNode = null;
                        if (i > 0)
                            prevNode = nodesMap.get(wayNodes.get(i - 1).getNodeId());
                        if (i < wayNodes.size() - 1)
                            nextNode = nodesMap.get(wayNodes.get(i + 1).getNodeId());
                        NodeContainer newNode = GeoUtil.moveNode(node, prevNode, nextNode, GeoUtil.RIGHT);
                        newNodesRight.add(newNode);
                        wayNodesRight.add(new WayNode(newNode.getEntity().getId()));
                        newWritableNodes.add(newNode);
/*                        Pair<NodeContainer, NodeContainer> newNodes =
                                GeoUtil.movePath(nodesMap.get(wayNodes.get(i - 1).getNodeId()), node, GeoUtil.RIGHT);
                        newNodesRight.add(newNodes.getKey());
                        newNodesRight.add(newNodes.getValue());
                        wayNodesRight.add(new WayNode(newNodes.getKey().getEntity().getId()));
                        wayNodesRight.add(new WayNode(newNodes.getValue().getEntity().getId()));
                        newWritableNodes.add(newNodes.getKey());
                        newWritableNodes.add(newNodes.getValue());*/
                    }
                }
            }

            Way way1 = new Way(new CommonEntityData(Main.getNewId(), 1, Calendar.getInstance().getTime(), Main.getOsmUser(), -100500), wayNodesLeft);
            Way way2 = new Way(new CommonEntityData(Main.getNewId(), 1, Calendar.getInstance().getTime(), Main.getOsmUser(), -100500), wayNodesRight);
            newWritableWays.add(new WayContainer(way1));
            newWritableWays.add(new WayContainer(way2));
        }
        writeOsmXml();
        writeSidewalks();
    }

    private ArrayList<WayContainer> newWritableWays = new ArrayList<>();
    private ArrayList<NodeContainer> newWritableNodes = new ArrayList<>();

    private String determineSidewalk(NodeContainer node) {
        Long wayId = currentNodeWayMap.get(node.getEntity().getId());
        for (Tag tag : waysMap.get(wayId).getEntity().getTags()) {
            if (tag.getKey().equals("sidewalk") && !tag.getValue().equals("none") && !tag.getValue().equals("no"))
                return tag.getValue();
        }
        return null;
    }

    private String determineSidewalk(WayContainer way) {
        for(Tag tag : way.getEntity().getTags()) {
            if (tag.getKey().equals("sidewalk") && !tag.getValue().equals("none") && !tag.getValue().equals("no"))
                return tag.getValue();
        }
        return null;
    }

    private Pair<Long, Long> findNearestNodes(NodeContainer node, double initialAngle, long unhandledNode) {

        ArrayList<Long> adjacents = findAllAdjacentNodes(node);

        Long nearestLeftId = null, nearestRightId = null;
        Double nearestLeftAngle = null, nearestRightAngle = null;

        for (Long adjId : adjacents) {
            if (!adjId.equals(unhandledNode)) {
                NodeContainer currAdjNode = nodesMap.get(adjId);
                double currAngle = GeoUtil.angle(node, currAdjNode);

                // Init it by some values
                if (nearestLeftAngle == null) {
                    nearestLeftAngle = currAngle;
                    nearestLeftId = adjId;
                }
                if (nearestRightAngle == null) {
                    nearestRightAngle = currAngle;
                    nearestRightId = adjId;
                }

                if (initialAngle < 0) {
                    if (nearestRightAngle < initialAngle) {
                        if (currAngle > initialAngle ||
                                currAngle < nearestRightAngle) {
                            nearestRightAngle = currAngle;
                            nearestRightId = adjId;
                        }
                    } else if (currAngle < nearestRightAngle
                            && currAngle > initialAngle) {
                        nearestRightAngle = currAngle;
                        nearestRightId = adjId;
                    }

                    if (nearestLeftAngle < initialAngle) {
                        if (currAngle > nearestLeftAngle
                                && currAngle < initialAngle) {
                            nearestLeftAngle = currAngle;
                            nearestLeftId = adjId;
                        }
                    } else if (currAngle < initialAngle
                            || currAngle > nearestLeftAngle) {
                        nearestLeftAngle = currAngle;
                        nearestLeftId = adjId;
                    }
                } else {
                    if (nearestRightAngle > initialAngle) {
                        if (currAngle > initialAngle
                                && currAngle < nearestRightAngle) {
                            nearestRightAngle = currAngle;
                            nearestRightId = adjId;
                        }
                    } else if (currAngle < nearestRightAngle
                            || currAngle > initialAngle) {
                        nearestRightAngle = currAngle;
                        nearestRightId = adjId;
                    }

                    if (nearestLeftAngle > initialAngle) {
                        if (currAngle > nearestLeftAngle
                                || currAngle < initialAngle) {
                            nearestLeftAngle = currAngle;
                            nearestLeftId = adjId;
                        }
                    } else if (currAngle < initialAngle
                            && currAngle > nearestLeftAngle) {
                        nearestLeftAngle = currAngle;
                        nearestLeftId = adjId;
                    }
                }
            }
        }
        return new Pair<>(nearestLeftId, nearestRightId);
    }

    private ArrayList<Long> findAllAdjacentNodes(NodeContainer node) {
        ArrayList<Long> adjacentNodes = new ArrayList<>();
        for (Long wayId : waysNodesMap.get(node.getEntity().getId())) {
            // Find adjacent nodes to specified node for each intersected way in this crossroad
            ArrayList<WayNode> wayNodes = (ArrayList<WayNode>) waysMap.get(wayId).getEntity().getWayNodes();
            for (int i = 0; i < wayNodes.size(); i++) {
                if (wayNodes.get(i).getNodeId() == node.getEntity().getId()) {
                    if (i > 0) {
                        if (!currentNodeWayMap.containsKey(wayNodes.get(i - 1).getNodeId())) {
                            adjacentNodes.add(wayNodes.get(i - 1).getNodeId());
                            currentNodeWayMap.put(wayNodes.get(i - 1).getNodeId(), wayId);
                        }
                    }
                    if (i < wayNodes.size() - 1) {
                        if (!currentNodeWayMap.containsKey(wayNodes.get(i + 1).getNodeId())) {
                            adjacentNodes.add(wayNodes.get(i + 1).getNodeId());
                            currentNodeWayMap.put(wayNodes.get(i + 1).getNodeId(), wayId);
                        }
                    }
                }
            }
        }
        return adjacentNodes;
    }

    private boolean isCrossroad(NodeContainer node) {
        log(node.getEntity().getId());
        return waysNodesMap.get(node.getEntity().getId()).size() > 1;
    }

    private void writeOsmXml() {
        XmlWriter xmlWriter = new XmlWriter(new File("output.osm"), CompressionMethod.None);

        for (BoundContainer bound : bounds)
            xmlWriter.process(bound);
        for (NodeContainer node : nodes)
            xmlWriter.process(node);
        for (NodeContainer node : newWritableNodes)
            xmlWriter.process(node);
        for (WayContainer way : ways)
            xmlWriter.process(way);
        for (WayContainer way : newWritableWays)
            xmlWriter.process(way);
        for (RelationContainer relation : relations)
            xmlWriter.process(relation);

        xmlWriter.complete();
        xmlWriter.release();
    }

    private void writeSidewalks()  {
        XmlWriter xmlWriter = new XmlWriter(new File("sidewalks2.osm"), CompressionMethod.None);

        for (BoundContainer bound : bounds)
            xmlWriter.process(bound);

        for (NodeContainer node : nodes)
            xmlWriter.process(node);

        for (WayContainer way : sidewalks) {
            xmlWriter.process(way);
        }

        for (NodeContainer node : newWritableNodes)
            xmlWriter.process(node);

        for (WayContainer way : newWritableWays)
            xmlWriter.process(way);

        for (RelationContainer relation : relations)
            xmlWriter.process(relation);

        xmlWriter.complete();
        xmlWriter.release();
    }

    public static void log(String s) {
        System.out.println(s);
    }
    public static void log(double d) {
        System.out.println(String.valueOf(d));
    }
    public static void log(long l) {
        System.out.println(String.valueOf(l));
    }

}
