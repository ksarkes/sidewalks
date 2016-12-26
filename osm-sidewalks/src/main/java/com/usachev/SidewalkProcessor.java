package com.usachev;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


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

    // For getting node by its id in WayNode
    private HashMap<Long, NodeContainer> nodesMap = new HashMap<>();
    // For getting way related to specified node id
    private HashMap<Long, ArrayList<Long>> waysMap = new HashMap<>();
    // For getting node`s adjacent nodes
    private HashMap<Long, ArrayList<Long>> adjacentNodes = new HashMap<>();

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
        List<WayNode> wayNodes = wayContainer.getEntity().getWayNodes();
        for (int i = 0; i < wayNodes.size(); i++) {
            WayNode wayNode = wayNodes.get(i);
            long nodeId = wayNode.getNodeId();
            if (waysMap.get(nodeId) == null) {
                ArrayList<Long> waysArray = new ArrayList<>();
                waysArray.add(wayContainer.getEntity().getId());
                waysMap.put(nodeId, waysArray);
//                ArrayList<Long> adjacents = new ArrayList<>();
//                adjacents.add(id);
//                adjacentNodes.put(id, adjacents);
            } else {
                waysMap.get(nodeId).add(wayContainer.getEntity().getId());
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

    public void process() {
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

        for (WayContainer way : ways) {

        }

        for (WayContainer way : sidewalks) {
            for (WayNode wayNode : way.getEntity().getWayNodes()) {
                NodeContainer node = nodesMap.get(wayNode.getNodeId());
                if (isCrossroad(node))
                    log("wow");
            }
        }
        writeOsmXml();
        writeSidiewalks();
    }
    private boolean isCrossroad(NodeContainer node) {
        return waysMap.get(node.getEntity().getId()).size() > 1;
    }


    private void writeOsmXml() {
        XmlWriter xmlWriter = new XmlWriter(new File("output.osm"), CompressionMethod.None);

        for (BoundContainer bound : bounds)
            xmlWriter.process(bound);
        for (NodeContainer node : nodes)
            xmlWriter.process(node);
        for (WayContainer way : ways)
            xmlWriter.process(way);
        for (RelationContainer relation : relations)
            xmlWriter.process(relation);

        xmlWriter.complete();
        xmlWriter.release();
    }

    private void writeSidiewalks()  {
        XmlWriter xmlWriter = new XmlWriter(new File("sidewalks2.osm"), CompressionMethod.None);

//        for (BoundContainer bound : bounds)
//            xmlWriter.process(bound);

//        for (NodeContainer node : nodes)
//            xmlWriter.process(node);

        for (WayContainer way : sidewalks) {
            xmlWriter.process(way);
        }

//        for (RelationContainer relation : relations)
//            xmlWriter.process(relation);

        xmlWriter.complete();
        xmlWriter.release();
    }

    public static void log(String l) {
        System.out.println(l);
    }
    public static void log(double d) {
        System.out.println(String.valueOf(d));
    }

}
