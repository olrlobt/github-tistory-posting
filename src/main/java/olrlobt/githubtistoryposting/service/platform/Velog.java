package olrlobt.githubtistoryposting.service.platform;

import java.io.IOException;
import java.util.Map;
import olrlobt.githubtistoryposting.domain.Posting;
import olrlobt.githubtistoryposting.domain.PostingBase;
import olrlobt.githubtistoryposting.domain.Watermark;
import olrlobt.githubtistoryposting.utils.DateUtils;
import olrlobt.githubtistoryposting.utils.UrlUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.view.RedirectView;

@Component
public class Velog implements Blog {

    private final WebClient WEB_CLIENT = WebClient.builder()
            .baseUrl("https://v2.velog.io/graphql")
            .build();

    private final String QUERY_POSTING =
            "query Posts($username: String, $limit: Int) { posts(username: $username, limit: $limit) "
                    + "{url_slug title thumbnail released_at comments_count tags likes}}";
    private final String QUERY_LINK = "query Posts($username: String, $limit: Int) { posts(username: $username, limit: $limit) { url_slug }}";
    private final String QUERY_BLOG = "query User($username: String) {user(username: $username) { username profile {  thumbnail }}}";
    @Value("classpath:static/img/velog.svg")
    private Resource watermark;


    @Override
    public Posting posting(String blogName, int index, PostingBase postingBase) throws IOException {
        Map<String, Object> variables = Map.of(
                "username", blogName,
                "limit", index + 1
        );

        VelogResponse response = request(QUERY_POSTING, variables);
        VelogResponse.Post post = response.getData().getPosts().get(index);
        String encodedUrlSlug = UrlUtils.decodeByKorean(post.getUrl_slug());
        Posting posting = new Posting(post.getThumbnail(), post.getTitle(), "", DateUtils.parser(post.getReleased_at()),
                encodedUrlSlug,
                postingBase);
        posting.setWatermark(new Watermark(watermark, "#5fc69a"));
        posting.setSiteName(blogName + ".log");
        return posting;
    }

    @Override
    public RedirectView link(String blogName, int index) {
        Map<String, Object> variables = Map.of(
                "username", blogName,
                "limit", index + 1
        );

        VelogResponse response = request(QUERY_LINK, variables);
        VelogResponse.Post post = response.getData().getPosts().get(index);
        String encodedUrlSlug = UrlUtils.encodeByKorean(post.getUrl_slug());
        return new RedirectView(createVelog(blogName, encodedUrlSlug));
    }

    @Override
    public Posting blog(String blogName) {
        Map<String, Object> variables = Map.of(
                "username", blogName
        );

        VelogResponse user = request(QUERY_BLOG, variables);
        String username = user.getData().getUser().getUsername();
        String thumbnail = user.getData().getUser().getProfile().getThumbnail();
        if (thumbnail != null) {
            String location = thumbnail.substring(0, thumbnail.lastIndexOf("/"));
            String param = UrlUtils.encodeByKorean(thumbnail.substring(thumbnail.lastIndexOf("/")));
            return new Posting(location + param, username, "", "", createVelog(blogName, ""), PostingBase.BlogInfo);
        }
        return new Posting(thumbnail, username, "", "", createVelog(blogName, ""), PostingBase.BlogInfo);
    }

    private VelogResponse request(String query, Map<String, Object> variables) {
        return WEB_CLIENT.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("query", query, "variables", variables))
                .retrieve()
                .bodyToMono(VelogResponse.class)
                .block();
    }

    private String createVelog(String blogName, String urlSlug) {
        return "https://velog.io/@" + blogName + "/" + urlSlug;
    }
}
