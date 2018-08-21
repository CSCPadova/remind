package unipd.dei.magnetophone.activity;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import unipd.dei.magnetophone.R;
import unipd.dei.magnetophone.database.DatabaseHelper;
import unipd.dei.magnetophone.utility.SearchCursorAdapter;

public class SearchActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);


        if(getActionBar()!=null) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Cursor cursor = doMySearch(query);

            ListAdapter adapter = new SearchCursorAdapter(this, cursor, 0);
            // Bind to our new adapter.
            setListAdapter(adapter);
        }


    }//fine onCreate


    /**
     * metodo chiamato quando un oggetto della lista viene selezionato, si preoccupa di
     * rimandarci alla SongListActivity con l'oggetto selezionato che apparirà nel dettail.
     * paramteri:
     * listView: la list view dove è successo
     * view: la view nella lista cliccata
     * position: la posizione nella lista dove è successo
     * id: il row id dell'oggetto che è stato cliccato
     */
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        //prendo la textView dove è presente l'id della song
        TextView songIdTextView = (TextView) view.findViewById(android.R.id.text2);
        String songIdAsString = songIdTextView.getText().toString();
        int songId = Integer.parseInt(songIdAsString);

        SharedPreferences sharedPref = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
        Editor editor = sharedPref.edit();
        editor.putInt("song_id", songId);
        editor.commit();

        Intent toList = new Intent(this, SongListActivity.class);
        toList.putExtra("modify", 1);
        toList.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    //evito che premendo BACK dalla lista dei brani si ritorni all'importazione
        startActivity(toList);

    }


    /**
     * interroga il Database e restituisce un cursore con la risposta
     *
     * @param query
     * @return
     */
    private Cursor doMySearch(String query) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = DatabaseHelper.SIGNATURE + " LIKE \"%" + query + "%\"";
        Cursor cursor = db.query(DatabaseHelper.SONG, null, selection, null, null, null, null, null);

        return cursor;
    }

    //chiamato quando qualche icona della action bar viene selezionata
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //http://developer.android.com/design/patterns/navigation.html#up-vs-back
            NavUtils.navigateUpTo(this, new Intent(this, SongListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
