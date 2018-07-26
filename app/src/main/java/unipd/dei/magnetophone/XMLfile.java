package unipd.dei.magnetophone;

/**
 * classe che rappresenta un file xml nella external storage
 */
public class XMLfile
{
	private int id;
	private String nomefile;
	private long datamodifica;
	
	public XMLfile() {}
	
	//%%%%%%% metodi setter %%%%%%%%%
	public void setId(int i)
	{
		id = i;
	}
	
	public void setNomeFile(String n)
	{
		nomefile=n;
	}
	
	public void setData(long d)
	{
		datamodifica=d;
	}

	//%%%%%%%% metodi getter %%%%%%%%%
	public int getId()
	{
		return id;
	}
	
	public String getNomeFile()
	{
		return nomefile;
	}
	
	public long getData()
	{
		return datamodifica;
	}
}
