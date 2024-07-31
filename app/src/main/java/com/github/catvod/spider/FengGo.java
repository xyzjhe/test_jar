package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FengGo extends Spider {
    private static String siteUrl ="http://m.fenggoudy2.com";

    private HashMap<String, String> getHeader() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        if (!extend.isEmpty()) {
            siteUrl = extend;
        }
    }

    public String homeContent(boolean filter) {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeader()));
        for (Element element : doc.select("div.page-header > h2 > a")) {
            if (element.attr("href").startsWith("/list-read")) {
                String id = element.attr("href").replaceAll("\\D+", "");
                String name = element.text();
                classes.add(new Class(id, name));
            }
        }
        for (Element element : doc.select("div.container > ul:nth-child(2) > li")) {
            String img = element.select("p.image > a > img").attr("data-original");
            String name = element.select("h2 > a").attr("title");
            String remark = element.select("p.image > a > span").text();
            String id = element.select("h2 > a").attr("href").replaceAll("\\D+","");
            list.add(new Vod(id, name, img, remark));
        }
        return Result.string(classes, list);
    }

    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        List<Vod> list = new ArrayList<>();
        String target = siteUrl + String.format("/list-select-id-%s-type--area--year--star--state--order-addtime-p-%s.html", tid, pg);
        Document doc = Jsoup.parse(OkHttp.string(target, getHeader()));
        for (Element element : doc.select("li.col-xs-4")) {
            if (list.size() > 11) {
                break;
            }
            String img = element.select("p.image > a > img").attr("data-original");
            String name = element.select("h2 > a").attr("title");
            String remark = element.select("p.image > a > span").text();
            String id = element.select("h2 > a").attr("href").replaceAll("\\D+","");
            list.add(new Vod(id, name, img, remark));
        }
        return Result.string(list);
    }

    public String detailContent(List<String> ids) {
        Document doc = Jsoup.parse(OkHttp.string(siteUrl.concat("/vod-read-id-").concat(ids.get(0)), getHeader()));
        String name = doc.select("a.ff-text").text();
        String remarks = doc.select("h2.text-nowrap:nth-child(1) > small:nth-child(2)").text();
        String img = doc.select(".media-object").attr("data-original");
        String type = doc.select("dd.ff-text-right:nth-child(6)").text();
        String actor = doc.select("dd.ff-text-right:nth-child(2)").text();
        String content = doc.select("div.tab-pane:nth-child(2)").text();
        String director = doc.select("dd.ff-text-right:nth-child(4) > a:nth-child(1)").text();
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(img);
        vod.setVodName(name);
        vod.setVodActor(actor);
        vod.setVodRemarks(remarks);
        vod.setVodContent(content);
        vod.setVodDirector(director);
        vod.setTypeName(type);

        Map<String, String> sites = new LinkedHashMap<>();
        Elements sources = doc.select("div.page-header > h2");
        Elements sourceList = doc.select("ul.row");
        for (int i = 0; i < sources.size() - 2; i++) {
            Element source = sources.get(i);
            String sourceName = source.text();
            Elements playList = sourceList.get(i).select("a");
            List<String> vodItems = new ArrayList<>();
            for (int j = 0; j < playList.size(); j++) {
                Element e = playList.get(j);
                vodItems.add(e.text() + "$" + e.attr("href"));
            }
            if (vodItems.size() > 0) {
                sites.put(sourceName, TextUtils.join("#", vodItems));
            }
        }
        if (sites.size() > 0) {
            vod.setVodPlayFrom(TextUtils.join("$$$", sites.keySet()));
            vod.setVodPlayUrl(TextUtils.join("$$$", sites.values()));
        }
        return Result.string(vod);
    }

    public String searchContent(String key, boolean quick) {
        List<Vod> list = new ArrayList<>();
        String target = siteUrl.concat("/vod-search-wd-").concat(key);
        Document doc = Jsoup.parse(OkHttp.string(target, getHeader()));
        for (Element element : doc.select("li.col-xs-4")) {
            String img = element.select("p:nth-child(1) > a:nth-child(1) > img:nth-child(1)").attr("data-original");
            String name = element.select("h2:nth-child(2) > a:nth-child(1)").text();
            String remark = element.select("p:nth-child(1) > a:nth-child(1) > span:nth-child(2)").text();
            String id = element.select("h2:nth-child(2) > a:nth-child(1)").attr("href").replaceAll("\\D+","");
            list.add(new Vod(id, name, img, remark));
        }
        return Result.string(list);
    }

    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String html = OkHttp.string(siteUrl + id, getHeader());
        String regex = "var cms_player = ([^<]+)</script>";
        Matcher matcher = Pattern.compile(regex).matcher(html);
        if (!matcher.find()) {
            return Result.get().url(siteUrl + id).parse().header(getHeader()).string();
        }
        String group = matcher.group(1);
        JSONObject player = new JSONObject(group);
        String url = player.getString("url");
        if (url.contains(".m3u8") || url.contains(".mp4")) {
            return Result.get().url(url).header(getHeader()).string();
        }
        if (url.contains(".html")) {
            return Result.get().url(siteUrl + id).parse().header(getHeader()).string();
        }
        String secUrl2 = "https://pl.qcheng.cc/jxpc.php?url=" + url;
        String html2 = OkHttp.string(secUrl2);
        String realUrl = StringUtils.substringBetween(html2, "url: '", "',");
        return Result.get().url(realUrl).header(getHeader()).string();
    }
}
