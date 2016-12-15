package com.usachev;

import org.osmtools.pbf.OsmProcessor;
import org.osmtools.pbf.data.Bounds;
import org.osmtools.pbf.data.Node;
import org.osmtools.pbf.data.Relation;
import org.osmtools.pbf.data.RelationMember;
import org.osmtools.pbf.data.Tag;
import org.osmtools.pbf.data.Way;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import crosby.binary.file.BlockInputStream;

/**
 * Created by Andrey on 16.11.2016.
 */

public class Main {
    public static void main(String[] args) throws IOException
    {
//        Main.class.getClass().getPackage();
//        InputStream input = Main.class.getResourceAsStream("ptz.osm.pbf");
        InputStream input = new FileInputStream("ptz.osm.pbf");

        MyParser parser = new MyParser(new OsmProcessor() {

            @Override
            public void process(Bounds bounds) {
            }

            @Override
            public void process(Node node) {
            }

            @Override
            public void process(Way way) {
//                if (way.getId() == 316635916)
//                    System.out.print(way.getNodes());

/*                for (Tag tag : way.getTags()) {
                    System.out.print(tag.getKey() + " ");
//                    if (tag.getKey().equals("sidewalk"))
//                        System.out.print(tag.getValue() + '\n');
                }
                        System.out.print('\n');*/
            }

            @Override
            public void process(Relation relation) {
/*                System.out.println("--------------------------------");
                for (RelationMember member : relation.getMembers())
                    if (member.getRole().equals("outer"))
                    System.out.println(member.getId() + " " + member.getRole() + " " + member.getType());
                System.out.println("--------------------------------");*/
            }

        });

        new BlockInputStream(input, parser).process();

    }

}