package olrlobt.githubtistoryposting.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.RedirectView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olrlobt.githubtistoryposting.domain.Posting;
import olrlobt.githubtistoryposting.service.platform.Blog;
import olrlobt.githubtistoryposting.service.platform.BlogFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostingService {

	private final BlogFactory blogFactory;

	public Posting posting(String blogName, String platform, int index) throws IOException {
		Blog blog = blogFactory.getBlog(platform);
		return blog.posting(blogName, index);
	}

	public RedirectView link(String blogName, String platform, int index) throws IOException {
		Blog blog = blogFactory.getBlog(platform);
		return blog.link(blogName, index);
	}

	public Posting blog(String blogName, String platform) throws IOException {
		Blog blog = blogFactory.getBlog(platform);
		return blog.blog(blogName);
	}

	public Posting anything(String url, String theme) throws IOException {
		if (theme != null && theme.equals("b")) {
			return blog(url, "else");
		}
		return posting(url, "else", 0);
	}
}