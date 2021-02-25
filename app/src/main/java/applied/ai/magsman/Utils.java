package applied.ai.magsman;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

//import static android.provider.Telephony.Mms.Part.FILENAME;
import static android.content.Context.ALARM_SERVICE;
import static androidx.core.content.MimeTypeFilter.matches;

public class Utils {

    public static class returnObject{
        public final PdfRenderer pdfRenderer;
        public final ParcelFileDescriptor parcelFileDescriptor;
        public returnObject(PdfRenderer pdfRenderer, ParcelFileDescriptor parcelFileDescriptor){
            this.pdfRenderer = pdfRenderer;
            this.parcelFileDescriptor = parcelFileDescriptor;
        }
    }

    public static String readJSONFromAsset(Context context, String filename) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static PdfRenderer.Page saveCover(int index, String filename, PdfRenderer pdfRenderer) throws FileNotFoundException {
        if (pdfRenderer.getPageCount() <= index) {
            return null ;
        }
        // Make sure to close the current page before opening another one.
//        if (null != currentPage) {
//            currentPage.close();
//        }
        PdfRenderer.Page currentPage = null;
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.
        File desfile = getFile(filename, "cover.png");
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(desfile.getPath()));
        return currentPage;
    }

    public static void closeRenderer(PdfRenderer.Page currentPage, PdfRenderer pdfRenderer, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        if (currentPage != null) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    public static returnObject openRenderer(Context context, File file) throws IOException {
        PdfRenderer pdfRenderer = null;
        ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        // This is the PdfRenderer we use to render the PDF.
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
//        parcelFileDescriptor.close();
        return new returnObject(pdfRenderer, parcelFileDescriptor);
    }

    // ---------------------------------------------------------
    public static File getFile(String folder, String filename){
        boolean suc = false;
        File dir = Environment.getExternalStorageDirectory();
        File filedir = new File(dir.getAbsoluteFile()+"/Magsman/"+folder+"/");
        Log.e("getFile: ", filedir.toString());
        try{
            Log.e("getFile: ", "Activated");
            suc = filedir.mkdirs();
        }catch (Exception ex){
            Log.e("getFile: ", "Exception");
        }
        Log.e("getFile: ", String.valueOf(suc));
        File desFile = new File(filedir, filename);
        return desFile;
    }

    public static ArrayList<String> readFromFile(Context context) {
        ArrayList<String> ret = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("qoutes.txt"), "UTF-8"));
            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                ret.add(mLine);
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
        return ret;
    }


    public static byte[] ProcessText(String text, Module taco, Module mel, G2P g2P) throws JSONException {
        ArrayList<Integer> seq = g2P.getSequence(text);
//        long[] input = Utils.convertIntegers(seq);
        Tensor textTensor = Tensor.fromBlob(Utils.convertIntegers(seq), new long[]{seq.size()});
//                    Tensor lenTensor = Tensor.fromBlob(new long[]{out.size()}, new long[]{1});
        Log.e("onClick: text Tensor", textTensor.toString());
        long text_te_len = textTensor.numel();
        if(text_te_len>=1000){return null;}
//                    Log.e("onClick: len Tensor", lenTensor.toString());
        Tensor out = taco.forward(IValue.from(textTensor), IValue.from(Tensor.fromBlob(new float[]{0.9f}, new long[0]))).toTensor();
//                IValue.from(Tensor.fromBlob(new float[]{0.9f}, new long[0])),
//                IValue.from(Tensor.fromBlob(new float[]{0.99f}, new long[0]))).toTensor();
        Log.e("onClick: ", out.toString());
//        Tensor([1, 80, 2000]
//        long out_len = out.numel();
//        Log.e("ProcessText: Numel", String.valueOf(out_len));
//        if(out_len>=16000){return null;}
        final IValue melout = mel.forward(IValue.from(out));
        Log.e("onClick: Module Out", Arrays.toString(melout.toTensor().getDataAsFloatArray()));
        float[] mel_arr = melout.toTensor().getDataAsFloatArray();
        byte[] audio = new byte[mel_arr.length*2];
        Log.e("onClick: len melarr", String.valueOf(mel_arr.length));
        short[] sh_arr  = new short[mel_arr.length];
        for(int i = 0; i < mel_arr.length; i++){
            sh_arr[i] = (short) mel_arr[i];
        }
        ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(sh_arr);
        Log.e("onClick: Arr", Arrays.toString(audio));
        return audio;
    }


    public static byte [] float2ByteArray (float value)
    {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }
    public static void rawToWave(final byte[] rawData, File waveFile) throws IOException {
//        File dir = Environment.getExternalStorageDirectory();// creating the empty wav file.
//        File filedir = new File(dir.getAbsoluteFile()+"/Books/");
//
//        File waveFile = new File(filedir, "Audio.wav");

        DataOutputStream output = null;//following block is converting raw to wav.
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 22050); // sample rate
            writeInt(output, 22050 * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            output.write(rawData);

        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    public static byte[] floatToByteArray(float value) {
        int intBits =  Float.floatToIntBits(value);
        return new byte[] {
                (byte) (intBits >> 24), (byte) (intBits >> 16), (byte) (intBits >> 8), (byte) (intBits) };
    }

    private static void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private static void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value);
        output.write(value >> 8);
    }

    private static void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
    public static byte[] intToBytes(final short[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length*2);
        ShortBuffer sBuffer = byteBuffer.asShortBuffer();
        sBuffer.put(data);

        byte[] array = byteBuffer.array();
        return array;
    }


    public static long[] convertIntegers(List<Integer> integers)
    {
        long[] ret = new long[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    public static int printNumberOfCores() {
        int cores = printFile("/sys/devices/system/cpu/present");
        printFile("/sys/devices/system/cpu/possible");
        return cores;
    }

    private static int printFile(String path) {
        InputStream inputStream = null;
        String line = "";
        try {
            inputStream = new FileInputStream(path);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            line = bufferedReader.readLine();
            Log.d(path, line);
        } catch (Exception e) {
            Log.e("printFile: ", e.toString());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return Integer.parseInt(line.split("-")[1])+1;
    }
}
