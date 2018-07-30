/**classe che si occupa di gestire l'importazione di file XML nella cartella Magnetophone 
 * presente nella memoria esterna. Verifica se sono stati modificati e li importa nel database se necessario
 *E' la classe che fisicamente "mette le mani" nella cartella Magnetofone, punto di partenza delle altre importazioni
 * 
 * */

package unipd.dei.magnetophone;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class XmlImport {
	private static Context context;

	//costruttore
	public XmlImport(Context ctx) {
		context = ctx;
	}

	/**
	 * metodo che restituisce il path della cartella dove si vanno a leggere i fle XML
	 * e dove si vanno a prendere le canzoni
	 */
	public static String getCurrentDirectory() {
		SharedPreferences shared = context.getSharedPreferences("current_directory", Context.MODE_PRIVATE);
		String name = shared.getString("name", "Magnetophone");

		String path = Environment.getExternalStorageDirectory().getAbsolutePath();//ritorna il file directory
		return path + "/" + name + "/";//restituiamo il nome della cartella dove lavoriamo
	}

	public static String getCurrentDirectory(Context cont) {
		SharedPreferences shared = cont.getSharedPreferences("current_directory", Context.MODE_PRIVATE);
		String name = shared.getString("name", "/Magnetophone/");

		String path = Environment.getExternalStorageDirectory().getAbsolutePath();//ritorna il file directory
		return path + "/" + name + "/";//restituiamo il nome della cartella dove lavoriamo

	}

	/**
	 * metodo di importazione XML, chiamato quando ci si rende conto che qualcosa
	 * è stato cambiato nella cartella dove sono posti i file
	 *
	 * @param cont
	 * @param dbHelper
	 */
	public void importazioneXML(Context cont, MagnetophoneOpenHelper dbHelper) {
		//######### inizializzazione di alcuni oggetti che servono nel corso del metodo ################	
		DatabaseManager dbManager = new DatabaseManager(cont);
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		String nomeFile;//stringa per tenere temporaneamente un nome di file xml

		LinkedList<XMLfile> xmlRow = new LinkedList<XMLfile>();//lista per mettere i nomi dei file xml
		XMLfile xmlFile = null;//oggetto XMLfile che serve per gestire il passaggio dati

		long lastModifiedDb;//ultima volta che è stato modificato a quanto risulta dal database
		long lastModifiedStorage;//ultima volta che è stato modificato il file nella external storage

		Cursor cursor;
		Cursor query;

		//###############################################################################

		//ci prendiamo il path della directory esterna, con questo avremo tutti i file audio ed xml ad essi associati
		File magnetophoneDirectory = new File(getCurrentDirectory(cont));
		//questa è la cartella Magnetophone, e dentro di essa ci sono le cartelle song
		File[] songsDirectories = magnetophoneDirectory.listFiles();


		try {
			//interroghiamo il database per conoscere i nomi dei file xml già presenti per controllare eventuali
			//conflitti
			cursor = db.query(MagnetophoneOpenHelper.XML, null, null, null, MagnetophoneOpenHelper.NOMEFILE, null, null, null);//prendo tutti gli XML nel database

			//prendo la lista di tutti i file XML
			int rows = cursor.getCount();//numero di tuple restituite dal database			
			for (int i = 0; i < rows; i++) {
				xmlFile = new XMLfile();
				cursor.moveToPosition(i);

				xmlFile.setId(cursor.getInt(0));
				xmlFile.setNomeFile(cursor.getString(1));
				xmlFile.setData(cursor.getLong(2));
				xmlRow.add(xmlFile);
			}
			//ora in xmlRaw ho tutti i nomi dei file xml che erano precedentemente nel database

			//poi scrivo un filtro che permetta di prendere solo i file xml
			FileFilter ff = new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					if (pathname.isDirectory())
						return false;
					return pathname.getName().endsWith("xml");
				}
			};

			if (songsDirectories != null) {
				//ora, cartella per cartella, si guarda dentro che cosa contiene
				for (int j = 0; j < songsDirectories.length; j++) {
					File currentSongDirectory = songsDirectories[j];
					//currentSongDirectory è la cartella di song che ci interessa, da dentro di essa vogliamo prendere solo i file xml
					File[] xmlFileList = currentSongDirectory.listFiles(ff);//prendo tutti i file xml nella attuale cartella xml, non mi interessao
					//le cartelle di songs, video e photos (in teoria dovrebbe essere uno solo, ma non si sa mai)

					//a questo punto, si prendono uno ad uno i file xml nella external storage
					//e controlla se sono dentro al database
					if (xmlFileList != null)//se è != null, ossia se c'è almeno 1 file xml
					{
						int fileListLength = xmlFileList.length;

						//per ogni file xml nella cartella (dovrebbe essercene 1 solo)
						for (int i = 0; i < fileListLength; i++) {
							nomeFile = xmlFileList[i].getName();//prendo il nome dell'i-esimo file xml
							//controllo se è già dentro il db
							String[] select = {MagnetophoneOpenHelper.XMLID, MagnetophoneOpenHelper.NOMEFILE, MagnetophoneOpenHelper.DATAMODIFICA};
							String where = MagnetophoneOpenHelper.NOMEFILE + "=\"" + nomeFile + "\"";

							query = db.query(MagnetophoneOpenHelper.XML, select, where, null, null, null, null, null);
							int numberOfCollision = query.getCount();//se c'è un risultato dal database, vuol dire che c'è gia un xml con quel nome dentro
							if (numberOfCollision != 0)//se c'è già un file con lo stesso nome nel database
							{
								//controllo data ultima modifica
								//prendo data ultima modifica del file nella external storage
								lastModifiedStorage = xmlFileList[i].lastModified();

								//per il momento considero che ci possa essere 1 solo elemento con lo stesso nome nel database
								query.moveToPosition(0);
								lastModifiedDb = query.getLong(2);

								if (lastModifiedStorage > lastModifiedDb)//se nel database è più vecchio, elimino e sostituisco
								{
									dbManager.removeXmlFromDatabase(nomeFile);
									//si inserisce il file xml aggiornato nel database
									dbManager.insertXMLInDatabase(xmlFileList[i]);
								}

								//altrimenti non si fa nulla, il file non è stato modificato e non necessita di aggiornament
							}//fine if se è stata trovata una collisione
							else//non c'è stata collisione, si deve inserire ex novo
							{
								//si inserisce il file xml nel database, insieme alla canzone/i che contiene
								dbManager.insertXMLInDatabase(xmlFileList[i]);
							}
							cursor.close();
							query.close();
						}//fine for per l'analisi dei file xml
					}

				}
			} else {
				//se non ci sono sottocartelle

				//fa in modo che la prossima volta riscansioni la cartella
				SharedPreferences sharedPref = cont.getSharedPreferences("last_modified", Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putLong("LastModified", 0);

				//e mostra un avviso
				String text = cont.getString(R.string.empty_folder1) + " " + getCurrentDirectory(cont) + " " + cont.getString(R.string.empty_folder2);
				Toast toast = Toast.makeText(cont, text, Toast.LENGTH_SHORT);
				toast.show();
			}

			db.close();
		} catch (Exception e) {
			e.printStackTrace();
			db.close();
		}
	}
}
