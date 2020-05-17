package com.scoder.jusic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author alang
 * @create 2020-01-18 23:07
 */
public class TestSum {
    public static void main(String[] args) {
        String s ="联杰2万+3万\n" +
                "狼杰5万+5万+3.5万\n" +
                "浚迪4万\n" +
                "王晓义12000+14000+4万+1.1万\n" +
                "连超群14000\n" +
                "婷云2988\n" +
                "联杰5万+5万\n" +
                "吴杰彬10000\n" +
                "嘉峰10万+4万\n" +
                "瑞牙5万+5万\n" +
                "少裕10万+5万\n" +
                "\n" +
                "未结SU\n" +
                "杨晓伟15000+11000+21+300\n" +
                "场自强15000+1.8万+3万+2万+2.4万\n" +
                "\n" +
                "王志超6306\n" +
                "王惠军1.6万+1.5万\n" +
                "\n" +
                "\n" +
                "\n" +
                "新群\n" +
                "林晓军8744\n" +
                "黄佳红5950\n" +
                "陈志元14000+8997\n" +
                "张浚榕8251\n";
        s=s.replace("万","*10000");
//        System.out.println(s);
        String reg = "[\u4e00-\u9fa5]";
        Pattern pat = Pattern.compile(reg);
        Matcher m=pat.matcher(s);
        s=m.replaceAll("");
        s=s.replaceAll("[a-zA-Z]","");
//        System.out.println(s);
         s = s.replaceAll("\n|\r|\\s","+");
         if(s.substring(s.length()-1).equals("+")){
             System.out.println(s.substring(0,s.length()-1));
         }
         String[] ss = s.split("\\+");
         Double sum = 0.0;
         for(String sss : ss){
             String[] ssss = sss.split("\\*");
             if(ssss.length != 1){
                 sum += Double.valueOf(ssss[0])*10000;
             }else{
                 if(!"".equals(ssss[0])){
                     sum += Double.valueOf(ssss[0]);
                 }
             }
         }
        System.out.println(sum);
    }
}
