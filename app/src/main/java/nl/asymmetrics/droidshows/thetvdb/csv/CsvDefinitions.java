package nl.asymmetrics.droidshows.thetvdb.csv;

/**
 * The column that are supported by csv
 */
public class CsvDefinitions {
    public static final char CSV_FIELD_DELIMITER_CHAR = ';';

    /** column names belonging to episodes */
    public static class Eposode {
        public static final String COLUMN_NAME_SERIES_ID = "seriesId";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_EPISODE_NAME = "episodeName";
        public static final String COLUMN_NAME_EPISODE_NUMBER = "episodeNumber";
        public static final String COLUMN_NAME_FIRST_AIRED = "firstAired";
        public static final String COLUMN_NAME_LANGUAGE = "language";
        public static final String COLUMN_NAME_OVERVIEW = "overview";
        public static final String COLUMN_NAME_SEASON_NUMBER = "seasonNumber";
        public static final String COLUMN_NAME_SEEN = "seen";

        // redundant columns in droidshows database but not uses in csv
        // public static final String COLUMN_NAME_COMBINED_EPISODE_NUMBER = "combinedEpisodeNumber";
        // public static final String COLUMN_NAME_COMBINED_SEASON = "combinedSeason";

        // static String[] CSV_HEADER needs java-verson greater than 1.8 :-(
        public final String[] CSV_HEADER = {
                COLUMN_NAME_SERIES_ID
                ,COLUMN_NAME_LANGUAGE
                ,COLUMN_NAME_SEASON_NUMBER
                ,COLUMN_NAME_EPISODE_NUMBER
                ,COLUMN_NAME_ID
//                ,COLUMN_NAME_COMBINED_EPISODE_NUMBER
//                ,COLUMN_NAME_COMBINED_SEASON
                ,COLUMN_NAME_EPISODE_NAME
                ,COLUMN_NAME_SEEN
                ,COLUMN_NAME_FIRST_AIRED
                ,COLUMN_NAME_OVERVIEW
        };
    }
}
