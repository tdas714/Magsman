package applied.ai.magsman;

import android.util.ArrayMap;
import android.util.Log;

import androidx.core.content.res.TypedArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import me.xuender.unidecode.Unidecode;

public class Text {

    private static final String _pad = "_";
    private static final String _punctuation = "!\'(),.:;? ";
    private static final String _special = "-";
    private static final String _letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String _vowels = "iyɨʉɯuɪʏʊeøɘəɵɤoɛœɜɞʌɔæɐaɶɑɒᵻ";
    private static final String _non_pulmonic_consonants = "ʘɓǀɗǃʄǂɠǁʛ";
    private static final String _pulmonic_consonants = "pbtdʈɖcɟkɡqɢʔɴŋɲɳnɱmʙrʀⱱɾɽɸβfvθðszʃʒʂʐçʝxɣχʁħʕhɦɬɮʋɹɻjɰlɭʎʟ";
    private static final String _suprasegmentals = "ˈˌːˑ";
    private static final String _other_symbols = "ʍwɥʜʢʡɕʑɺɧ";
    private static final String _diacrilics = "ɚ˞ɫ";

    private static Map<String, Integer> _symbol_to_id = new HashMap<>();
    private static final String[] valid_symbols = {
            "AA", "AA0", "AA1", "AA2", "AE", "AE0", "AE1", "AE2", "AH", "AH0", "AH1", "AH2",
            "AO", "AO0", "AO1", "AO2", "AW", "AW0", "AW1", "AW2", "AY", "AY0", "AY1", "AY2",
            "B", "CH", "D", "DH", "EH", "EH0", "EH1", "EH2", "ER", "ER0", "ER1", "ER2", "EY",
            "EY0", "EY1", "EY2", "F", "G", "HH", "IH", "IH0", "IH1", "IH2", "IY", "IY0", "IY1",
            "IY2", "JH", "K", "L", "M", "N", "NG", "OW", "OW0", "OW1", "OW2", "OY", "OY0",
            "OY1", "OY2", "P", "R", "S", "SH", "T", "TH", "UH", "UH0", "UH1", "UH2", "UW",
            "UW0", "UW1", "UW2", "V", "W", "Y", "Z", "ZH" };
    private static ArrayList<String> _arpabet = new ArrayList<>();
    private static final Pattern sPattern = Pattern.compile("[{}]");
    private static final Map<String, String> _abbreviations = new HashMap<String , String>(){{
        put("mrs", "misess");
        put("mr", "mister");
        put("dr", "doctor");
        put("st", "saint");
        put("co", "company");
        put("jr", "junior");
        put("maj", "major");
        put("gen", "general");
        put("drs", "doctors");
        put("rev", "reverend");
        put("lt", "lieutenant");
        put("hon", "honorable");
        put("sgt", "sergeant");
        put("capt", "captain");
        put("esq", "esquire");
        put("ltd", "limited");
        put("col", "colonel");
        put("ft", "fort");
        put("'aight","alright");
        put("ain't","am not");
        put("amn't", "am not");
        put("aren't", "are not");
        put("can't", "cannot");
        put("'cause", "because");
        put("could've", "could have");
        put("couldn't", "could not");
        put("couldn't've", "could not have");
        put("daren't", "dare not");
        put("daresn't", "dare not");
        put("dasn't", "dare not");
        put("didn't", "did not");
        put("doesn't", "does not");
        put("don't", "do not");
        put("dunno", "do not know");
        put("d'ye", "do you");
        put("e'er", "ever");
        put("'em", "them");
        put("everybody's", "everybody is");
        put("everyone's", "everyone is");
        put("finna", "fixing to");
        put("g'day", "good day");
        put("gimme", "give me");
        put("giv'n", "given");
        put("gonna", "going to");
        put("gon't", "go not");
        put("gotta", "got to");
        put("hadn't", "had not");
        put("had've", "had have");
        put("hasn't", "has not");
        put("haven't", "have not");
        put("he'd", "he had");
        put("he'll", "he will");
        put("he's", "he is");
        put("he've", "he have");
        put("how'd", "how would");
        put("howdy", "how do you do");
        put("how'll", "how will");
        put("how're", "how are");
        put("how's", "how has");
        put("I'd", "I had");
        put("I'd've", "I would have");
        put("I'll", "I will");
        put("I'm", "I am");
        put("I'm'a", "I am about to");
        put("I'm'o", "I am going to");
        put("innit", "is it not");
        put("I've", "I have");
        put("isn't", "is not");
        put("it'd", "it would");
        put("it'll", "it will");
        put("it's", "it is");
        put("iunno", "I don't know");
        put("let's", "let us");
        put("ma'am", "madam");
        put("mayn't", "may not");
        put("may've", "may have");
        put("methinks", "me thinks");
        put("mightn't", "might not");
        put("might've", "might have");
        put("mustn't", "must not");
        put("mustn't've", "must not have");
        put("must've", "must have");
        put("needn't", "need not");
        put("nal", "and all");
        put("ne'er", "never");
        put("o'clock", "of the clock");
        put("o'er", "over");
        put("ol'", "old");
        put("oughtn't", "ought not");
        put("'s", "is");
        put("shalln't", "shall not");
        put("shan't", "shall not");
        put("she'd", "she had");
        put("she'll", "she will");
        put("she's", "she is");
        put("should've", "should have");
        put("shouldn't", "should not");
        put("shouldn't've", "should not have");
        put("somebody's", "somebody is");
        put("someone's", "someone is");
        put("something's", "something is");
        put("so're", "so are");
        put("that'll", "that will");
        put("that're", "that are");
        put("that's", "that is");
        put("that'd", "that had");
        put("there'd", "there had");
        put("there'll", "there shall");
        put("there're", "there are");
        put("there's", "there is");
        put("these're", "these are");
        put("these've", "these have");
        put("they'd", "they had");
        put("they'll", "they will");
        put("they're", "they are");
        put("they've", "they have");
        put("this's", "this is");
        put("those're", "those are");
        put("those've", "those have");
        put("'tis", "it is");
        put("to've", "to have");
        put("'twas", "it was");
        put("wanna", "want to");
        put("wasn't", "was not");
        put("we'd", "we had");
        put("we'd've", "we would have");
        put("we'll", "we will");
        put("we're", "we are");
        put("we've", "we have");
        put("weren't", "were not");
        put("what'd", "what did");
        put("what'll", "what will");
        put("what're", "what are");
        put("what's", "what has");
        put("what've", "what have");
        put("when's", "when is");
        put("where'd", "where did");
        put("where'll", "where shall");
        put("where're", "where are");
        put("where's", "where is");
        put("where've", "where have");
        put("which'd", "which had");
        put("which'll", "which will");
        put("which're", "which are");
        put("which's", "which is");
        put("which've", "which have");
        put("who'd", "who would");
        put("who'd've", "who would have");
        put("who'll", "who will");
        put("who're", "who are");
        put("who's", "who is");
        put("who've", "who have");
        put("why'd", "why did");
        put("why're", "why are");
        put("why's", "why is");
        put("willn't", "will not");
        put("won't", "will not");
        put("wonnot", "will not");
        put("would've", "would have");
        put("wouldn't", "would not");
        put("wouldn't've", "would not have");
        put("y'all", "you all");
        put("y'all'd've", "you all would have");
        put("y'all'd'n've", "you all would not have");
        put("y'all're", "you all are");
        put("you'd", "you had");
        put("you'll", "you will");
        put("you're", "you are");
        put("you've", "you have");
        put("hadn’t", "had not");
        put("havn't", "have not");
    }};

    public static Map<String, Integer> GET(){

//        for(String s: valid_symbols){
//            _arpabet.add("@"+s);
//        }
        String allSym = _pad+_punctuation+_special+_vowels+_non_pulmonic_consonants+_pulmonic_consonants+_suprasegmentals+_other_symbols+_diacrilics;
        char[] allSymChar = allSym.toCharArray();
        Log.e("GET: ", Arrays.toString(allSymChar));
        Arrays.sort(allSymChar);
        Log.e("GET: Sorted", Arrays.toString(allSymChar));
        ArrayList<String> symbols = new ArrayList<>();
        for(char c: allSymChar){
            symbols.add(String.valueOf(c).trim());
        }
//        symbols.addAll(_arpabet);
        Log.e("text_to_sequence: SYMBOLS", String.valueOf(symbols));
        int i = 0;
        for(String s : symbols){
            _symbol_to_id.put(s, i);
            i += 1;
        }

        _symbol_to_id.remove("", 0);
        _symbol_to_id.put(" ", 0);
        Log.e("text_to_sequence: Symbol to ID", String.valueOf(_symbol_to_id));
        return _symbol_to_id;
    }

    public static ArrayList<Integer> text_to_sequence(String text){
//        GET();
        String[] textS = sPattern.split(text);
//        Log.e("text_to_sequence: SPLIT", Arrays.toString(textS));
//        int snum = 0;
        ArrayList<Integer> sequence = new ArrayList<>();

        for(int nums=0; nums < textS.length; nums++){
//            Log.e("text_to_sequence: String", textS[nums]);
            if (nums%2==0){
                sequence.addAll(_symbols_to_sequence(textS[nums]));
            }else{
                sequence.addAll(_arphabet_to_sequence(textS[nums]));
//                Log.e("text_to_sequence: ", "Acrtivated");
            }
        }
        Log.e("text_to_sequence: End result", String.valueOf(sequence));
        return sequence;
    }

    public static ArrayList<Integer> _arphabet_to_sequence(String text){
        ArrayList<Integer> sequence = new ArrayList<>();
        for(String s: text.split(" ")){
//            Log.e("_arphabet_to_sequence: ", "@"+s);
            sequence.addAll(_symbols_to_sequence("@"+s));
        }
        return sequence;
    }

    public static ArrayList<Integer> _symbols_to_sequence(String text){
//        Log.e("_symbols_to_sequence: ", text);
        ArrayList<Integer> sequence = new ArrayList<>();
        if(text.contains("@")){
            if(_should_keep_symbol(text)){
                sequence.add(_symbol_to_id.get(text));
            }
        }else{
            for(char c: text.toCharArray()){
                String s = String.valueOf(c);
                if(_should_keep_symbol(s)){
//                    Log.e("_symbols_to_sequence: ", s);
                    sequence.add(_symbol_to_id.get(s));
                }
            }
        }
        return sequence;
    }

    public static boolean _should_keep_symbol(String s){
        if(_symbol_to_id.containsKey(s) && !s.equals("_") && !s.equals("~")){
            return true;
        }
        return false;
    }
    public static String expand_abb(String text){
        Iterator it = _abbreviations.entrySet().iterator();
//        String result="";
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();

            text = text.replaceAll(pair.getKey().toString()+"\\.", String.valueOf(pair.getValue()));
//            Log.e("expand_abb: TEXT", result);
//            it.remove();
        }

        return text;
    }

    public static String cleaner(String text){
        text = Unidecode.decode(text);
//        Log.e("cleaner: Unidecode", text);
        text = text.toLowerCase();
//        Log.e("cleaner: Lower", text);
        text = numbers.normalize_numbers(text);
//        Log.e("cleaner: Numbers", text);
        text = expand_abb(text);
//        Log.e("cleaner: Abbriviation", text);
        text = text.replaceAll("\\s+", " ");
//        Log.e("cleaner: WhiteSpace", text);
        text = text.replace("i.e.", "that is").replace("e.g.", "for example");
        return text;
    }

}
