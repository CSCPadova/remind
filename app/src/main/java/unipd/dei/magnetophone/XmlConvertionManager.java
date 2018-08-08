package unipd.dei.magnetophone;

/**
 * Classe che fa il lavoro sporco di prendere il dcumento e restituire
 * una lista di elementi, ognuno contenete un oggetto song
 * con gli attributi letti da XML.
 * <p>
 * Si sono fatte alcune scelte nel gestire eventuali errori o mancanze
 * dell'utente nello scrivere gli xml:
 * •se sbalia il numero di tracce, ad es 3 o più di 4, la canzone non è importata
 * •se il path appartiene ad un file non presente in memoria, la canzone non viene importata
 * •il path delle foto deve appartenere ad una cartella di foto. Se la cartella non esiste o non è una
 * cartella, l'informazione relativa alle photos non viene importata
 * •il path del video deve appartenere ad un video realmente esistente. Se il video non esiste
 * o non è un file, l'informazione relativa al video non viene importata
 * <p>
 * <p>
 * Nel caso una canzone sia definita con più tracce di 4, viene invalidata
 */

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.LinkedList;

public class XmlConvertionManager {
    private static final String TITLE = "title";
    private static final String AUTHOR = "author";
    private static final String YEAR = "year";
    private static final String SPEED = "speed";
    private static final String EQUALIZATION = "equalization";

    private static final String SIGNATURE = "signature";
    private static final String PROVENANCE = "provenance";
    private static final String DURATION = "duration";
    private static final String EXTENSION = "extension";
    private static final String BITDEPTH = "bitdepth";
    private static final String SAMPLERATE = "sample rate";
    private static final String TAPEWIDTH = "tape width";
    private static final String DESCRIPTION = "description";

    private static Context context;

    public XmlConvertionManager(Context cont) {
        context = cont;
    }

    /**
     * Adatta un path: se è assoluto lo ritorna com'è, se è relativo ritorna
     * il path assoluto preso nella cartella dell'app.
     */
    private static String adaptPath(String path) {
        File test = new File(path);
        if (test.isAbsolute())
            // Se il path è assoluto lo copio così com'è
            return path;
        else
            // altrimenti lo prendo relativo alla cartella dell'app
            return XmlImport.getCurrentDirectory(context) + path;
    }

    /**
     * esegue un parsing del documento passato come parametro ottenuto da un file
     * XML e ne restituisce una lista con le canzoni correttamente definite all'interno
     * @param doc: documento ottenuto dal file XML che contiene le informazioni sul brano
     * @return lista di canzoni correttamente definite nell'XML
     */
    public static LinkedList<Song> XmlToDataConvertion(Document doc, Context cont) {
        context = cont;

        boolean validNumberOfTracks = false; //dice se il brano ha un numero corretto di tracce
        int tracksCount;//conta le track che ha la canzone

        //lista su cui si testa XmlConvertionManager
        LinkedList<Song> myList = new LinkedList<Song>();
        //comando necessario per normalizzare il documento
        doc.getDocumentElement().normalize();
        //prendiamo dal doc una lista di tutti e soli i nodi che hanno nome song
        NodeList nodes = doc.getElementsByTagName("song");

        int nodelistLength = nodes.getLength();

        //inizio del parsing
        for (int i = 0; i < nodelistLength; i++)//per ogni song nel file XML
        {
            Song song = new Song();

            //per ognuno degli elementi della nodelist, che è una song
            Node songNode = nodes.item(i);
            NamedNodeMap songNodeAttributes = songNode.getAttributes();    // mi prendo tutti gli attributi del nodo
            int number_of_attributes = songNodeAttributes.getLength();    // mi prendo il numero di attributi del singolo nodo

            //ora, song per song, si analizzano gli attributi
            //########## operiamo sugli attributi propri del nodo song ################

            for (int n = 0; n < number_of_attributes; n++)        // per ognuno degli attributi
            {
                Node subnode = songNodeAttributes.item(n);    // prendo l'attributo n-esimo, che è un nodo di tipo attributo=valore
                String name = subnode.getNodeName();    // mi prendo la tipologia dell'attributo
                String text = subnode.getTextContent();    // qui prendo il valore dall'attributo

                //settiamo i valori della canzone elemento per elemento
                if (name.equals(AUTHOR))
                    song.setAuthor(text);
                else if (name.equals(TITLE))
                    song.setTitle(text);
                else if (name.equals(YEAR))
                    song.setYear(text);
                else if (name.equals(EQUALIZATION))
                    song.setEqualization(text);
                else if (name.equals(SPEED))
                    song.setSpeed(Float.parseFloat(text));
                else if (name.equals(SIGNATURE))
                    song.setSignature(text);
                else if (name.equals(PROVENANCE))
                    song.setProvenance(text);
                else if (name.equals(DURATION))
                    song.setDuration(Float.parseFloat(text));
                else if (name.equals(EXTENSION))
                    song.setExtension(text);
                else if (name.equals(BITDEPTH))
                    song.setBitDepth(Integer.parseInt(text));
                else if (name.equals(SAMPLERATE))
                    song.setSampleRate(Integer.parseInt(text));
//				else if(name.equals(NUMBEROFTRACKS))
//					song.setNumberOfTracks(Integer.parseInt(text));
                else if (name.equals(TAPEWIDTH))
                    song.setTapeWidth(text);
                else if (name.equals(DESCRIPTION))
                    song.setDescription(text);
            }

            //##########adesso bisogna passare ai figli interni del nodo ##########

            validNumberOfTracks = false;//serve per capire se l'utente ha scritto bene
            tracksCount = 0;

            NodeList nodeChildList = songNode.getChildNodes();//preso i figli del nodo, che sono le track (1, 2 o 4),
            //i dati del video e delle foto
            int nodeListChildrenLength = nodeChildList.getLength();//lunghezza della lista dei figli, che sono i track

            //attenzione, nel file XML potrebbero esserci "strane cose" lette, come gli \n,
            //perciò devo leggere tutti gli oggetti uno per uno

            int[] nodePosition = new int[4];//array che mi indica gli indici dove si trovano i nodi track


            //questo ciclo for serve per vedere quanti elementi abbiamo e dove sono
            for (int n1 = 0; n1 < nodeListChildrenLength; n1++) {
                Node child = nodeChildList.item(n1);
                if (child.getNodeName().equals("track")) {
                    nodePosition[tracksCount] = n1;//tengo traccia di dove si trovano le track e quante sono, le gestico + avanti
                    tracksCount++;
                } else if (child.getNodeName().equals("video")) {
                    //prevedendo in futuro altri metadati, gestisco in maniera generica la ricerca del path
                    Node childVideo = child;
                    NamedNodeMap childAttributes = childVideo.getAttributes();

                    //per come impostiamo la sintassi, il path del video viene come primo elemento
                    Node videoPath = childAttributes.item(0);
                    String video_proposal = videoPath.getNodeName();
                    if (video_proposal.equals("path")) {
                        String video_path = videoPath.getTextContent();
                        File testPath = new File(adaptPath(video_path));

                        //testPath.mkdir();
                        if (testPath.exists() && testPath.isDirectory()) {
                            File[] videos = testPath.listFiles();
                            //prendo il primo file come buono. Se ce ne fosse + di uno? Discutere TODO

                            if (videos.length != 0) {
                                File video = videos[0];
                                song.setVideo(video.getAbsolutePath());
                            }
                        }
                    }
                } else if (child.getNodeName().equals("photos")) {
                    //prevedendo in futuro altri metadati, gestisco in maniera generica la ricerca del path
                    Node childPhoto = child;//prendo il nodo video
                    NamedNodeMap childAttributes = childPhoto.getAttributes();
                    Node photoPath = childAttributes.item(0);
                    String photo_proposal = photoPath.getNodeName();
                    //un minimo di controllo: deve essere il path
                    if (photo_proposal.equals("path")) {
                        String photo_path = photoPath.getTextContent();//prendo il contenuto del nodo, che è il path
                        //sono se la cartella foto esiste ed è una directory la inserisco
                        File testPath = new File(adaptPath(photo_path));
                        testPath.mkdir();
                        if (testPath.exists() && testPath.isDirectory())
                            song.setPhotos(adaptPath(photo_path));
                    }
                }
            }

            //gestiamo ora le canzoni
            if (tracksCount == 1)//la canzone è mono, per tradizione il canale è posto come sinistro
            {
                Node child = nodeChildList.item(nodePosition[0]);//prendo l'unica track presente
                NamedNodeMap childAttributes = child.getAttributes();
                Node track = childAttributes.item(0);//prendo il primo elemento, conterrà path = "<path>"

                if (track.getNodeName().equals("path")) {
                    String track_path = track.getTextContent();//prendo il path vero e proprio
                    song.setTrack(adaptPath(track_path), 1);

                    validNumberOfTracks = true;
                }
            } else if (tracksCount == 2) {
                Node child1 = nodeChildList.item(nodePosition[0]);//prendo la prima track
                NamedNodeMap childAttributes1 = child1.getAttributes();//prendo i suoi attributi
                Node track = childAttributes1.item(0);//prendo il path della track

                Node child2 = nodeChildList.item(nodePosition[1]);//prendo la seconda track
                NamedNodeMap childAttributes2 = child2.getAttributes();//prendo i suoi attributi
                Node track2 = childAttributes2.item(0);//prendo il nome della prima track

                if (track.getNodeName().equals("path") && track2.getNodeName().equals("path")) {
                    String track_path1 = track.getTextContent();//prendo il path del primo file audio
                    String track_path2 = track2.getTextContent();//prendo il path del secondo file audio

                    song.setTrack(adaptPath(track_path1), 1);
                    song.setTrack(adaptPath(track_path2), 2);
                    validNumberOfTracks = true;
                }

            }//fine else se abbiamo 2 tracce
            else if (tracksCount == 4)//traccia polifonica
            {
                Node child1 = nodeChildList.item(nodePosition[0]);//prendo la prima track
                NamedNodeMap childAttributes1 = child1.getAttributes();//prendo i suoi attributi
                Node track = childAttributes1.item(0);//prendo il path della track


                Node child2 = nodeChildList.item(nodePosition[1]);//prendo la seconda track
                NamedNodeMap childAttributes2 = child2.getAttributes();//prendo i suoi attributi
                Node track2 = childAttributes2.item(0);//prendo il nome della prima track

                Node child3 = nodeChildList.item(nodePosition[2]);//prendo la terza track
                NamedNodeMap childAttributes3 = child3.getAttributes();//prendo i suoi attributi
                Node track3 = childAttributes3.item(0);//prendo il path della track

                Node child4 = nodeChildList.item(nodePosition[3]);//prendo la quarta track
                NamedNodeMap childAttributes4 = child4.getAttributes();//prendo i suoi attributi
                Node track4 = childAttributes4.item(0);//prendo il nome della prima track

                if (track.getNodeName().equals("path") && track2.getNodeName().equals("path") && track3.getNodeName().equals("path") && track4.getNodeName().equals("path")) {
                    String track_path1 = track.getTextContent();//prendo il nome del file
                    String track_path2 = track2.getTextContent();//prendo il nome del file
                    String track_path3 = track3.getTextContent();//prendo il nome del file
                    String track_path4 = track4.getTextContent();//prendo il nome del file

                    validNumberOfTracks = true;
                    song.setTrack(adaptPath(track_path1), 1);
                    song.setTrack(adaptPath(track_path2), 2);
                    song.setTrack(adaptPath(track_path3), 3);
                    song.setTrack(adaptPath(track_path4), 4);
                }
            } else if (tracksCount > 4 || tracksCount == 3)//abbiamo più di 2 tracce
            {
                validNumberOfTracks = false;
            }

            //solo se c'è un giusto numero di tracce e se ogni path corrisponde ad un effettivo file presente in memoria
            if (validNumberOfTracks && song.isValid()) {
                //prima di inserirla aggiungo alcuni valori
                myList.add(song);
            }
        }    //fine for per processare tutti i nodi

        return myList;

    }    //fineXmlToDataConvertion
}
