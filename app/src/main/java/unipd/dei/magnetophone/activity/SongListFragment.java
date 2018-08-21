package unipd.dei.magnetophone.activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.util.LinkedList;

import unipd.dei.magnetophone.R;
import unipd.dei.magnetophone.SongDetailFragment;
import unipd.dei.magnetophone.database.DatabaseHelper;
import unipd.dei.magnetophone.database.DatabaseManager;
import unipd.dei.magnetophone.utility.CustomAdapterListFragment;
import unipd.dei.magnetophone.utility.Song;
import unipd.dei.magnetophone.xml.XmlImport;

/**
 * A list fragment representing a list of Songs. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link SongDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class SongListFragment extends ListFragment {

    /**
     * Una stringa che gunge da chiave per conoscere la posizione dell'oggetto selezionato nell'ultima volta
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    protected static LinkedList<Song> finalList;
    protected static CustomAdapterListFragment adapter;
    private static long lastModified;
    /**
     * Un'implementazione "dummy", ossia vuota, che non fa niente
     * del callback che il fragment richiede, si usa in caso di detach
     * dall'activity
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(ListView v, View view, int id) {
        }
    };
    private XmlImport imp = new XmlImport();
    /**
     * l'attuale callbach utilizzato dal fragment
     */
    private Callbacks mCallbacks = sDummyCallbacks;
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SongListFragment() {
    }

    //metodo di onCreate del fragment, si preoccupa di settare l'adapetr
    //viene chiamato per creare il fragment, invocato subito dopo onAttach e prima di on createView
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manageListRefresh();
        adapter = new CustomAdapterListFragment(getActivity(), R.layout.element_of_the_fragment_list_layout, finalList);

        setListAdapter(adapter);

    }

    /**
     * metodo che si preoccupa di controllare se ci sono le condizioni scelte per avviare il refresh
     * dei dati dalla cartella dei file xml.
     * Se così è, esegue il refresh del database e della lista dei brani
     */
    private void manageListRefresh() {
        SharedPreferences sharedPref = getActivity().getSharedPreferences("last_modified", Context.MODE_PRIVATE);
        lastModified = sharedPref.getLong("LastModified", 0);

        //creiamo o controlliamo che esista già la cartella contenente i file XML
        File xmlDirectory = new File(XmlImport.getCurrentDirectory(getActivity()));
        xmlDirectory.mkdir();
        //prendo il dato dell'ultima modifica della directory (i.e. è stata aggiunta una cartella)
        long currentModified = xmlDirectory.lastModified();

        int time = checkTheFirstTime();//guardo se questa è la prima volta che l'applicazione parte

        //TODO DEBUG TIME=0 per fare tests
        //time = 0;

        //se c'è stata una qualche modifica alla cartella oppure se questa è la prima volta che partiamo
        if (currentModified > lastModified || time == 0) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong("LastModified", currentModified);//aggiorniamo la nostra memoria sull'ultima volta che
            //ci siamo modificati
            editor.commit();

            //Carichiamo tutte le canzoni del database con le relative informazioni
            loadListFromDatabaseAndXml();
        } else
            loadListFromDatabase();
    }

    private int checkTheFirstTime() {
        SharedPreferences pref = getActivity().getSharedPreferences("first_time", Context.MODE_PRIVATE);
        int toReturn = pref.getInt("FirstTime", 0);
        //SharedPreferences.Editor editor = pref.edit();
        //editor.putInt("FirstTime", toReturn + 1);
        //editor.commit();

        return toReturn;
    }

    /**
     * Carica nel database le canzoni presenti nei file XML nel caso di modifiche, dopodichè
     * riempie la finalList (variabile globale) con gli oggetti Song presi
     * dal database aggiornato
     */
    private void loadListFromDatabaseAndXml() {
        // creo/apro il database
        DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
        //invoco il metodo che si occupa di importre i file XML nuovi o modificati
        imp.importazioneXML(getActivity(), dbHelper, false);

        loadListFromDatabase();
    }

    /**
     * metodo che carica nella lista tutti i brani nel database
     */
    private void loadListFromDatabase() {
        DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(DatabaseHelper.SONG, null, null, null, null, null, null, null);
        int cursorLength = cursor.getCount();

        finalList = new LinkedList<Song>();
        for (int j = 0; j < cursorLength; j++) {
            cursor.moveToPosition(j);
            int songid = cursor.getInt(0);
            Song new_song = DatabaseManager.getSongFromDatabase(songid, getActivity());
            //carico nella lista la song ottenuta dal database con id songid
            finalList.add(new_song);
        }
        db.close();
    }

    //chiamato subito dopo onCreateView e prima che ogni stato precedentemente salvato venga restaurato
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // se avevamo uno stato salvato e se avevamo una posizione salvato, settiamo quella posizione
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }


//		SharedPreferences songPrefSelected = getActivity().getSharedPreferences("selected", Context.MODE_PRIVATE);
//		int viewId = songPrefSelected.getInt("view", -1);
//		int position = songPrefSelected.getInt("position", -1);
//		int listId = songPrefSelected.getInt("listview", -1);
//
//		if(viewId!=-1 && position !=-1 && listId!=-1)
//		{
//			ListView listView = getListView();
//
//			View v = null;
//			if(listView!=null)
//				v = listView.getChildAt(position);
//
//			if(listView!=null && v !=null)
//			{
//				for(int i = 0; i< listView.getChildCount(); i++)
//					listView.getChildAt(i).setSelected(false);
//
//				StateListDrawable states = new StateListDrawable();
//				states.addState(new int[] {android.R.attr.state_selected},
//						getResources().getDrawable(R.drawable.list_pressed));
//				states.addState(new int[] { },
//						getResources().getDrawable(R.drawable.list_altered));
//				v.setBackground(states);
//				v.setSelected(true);
//			}
//		}
    }

    //controlla che l'activity che tiene questo fragment implementi il callback
    //in caso affermativo si prendo il riferimento a tale activity, altrimenti lancia eccezione
    //questo metodo viene chiamato quando un fragment viene attaccato per la prima volta ad una activity (modularità)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    //chiamato quando un fragment viene staccato dalla sua activity
    @Override
    public void onDetach() {
        super.onDetach();

        // Resetta il callback da quello che aveva l'activity che ci conteneva a quello
        //di default nostro che non fa nulla
        mCallbacks = sDummyCallbacks;
    }

    /**
     * metodo chiamato quando un oggetto della lista viene selezionato.
     * paramteri:
     * listView: la list view dove è successo
     * view: la view nella lista cliccata
     * position: la posizione nella lista dove è successo
     * id: il row id dell'oggetto che è stato cliccato
     */
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        //avviso la mia activity padre di quale elemento io abbia ricevuto
        //la posizione
        mCallbacks.onItemSelected(listView, view, position);

    }

    /**
     * salva la posizione dell'elemento selezionato nella lista
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) //se abbiamo una posizione valida
        {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Prende
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(
                activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
                        : ListView.CHOICE_MODE_NONE);
    }

    /**
     * metodo privato, chiamato quando si ritorna, evidenzia l'ultimo elemento che abbiamo selezionato
     *
     * @param position
     */
    private void setActivatedPosition(int position) {//se la posizione è invalida, togliamo l'evidenziazione
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else //altrimenti la mettiamo
        {
            getListView().setItemChecked(position, true);
        }

        //salviamo l'ultima posizione, sia che sia valida sia che non lo sia
        mActivatedPosition = position;
    }

    public LinkedList<Song> getTheLinkedList() {
        return finalList;
    }

    /**
     * Una funzione di callback che tutte le activity che contengono questo fragment devono implementare
     * Permette all'activity di sapere della selezione di un oggetto della lista ed agire di conseguenza
     */
    public interface Callbacks {
        public void onItemSelected(ListView v, View view, int id);
    }
}
