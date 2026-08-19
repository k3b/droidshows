package nl.asymmetrics.droidshows.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nl.asymmetrics.droidshows.DroidShows;
import nl.asymmetrics.droidshows.R;
import nl.asymmetrics.droidshows.thetvdb.model.Season;
import nl.asymmetrics.droidshows.utils.PermissionHelper;
import nl.asymmetrics.droidshows.utils.SQLiteStore;
import nl.asymmetrics.droidshows.utils.SwipeDetect;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

public class SerieSeasons extends ListActivity  implements ActivityCompat.OnRequestPermissionsResultCallback
{
	private String serieId;
	private List<Integer> seasonNumbers = new ArrayList<Integer>();
	private static List<Season> seasons;
	// Context Menus
	private static final int ALLEPSEEN_CONTEXT = Menu.FIRST;
	private static final int ALLUPTOTHIS_CONTEXT = ALLEPSEEN_CONTEXT + 1;
	private static final int ALLEPUNSEEN_CONTEXT = ALLUPTOTHIS_CONTEXT + 1;
	private static ListView listView;
	public static SeriesSeasonsAdapter seasonsAdapter;
	private SwipeDetect swipeDetect = new SwipeDetect();
	private static SQLiteStore db;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.overridePendingTransition(R.anim.right_enter, R.anim.right_exit);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.serie_seasons);
		db = SQLiteStore.getInstance(this);
		serieId = getIntent().getStringExtra("serieId");
		setTitle(db.getSerieName(serieId));
		getSeasons();
		seasonsAdapter = new SeriesSeasonsAdapter(this, R.layout.row_serie_seasons, seasons);
		setListAdapter(seasonsAdapter);
		listView = getListView();
		listView.getViewTreeObserver().addOnGlobalLayoutListener(listDone);
		registerForContextMenu(listView);
		listView.setOnTouchListener(swipeDetect);
		listView.setSelection(getIntent().getIntExtra("season", 1) -1);
		if (getIntent().getBooleanExtra("nextEpisode", false))
			listView.setSelection(db.getNextEpisode(serieId).season -1);
	}

	/* context menu */
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, ALLEPSEEN_CONTEXT, 0, getString(R.string.messages_context_mark_seasonseen));
		menu.add(0, ALLUPTOTHIS_CONTEXT, 0, getString(R.string.messages_context_mark_asseenuptothis));
		menu.add(0, ALLEPUNSEEN_CONTEXT, 0, getString(R.string.messages_context_mark_seasonunseen));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			menu.add(0, DroidShows.EXPORT_AS_CSV_MENU_ID, DroidShows.EXPORT_AS_CSV_MENU_ID, getString(R.string.menu_context_export_csv));
		}
		menu.setHeaderTitle(seasonsAdapter.getItem(((AdapterContextMenuInfo) menuInfo).position).getSeason());
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case ALLEPSEEN_CONTEXT :
				db.updateUnwatchedSeason(serieId, seasonNumbers.get(info.position));
				getInfo();
				return true;
			case ALLEPUNSEEN_CONTEXT :
				db.updateWatchedSeason(serieId, seasonNumbers.get(info.position));
				getInfo();
				return true;
			case ALLUPTOTHIS_CONTEXT :
				for (int i = 1; i <= seasonNumbers.get(info.position); i++) {
					db.updateUnwatchedSeason(serieId, i);
				}
				getInfo();
				return true;
			case DroidShows.EXPORT_AS_CSV_MENU_ID:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    exportAsCsv(serieId,seasonNumbers.get(info.position));
                }
                return true;
			default :
				return super.onContextItemSelected(item);
		}
	}

	@SuppressLint("NewApi")
	public void openContext(View v) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			listView.showContextMenuForChild(v, v.getX(), v.getY());
		else
			openContextMenu(v);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (swipeDetect.value != 0) return;
		Intent serieEpisode = new Intent(SerieSeasons.this, SerieEpisodes.class);
		serieEpisode.putExtra("serieId", serieId);
		serieEpisode.putExtra("seasonNumber", seasonNumbers.get(position));
		if (seasonsAdapter.getItem(position).getUnwatched() > 0)
			serieEpisode.putExtra("nextEpisode", true);
		startActivity(serieEpisode);
	}

	private void getSeasons() {
		seasons = new ArrayList<Season>();
		try {
			Cursor cseasons = db.Query("SELECT season FROM serie_seasons WHERE serieId = '"+ serieId +"'  ORDER BY 0+season ASC");	// 0+ to treat VARCHAR as integer an sort properly
			cseasons.moveToFirst();
			if (cseasons.getCount() != 0) {
				do {
					seasonNumbers.add(cseasons.getInt(0));
				} while (cseasons.moveToNext());
			}
			cseasons.close();
		} catch (Exception e) {
			Log.e(SQLiteStore.TAG, "Error getting seasons: "+ e.getMessage());
		}
		final String strSeason = getString(R.string.messages_season);
		for (int i = 0; i < seasonNumbers.size(); i++) {
			String season = "";
			if (seasonNumbers.get(i) == 0) {
				season = getString(R.string.messages_specials);
			} else {
				season = strSeason + " " + seasonNumbers.get(i);
			}
			Season newSeason = new Season(serieId, seasonNumbers.get(i), season, -1, -1, null, null);
			seasons.add(newSeason);
		}
	}

	private void getInfo() {
		new AsyncInfo().execute();
	}

	private final OnGlobalLayoutListener listDone = new OnGlobalLayoutListener() {
		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		public void onGlobalLayout() {
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
				listView.getViewTreeObserver().removeGlobalOnLayoutListener(listDone);
			else
				listView.getViewTreeObserver().removeOnGlobalLayoutListener(listDone);
			getInfo();
		}
	};

	private static class AsyncInfo extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				for (int i = 0; i < seasons.size(); i++) {
					String serieId = seasons.get(i).getSerieId();
					int seasonNumber = seasons.get(i).getSNumber();
					int unwatchedAired = db.getEpsUnwatchedAired(serieId, seasonNumber);
					int unwatched = db.getEpsUnwatched(serieId, seasonNumber);
					seasons.get(i).setUnwatchedAired(unwatchedAired);
					seasons.get(i).setUnwatched(unwatched);
					if (unwatched > 0) {
						SQLiteStore.NextEpisode nextEpisode = db.getNextEpisode(serieId, seasonNumber);
						seasons.get(i).setNextAir(nextEpisode.firstAiredDate);
						seasons.get(i).setNextEpisode(db.getNextEpisodeString(nextEpisode));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			seasonsAdapter.notifyDataSetChanged();
			super.onPostExecute(result);
		}
	}

	@Override
	public void onRestart() {
		super.onRestart();
		getInfo();
	}

	private class SeriesSeasonsAdapter extends ArrayAdapter<Season>
	{
		private List<Season> items;
		private LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		private final String strEpAired = getString(R.string.messages_ep_aired);
		private final String strEps = getString(R.string.messages_episodes);
		private final String strNewEp = getString(R.string.messages_new_episode);
		private final String strNewEps = getString(R.string.messages_new_episodes);
		private final String strNextEp = getString(R.string.messages_next_episode);
		private final String strOf = getString(R.string.messages_of);
		private final String strOn = getString(R.string.messages_on);
		private final String strSeasonWatched = getString(R.string.messages_season_completely_watched);
		private final String strToBeAired = getString(R.string.messages_to_be_aired);
		private final String strToBeAiredPl = getString(R.string.messages_to_be_aired_pl);

		public SeriesSeasonsAdapter(Context context, int textViewResourceId, List<Season> seasons) {
			super(context, textViewResourceId, seasons);
			this.items = seasons;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = vi.inflate(R.layout.row_serie_seasons, parent, false);
				holder = new ViewHolder();
				holder.season = (TextView) convertView.findViewById(R.id.serieseason);
				holder.unwatched = (TextView) convertView.findViewById(R.id.unwatched);
				holder.nextEpisode = (TextView) convertView.findViewById(R.id.nextepisode);
				holder.context = (ImageView) convertView.findViewById(R.id.seriecontext);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
				holder.unwatched.setText("");
				holder.nextEpisode.setText("");
			}
			Season s = items.get(position);
			int nunwatched = s.getUnwatched();
			int nunwatchedAired = s.getUnwatchedAired();
			if (holder.season != null) {
				holder.season.setText(s.getSeason());
			}
			if (holder.unwatched != null) {
				String unwatchedText = db.getSeasonEpisodeCount(serieId, s.getSNumber())
						+" "+ strEps;
				if (nunwatched > 0) {
					String unwatched = "";
					unwatched = nunwatched +" "+ (nunwatched > 1 ? strNewEps : strNewEp) +" ";
					if (nunwatchedAired > 0)
						unwatched = (nunwatchedAired == nunwatched ? "" : nunwatchedAired
							+" "+ strOf +" ") + unwatched + strEpAired;
					else
						unwatched += (nunwatched > 1 ? strToBeAiredPl : strToBeAired);
					unwatchedText += " | "+ unwatched;
				}
				holder.unwatched.setText(unwatchedText);
			}
			if (holder.nextEpisode != null) {
				if (nunwatched == 0) {
					holder.nextEpisode.setText(strSeasonWatched);
					holder.nextEpisode.setEnabled(false);
				} else if (nunwatched > 0) {
					holder.nextEpisode.setText((s.getNextEpisode() == null ? "" : s.getNextEpisode()
						.replace("[ne]", strNextEp)
						.replace("[on]", strOn)));
					holder.nextEpisode.setEnabled(s.getNextAir() != null && s.getNextAir().compareTo(Calendar.getInstance().getTime()) <= 0);
				}
			}
			if (holder.context != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					holder.context.setImageResource(R.drawable.context_material);
				holder.context.setVisibility(View.VISIBLE);
			}
			return convertView;
		}
	}
	static class ViewHolder
	{
		TextView season;
		TextView unwatched;
		TextView nextEpisode;
		ImageView context;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.left_enter, R.anim.left_exit);
	}

	/***************************************************************/
	///////////// Android6-filePermission-Support
	/***************************************************************/

	/**
	 * Implementation of interface {@link ActivityCompat.OnRequestPermissionsResultCallback}.
	 *
	 * Callback for the result from requesting permissions. This method
	 * is invoked for every call on {@link ActivityCompat#requestPermissions(android.app.Activity,
	 * String[], int)}.
	 * <p>
	 * <strong>Note:</strong> It is possible that the permissions request interaction
	 * with the user is interrupted. In this case you will receive empty permissions
	 * and results arrays which should be treated as a cancellation.
	 * </p>
	 *
	 * @param requestCode The request code passed in {@link ActivityCompat#requestPermissions(
	 * android.app.Activity, String[], int)}
	 * @param permissions The requested permissions. Never null.
	 * @param grantResults The grant results for the corresponding permissions
	 *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
	 *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
	 *
	 * @see ActivityCompat#requestPermissions(android.app.Activity, String[], int)
	 */
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		if (PermissionHelper.USE_NEW_PERMISSIONS_FILE_API) {
			if (requestCode == DroidShows.EXPORT_CSV_CODE && PermissionHelper.receivedPermissionsOrFinish(this, grantResults)) {
				// permission granted
				DroidShowCsvHelper.openDocumentFilePickForCsvExport(
						this, DroidShows.EXPORT_CSV_CODE,
						DroidShows.getLastUsedBackupUri(this), lastCsvExportSeriesId, lastCsvExportSeasonNumber);
			}
		}

	}

	/**
	 * Callback received from {@link  Activity#startActivityForResult(Intent, int)} when using the
	 * Directory picker.
	 * @param requestCode The integer request code originally supplied to
	 *                    startActivityForResult(), allowing you to identify who this
	 *                    result came from.
	 * @param resultCode The integer result code returned by the child activity
	 *                   through its setResult().
	 * @param data An Intent, which can return result data to the caller
	 *               (various data can be attached to Intent "extras").
	 *
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (PermissionHelper.USE_NEW_PERMISSIONS_FILE_API && resultCode == Activity.RESULT_OK && data != null) {
			if (requestCode == DroidShows.EXPORT_CSV_CODE) {
				DroidShowCsvHelper.onOpenDocumentFilePickForCsvExportResult(this, data.getData(), lastCsvExportSeriesId, lastCsvExportSeasonNumber);
			}
		}
	}

	/***************************************************************/
	///////////// CSV-Export support
	/***************************************************************/

	private static String lastCsvExportSeriesId = null;
	private static Integer lastCsvExportSeasonNumber = null;
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void exportAsCsv(String serieId, Integer seasonNumber) {
		lastCsvExportSeriesId = serieId;
		lastCsvExportSeasonNumber = seasonNumber;
		DroidShowCsvHelper.openDocumentFilePickForCsvExport(this, DroidShows.EXPORT_CSV_CODE,
				DroidShows.getLastUsedBackupUri(this), lastCsvExportSeriesId, lastCsvExportSeasonNumber);
	}


}
