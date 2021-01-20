package org.matsim.analysis.emissions_HaoWu;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.*;

// haowu ***
//import org.matsim.core.events.EventsUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
// haowu ***

import org.matsim.core.utils.collections.Tuple;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateSingleAirPollutionValue {

    private final Map<Pollutant, Double> pollution = new HashMap<>();

    @Parameter(names = {"-events", "-e"}, required = true)
    private List<String> eventsFiles = new ArrayList<>();

    @Parameter(names = {"-output", "-o"}, required = true)
    private String outputFile = "";

    /**
     * Run the script with command line args
     *
     * @param args e.g. -e /path/to/your/emission/events-file.xml.gz -e /path/to/your/other/emission/events-file.xml.gz -o /path/to/your/output/file.csv
     */
    // haowu ***
    // Program Arguments:
    //base Case: -e scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_baseCase_100Iterations/berlin-v5.5-1pct.emission.events.offline.xml.gz -o scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_baseCase_100Iterations/berlin-v5.5-1pct.emission.SingleAirPollutionValue.csv
    //PlanA: -e scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct-subpop_PlanA_Version1_secondSuccessfulRun_100Iterations/berlin-v5.5-1pct.emission.events.offline.xml.gz -o scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct-subpop_PlanA_Version1_secondSuccessfulRun_100Iterations/berlin-v5.5-1pct.emission.SingleAirPollutionValue.csv
    //PlanB: -e scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_PlanB_Network2_Version1_secondSuccessfulRun_100Iterations/berlin-v5.5-1pct.emission.events.offline.xml.gz -o scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_PlanB_Network2_Version1_secondSuccessfulRun_100Iterations/berlin-v5.5-1pct.emission.SingleAirPollutionValue.csv
    //PlanC: -e scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_PlanC_firstSuccessfulRun_100Iterations/berlin-v5.5-1pct.emission.events.offline.xml.gz -o scenarios/berlin-v5.5-1pct/input/emissions_HaoWu/output-berlin-v5.5-1pct_PlanC_firstSuccessfulRun_100Iterations/berlin-v5.5-1pct.emission.SingleAirPollutionValue.csv
    // haowu ***
    public static void main(String[] args) throws IOException {

        var analysis = new GenerateSingleAirPollutionValue();
        JCommander.newBuilder().addObject(analysis).build().parse(args);
        analysis.run();
    }

    public void run() throws IOException {

        var pollutionPerRun = eventsFiles.parallelStream()
                .map(file -> {
                    Map<Pollutant, Double> pollution = new TreeMap<>();

                    // haowu ***
                    //var manager = EventsUtils.createEventsManager();
                    EventsManager manager = new EventsManagerImpl();
                    // haowu ***

                    manager.addHandler(new Handler(pollution));
                    var reader = new EmissionEventsReader(manager);
                    reader.readFile(file);
                    return Tuple.of(file, pollution);
                })
                .peek(tuple -> {
                    // some debugging output
                    for (var p : tuple.getSecond().entrySet()) {
                        System.out.println(extractFilename(tuple.getFirst()) + ": " + p.getKey() + ": \t\t" + p.getValue());
                    }
                })
                .collect(Collectors.toList());

        // create the header out of the pollution map of the first run. Assuming, all runs emitted the same set of pollutants
        var csvHeader = pollutionPerRun.get(0).getSecond().keySet().stream()
                .map(pollutant -> pollutant.toString())
                .collect(Collectors.toList());

        csvHeader.add(0, "Run");


        try (Writer writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                printer.printRecord(csvHeader);

                for (var tuple : pollutionPerRun) {

                    // this most probably only works if the filename is the standard offline emissions events file
                    var filename = extractFilename(tuple.getFirst());
                    var runId = filename.substring(0, filename.indexOf('.'));
                    printer.print(runId);

                    for (Double value : tuple.getSecond().values()) {
                        printer.print(value);
                    }
                    printer.println();
                }
                printer.flush();
            }
        }
    }

    private String extractFilename(String filePath) {

        var index = filePath.lastIndexOf("/");
        if (index < 0) {
            index = filePath.lastIndexOf("\\");
        }
        return index >= 0 ? filePath.substring(index + 1) : filePath;
    }


    private static class Handler implements ColdEmissionEventHandler, WarmEmissionEventHandler {

        private final Map<Pollutant, Double> pollution;

        private Handler(Map<Pollutant, Double> pollution) {
            this.pollution = pollution;
        }

        @Override
        public void handleEvent(ColdEmissionEvent event) {

            for (Map.Entry<Pollutant, Double> pollutant : event.getColdEmissions().entrySet()) {
                pollution.merge(pollutant.getKey(), pollutant.getValue(), Double::sum);
            }
        }

        @Override
        public void handleEvent(WarmEmissionEvent event) {

            for (Map.Entry<Pollutant, Double> pollutant : event.getWarmEmissions().entrySet()) {
                pollution.merge(pollutant.getKey(), pollutant.getValue(), Double::sum);
            }
        }
    }
}
