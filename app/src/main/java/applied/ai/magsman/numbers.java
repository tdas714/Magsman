package applied.ai.magsman;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class numbers {
    public static Pattern _comma_number_re = Pattern.compile("([\\D0-9]+[0-9,]+[0-9.\\s\\D]+)");
    public static Pattern _decimal_number_re = Pattern.compile("([0-9]+\\.[0-9]+)");
    public static Pattern _pounds_re = Pattern.compile("£([\\D\\d][0-9,]*[0-9\\D\\s]+)");
    public static Pattern _dollars_re = Pattern.compile("\\$([0-9,.]*[0-9])");
    public static Pattern _ordinal_re = Pattern.compile("[0-9]+(st|nd|rd|th)");
    public static Pattern _number_re = Pattern.compile("[0-9]+");
    public static int INDEX_NOT_FOUND = -1;
    public static String normalize_numbers(String text){
//        Log.e("normalize_numbers: Input", text);
        Matcher m = _comma_number_re.matcher(text);
        boolean t = m.lookingAt();
        if (t){ text = remove_commas(m); }
//        Log.e("normalize_numbers: Remove Commas", text);
        m = _pounds_re.matcher(text);
        t = m.lookingAt();
        if (t){text = m.replaceAll("\1 pounds");}
//        Log.e("normalize_numbers: pounds", text);
//      -------------------------------------------------------------------------------------------
        if (text.contains("$")){
            String[] do_wo = text.replaceAll("\\$([0-9,.]*[0-9])", "").trim().replace("  ", " ").split("\\s");
//            Log.e("normalize_numbers: Dollars replaced", Arrays.toString(do_wo));
            String[] diff = text.split("\\s");
//            Log.e("normalize_numbers: Text", Arrays.toString(diff));
            ArrayList<String> missingD = findMissing(diff, do_wo, diff.length, do_wo.length);
//            Log.e("normalize_numbers: Diffrence", String.valueOf(missingD));
//            ArrayList<String> missingDex = new ArrayList<>();
            for (String s : missingD){
                text = text.replace(s, expand_dollers(s));
            }
//            Log.e("normalize_numbers: Dollers", text);
        }
//       ------------------------------------------------------------------------------------------
        String[] de_wo = text.replaceAll("([0-9]+\\.[0-9]+)", "").trim().replace("  ", " ").split("\\s");
//        Log.e("normalize_numbers: decimal Point", Arrays.toString(de_wo));
        String[] diff = text.split("\\s");
        ArrayList<String> missingD = findMissing(diff, de_wo, diff.length, de_wo.length);
//        Log.e("normalize_numbers: Diffrence Decimal", String.valueOf(missingD));
        if(!missingD.isEmpty()){
//            Log.e("normalize_numbers: Decimal missing", "Activated");
            for (String s: missingD){
                text = text.replace(s, s.replace(".", " point "));
            }
        }
//        Log.e("normalize_numbers: Decimal", text);
//      -------------------------------------------------------------------------------------------
        String[] or_wo = text.replaceAll("[0-9]+(st|nd|rd|th)", "").trim().replace("  ", " ").split("\\s");
        diff = text.split("\\s");
        missingD = findMissing(diff, or_wo, diff.length, or_wo.length);
//        Log.e("normalize_numbers: Diffrence ordinal", String.valueOf(missingD));
        if(!missingD.isEmpty()){
            for(String s: missingD){
                text = text.replace(s, divideOrdinal(s));
            }
        }
//        Log.e("normalize_numbers: ordinal", text);
//      --------------------------------------------------------------------------------------------
        String[] num_wo = text.replaceAll("\\d", "").trim().replace("  ", " ").split("[\\s!-\\'(),.:;?%]");
        diff = text.split("[\\s-!\\'(),.:;?%]");
        missingD = findMissing(diff, num_wo, diff.length, num_wo.length);
        Log.e("normalize_numbers: Diffrence expand numbers", String.valueOf(missingD));
        if(!missingD.isEmpty()){
            for(String s: missingD){
                try{
                    text = text.replaceFirst(s, convert_to_words(s.toCharArray()));
                }catch (Exception ex){
                    s = s.replace("[", "").replace("]", "");
                    Log.e("normalize_numbers: CATCH", s);
                    text = text.replaceFirst(s, s);
                }
            }
        }
//        Log.e("normalize_numbers: Expand NUmbers", text);
//      --------------------------------------------------------------------------------------------
        return text;
    }
    public static String divideOrdinal(String text){
        String result = "";
        String[] r = text.replaceAll("\\D+", "").trim().replace("  ", " ").split("\\s");
        for(String s: r){
            try{
                int n = Integer.parseInt(s);
                float num = (float) ((float) n/10.0);
                String[] temp = String.valueOf(num).split("\\.");
                result = String.valueOf(Integer.parseInt(temp[0])*10)+" "+convertOrdinal(temp[1]);
//            Log.e("divideOrdinal: NUmbers", result);
            }catch (Exception ex){
                Log.e("divideOrdinal: ", ex.toString());
            }
        }
//        Log.e("divideOrdinal: ", Arrays.toString(r));

        return result;
    }
    public static String convertOrdinal(String text){
        String result = "";
        if(text.equals("1")){result = "first";}
        else if(text.equals("2")){result="second";}
        else if(text.equals("3")){result="third";}
        else if(text.equals("4")){result="forth";}
        else if(text.equals("5")){result="fifth";}
        else if(text.equals("6")){result="sixth";}
        else if(text.equals("7")){result="seventh";}
        else if(text.equals("8")){result="eigth";}
        else if(text.equals("9")){result="ninth";}
        return result;
    }
    public static ArrayList<String> findMissing(String a[], String b[], int n, int m) {
        ArrayList<String> fm = new ArrayList<>();
        // Store all elements of
        // second array in a hash table
        HashSet<String> s = new HashSet<>();
        for (int i = 0; i < m; i++)
            s.add(b[i]);
        // Print all elements of first array
        // that are not present in hash table
        for (int i = 0; i < n; i++)
            if (!s.contains(a[i])){
//                Log.e("find_missing", a[i] + " ");
                fm.add(a[i]);
            }
//        assert fm != null;
        return fm;
    }
    public static String expand_dollers(String match){
//        Log.e("expand_dollers: ", match);
        match = match.replace("$", "");
        String[] parts = match.split("\\.");
        if (parts.length > 2){
//            Log.e("expand_dollers: parts", "Activated");
            return match+" dollars";}
        int dollers = 00;
//        Log.e("expand_dollers: Parts", Arrays.toString(parts));
        if (!parts[0].isEmpty()){
            dollers = Integer.parseInt(parts[0]);
        }else{dollers = 00;}
        int cents = 00;
        if(parts.length>1){
            cents = Integer.parseInt(parts[1]);
        }else{cents = 00;}
        String doller_unit = "";
        String cent_unit = "";
        if (dollers > 0 || cents > 0){
            if(dollers==1){
                doller_unit = "dollar";
            }else{doller_unit="dollars";}
            if(cents==1){
                cent_unit = "cent";
            }else{cent_unit="cents";}
            return String.format("%s %s, %s %s." ,dollers,doller_unit,cents, cent_unit);
        }
        else if(dollers > 0){
            if(dollers==1){
                doller_unit = "dollar";
            }else{doller_unit="dollars";}
            return String.format("%s %s." ,dollers, doller_unit);
        }
        else if(cents> 0){
            if(cents==1){
                cent_unit = "cent";
            }else{cent_unit="cents";}
            return String.format("%s %s." ,cents, cent_unit);
        }
        else{return "zero dollars.";}
    }
    public static String remove_commas(Matcher m){
        return m.group(0).replace(",", "");
    }
    static String convert_to_words(char[] num) {
        String result = "";
        // Get number of digits
        // in given number
        int len = num.length;

    /* The first string is not used, it is to make
        array indexing simple */
        String[] single_digits = new String[]{ "zero", "one",
                "two", "three", "four",
                "five", "six", "seven",
                "eight", "nine"};

        // Base cases
        if (len == 0)
        {
            result += "";
            return result;
        }
        if (len > 4)
        {
//            result += "Length more than 4 is not supported";
            int x = 0;
//            Log.e("convert_to_words: NUmber", String.valueOf(num));
            while(x<len){
                result += single_digits[num[x]-'0'];
                x+=1;
            }
            return result;
        }

    /* The first string is not used, it is to make
        array indexing simple */
        String[] two_digits = new String[]{"", "ten", "eleven", "twelve",
                "thirteen", "fourteen",
                "fifteen", "sixteen", "seventeen",
                "eighteen", "nineteen"};

        /* The first two string are not used, they are to make array indexing simple*/
        String[] tens_multiple = new String[]{"", "", "twenty", "thirty", "forty",
                "fifty","sixty", "seventy",
                "eighty", "ninety"};

        String[] tens_power = new String[] {"hundred", "thousand"};

        /* Used for debugging purpose only */
//        System.out.print(String.valueOf(num)+": ");

        /* For single digit number */
        if (len == 1)
        {
            result += single_digits[num[0] - '0'];
        }

    /* Iterate while num
        is not '\0' */
        int x = 0;
        while (x < num.length)
        {

            try{

                /* Code path for first 2 digits */
                if (len >= 3)
                {
                    if (num[x]-'0' != 0)
                    {
//                    Log.e("convert_to_words: Len>3", String.valueOf(num[x]));
                        result += single_digits[num[x] - '0']+" "+ tens_power[len - 3]+" ";
//                    Log.e("convert_to_words: Len>3Re", result);
                        // here len can be 3 or 4
//                    num = removeTheElement(num, x);
                    }
                    --len;
                }

                /* Code path for last 2 digits */
                else
                {
            /* Need to explicitly handle
            10-19. Sum of the two digits
            is used as index of "two_digits"
            array of strings */
                    if (num[x] - '0' == 1)
                    {
                        int sum = num[x] - '0' +
                                num[x] - '0';
                        result += two_digits[sum];
//                    Log.e("convert_to_words: Else", result);
//                    return;
                    }

                    /* Need to explicitely handle 20 */
                    else if (num[x] - '0' == 2 &&
                            num[x + 1] - '0' == 0)
                    {
                        result += "twenty";
//                    return;
                    }

            /* Rest of the two digit
            numbers i.e., 21 to 99 */
                    else
                    {
                        int i = (num[x] - '0');
                        if(i > 0)
                            result += tens_multiple[i]+" ";
                        else {
                            result += "";
                        }
                        ++x;
//                    Log.e("convert_to_words: ", Arrays.toString(num));
                        try{
                            if (num[x] - '0' != 0)
                                result += single_digits[num[x] - '0'];
                        }catch (Exception ex){
//                        result += single_digits[num[0] - '0'];
                        }
                    }
                }
                ++x;
            }catch (Exception ex){
                x = 0;
//            Log.e("convert_to_words: NUmber", String.valueOf(num));
                while(x<len){
                    result += single_digits[num[x]-'0'];
                    x+=1;
                }
                return result;
            }
        }
        Log.e("convert_to_words: REsults", result);
        return result;
    }
}
