package com.usachev;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;
import org.openstreetmap.osmosis.xml.v0_6.impl.FastXmlParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Created by Andrey on 16.11.2016.
 */
public class Main {
    public static void main(String[] args) throws IOException, XMLStreamException
    {
        InputStream input = new FileInputStream("ptz.osm_02.osm");

        final XmlWriter xmlWriter = new XmlWriter(new File("output.osm"), CompressionMethod.None);

        XMLInputFactory factory = XMLInputFactory.newInstance();
        // configure it to create readers that coalesce adjacent character sections
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader r = factory.createXMLStreamReader(input);
        Sink sink = new Sink() {
            @Override
            public void process(EntityContainer entityContainer) {
                entityContainer.process(new EntityProcessor() {
                    @Override
                    public void process(BoundContainer bound) {
                        System.out.println("fds");
                        xmlWriter.process(bound);
//                        xmlWriter.writeBounds(bound);
                    }

                    @Override
                    public void process(NodeContainer node) {
                        log(node.getEntity().toString());
                        xmlWriter.process(node);
//                        node.getEntity().getWriteableInstance().toString();
                    }

                    @Override
                    public void process(WayContainer way) {
                        xmlWriter.process(way);
/*                        for (Tag tag : way.getEntity().getTags())
                        if (tag.getValue().equals("sidewalk") || tag.getKey().equals("sidewalk")) {
                            XmlWriter xmlWriter = new XmlWriter(new File("output.osm"), CompressionMethod.None);
                            xmlWriter.process(new EntityContainer() {
                                @Override
                                public void process(EntityProcessor processor) {

                                }

                                @Override
                                public Entity getEntity() {
                                    return null;
                                }

                                @Override
                                public EntityContainer getWriteableInstance() {
                                    return null;
                                }

                                @Override
                                public void store(StoreWriter writer, StoreClassRegister storeClassRegister) {

                                }
                            });
                        }*/
//                            System.out.println(tag.getKey() + " " + tag.getValue());
                    }

                    @Override
                    public void process(RelationContainer relation) {
                        xmlWriter.process(relation);
                    }
                });
            }

            @Override
            public void complete() {

            }

            @Override
            public void initialize(Map<String, Object> metaData) {

            }

            @Override
            public void release() {

            }
        };

        FastXmlParser fastXmlParser = new FastXmlParser(sink, r, true);

        fastXmlParser.readOsm();
        xmlWriter.complete();
        xmlWriter.release();
    }

    private static void log(String l) {
        System.out.println(l);
    }

}