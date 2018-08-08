package unipd.dei.magnetophone;

/**
 * classe che rappresenta l'header di un file wav
 */

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class WaveHeader {

    public static final String RIFF_HEADER = "RIFF";
    public static final String WAVE_HEADER = "WAVE";
    public static final String FMT_HEADER = "fmt ";
    public static final String DATA_HEADER = "data";
    public static final int HEADER_BYTE_LENGTH = 44;//è la lunghezza dell'header del file wav

    private boolean valid;

    // -------------- RIFF HEADER ----------------------------//

    private String chunkId; // 4 bytes, big endian, contiene la parola RIFF in
    // ASCII
    private long chunkSize; // unsigned 4 bytes, little endian, la lunghezza del
    // chunck dopo questo
    // numero
    private String format; // 4 bytes, big endian, contiene la parola WAVE
    // -------------------------------------------------------//

    // ------------- FMT SUBCHUNK ----------------------------//
    // descrive il formato dei dati
    private String subChunk1Id; // 4 bytes, big endian, contiene la parole fmt

    private long subChunk1Size; // unsigned 4 bytes, little endian, contiene la
    // lunghezza del
    // subchunk dopo di lui

    private int audioFormat; // unsigned 2 bytes, little endian, contiene un
    // numero che indica il
    // formato di compressione usato per il file. 1 per PCM
    private int channels; // unsigned 2 bytes, little endian, numero di canali,
    // 1, 2 etch.

    private long sampleRate; // unsigned 4 bytes, little endian, 4000, 8000, ...
    // Hz - campioni al secondo -

    private long byteRate; // unsigned 4 bytes, little endian, pari al seguente
    // valore:
    // SampleRate * channels * bitsPerSample / 8

    private int blockAlign; // unsigned 2 bytes, little endian, pari a
    // channels*bitsPerSample/8, è il numero di byte per un sample contando
    // tutti i canali

    private int bitsPerSample; // unsigned 2 bytes, little endian, pari alla
    // nostra bitDepth
    // può essere 8, 16, 24, 32 etc.
    // --------------------------------------------------------------//

    // -------------------- DATA SUBCHUNK --------------------------//
    private String subChunk2Id; // 4 bytes, big endian, contiene la parola
    // "data"
    private long subChunk2Size; // unsigned 4 bytes, little endian, pari a
    // NumberOfSamples*channels*BitsPerSample/8
    // lunghezza del subchunk seguente
    // ---------------------------------------------------------------//

    private float duration;//lunghezza del file in secondi, float perché teniamo anche i millisencondi

    /**
     * costruttore di default, inizializza un header
     * senza dati nel secondo subchunck, con traccia mono a 8kHz di sampleRate
     * e 16 di bitDepth
     */
    public WaveHeader() {
        chunkSize = 36;
        subChunk1Size = 16;
        audioFormat = 1;
        channels = 1;
        sampleRate = 8000;
        byteRate = 16000;
        blockAlign = 2;
        bitsPerSample = 16;
        subChunk2Size = 0;
        valid = true;
    }

    public WaveHeader(String path) {
        this(new File(path));
    }

    /**
     * costruttore che accetta come parametro il file wav
     * @param f
     */
    public WaveHeader(File f) {
        try {
            InputStream input = new FileInputStream(f);
            valid = loadHeader(input);
            duration = estimateDuration(f);
            input.close();
        } catch (FileNotFoundException e) {
            Log.e("WaveHeader", "File wav non trovato: " + f.getPath());
        } catch (IOException e) {
            Log.e("WaveHeader", "errore di I/O: " + f.getPath());
        }
    }

    /**
     * costruttore che prende come parametro un InputStream rappresentante un file audio WAV e controlla se è
     * valido, in questo caso inizializza i campi
     * @param inputStream
     */
    public WaveHeader(InputStream inputStream) {
        valid = loadHeader(inputStream);
    }

    /**
     * metodo privato chiamato dal costruttore, prende l'InputSteam che sta sopra il file wav e ne legge i metadati,
     * ossia effettua il parsing dell'header inserendo i campi nell'oggetto WaveHeader.
     *
     * @param inputStream
     * @return
     * true se la costruzione dell'header ha avuto successo, false altrimenti, o anche se il formato non
     * è supportato dalla nostra applicazione
     */
    private boolean loadHeader(InputStream inputStream) {

        //inizializzo l'array di 44 byte che conterrà i byte dell'header del file wav
        byte[] headerBuffer = new byte[HEADER_BYTE_LENGTH];
        try {
            //leggiamo dallo stream tutti i byte che possono stare nell'headerBuffer
            inputStream.read(headerBuffer);

            //a questo punto, ci facciamo un parsing
            // "puntatore" al buffer, ci serve per passare da un byte all'altro dell'array
            //mentre facciamo il parsing
            int pointer = 0;

            //una ad una, prendo le lettere nel buffer e creo la string per chunkId
            //conterrà la parola RIFF
            //nota: essendo in bigEndian, il primo è il byte più significativo, quindi la lettera R
            chunkId = new String(new byte[]{headerBuffer[pointer++],
                    headerBuffer[pointer++], headerBuffer[pointer++],
                    headerBuffer[pointer++]});

            //ci prendiamo la lunghezza restante dell'header
            // è little endian
            //ricordo che headerBuffer è un array di Byte, prendo un byte alla volta, lo maschero per
            //tenere buoni solo gli otto bit che interessano, poi shifto quello che serve per via del little endian
            //trasformando il tutto in un long da 4 byte
            chunkSize = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff << 24);

            //contiene la parola WAVE in big endian
            format = new String(new byte[]{headerBuffer[pointer++],
                    headerBuffer[pointer++], headerBuffer[pointer++],
                    headerBuffer[pointer++]});

            //contiene la parola fmt, big endian
            subChunk1Id = new String(new byte[]{headerBuffer[pointer++],
                    headerBuffer[pointer++], headerBuffer[pointer++],
                    headerBuffer[pointer++]});
            //big endian, lunghezza del subchunk dopo di lui
            subChunk1Size = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff) << 24;
            //prendiamo il numero che indica il formato
            audioFormat = (int) ((headerBuffer[pointer++] & 0xff) | (headerBuffer[pointer++] & 0xff) << 8);
            //prendiamo il numero di canali
            channels = (int) ((headerBuffer[pointer++] & 0xff) | (headerBuffer[pointer++] & 0xff) << 8);
            //prendiamo il SampleRate
            sampleRate = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff) << 24;
            //prendiamo il byteRate
            byteRate = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff) << 24;

            blockAlign = (int) ((headerBuffer[pointer++] & 0xff) | (headerBuffer[pointer++] & 0xff) << 8);

            bitsPerSample = (int) ((headerBuffer[pointer++] & 0xff) | (headerBuffer[pointer++] & 0xff) << 8);
            //contiene la parola DATA
            subChunk2Id = new String(new byte[]{headerBuffer[pointer++],
                    headerBuffer[pointer++], headerBuffer[pointer++],
                    headerBuffer[pointer++]});
            //contiene la lunghezza dei dati in byte
            subChunk2Size = (long) (headerBuffer[pointer++] & 0xff)
                    | (long) (headerBuffer[pointer++] & 0xff) << 8
                    | (long) (headerBuffer[pointer++] & 0xff) << 16
                    | (long) (headerBuffer[pointer++] & 0xff) << 24;
            // end read header

            // the inputStream should be closed outside this method

            // dis.close();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (bitsPerSample != 8 && bitsPerSample != 16) {
            System.err.println("WaveHeader: only supports bitsPerSample 8 or 16");
            return false;
        }

        // controlla che il file letto sia quello che vogliamo noi
        if (chunkId.toUpperCase().equals(RIFF_HEADER)
                && format.toUpperCase().equals(WAVE_HEADER) && audioFormat == 1) {
            return true;
        } else {
            System.err.println("WaveHeader: Unsupported header format");
        }

        return false;
    }

    /**
     * Avvisa se la costruzione dell'header ha avuto successo e se il formato è da noi supportato
     * @return
     */
    public boolean isValid() {
        return valid;
    }

    //metodi getter
    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSubChunk1Id() {
        return subChunk1Id;
    }

    public void setSubChunk1Id(String subChunk1Id) {
        this.subChunk1Id = subChunk1Id;
    }

    public long getSubChunk1Size() {
        return subChunk1Size;
    }

    public void setSubChunk1Size(long subChunk1Size) {
        this.subChunk1Size = subChunk1Size;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(int audioFormat) {
        this.audioFormat = audioFormat;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getSampleRate() {
        return (int) sampleRate;
    }

    //metodi setter
    public void setSampleRate(int sampleRate) {
        int newSubChunk2Size = (int) (this.subChunk2Size * sampleRate / this.sampleRate);
        // if num bytes for each sample is even, the size of newSubChunk2Size also needed to be in even number
        if ((bitsPerSample / 8) % 2 == 0) {
            if (newSubChunk2Size % 2 != 0) {
                newSubChunk2Size++;
            }
        }

        this.sampleRate = sampleRate;
        this.byteRate = sampleRate * bitsPerSample / 8;
        this.chunkSize = newSubChunk2Size + 36;
        this.subChunk2Size = newSubChunk2Size;
    }

    public int getByteRate() {
        return (int) byteRate;
    }

    public void setByteRate(long byteRate) {
        this.byteRate = byteRate;
    }

    public int getBlockAlign() {
        return blockAlign;
    }

    public void setBlockAlign(int blockAlign) {
        this.blockAlign = blockAlign;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public void setBitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public String getSubChunk2Id() {
        return subChunk2Id;
    }

    public void setSubChunk2Id(String subChunk2Id) {
        this.subChunk2Id = subChunk2Id;
    }

    public long getSubChunk2Size() {
        return subChunk2Size;
    }

    public void setSubChunk2Size(long subChunk2Size) {
        this.subChunk2Size = subChunk2Size;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float f) {
        duration = f;
    }

    public boolean isMono() {
        return this.getChannels() == 1;
    }

    public float estimateDuration(File f) {
        return (subChunk2Size) / (sampleRate * channels * bitsPerSample / 8);
    }


    @Override
    public boolean equals(Object c) {
        WaveHeader wave2 = (WaveHeader) c;
        return (this.bitsPerSample == wave2.getBitsPerSample()) &&
                (this.sampleRate == wave2.getSampleRate()) &&
                (this.duration == wave2.getDuration()) &&
                (this.channels == wave2.getChannels());
    }

    public String toString() {

        StringBuffer sb = new StringBuffer();
        sb.append("chunkId: " + chunkId);
        sb.append("\n");
        sb.append("chunkSize: " + chunkSize);
        sb.append("\n");
        sb.append("format: " + format);
        sb.append("\n");
        sb.append("subChunk1Id: " + subChunk1Id);
        sb.append("\n");
        sb.append("subChunk1Size: " + subChunk1Size);
        sb.append("\n");
        sb.append("audioFormat: " + audioFormat);
        sb.append("\n");
        sb.append("channels: " + channels);
        sb.append("\n");
        sb.append("sampleRate: " + sampleRate);
        sb.append("\n");
        sb.append("byteRate: " + byteRate);
        sb.append("\n");
        sb.append("blockAlign: " + blockAlign);
        sb.append("\n");
        sb.append("bitsPerSample: " + bitsPerSample);
        sb.append("\n");
        sb.append("subChunk2Id: " + subChunk2Id);
        sb.append("\n");
        sb.append("subChunk2Size: " + subChunk2Size);
        return sb.toString();
    }
}
