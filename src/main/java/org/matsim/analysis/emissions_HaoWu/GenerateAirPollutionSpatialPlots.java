package org.matsim.analysis.emissions_HaoWu;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
//package org.matsim.ruhrgebiet.analysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author amit, ihab
 */

public class GenerateAirPollutionSpatialPlots {
    private static final Logger log = Logger.getLogger(GenerateAirPollutionSpatialPlots.class);

    // haowu ***
/*    private static final double xMin = 317373;
    private static final double yMin = 5675521.;
    private static final double xMax = 418575.;
    private static final double yMax = 5736671.;*/
/*    //smaller BoundingBox:
    private static final double xMin = 4588081.472;
    private static final double yMin = 5818614.185;
    private static final double xMax = 4592318.063;
    private static final double yMax = 5821261.458;*/
    //bigger BoundingBox:
    private static final double xMin = 4588081.472-1/3*(4592318.063-4588081.472);
    private static final double yMin = 5818614.185-1/3*(5821261.458-5818614.185);
    private static final double xMax = 4592318.063+1/3*(4592318.063-4588081.472);
    private static final double yMax = 5821261.458+1/3*(5821261.458-5818614.185);
    // haowu ***

    private static final double gridSize = 100.;
    private static final double smoothingRadius = 500.;
    private static final double scaleFactor = 100.;

    @Parameter(names = {"-dir"}, required = true)
    private String runDir = "";

    @Parameter(names = {"-runId"}, required = true)
    private String runId = "";

    @Parameter(names = {"-outDir"})
    private String outDir = "";

    // haowu ***
    // Program Arguments:
    //baseCase: -dir scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_baseCase_100Iterations/ -runId berlin-v5.5-1pct -outDir scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_baseCase_100Iterations/
    //PlanA: -dir scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct-subpop_PlanA_Version1_secondSuccessfulRun_100Iterations/ -runId berlin-v5.5-1pct -outDir scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct-subpop_PlanA_Version1_secondSuccessfulRun_100Iterations/
    //PlanB: -dir scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_PlanB_Network2_Version1_secondSuccessfulRun_100Iterations/ -runId berlin-v5.5-1pct -outDir scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_PlanB_Network2_Version1_secondSuccessfulRun_100Iterations/
    //PlanC: -dir scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_PlanC_firstSuccessfulRun_100Iterations -runId berlin-v5.5-1pct -outDir scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_PlanC_firstSuccessfulRun_100Iterations/
    // haowu ***

    private GenerateAirPollutionSpatialPlots() {

    }

    public static void main(String[] args) {

        GenerateAirPollutionSpatialPlots plots = new GenerateAirPollutionSpatialPlots();

        JCommander.newBuilder().addObject(plots).build().parse(args);

        plots.writeEmissions();
    }

    private void writeEmissions() {

        final String configFile = runDir + runId + ".output_config.xml";
        final String events = runDir + runId + ".emission.events.offline.xml.gz";
        final String outputDir = StringUtils.isBlank(outDir) ? runDir : outDir;
        final String outputFile = outputDir + runId + ".emissionsgrid.csv";

        Config config = ConfigUtils.loadConfig(configFile);
        config.plans().setInputFile(null);
        config.transit().setTransitScheduleFile(null);
        config.transit().setVehiclesFile(null);
        config.vehicles().setVehiclesFile(null);
        config.network().setInputFile(/*runDir +*/ runId + ".output_network.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        double binSize = 200000; // make the bin size bigger than the scenario has seconds
        Network network = scenario.getNetwork();

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withGridSize(gridSize)
                .withTimeBinSize(binSize)
                .withNetwork(network)
                .withBounds(createBoundingBox())
                .withSmoothingRadius(smoothingRadius)
                .withCountScaleFactor(scaleFactor)
                .withGridType(EmissionGridAnalyzer.GridType.Hexagonal)
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBins = analyzer.process(events);
        //analyzer.processToJsonFile(events, outputFile + ".json");

        log.info("Writing to csv...");
        writeGridToCSV(timeBins, outputFile);
    }

    private void writeGridToCSV(TimeBinMap<Grid<Map<Pollutant, Double>>> bins, String outputPath) {

        var pollutants = Pollutant.values();

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputPath), CSVFormat.TDF)) {

            //print header with all possible pollutants
            printer.print("timeBinStartTime");
            printer.print("x");
            printer.print("y");

            for (var p : pollutants) {
                printer.print(p.toString());
            }
            printer.println();

            //print values if pollutant was not present just print 0 instead
            for (TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin : bins.getTimeBins()) {
                final double timeBinStartTime = bin.getStartTime();
                for (Grid.Cell<Map<Pollutant, Double>> cell : bin.getValue().getCells()) {

                    printer.print(timeBinStartTime);
                    printer.print(cell.getCoordinate().x);
                    printer.print(cell.getCoordinate().y);

                    for (var p : pollutants) {
                        if (cell.getValue().containsKey(p)) {
                            printer.print(cell.getValue().get(p));
                        } else {
                            printer.print(0);
                        }
                    }
                    printer.println();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Geometry createBoundingBox() {
/*        return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
                new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
                new Coordinate(xMin, yMin)
        });*/

        // haowu ***
        //same kind of BoundingBox but using my own shapeFile as the BoundingBox
        String areaShapeFile = "/Users/haowu/Workspace/QGIS/MATSim_HA2/NoCarZone_withRoundabout/NoCarZone_withRoundabout.shp";
        Collection<SimpleFeature> features = (new ShapeFileReader()).readFileAndInitialize(areaShapeFile);

        Map<String, Geometry> zoneGeometries = new HashMap<>();
        for (SimpleFeature feature : features) {
            zoneGeometries.put((String)feature.getAttribute("Name"),(Geometry)feature.getDefaultGeometry());
        }

        Geometry areaGeometry = zoneGeometries.get(("NoCarZone"));
        return areaGeometry;
        // haowu ***
    }
}
