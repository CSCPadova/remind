package unipd.dei.magnetophone.activity.pdf;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.util.List;

import unipd.dei.magnetophone.R;

import static unipd.dei.magnetophone.utility.Utility.showSupportActionBar;

public class PDFActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener,
        OnPageErrorListener {

    PDFView pdfView;

    Integer pageNumber = 0;
    String pdfFileName;

    String TAG="PDFView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        //poniamo la possibilità sulla action bar del tasto indietro
        showSupportActionBar(this, null, getWindow().getDecorView());

        Bundle p = getIntent().getExtras();
        String path =p.getString("path");
        String file =p.getString("file");
        File outFile=null;

        pdfFileName=file;
        if(pdfFileName.compareTo("")==0)
            outFile = new File(path);
        else
            outFile = new File(path,file);

        if(outFile.exists()) {


            pdfView = findViewById(R.id.pdfView);
            pdfView.fromFile(outFile)
                    .defaultPage(0)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .spacing(10) // in dp
                    .onPageError(this)
                    .load();
        }
        else
        {
            Toast.makeText(this, "impossibile caricare "+ pdfFileName, Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

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

    @Override
    public void onPageError(int page, Throwable t) {
        Toast.makeText(this, "impossibile caricare "+ pdfFileName, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Cannot load page " + page);
    }
}
