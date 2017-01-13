package com.usachev;


import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import java.util.Calendar;


/**
 * Created by Andrey on 29.03.2016.
 */
@SuppressWarnings("WeakerAccess")
public class GeoUtil {
    private static final double EARTH_RADIUS = 6371000;
    private static final double RADIANS = Math.PI / 180;
    private static final double DEGREES = 180 / Math.PI;

    public static Point toMerkator(NodeContainer nodeContainer) {
        return toMerkator(new LatLng(nodeContainer.getEntity().getLatitude(), nodeContainer.getEntity().getLongitude()));
    }

    public static Point toMerkator(LatLng point) {
        // lng as lambda, lat as phi
        double radlat = point.getLatitude() * Math.PI / 180;
        double radlng = point.getLongitude() * Math.PI / 180;
        return new Point(radlng, Math.log(Math.tan(Math.PI/4 + radlat/2)));
    }

    public static LatLng toLatLng(Point p) {
        return toLatLng(p.x, p.y);
    }

    public static LatLng toLatLng(double x, double y) {
        double radlat = 2 * Math.atan(Math.exp(y)) - Math.PI/2;
        double deglat = radlat * DEGREES;
        double deglng = x * DEGREES;
        return new LatLng(deglat, deglng);
    }

    /**
     * Point projection on the line
     * @param projected point which need to be projected
     * @param point1 start of the line
     * @param point2 end of the line
     */
    public static LatLng projectPointToLine(LatLng projected, LatLng point1, LatLng point2) {
        Point mercProjected = toMerkator(projected);
        Point mercPoint1 = toMerkator(point1);
        Point mercPoint2 = toMerkator(point2);

        double x1 = mercPoint1.x;
        double y1 = mercPoint1.y;
        double x2 = mercPoint2.x;
        double y2 = mercPoint2.y;
        double x3 = mercProjected.x;
        double y3 = mercProjected.y;
        double x4, y4;

        x4 = ((x2 - x1) * (y2 - y1) * (y3 - y1) + x1 * Math.pow(y2 - y1, 2) + x3 * Math.pow(x2 - x1, 2)) / (Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
        y4 = (y2 - y1) * (x4 - x1) / (x2 - x1) + y1;
        return toLatLng(x4, y4);
    }

    public static double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    public static double angle(NodeContainer n1, NodeContainer n2) {
        return angle(new LatLng(n1.getEntity().getLatitude(), n1.getEntity().getLongitude()),
                new LatLng(n2.getEntity().getLatitude(), n2.getEntity().getLongitude()));
    }

    public static double angle(LatLng l1, LatLng l2) {
        return Math.atan2(l1.getLatitude() - l2.getLatitude(), l1.getLongitude() - l2.getLongitude()) * DEGREES;
    }

    /**
     * @param center point geo coordinates
     * @param distance shift in meters
     * @param bearing angle in degrees
     * @return moved point coordinates
     */
    private static LatLng movePoint(LatLng center, int distance, double bearing) {
        double lat1 = center.getLatitude() * RADIANS;
        double lon1 = center.getLongitude() * RADIANS;
        double radbear = bearing * RADIANS;

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / EARTH_RADIUS) +
                Math.cos(lat1) * Math.sin(distance / EARTH_RADIUS) * Math.cos(radbear));
        double lon2 = lon1 + Math.atan2(Math.sin(radbear) * Math.sin(distance / EARTH_RADIUS) * Math.cos(lat1),
                Math.cos(distance / EARTH_RADIUS) - Math.sin(lat1) * Math.sin(lat2));

        return new LatLng(lat2 * DEGREES, lon2 * DEGREES);
    }

    private static Point orth1(Point vec) {
        return new Point(-vec.y, vec.x);
    }

    private static Point orth2(Point vec) {
        return new Point(vec.y, -vec.x);
    }

    public static final int LEFT = 1;
    public static final int RIGHT = 2;

/*
    public static Pair<NodeContainer, NodeContainer> movePath(NodeContainer n1, NodeContainer n2, int direction) {
        Pair<LatLng, LatLng> pair = moveLine(new LatLng(n1.getEntity().getLatitude(), n1.getEntity().getLongitude()),
                new LatLng(n2.getEntity().getLatitude(), n2.getEntity().getLongitude()), direction);


        Node newNode1 = new Node(new CommonEntityData(Main.getNewId(), 1, Calendar.getInstance().getTime(), Main.getOsmUser(), -100500),
                pair.getKey().getLatitude(), pair.getKey().getLongitude());
        Node newNode2 = new Node(new CommonEntityData(Main.getNewId(), 1, Calendar.getInstance().getTime(), Main.getOsmUser(), -100500),
                pair.getValue().getLatitude(), pair.getValue().getLongitude());

//        n1.getEntity().setLatitude(pair.getKey().getLatitude());
//        n1.getEntity().setLongitude(pair.getKey().getLongitude());
//        n1.getEntity().setId(Main.getNewId());

//        n2.getEntity().setLatitude(pair.getValue().getLatitude());
//        n2.getEntity().setLongitude(pair.getValue().getLongitude());
//        n2.getEntity().setId(Main.getNewId());

        return new Pair<>(new NodeContainer(newNode1), new NodeContainer(newNode2));
    }
*/

    public static NodeContainer moveNode(NodeContainer node, NodeContainer prevNode, NodeContainer nextNode, int direction) {
        Point p1 = toMerkator(node);
        Point p2 = null;
        Point p3 = null;
        Point bisector;

        if (prevNode != null && nextNode != null) {
            p2 = toMerkator(prevNode);
            p3 = toMerkator(nextNode);
            Point vec1 = new Point(p1.x - p2.x, p1.y - p2.y);
            Point vec2 = new Point(p3.x - p1.x, p3.y - p1.y);
            double p = vec1.y * vec2.x - vec1.x * vec2.y;

            if (direction == LEFT) {
                if (p < 0)
                    bisector = new Point(vec2.x - vec1.x, vec2.y - vec1.y);
                else if (p > 0)
                    bisector = new Point(-(vec2.x - vec1.x), -(vec2.y - vec1.y));
                else
                    bisector = orth1(vec1);

            } else {
                if (p > 0)
                    bisector = new Point(vec1.x - vec2.x, vec1.y - vec2.y);
                else if (p < 0)
                    bisector = new Point(-(vec1.x - vec2.x), -(vec1.y - vec2.y));
                else
                    bisector = orth2(vec1);
            }
        } else {
            if (direction == LEFT) {
                if (prevNode != null)
                    bisector = orth1(toMerkator(prevNode));
                else
                    bisector = orth1(toMerkator(nextNode));
            } else {
                if (prevNode != null)
                    bisector = orth2(toMerkator(prevNode));
                else
                    bisector = orth2(toMerkator(nextNode));
            }
        }

        Point newPoint = new Point(p1.x + bisector.x, p1.y + bisector.y);
        LatLng latLng = new LatLng(node.getEntity().getLatitude(), node.getEntity().getLongitude());
        double angle = angle(latLng, toLatLng(newPoint));
        LatLng newStart = movePoint(latLng, 3, angle);

        return new NodeContainer(new Node(new CommonEntityData(Main.getNewId(), 1, Calendar.getInstance().getTime(), Main.getOsmUser(), -100500),
                newStart.getLatitude(), newStart.getLongitude()));

    }

/*    public static Pair<LatLng,LatLng> moveLine(LatLng lineStart, LatLng lineEnd, int direction) {
        Point p1 = toMerkator(lineStart);
        Point p2 = toMerkator(lineEnd);

        Point vec = new Point(p1.x - p2.x, p1.y - p2.y);

        // TODO
        Point orth;
        switch (direction) {
            case LEFT:
                orth = orth1(vec);
                break;
            case RIGHT:
                orth = orth2(vec);
                break;
            default:
                orth = orth2(vec);
                break;
        }

        // Init new start/end without precise distance, just orthogonal direction
        Point newStartXY = new Point(p1.x + orth.x, p1.y + orth.y);
        Point newEndXY = new Point(p2.x + orth.x, p2.y + orth.y);

        double angle = angle(lineStart, toLatLng(newStartXY));
        LatLng newStart = movePoint(lineStart, 3, angle);
        LatLng newEnd = movePoint(lineEnd, 3, angle);
        return new Pair<>(newStart, newEnd);
    }*/

/*    public static double calculateAngle(NodeContainer start, NodeContainer v1End, NodeContainer v2End) {
        Point pStart = toMerkator(start);
        Point p1End = toMerkator(v1End);
        Point p2End = toMerkator(v2End);
        return Math.acos()
    }*/

    private double dot(Point p1, Point p2) {
        return p1.x * p2.x + p1.y * p2.y;
    }

    private double norm(Point p) {
        return Math.sqrt(Math.pow(p.x, 2) + Math.pow(p.y, 2));
    }
}
