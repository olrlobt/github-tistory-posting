package olrlobt.githubtistoryposting.service.platform;

import java.io.IOException;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.RedirectView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olrlobt.githubtistoryposting.domain.Posting;
import olrlobt.githubtistoryposting.domain.PostingType;
import olrlobt.githubtistoryposting.utils.DateUtils;
import olrlobt.githubtistoryposting.utils.ScrapingUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class Anything implements Blog {

    private final ScrapingUtils scrapingUtils;

    @Override
    public Posting posting(String url, int index, PostingType postingType) throws IOException {
        Document document = scrapingUtils.byUrl(url);
        String thumb = document.select("head meta[property=og:image]").attr("content");
        String title = document.select("head meta[property=og:title]").attr("content");
        String content = document.select("head meta[property=og:description]").attr("content");
        String publishedTime = document.select("head meta[property=article:published_time]").attr("content");

        return new Posting(thumb, title, content, DateUtils.parser(publishedTime), url, postingType);
    }

    @Override
    public RedirectView link(String url, int index) {
        return new RedirectView(url);
    }

    @Override
    public Posting blog(String url) throws IOException {
        Document document = scrapingUtils.byUrl(url);
        String thumb = document.select("head meta[property=og:image]").attr("content");
        String title = document.select("head meta[property=og:title]").attr("content");
        return new Posting(thumb, title, "", "", url, PostingType.BlogInfo);
    }
}
