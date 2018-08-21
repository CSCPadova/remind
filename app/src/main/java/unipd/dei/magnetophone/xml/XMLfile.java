package unipd.dei.magnetophone.xml;

/**
 * classe che rappresenta un file xml nella external storage
 */
public class XMLfile {
    private int id;
    private String nomefile;
    private long datamodifica;

    public XMLfile() {
    }

    //%%%%%%%% metodi getter %%%%%%%%%
    public int getId() {
        return id;
    }

    //%%%%%%% metodi setter %%%%%%%%%
    public void setId(int i) {
        id = i;
    }

    public String getNomeFile() {
        return nomefile;
    }

    public void setNomeFile(String n) {
        nomefile = n;
    }

    public long getData() {
        return datamodifica;
    }

    public void setData(long d) {
        datamodifica = d;
    }
}
