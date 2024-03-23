package olrlobt.githubtistoryposting.service.platform;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.view.RedirectView;

import olrlobt.githubtistoryposting.domain.Posting;
import olrlobt.githubtistoryposting.utils.DateUtils;
import olrlobt.githubtistoryposting.utils.UrlUtils;

@Component
public class Velog implements Blog {

	private final WebClient webClient = WebClient.builder()
		.baseUrl("https://v2.velog.io/graphql")
		.build();

	@Override
	public Posting posting(String blogName, int index) {
		String query =
			"query Posts($cursor: ID, $username: String, $temp_only: Boolean, $tag: String, $limit: Int) {\n" +
				"posts(cursor: $cursor, username: $username, temp_only: $temp_only, tag: $tag, limit: $limit) {title"
				+ " thumbnail user { username profile{thumbnail}} url_slug released_at comments_count tags likes}}";

		Map<String, Object> variables = Map.of(
			"username", blogName,
			"limit", index + 1
		);

		VelogResponse response = webClient.post()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("query", query, "variables", variables))
			.retrieve()
			.bodyToMono(VelogResponse.class)
			.block();

		VelogResponse.Post post = response.getData().getPosts().get(index);
		return new Posting(post.getThumbnail(), post.getTitle(), DateUtils.parser(post.getReleased_at()));
	}

	@Override
	public RedirectView link(String blogName, int index) {
		String query = "query Posts($username: String, $limit: Int) { posts(username: $username, limit: $limit) { url_slug }}";

		Map<String, Object> variables = Map.of(
			"username", blogName,
			"limit", index + 1
		);

		VelogResponse response = webClient.post()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("query", query, "variables", variables))
			.retrieve()
			.bodyToMono(VelogResponse.class)
			.block();

		VelogResponse.Post post = response.getData().getPosts().get(index);

		String encodedUrlSlug = UrlUtils.encodeByKorean(post.getUrl_slug());
		return new RedirectView(createVelog(blogName, encodedUrlSlug));
	}

	@Override
	public Posting blog(String blogName) {
		String query = "query User($username: String) {user(username: $username) { username profile {  thumbnail }}}";

		Map<String, Object> variables = Map.of(
			"username", blogName
		);

		VelogResponse user = webClient.post()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(Map.of("query", query, "variables", variables))
			.retrieve()
			.bodyToMono(VelogResponse.class)
			.block();

		String username = user.getData().getUser().getUsername();
		String thumbnail = user.getData().getUser().getProfile().getThumbnail();
		String location = thumbnail.substring(0, thumbnail.lastIndexOf("/"));
		String param = UrlUtils.encodeByKorean(thumbnail.substring(thumbnail.lastIndexOf("/")));
		return new Posting(location + param, username, createVelog(blogName,""));
	}

	private String createVelog(String blogName, String urlSlug){
		return "https://velog.io/@" + blogName + "/" + urlSlug;
	}
}
