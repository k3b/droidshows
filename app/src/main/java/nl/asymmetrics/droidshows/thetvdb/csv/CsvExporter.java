/*
 This file is licensed under the GPLv3.
 Copyright (C) 2023 MaxIsV, k3b

 This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 General Public License as published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with this program.
 If not, see http://www.gnu.org/licenses/.
 */

package nl.asymmetrics.droidshows.thetvdb.csv;

import androidx.annotation.NonNull;

import com.opencsv.CSVWriter;

import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.List;

import nl.asymmetrics.droidshows.thetvdb.model.Episode;

/**
 * converts (list of) {@link Episode} items to csv format used for export.
 */
public class CsvExporter { //  implements AutoCloseable requires android-4.4 :-(
    public static DecimalFormat decimalFormat = new DecimalFormat("0.00");

    CSVWriter csvWriter;


    /**
     * Export these eposodes as csv to the resultWriter
     * @param resultWriter must be closed by the caller
     * @param episodes episode data to be exported
     */
    public static void writeEpisodes(@NonNull Writer resultWriter, String seriesId, @NonNull Iterator<Episode> episodes) {
        CsvExporter csvExporter = new CsvExporter(resultWriter);
        csvExporter.writeCsvHeader();

        while(episodes.hasNext()) {
            csvExporter.writeCsvLine(seriesId,episodes.next());
        }
    }

    public CsvExporter(Writer resultWriter) {
        /* requires com.opencsv:opencsv:5.7.1 that is not compatible with old java binary format JavaVersion.VERSION_1_8
        csvWriter = new CSVWriterBuilder(resultWriter)
                .withSeparator(CsvDefinitions.CSV_FIELD_DELIMITER_CHAR)
                .build();

         */
        // compatible with com.opencsv:opencsv:3.10
        csvWriter = new CSVWriter(resultWriter, CsvDefinitions.CSV_FIELD_DELIMITER_CHAR);

        setDecimalFormat();
    }

    private void writeCsvLine(String... columns) {
        csvWriter.writeNext(columns, false);
        csvWriter.flushQuietly();
    }

    public void writeCsvHeader() {
        writeCsvLine(new CsvDefinitions.Eposode().CSV_HEADER);
    }

    /** writes in the same column order as CsvDefinitions#Eposode#CSV_HEADER */
    public void writeCsvLine(String seriesId, @NonNull Episode episode) {
        writeCsvLine(
                toString(seriesId)
                ,toString(episode.getLanguage())
                ,toString(episode.getSeasonNumber())
                ,toString(episode.getEpisodeNumber())
                ,getEpisodeId(episode)
//                ,toString(episode.getCombinedEpisodeNumber())
//                ,toString(episode.getCombinedSeason())
                ,toString(episode.getEpisodeName())
                ,toString(episode.getSeenDate())
                ,toString(episode.getFirstAired())
                ,toString(episode.getOverview())
                );

    }

    private String getEpisodeId(@NonNull Episode episode) {
        if (!isEmpty(episode.getImdbId())) {
            return "imdb:" + episode.getImdbId();
        }
        return "local:" + episode.getId();
    }

    private void setDecimalFormat() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(dfs);
    }

    /**
     * nullsafe: converts object to string
     */
    private String toString(Object o) {
        return o != null ? o.toString() : null;
    }


    /**
     * return true if o is null or an empty string
     */
    private boolean isEmpty(Object o) {
        return o == null || toString(o).isEmpty() || 0 == "null".compareToIgnoreCase(toString(o));
    }

    // @Override implements AutoCloseable in android-4.4 and later
    public void close() throws Exception {
        csvWriter.close();
    }
}
