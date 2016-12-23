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

        XMLInputFactory factory = XMLInputFactory.newInstance();
        // configure it to create readers that coalesce adjacent character sections
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader r = factory.createXMLStreamReader(input);
        SidewalkProcessor processor = new SidewalkProcessor();
        Sink sink = new Sink() {
            @Override
            public void process(EntityContainer entityContainer) {
                entityContainer.process(new EntityProcessor() {
                    @Override
                    public void process(BoundContainer bound) {
                        processor.addBoundContainer(bound);
                    }

                    @Override
                    public void process(NodeContainer node) {
                        processor.addNodeContainer(node);
                    }

                    @Override
                    public void process(WayContainer way) {
                        processor.addWayContainer(way);
                    }

                    @Override
                    public void process(RelationContainer relation) {
                        processor.addRelationContainer(relation);
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

        processor.process();
    }


}