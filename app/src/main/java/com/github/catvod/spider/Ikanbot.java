package com.github.catvod.spider;


import android.text.TextUtils;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;





import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;


public class Ikanbot extends Spider {


    private static final String siteUrl = "https://www.ikanbot.com";


    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }


    private HashMap<String, String> Headers() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/116.0");
        headers.put("cookie","ipLoc=CN");
        headers.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        headers.put("Referer","https://www.ikanbot.com/");
        headers.put("Accept-Encoding","gzip, deflate, br");
        headers.put("Host","www.ikanbot.com");

        return headers;
    }


    public String homeContent(boolean filter) {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("div.jumbotron > div > ul > li > a")) {
            if (element.attr("href").startsWith("/hot")) {
                String id = element.attr("href").split("-")[1];
                String name = element.text();
                classes.add(new Class(id, name));
            }
        }
        for (Element element : doc.select("div.col-xs-4 > a")) {
            String img = element.select("div:nth-child(1) > img:nth-child(1)").attr("src");
            String name = element.select("p").text();
            String id = element.attr("href").replaceAll("\\D+","");
            list.add(new Vod(id, name, img));
        }
        return Result.string(classes, list);

    }


    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        List<Vod> list = new ArrayList<>();
        String target = siteUrl + String.format("/hot/index-%s-热门.html", tid);
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("div.col-xs-4 > a")) {
            String img = element.select("div:nth-child(1) > img:nth-child(1)").attr("src");
            String name = element.select("p").text();
            String id = element.attr("href").replaceAll("\\D+","");
            list.add(new Vod(id, name, img));
        }
        return Result.string(list);
    }




    public String detailContent(List<String> ids) throws JSONException {
        Document doc = Jsoup.parse(OkHttp.string(siteUrl.concat("/play/").concat(ids.get(0)), getHeaders()));
        String name = doc.select("h2.meta").text();
        String img = doc.select(".cover").attr("src");
        String type = doc.select("h3.meta:nth-child(4)").text();
        String actor = doc.select("h3.meta:nth-child(5)").text();

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(img);
        vod.setVodName(name);
        vod.setVodActor(actor);
        vod.setTypeName(type);



        Map<String, String> sites = new LinkedHashMap<>();
        String DetaInfo = OkHttp.string(siteUrl.concat("/api/getResN?videoId=").concat(ids.get(0)), getHeaders());
        JSONObject JSON = new JSONObject(DetaInfo);
        JSONArray Data = JSON.getJSONObject("data").getJSONArray("list");
        for (int i = 0; i < Data.length(); i++) {
            JSONObject item = Data.getJSONObject(i);
            String resData = item.getString("resData");
            JSONArray data = new JSONArray(resData);
            JSONObject Player = data.getJSONObject(0);
            String Pname = Player.getString("flag");
            String Purl = Player.getString("url");
            sites.put(Pname, Purl);
        }
        if (sites.size() > 0) {
            vod.setVodPlayFrom(TextUtils.join("$$$", sites.keySet()));
            vod.setVodPlayUrl(TextUtils.join("$$$", sites.values()));
        }

        return Result.string(vod);
    }



    public String searchContent(String key, boolean quick) {
        List<Vod> list = new ArrayList<>();
        String target = siteUrl.concat("/search?q=").concat(key);
        Document doc = Jsoup.parse(OkHttp.string(target, Headers()));
        for (Element element : doc.select("div.media")) {
            String img = element.select("div:nth-child(1) > a:nth-child(1) > img:nth-child(1)").attr("src");
            String name = element.select("div:nth-child(2) > h5:nth-child(1) > a:nth-child(1)").text();
            String id = element.select("div:nth-child(1) > a:nth-child(1)").attr("href").replaceAll("\\D+","");
            list.add(new Vod(id, name, img));
        }
        return Result.string(list);
    }

    public String playerContent(String flag, String id, List<String> vipFlags) {
        return Result.get().url(id).parse().header(getHeaders()).string();
    }
}

