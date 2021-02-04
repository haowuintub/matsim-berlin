package org.matsim.analysis.emissions_HaoWu;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

public class NetworkModifier_NoCarZone {

    public static void main(String[] args) {
        String networkInputFile = "scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_baseCase_100Iterations/berlin-v5.5-1pct.output_network.xml.gz";
        String networkOutputFile = "scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/berlin-v5.5-1pct.output_network_NoCarZone.xml.gz";

        String areaShapeFile = "/Users/haowu/Workspace/QGIS/MATSim_HA2/NoCarZone_withRoundabout_fixed/NoCarZone_withRoundabout_fixed.shp";

        Collection<SimpleFeature> features = (new ShapeFileReader()).readFileAndInitialize(areaShapeFile);

        Map<String, Geometry> zoneGeometries = new HashMap<>();
        for (SimpleFeature feature : features) {
            zoneGeometries.put((String)feature.getAttribute("Name"),(Geometry)feature.getDefaultGeometry());
        }

        Geometry areaGeometry = zoneGeometries.get(("NoCarZone"));

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        //for(Link link : scenario.getNetwork().getLinks().values()){
        for (Map.Entry<Id<Link>, ? extends Link> entry : scenario.getNetwork().getLinks().entrySet()) {
        Link link = entry.getValue();
            Point linkCenterAsPoint = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());
            if(areaGeometry.contains(linkCenterAsPoint)) {

            }else{
                scenario.getNetwork().removeLink(entry.getKey());
            }
        }

        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
        writer.write(networkOutputFile);
    }
}