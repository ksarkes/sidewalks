package com.usachev;

import org.osmtools.pbf.OsmProcessor;
import org.osmtools.pbf.data.Bounds;

import crosby.binary.Osmformat;

/**
 * Created by Andrey on 19.12.2016.
 */

public interface NotifyingOsmProcessor {

    void complete();

    void process(Osmformat.Node node);

    void process(Osmformat.Way way);

    void process(Osmformat.Relation relation);

    void process(Bounds bounds);
}
