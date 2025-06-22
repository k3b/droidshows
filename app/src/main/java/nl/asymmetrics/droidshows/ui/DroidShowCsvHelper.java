package nl.asymmetrics.droidshows.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nl.asymmetrics.droidshows.DroidShows;
import nl.asymmetrics.droidshows.R;
import nl.asymmetrics.droidshows.thetvdb.csv.CsvExporter;
import nl.asymmetrics.droidshows.thetvdb.model.Episode;
import nl.asymmetrics.droidshows.utils.AndroidFileUtils;
import nl.asymmetrics.droidshows.utils.PermissionHelper;
import nl.asymmetrics.droidshows.utils.SQLiteStore;

/**
 * Android specific workflow/callback-logic to implement csv export
 * Workflow:
 *  * Activity.onMenuExportAsCsv -> DroidShowCsvHelper.openDocumentFilePickForCsvExport(..,requestCode,..)
 *  * DroidShowCsvHelper: if permission needed:
 *  * * ActivityCompat.requestPermissions(..., requestCode);
 *  * * Activity.onRequestPermissionsResult(requestCode,..) -> DroidShowCsvHelper.openDocumentFilePickForCsvExport(..,requestCode,..)
 *  * DroidShowCsvHelper: if permission granted:
 *  * * ACTION_CREATE_DOCUMENT -> Activity.onActivityResult(requestCode,..) -> DroidShowCsvHelper.onOpenDocumentFilePickForCsvExportResult(..)
 */

@RequiresApi(Build.VERSION_CODES.M)
public class DroidShowCsvHelper {
    /**
     * * false: ask user for manually enter csv output file name
     * * true: ask for csv output directory and app creates file name based on context and date.
     */
    final private static boolean USE_PICK_CSV_OUTPUT_DIRECTORY = true;

    /**
     * Shows picker to get output file for csv export and calls
     * Activity#onActivityResult(int, int, Intent) on success which
     * should call {@link #onOpenDocumentFilePickForCsvExportResult(Context, Uri, String, Integer)}.
     * May ask for read/write permissions before.
     *
     * @param activity     owner of the callback #onActivityResult
     * @param requestCode  to be used by callback #onActivityResult and by PermissionHelper
     * @param seriesId series beeing used
     * @param seasonNumber seasonNumber beeing used
     */
    public static void openDocumentFilePickForCsvExport(
            Activity activity, int requestCode,
            @Nullable Uri lastUsedBackupUri, String seriesId, Integer seasonNumber) {
        if (PermissionHelper.hasPermissionOrRequest(activity, requestCode)) {
            // has permission. Ask for output dir or output file

            Intent intent;
            if (USE_PICK_CSV_OUTPUT_DIRECTORY) {
                intent= new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                if (lastUsedBackupUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // can only tell last used name
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, lastUsedBackupUri);
                }

            } else {
                intent= new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType(AndroidFileUtils.getCsvMimeType());
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // android specific: found no way to tell the output file chooser a default filename :-(
                if (lastUsedBackupUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // can only tell last used name
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, lastUsedBackupUri);
                }
            }
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

            activity.startActivityForResult(intent, requestCode);
            // continue with onActivityResult() -> onOpenDocumentFilePickForCsvExportResult()

        } // else have no permession yet.
        // openDocumentFilePickForRestore -> permissionRequest -> onRequestPermissionsResult() -> onOpenDocumentFilePickForCsvExportResult(uri) -> restore(inFile)
    }

    /**
     * Example: createCsvFileName("Kant für Anfänger", "2","2024-12-24") becomes "KantFürAnfänger-2-20241224.csv"
     */
    private static String createCsvFileName(Context context, @Nullable String filterSeriesId, @Nullable Integer filterSeasonNumber, @Nullable Date exportDate) {
        StringBuilder result = new StringBuilder();
        result.append(getSerieNameAsCamelCase(context, filterSeriesId));

        if (filterSeasonNumber != null) {
            result.append("-").append(filterSeasonNumber);
        }
        if (exportDate != null) {
            result.append("-").append(new SimpleDateFormat("yyyyMMdd").format(exportDate));

        }
        result.append(".csv");

        return result.toString();
    }

    private static @NonNull String getSerieNameAsCamelCase(Context context, @Nullable String filterSeriesId) {
        String serieName = "";
        if (!StringUtils.isEmpty(filterSeriesId)) {
            serieName = nl.asymmetrics.droidshows.utils.MyStringUtils.toCamelCase(SQLiteStore.getInstance(context).getSerieName(filterSeriesId));
        }

        // fallback if there is no serieName: use app name instead
        if (StringUtils.isEmpty(serieName)) {
            serieName = context.getString(R.string.layout_app_name);
        }
        return serieName;
    }

    /**
     * Executes csv Export
     *
     * @param context        - Android context to access activity-instance specivic data.
     * @param outputUri  where the csv will be written to: either outDirectory or outFile.
     * @param filterSeriesId if not null: only episodes belonging to this series are exported
     * @param filterSeasonNumber if not null: only episodes belonging to this season are exported
     */
    public static void onOpenDocumentFilePickForCsvExportResult(Context context, Uri outputUri, @Nullable String filterSeriesId,@Nullable Integer filterSeasonNumber) {
        if (outputUri != null) {
            String toastTxt = null;
            PrintWriter out = null;
            String csvFileName = "";

            try {
                if (USE_PICK_CSV_OUTPUT_DIRECTORY) {
                    DocumentFile csvOutDir = DocumentFile.fromTreeUri(context, outputUri);
                    csvFileName = createCsvFileName(context, filterSeriesId, filterSeasonNumber, null);
                    if (csvOutDir != null) {
                        DocumentFile csvOutFile = csvOutDir.createFile(AndroidFileUtils.getCsvMimeType(), csvFileName);
                        if (csvOutFile != null) {
                            out = new PrintWriter(context.getContentResolver().openOutputStream(csvOutFile.getUri(), "wt"));
                            DroidShows.saveLastUsedBackupFolder(context, outputUri);
                        }
                    }
                } else {
                    out = new PrintWriter(context.getContentResolver().openOutputStream(outputUri, "wt"));
                }
                if (out != null) {
                    List<Episode> episodes = SQLiteStore.getInstance(context).getEpisodes(filterSeriesId, filterSeasonNumber);
                    CsvExporter.writeEpisodes(out, filterSeriesId, episodes.iterator());
                    toastTxt = context.getString(R.string.dialog_csv_export_done) +" " + outputUri + "/" + csvFileName;
                } else {
                    toastTxt = context.getString(R.string.dialog_csv_export_done) +" " + outputUri + "/" + csvFileName;
                }
                Log.i(SQLiteStore.TAG, toastTxt
                        + " for series: " + filterSeriesId
                        + " seasonNumber: " + filterSeasonNumber);
            } catch (Exception e) {
                toastTxt = context.getString(R.string.dialog_csv_export_done) +" " + outputUri + "/" + csvFileName;
                Log.e(SQLiteStore.TAG, toastTxt
                        + " for series: " + filterSeriesId
                        + " seasonNumber: " + filterSeasonNumber, e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception closeException) {
                        // ignore closeException
                    }
                }
            }
            Toast.makeText(context.getApplicationContext(), toastTxt, Toast.LENGTH_LONG).show();
        }
    }

}
