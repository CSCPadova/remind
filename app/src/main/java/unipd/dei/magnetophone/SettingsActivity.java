package unipd.dei.magnetophone;

////import it.unipd.dei.esp1314.magnetophone.R;


import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;

/**
 * Activity che gestisce le impostazioni generali del magnetofono.
 * E' possbile permettere o meno l'accesso alle opzioni avanzate dell'applicazione le quali consistono in:
 * •abilitazione al menu' della status bar che consiste in importazione dei brani, modifica dei brani, cancellazione dei brani,
 * refresh.
 * •cambiare password
 * •cambiare directory di riferiemento per l'applicazione
 * •esportare il database in formato xml nella directory di riferimento
 */

public class SettingsActivity extends AppCompatActivity {
    private static CheckBoxPreference prefAdvancedOptions;
    // preference in un activity e' deprecato
    //	private Spinner language_spinner;
    private static PreferenceCategory preferenceCategory;
    private static CheckBoxPreference prefMaintenance;
    private static Preference prefPath;
    private static Preference prefXml;
    private static Preference prefPassword;
    private static XmlImport imp;
    private SettingsFragment frag; // Uso un fragment in quanto l'uso delle

    /**
     * @return: lista con i path delle cartelle nella memoria interna
     */
    public static LinkedList<String> getTheFilesList(LinkedList<String> nameList, Context appContext) {
        LinkedList<String> fileList = new LinkedList<String>();

        //prendo la cartella di di memoria esterna
        File external = Environment.getExternalStorageDirectory();
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            //dentro questa cartella, prendo le sottocartelle che rappresentano le cartelle a disposizione come path
            File[] directories = external.listFiles();

            if (directories != null) {
                //inserisco i path delle directory nella lista
                for (int g = 0; g < directories.length; g++) {
                    directories[g].mkdir();
                    if (directories[g].isDirectory()) {
                        //mi prendo il path assoluto e il nome di ogni cartella
                        String pa = directories[g].getAbsolutePath();
                        String name = directories[g].getName();
                        //prendo il path delle foto
                        fileList.add(pa);
                        //setto anche il suo nome
                        nameList.add(name);
                    }
                }
            } else {
                String text = appContext.getString(R.string.external_permission_error);
                Toast toast = Toast.makeText(appContext, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            //impossibile leggere la memoria esterna
            String text = appContext.getString(R.string.external_error);
            Toast toast = Toast.makeText(appContext, text, Toast.LENGTH_SHORT);
            toast.show();
        }
        return fileList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //poniamo la possibilità sulla action bar del tasto indietro
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imp = new XmlImport();
        //ci creiamo il nostro fragment
        frag = new SettingsFragment(); //classe creata più giù


        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, frag).commit();

    }

    //######################## PREFERENCE FRAGMENT #######################################

    /**
     * ActionBar per tornare indietro usando HOME
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Questo fragment mostra le opzioni definite nel file xml prefs
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        //@Override
        //public void onCreate(Bundle savedInstanceState){

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            //super.onCreate(bundle);
            addPreferencesFromResource(R.xml.prefs);

            //PreferenceManager.setDefaultValues(getActivity(),
            //		R.xml.prefs, false);

            //prendo i riferimenti per le preference

            //per la lingua
//			final ListPreference prefLanguage = (ListPreference)findPreference("listLanguages");


            //mostro un titolo più carino per l'utente, che gli mostra cosa è scelto attualmente
//			prefLanguage.setTitle(getString(R.string.choose_a_language) + ": " + prefLanguage.getValue());

            final PreferenceScreen preferenceScreen = getPreferenceScreen();
            preferenceCategory = (PreferenceCategory) preferenceScreen
                    .findPreference("secondaCategoriaImpostazioni");


            prefAdvancedOptions = (CheckBoxPreference) preferenceCategory
                    .findPreference("OpzioniAvanzateImpostazioni");
            prefMaintenance = (CheckBoxPreference) preferenceCategory
                    .findPreference("Maintenance");

//			final CheckBoxPreference prefImport = (CheckBoxPreference) preferenceCategory
//					.findPreference("ImportareImpostazioni");
//			final CheckBoxPreference prefDelete = (CheckBoxPreference) preferenceCategory
//					.findPreference("EliminareImpostazioni");
//			final CheckBoxPreference prefModify = (CheckBoxPreference) preferenceCategory
//					.findPreference("ModificareImpostazioni");


            prefPath = preferenceCategory
                    .findPreference("ModificaPathImpostazioni");
            prefXml = preferenceCategory
                    .findPreference("ExportDatabaseXml");
            prefPassword = preferenceCategory
                    .findPreference("ChangePassword");

            // Mostro la path dove si salvano gli xml
            prefPath.setTitle(getString(R.string.text1_title_activity_settings)
                    + ":" + XmlImport.getCurrentDirectory(getActivity()));

            //Mostro la path dove si esportano gli xml
            prefXml.setSummary(XmlImport.getCurrentDirectory(getActivity()));

            //************ iniziano i listener *******************


//			prefLanguage.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
//			{
//				public boolean onPreferenceChange(Preference preference, Object newValue)
//				{
//					preference.setTitle(getString(R.string.choose_a_language) + ": " + (String)newValue);
//					prefLanguage.setValue((String)newValue);
//
//					return false;
//				}
//			});

            prefPassword.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) { // Lancio il Popup al click
                    InsertPasswordFragment dialog = new InsertPasswordFragment();//crea il nuovo fragment, definito a fine file

                    dialog.show(getFragmentManager(), null);
                    return false;
                }
            });

            // Mostro un popup per visualizzare l'attuale path dove si
            //prendono le cartelle e eventualmente per cambiarlo
            prefPath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) { // Lancio il Popup al click
                    //PathAlertFragment prefPath = (new SettingsActivity()).new PathAlertFragment();//crea il nuovo fragment, definito a fine file
                    PathAlertFragment prefPath = new SettingsActivity.PathAlertFragment();//crea il nuovo fragment, definito a fine file
                    prefPath.show(getFragmentManager(), null);
                    return false;
                }
            });

            //Mostro un popup per permettere l'esportazione
            prefXml.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) { // Lancio il Popup al click
                    PathXmlFragment prefPath = new PathXmlFragment();//crea il nuovo fragment, definito a fine file
                    prefPath.show(getFragmentManager(), null);
                    return false;
                }
            });


            // Opzioni avanzate
            prefAdvancedOptions
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
                            prefAdvancedOptions.setChecked((boolean) newValue);

                            if (prefAdvancedOptions.isChecked()) // attivo o disattivo le preference delle opzioni avanzate
                            {
                                PasswordFragment prefPath = new PasswordFragment();//crea il nuovo fragment, definito a fine file
                                prefPath.show(getFragmentManager(), null);
                            } else//se stiamo disattivando non richiediamo alcuna password
                            {
                                preferenceCategory.removePreference(prefMaintenance);
                                preferenceCategory.removePreference(prefPath);
                                preferenceCategory.removePreference(prefXml);
                                preferenceCategory.removePreference(prefPassword);
                            }
                            return false;
                        }
                    });

            // Rimuovo le opzioni a seconda se opzioni avanzate è gia'
            // abilitato o meno
            if (!(prefAdvancedOptions.isChecked())) {
                preferenceCategory.removePreference(prefMaintenance);
//				preferenceCategory.removePreference(prefImport);
//				preferenceCategory.removePreference(prefDelete);
                preferenceCategory.removePreference(prefPath);
//				preferenceCategory.removePreference(prefModify);
                preferenceCategory.removePreference(prefXml);
                preferenceCategory.removePreference(prefPassword);
            }
        }

    }// fine fragment

    /**
     * Fragment che visualizza un popup menu per mostrare la path della cartella Magnetophone
     * completa, che altrimenti non si vedrebbe
     */
    public static class PathAlertFragment extends DialogFragment {
        @Override
        public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.text1_subtitle_activity_settings));
            builder.setMessage(XmlImport.getCurrentDirectory(getActivity()));


            builder.setPositiveButton(
                    getString(R.string.lets_change_the_path), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // qui si farà cambiare path

                            PathChangerFragment prefPath = new PathChangerFragment();//crea il nuovo fragment, definito a fine file
                            prefPath.show(getFragmentManager(), null);

                        }
                    });

            builder.setNegativeButton(
                    getString(R.string.path_ok_activity_settings), null);
            return builder.create();
        }
    }

    /**
     * Fragment che visualizza una lista di cartelle interne all'external storage e permette all'utente di selezionarne
     * una che diventerà il nuovo path di lettura dei file xml. Esegue anche un refresh dei dati
     */
    public static class PathChangerFragment extends AppCompatDialogFragment {
        @Override
        public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.text1_subtitle_activity_settings));
//			builder.setMessage(XmlImport.getCurrentDirectory());

            //mi prendo i path e i nomi delle cartelle
            final LinkedList<String> directoriesName = new LinkedList<String>();//lista che conterrà solo i nomi delle cartelle


            //final LinkedList<String> pathList = getTheFilesList(directoriesName);//lista che conterrà i path
            final LinkedList<String> pathList = getTheFilesList(directoriesName, this.getActivity());//lista che conterrà i path

            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.select_dialog_singlechoice, directoriesName);

            builder.setAdapter(adapter,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position
                            // of the selected item
                            String choicePath = pathList.get(which);
                            String choiceName = directoriesName.get(which);
                            //salvo nelle sharedPreferences il path assoluto della nuova cartella
                            SharedPreferences shared = getActivity().getSharedPreferences("current_directory", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = shared.edit();
                            editor.putString("name", choiceName);
                            editor.putString("path", choicePath);
                            editor.commit();

                            //cambio quanto si vede nella preference
                            prefPath.setTitle(getString(R.string.text1_title_activity_settings)
                                    + ":" + XmlImport.getCurrentDirectory(getActivity()));
                            prefXml.setSummary(XmlImport.getCurrentDirectory(getActivity()));

                            //do conferma dell'avvenuto cambiamento
                            String text = getString(R.string.directory_changed);
                            Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    });

            builder.setNegativeButton(
                    getString(R.string.cancel), null);
            return builder.create();
        }
    }

    /**
     * Fragment che mostra l'attuale path di esportazione e permette di esportare il database in formato xml
     * se si preme il tasto OK
     */
    public static class PathXmlFragment extends AppCompatDialogFragment {
        @Override
        public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.where_we_export));
            builder.setMessage(XmlImport.getCurrentDirectory(getActivity()));
            // Si preme per procedere all'espertazione
            builder.setPositiveButton(
                    getString(R.string.lets_begin_the_import), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // qui si farà partire l'esportazione
                            DatabaseConverter.exportTheDatabase(getActivity());
                        }
                    });
            builder.setNegativeButton(R.string.revert_from_exporting, null);
            return builder.create();
        }
    }

    /**
     * Fragment che richiede all'utente la password per mostrargli poi le opzioni avanzate
     */
    public static class PasswordFragment extends DialogFragment {
        @Override
        public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View v = inflater.inflate(R.layout.password_layout, null);
            builder.setView(v);

            builder.setTitle(getString(R.string.password));
            builder.setMessage(R.string.password_needed);
            // Si preme per procedere all'espertazione
            builder.setPositiveButton(
                    "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            //controlliamo l'esattezza della password
                            EditText pass = (EditText) v.findViewById(R.id.password);
                            String pa = pass.getText().toString();

                            SharedPreferences sharedPass = getActivity().getSharedPreferences("password", Context.MODE_PRIVATE);
                            String currentPass = sharedPass.getString("password", "magnetophone");

                            if (pa.equals(currentPass)) {
                                //password giusta, possiamo mostrare le opzioni avanzate
                                preferenceCategory.addPreference(prefMaintenance);
                                preferenceCategory.addPreference(prefPath);
                                preferenceCategory.addPreference(prefXml);
                                preferenceCategory.addPreference(prefPassword);
                            } else {
                                //password errata, riprovare
                                String text = getString(R.string.wrong_password);
                                Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                                toast.show();

                                //disattiviamo il checkbox altrimenti resterebbe selezionato
                                prefAdvancedOptions.setChecked(false);

                            }
                        }
                    });
            builder.setNegativeButton(R.string.revert_from_exporting, new DialogInterface.OnClickListener() {
                //l'utente ha selezionato il checkbox, ma non aveva la password. Bisogna pertanto togliere
                //il check altrimenti alla prossima apertura si vedrà le opzioni avanzate
                public void onClick(DialogInterface dialog, int id) {
                    prefAdvancedOptions.setChecked(false);
                }

            });
            return builder.create();
        }
    }

    /**
     * dialog intermedio, chiede all'utente la password per poterla poi cambiare
     */
    public static class InsertPasswordFragment extends AppCompatDialogFragment {
        @Override
        public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View v = inflater.inflate(R.layout.password_layout, null);
            builder.setView(v);

            builder.setTitle(getString(R.string.change_password_title));
            builder.setMessage(R.string.insert_password);

            // Si preme OK per dare conferma dell'avvenuta importazione
            builder.setPositiveButton(
                    "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // qui si controllerà l'esattezza della password

                            EditText pass = (EditText) v.findViewById(R.id.password);
                            String pa = pass.getText().toString();

                            SharedPreferences sharedPass = getActivity().getSharedPreferences("password", Context.MODE_PRIVATE);
                            String currentPass = sharedPass.getString("password", "magnetophone");
                            //qui si controlla che la password sia attualmente valida

                            if (pa.equals(currentPass)) {
                                //se la password è valida
                                ChangePasswordFragment prefChange = new ChangePasswordFragment();//crea il nuovo fragment, definito a fine file
                                prefChange.show(getFragmentManager(), null);
                            } else {
                                //password errata, riprovare
                                String text = getString(R.string.wrong_password);
                                Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                                toast.show();
                            }

                        }
                    });
            builder.setNegativeButton(R.string.cancel, null);
            return builder.create();
        }
    }

    /**
     * fragment che chiede all'utente di inserire la nuova password
     *
     * @author dennisdosso
     */
    public static class ChangePasswordFragment extends AppCompatDialogFragment {
        @Override
        public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View v = inflater.inflate(R.layout.password_layout, null);
            builder.setView(v);

            builder.setTitle(getString(R.string.change_password_title));
            builder.setMessage(R.string.insert_new_password);

            // Si preme OK per dare conferma dell'avvenuta importazione
            builder.setPositiveButton(
                    "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // qui si controllerà l'esattezza della password

                            EditText pass = (EditText) v.findViewById(R.id.password);
                            String pa = pass.getText().toString();

                            //qui si salva la password nelle shared preferences
                            SharedPreferences sharedPass = getActivity().getSharedPreferences("password", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPass.edit();
                            editor.putString("password", pa);
                            editor.commit();

                            int duration = Toast.LENGTH_SHORT;
                            String text = getString(R.string.password_success);
                            Toast toast = Toast.makeText(getActivity(), text, duration);
                            toast.show();
                        }
                    });

            builder.setNegativeButton(R.string.cancel, null);
            return builder.create();
        }
    }
}
