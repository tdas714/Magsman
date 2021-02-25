package applied.ai.magsman;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.Serializable;

public class Audio implements Serializable {

    private String filename;

    public Audio(String filename){
        this.filename = filename;
    }

    public String getData(int index){
        String data = Utils.getFile(filename, index+".wav").getPath();
        return data;
    }

    public String getTitle(){
        return filename.split(".pdf")[0];
    }

    public String getPreview(int index){
        String text = "";
        try{
            PdfReader reader = new PdfReader(Utils.getFile("Books", filename).getPath());
            String[] pageText = PdfTextExtractor.getTextFromPage(reader, index+1).split("\n");
            int num = 0;
            if (pageText.length >= 5) {
                num = 4;
            } else if (pageText.length >= 4) {
                num = 3;
            } else if (pageText.length >= 3) {
                num = 2;
            } else if (pageText.length >= 2) {
                num = 1;
            } else if (pageText.length >= 1) {
                num = 0;
            }
            text = pageText[num];
            if (text.split("\\s").length <= 5) {
                text = pageText[num - 1] + text;
            }
        }catch (Exception ex){
            Log.d("getPreview: ", String.valueOf(ex));
        }

        return text;
    }

    public Bitmap getCover(){
        Bitmap bitmap = BitmapFactory.decodeFile(Utils.getFile(filename, "cover.png").getPath());
        return bitmap;
    }

}
