package com.usachev;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.osm.schema.Osm;
import org.osmtools.pbf.OsmProcessor;
import org.osmtools.pbf.data.Bounds;
import org.osmtools.pbf.data.Node;
import org.osmtools.pbf.data.Tag;
import org.osmtools.pbf.data.Way;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import crosby.binary.BinaryParser;
import crosby.binary.file.BlockOutputStream;
import crosby.binary.OsmObjectFactory;
import crosby.binary.Osmformat;
import crosby.binary.file.FileBlock;
import crosby.binary.file.FileBlockPosition;

/**
 * Created by Andrey on 16.11.2016.
 */

public class MyParser extends BinaryParser {

    private OsmProcessor sink;

    private BlockOutputStream output = null;

    private Osmformat.PrimitiveBlock.Builder b1 = Osmformat.PrimitiveBlock.newBuilder();
    private Osmformat.PrimitiveBlock b2;
    private Osmformat.HeaderBlock.Builder headerBlock;

    public MyParser(OsmProcessor sink) {
        this.sink = sink;
//        b1.setStringtable(makeStringTable("B1"));
        try {
            output = new BlockOutputStream(new FileOutputStream("1.pbf"));
//            output.setCompress("none");
//            headerBlock = Osmformat.HeaderBlock.newBuilder();
//            headerBlock.addRequiredFeatures("OsmSchema-V0.6").addRequiredFeatures("DenseNodes").setSource("QuickBrownFox");
//            output.write(FileBlock.newInstance("OSMHeader",b.build().toByteString(),null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Osmformat.StringTable makeStringTable(String prefix) {
        return
                Osmformat.StringTable.newBuilder()
//                        .addS(ByteString.copyFromUtf8("")) // Never used.
//                        .addS(ByteString.copyFromUtf8(prefix + "Offset1"))
//                        .addS(ByteString.copyFromUtf8(prefix + "Offset2"))
//                        .addS(ByteString.copyFromUtf8(prefix + "Offset3"))
//                        .addS(ByteString.copyFromUtf8(prefix + "Offset4"))
//                        .addS(ByteString.copyFromUtf8(prefix + "Offset5"))
//                        .addS(ByteString.copyFromUtf8(prefix + "Offset6"))
//                        .addS(ByteString.copyFromUtf8(prefix + "Offset7"))
//                        .addS(ByteString.copyFromUtf8(prefix + "Offset8"))
                        .build();
    }

/*    @Override
    public void handleBlock(FileBlock message) {
        super.handleBlock(message);
        try {
            b2 = Osmformat.PrimitiveBlock.parseFrom(message.getData());
            output.write(FileBlock.newInstance("OSMData", b2.toByteString(),null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    @Override
    public void parse(Osmformat.PrimitiveBlock block) {
        System.out.print("dsad");
        b1.setStringtable(block.getStringtable());
        b1.setDateGranularity(block.getDateGranularity());
        b1.setGranularity(block.getGranularity());
        b1.setLatOffset(block.getLatOffset());
        b1.setLonOffset(block.getLonOffset());
        super.parse(block);
    }

    @Override
    public void complete() {
        try {
            output.write(FileBlock.newInstance("OSMData", b1.build().toByteString(),null));
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void parseNodes(List<Osmformat.Node> nodes, OsmObjectFactory blockContext) {
        System.out.print(1);
        for (Osmformat.Node pbfNode : nodes) {
            sink.process(blockContext.createNode(pbfNode));
            b1.addPrimitivegroup(
                    Osmformat.PrimitiveGroup.newBuilder()
                            .addNodes(pbfNode)
            );
//            try {
//                output.write(FileBlock.newInstance("OSMData", pbfNode.toByteString(), null));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }

    @Override
    protected void parseWays(List<Osmformat.Way> ways, OsmObjectFactory blockContext) {
        System.out.print(2);
        for (Osmformat.Way way : ways) {
//            System.out.println(way.getKeysList().toString());
            Way way1 = blockContext.createWay(way);
            sink.process(way1);
            b1.addPrimitivegroup(
                    Osmformat.PrimitiveGroup.newBuilder()
                            .addWays(way)
            );
//            System.out.println("GSOM " + b1.getPrimitivegroup(b1.getPrimitivegroupCount() - 1).getWays(0).getKeysList().toString());
//            for (Tag tag : way1.getTags()) {
//                if (tag.getKey().equals("highway") && tag.getValue().equals("footway")) {
//                    try {
//                        output.write(FileBlock.newInstance("OSMData", way.toByteString(), null));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    break;
//                }
//            }
        }
    }

    @Override
    public void parse(Osmformat.HeaderBlock block) {
        System.out.print(3);
        for (String s : block.getRequiredFeaturesList()) {
            if (s.equals("OsmSchema-V0.6")) {
                continue; // We can parse this.
            }
            if (s.equals("DenseNodes")) {
                continue; // We can parse this.
            }
            throw new RuntimeException("File requires unknown feature: " + s);
        }

        if (block.hasBbox()) {
            String source = "myVersion";
            if (block.hasSource()) {
                source = block.getSource();
            }

            double multiplier = .000000001;
            double rightf = block.getBbox().getRight() * multiplier;
            double leftf = block.getBbox().getLeft() * multiplier;
            double topf = block.getBbox().getTop() * multiplier;
            double bottomf = block.getBbox().getBottom() * multiplier;
            sink.process(new Bounds(rightf, leftf, topf, bottomf, source));
        }
        try {
            output.write(FileBlock.newInstance("OSMHeader", block.toByteString(), null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void parseDense(Osmformat.DenseNodes nodes, OsmObjectFactory blockContext) {
        System.out.print(4);
        Osmformat.DenseNodes.Builder builder = Osmformat.DenseNodes.newBuilder();
        if (nodes.hasDenseinfo())
            builder.setDenseinfo(nodes.getDenseinfo());

        // Create pretty nodes objects from pbf
        List<Node> nodeList = blockContext.createNodes(nodes);
/*        for (int i = 0; i < nodes.getIdCount(); i++) {
            boolean someCondition = true;
            if (someCondition) {
                builder.addId(nodes.getLat(i));
                builder.addLat(nodes.getLat(i));
                builder.addLon(nodes.getLon(i));
                builder.addKeysVals(nodes.getKeysVals(i));
            }
            sink.process(nodeList.get(i));
        }*/

        b1.addPrimitivegroup(
                Osmformat.PrimitiveGroup.newBuilder().setDense(nodes)
        );
//        try {
//            output.write(FileBlock.newInstance("OSMData", nodes.toByteString(), null));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void parseRelations(List<Osmformat.Relation> rels, OsmObjectFactory objectFactory) {
        System.out.print(5);
        for (Osmformat.Relation relation : rels) {
//            Osmformat.Relation.newBuilder(relation).addAllKeys(relation.getKeysList());
            sink.process(objectFactory.createRelation(relation));
            b1.addPrimitivegroup(
                    Osmformat.PrimitiveGroup.newBuilder()
                            .addRelations(Osmformat.Relation.newBuilder(relation).addAllKeys(relation.getKeysList()))
            );
//            try {
//                output.write(FileBlock.newInstance("OSMData", relation.toByteString(), null));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }
}
