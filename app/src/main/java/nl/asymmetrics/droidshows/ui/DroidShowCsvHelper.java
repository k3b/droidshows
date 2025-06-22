package nl.asymmetrics.droidshows.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.PrintWriter;
import java.util.List;

import nl.asymmetrics.droidshows.R;
import nl.asymmetrics.droidshows.thetvdb.csv.CsvExporter;
import nl.asymmetrics.droidshows.thetvdb.model.Episode;
import nl.asymmetrics.droidshows.utils.AndroidFileUtils;
import nl.asymmetrics.droidshows.utils.PermissionHelper;
import nl.asymmetrics.droidshows.utils.SQLiteStore;

/**
 * Android specific callback logic to implement csv export
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
            // has permission. Ask for output dir
            String mimetype = AndroidFileUtils.getCsvMimeType();

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            intent.setType(mimetype);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // String newUrl = ""
            if (lastUsedBackupUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,lastUsedBackupUri);
            }

            activity.startActivityForResult(intent, requestCode);
            // continue with onActivityResult() -> onOpenDocumentFilePickForCsvExportResult()

        } // else have no permession yet.
        // openDocumentFilePickForRestore -> permissionRequest -> onRequestPermissionsResult() -> onOpenDocumentFilePickForCsvExportResult(uri) -> restore(inFile)
    }

    /**
     * Executes csv Export
     *
     * @param context        - Android context to access activity-instance specivic data.
     * @param outputFileUri  where the csv will be written to.
     * @param filterSeriesId if not null: only episodes belonging to this series are exported
     * @param filterSeasonNumber if not null: only episodes belonging to this season are exported
     */
    public static void onOpenDocumentFilePickForCsvExportResult(Context context, Uri outputFileUri, @Nullable String filterSeriesId,@Nullable Integer filterSeasonNumber) {
        if (outputFileUri != null) {

            int toastTxt = R.string.dialog_csv_export_done;
            PrintWriter out = null;
            try {
                List<Episode> episodes = SQLiteStore.getInstance(context).getEpisodes(filterSeriesId, filterSeasonNumber);
                out = new PrintWriter(context.getContentResolver().openOutputStream(outputFileUri, "wt"));
                CsvExporter.writeEpisodes(out, filterSeriesId, episodes.iterator());
            } catch (Exception e) {
                toastTxt = R.string.dialog_csv_export_failed;
                Log.e(SQLiteStore.TAG, "Csv export to " + outputFileUri +
                        " failed for series " + filterSeriesId, e);
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
