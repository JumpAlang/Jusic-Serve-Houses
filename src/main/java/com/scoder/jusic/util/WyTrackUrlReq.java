package com.scoder.jusic.util;

import cn.hutool.core.util.ReUtil;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

/**
 * @author JumpAlang
 * @create 2023-11-12 15:22
 */
public class WyTrackUrlReq {
    /**
     * '128k': 'standard',
     *         '320k': 'exhigh',
     *         'flac': 'lossless',
     *         'flac24bit': 'hires',
     *         "jyeffect": "jyeffect",
     *         "jysky": "jysky",
     *         "jymaster": "jymaster",
     * @param mid
     * @param quality
     * @return
     */
    public String getTrackUrl(String mid, String quality,String cookies) throws Exception {
        String path = "/api/song/enhance/player/url/v1";
        String params = mid+"?isMv=0&format=%s&br=%s&level=";
        switch (quality) {
            case "320k":
                params = "exhigh";
                break;
            case "flac":
                params = "lossless";
                break;
        }
        String url = "https://interface.music.163.com/eapi/song/enhance/player/url/v1";
        String reqBody = EncryptionUtils.eapiEncrypt(path,"{'ids':['"+mid+"'],'level':'"+params+"','encodeType':'"+"flac'}");
        HttpResponse resp = Unirest.post(url)
                .header("Cookie", cookies)
                .field("params",reqBody)
                .asString();
        String trackUrl = ReUtil.get("\"url\":\"(.*?)\"", resp.getBody().toString(), 1);
        return trackUrl;
    }

    public static void main(String[] args) throws Exception {
//        WyTrackUrlReq wyTrackUrlReq = new WyTrackUrlReq();
//        wyTrackUrlReq.getTrackUrl("1397345903","320k","MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/weapi/feedback;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/weapi/feedback;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/wapi/feedback;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/api/clientlog;;MUSIC_U=0001C3105E416F4F0C71F0312582E1000956B7D968C851F962E32C81EB72DE78C2B53D266C669F4B099E5C0792D041662BB79D2B662DDB19E6D1C015AB048E80E62CD01D447A85580EBCEEB5CDDC6CEE3BA2928977ADC9720A402A7668E747838A2495152C65F4261B66B567EF9C3D442887CA9BB828B6D019E5CC70CD5FC8C520F702DE7B0E7C966CE3BE8668A2C8C5396909DAFDFE57931652A53CC26678079C5752F54AA18B2F4AB04E853BF4F2F1B4F8F56EC6694419C7E2ACDC10117660AC2A28923366F59486FBD61CD43B6D6ABF050E4215F44A6E75BEB13AA8BAF46987714A7F276493B3A710FB08ADB8BB50F74F85EEA0A1BC2F60A425E104B7FF955D064CC12B38A4441FBC950364ED8B873BCCE25A80A38EFEA18076CE7F7AB6AF76E0BCEF42C2535610AF06A984047043432B1A1B25EA65D01393E2CA5E68AA9858; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/eapi/feedback;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/api/clientlog;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/eapi/feedback;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/openapi/clientlog;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/openapi/clientlog;;__remember_me=true; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/api/feedback;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/neapi/clientlog;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/neapi/feedback;;MUSIC_SNS=; Max-Age=0; Expires=Sun, 12 Nov 2023 09:07:14 GMT; Path=/;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/wapi/feedback;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/api/feedback;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/weapi/clientlog;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/neapi/clientlog;;__csrf=ff2a2ea66b856b82bca9362d16fb7dcb; Max-Age=1296010; Expires=Mon, 27 Nov 2023 09:07:24 GMT; Path=/;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/neapi/feedback;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/wapi/clientlog;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/wapi/clientlog;;MUSIC_R_T=1611480304597; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/eapi/clientlog;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/weapi/clientlog;;MUSIC_A_T=1611480304585; Max-Age=2147483647; Expires=Fri, 30 Nov 2091 12:21:21 GMT; Path=/eapi/clientlog;");
        String url =  "/api/song/enhance/player/url/v1";
        String text = "{'a':'a'}";
//        byte[] result = EncryptionUtils.addPadding(text.getBytes());
//        System.out.println(Arrays.toString(result));
        String result = EncryptionUtils.eapiEncrypt(url,text);
        System.out.println(result);
    }
}
