/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.analysis.emissions_HaoWu;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
* @author ikaddoura
*/

public class RunOfflineAirPollutionAnalysis_NoCarZone_PlanA_BoundingBox {

	/* *********************************************************************** *
	   link of Google Drive for the input data of this class from Car-free zone project:
	   https://drive.google.com/drive/folders/1t2XWsdwyVPdgXowkd6Ku62-rtE8Ggf9k?usp=sharing
	 * *********************************************************************** */
//	final static String runDirectory = "public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/output-berlin-v5.4-10pct/";
//	final static String runId = "berlin-v5.4-10pct";
//
//	final static String hbefaFileCold = "shared-svn/projects/matsim-germany/hbefa/hbefa-files/v3.2/EFA_ColdStart_vehcat_2005average.txt";
//	final static String hbefaFileWarm = "shared-svn/projects/matsim-germany/hbefa/hbefa-files/v3.2/EFA_HOT_vehcat_2005average.txt";
	final static String runDirectory = "output-berlin-v5.5-1pct-subpop_PlanA_Version1_secondSuccessfulRun_100Iterations/";
	final static String runId = "berlin-v5.5-1pct";

	final static String hbefaFileCold = "EFA_ColdStart_Vehcat_2020_Average_perVehCat_Bln_carOnly.csv";
	final static String hbefaFileWarm = "EFA_HOT_Vehcat_2020_Average_perVehCat_Bln_carOnly.csv";

	public static void main(String[] args) {
		
//		String rootDirectory = null;
		String rootDirectory = "scenarios/berlin-v5.5-1pct/input/emissions_HaoWu";

//		if (args.length == 1) {
//			rootDirectory = args[0];
//		} else {
//			throw new RuntimeException("Please set the root directory. Aborting...");
//		}
		
		if (!rootDirectory.endsWith("/")) rootDirectory = rootDirectory + "/";
		
		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(rootDirectory + runDirectory + runId + ".output_vehicles.xml.gz");
		config.network().setInputFile(rootDirectory + runDirectory + runId + ".output_network.xml.gz");
		config.transit().setTransitScheduleFile(rootDirectory + runDirectory + runId + ".output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile(rootDirectory + runDirectory + runId + ".output_transitVehicles.xml.gz");
		//config.global().setCoordinateSystem("GK4");
		config.global().setCoordinateSystem("EPSG:31468");
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);
		
		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.directlyTryAverageTable);
		eConfig.setAverageColdEmissionFactorsFile(rootDirectory + hbefaFileCold);
		eConfig.setAverageWarmEmissionFactorsFile(rootDirectory + hbefaFileWarm);
		eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);

		final String emissionEventOutputFile = rootDirectory + runDirectory + runId + ".emission.events.offline.boundingbox.xml.gz";
		final String eventsFile = rootDirectory + runDirectory + runId + ".output_events.xml.gz";
		
		Scenario scenario = ScenarioUtils.loadScenario(config);



		//haowu
		String areaShapeFile = "/Users/haowu/Workspace/QGIS/MATSim_HA2/NoCarZone_withRoundabout/NoCarZone_withRoundabout.shp";
		Collection<SimpleFeature> features = (new ShapeFileReader()).readFileAndInitialize(areaShapeFile);

		Map<String, Geometry> zoneGeometries = new HashMap<>();
		for (SimpleFeature feature : features) {
			zoneGeometries.put((String)feature.getAttribute("Name"),(Geometry)feature.getDefaultGeometry());
		}

		Geometry areaGeometry = zoneGeometries.get(("NoCarZone"));

/*		//Prof. Nagel
		Collection<SimpleFeature>features = ShapeFileReader.getAllFeatures("areaShapeFile");
		GeometryFactory gf = new GeometryFactory();
		Geometry polygon = (Geometry)feature.getDefaultGeometry();*/

		// network
		for (Link link : scenario.getNetwork().getLinks().values()) {
			//for (Map.Entry<Id<Link>, ? extends Link> entry : scenario.getNetwork().getLinks().entrySet()) {
			//Link link = entry.getValue();

/*			//Prof. Nagel
			final Coordinate coord = CoordUtils.createGeotoolsCoordinate(link.getToNode().getCoord());
			//for(SimpleFeature feature:features){
				if(polygon.contains(gf.createPoint(coord))){
					RoadPricingUtils.addLink(scheme,link.getId());
				}
			//}*/

			//haowu
			Point linkCenterAsPoint = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());
			if(areaGeometry.contains(linkCenterAsPoint)){


				double freespeed = Double.NaN;

			if (link.getFreespeed() <= 13.888889) {
				freespeed = link.getFreespeed() * 2;
				// for non motorway roads, the free speed level was reduced
			} else {
				freespeed = link.getFreespeed();
				// for motorways, the original speed levels seems ok.
			}
			
			if(freespeed <= 8.333333333){ //30kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
			} else if(freespeed <= 11.111111111){ //40kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
			} else if(freespeed <= 13.888888889){ //50kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 16.666666667){ //60kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 19.444444444){ //70kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
			} else if(freespeed <= 22.222222222){ //80kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
			} else if(freespeed > 22.222222222){ //faster
				link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
			} else{
				throw new RuntimeException("Link not considered...");
			}
			}else{
				link.setLength(0);
				//scenario.getNetwork().removeLink(entry.getKey());
			}
		}
		
		// vehicles

		Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
		Id<VehicleType> freightVehicleTypeId = Id.create("freight", VehicleType.class);
		
		VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
		VehicleType freightVehicleType = scenario.getVehicles().getVehicleTypes().get(freightVehicleTypeId);
		
		EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaTechnology( carEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( carEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( carEngineInformation, "average" );

		//?????machbar?????
		//?????machbar?????
		EngineInformation freightEngineInformation = freightVehicleType.getEngineInformation();
		//VehicleUtils.setHbefaVehicleCategory( freightEngineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
		VehicleUtils.setHbefaVehicleCategory( freightEngineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
		VehicleUtils.setHbefaTechnology( freightEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( freightEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( freightEngineInformation, "average" );


		// vehicles for carInternal ***
		Id<VehicleType> carInternalVehicleTypeId = Id.create("carInternal", VehicleType.class);
		VehicleType carInternalVehicleType = scenario.getVehicles().getVehicleTypes().get(carInternalVehicleTypeId);

		EngineInformation carInternalEngineInformation = carInternalVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( carInternalEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaTechnology( carInternalEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( carInternalEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( carInternalEngineInformation, "average" );
		// vehicles for carInternal ***


		// public transit vehicles should be considered as non-hbefa vehicles
		for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();
			// TODO: Check! Is this a zero emission vehicle?!
			VehicleUtils.setHbefaVehicleCategory( engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
			VehicleUtils.setHbefaTechnology( engineInformation, "average" );
			VehicleUtils.setHbefaSizeClass( engineInformation, "average" );
			VehicleUtils.setHbefaEmissionsConcept( engineInformation, "average" );			
		}
		
		// the following is copy paste from the example...

        //Use the (old) single threaded events manager, since the multiThread one has problems with the hugh number of events: Exception: "queue full",
		// see also https://github.com/matsim-org/matsim-libs/issues/1091
		//The parallel EventsManager seems to work, if the number of Events is reduced, e.g. by reducing the number of links for which emissions should be calculated.
		// I did this by setting other link length to 0, so they get ignored. (Not a good solution but it was a nice quick fix/test)
		// KMT Jan'21
		// EventsManager eventsManager = EventsUtils.createEventsManager();  //This will at the moment create the ParallelEventsManager ... which causes problems
		EventsManager eventsManager = new EventsManagerImpl();  //The old, single threaded one.. running :)

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module);

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
        eventsManager.finishProcessing();

        emissionEventWriter.closeFile();
	}

}

