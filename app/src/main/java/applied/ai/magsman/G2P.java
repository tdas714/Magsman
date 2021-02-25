package applied.ai.magsman;

import android.content.Context;
import android.util.Log;
import android.view.textclassifier.TextClassifierEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class G2P {
        private JSONObject obj;
        private Map<String, Integer> _symbol_to_id;
    public G2P(Context context) throws JSONException {
        obj = new JSONObject(Objects.requireNonNull(Utils.readJSONFromAsset(context, "MapG2P.json")));
//        obj.put(" ", " ");
        _symbol_to_id = Text.GET();
    }

    public ArrayList<Integer> getSequence(String text){
        Log.e("getSequence: Input", text);
        text = Text.cleaner(text);
        Log.e("getSequence: Cleaner", text);
        String out = "";
        for(String t: text.split("\\b")){
//            Log.e("getSequence: T", t);
            try{
                out += obj.getString(t);
            }catch (Exception ex){
                Log.e("getSequence: ERROR", "["+t+"]");
            }
        }
        Log.e("getSequence: Out", out);
        ArrayList<Integer> seq = Text.text_to_sequence(out);
        return seq;
    }
}
