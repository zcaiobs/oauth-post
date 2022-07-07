package com.example.oauthpost.api;


import com.example.oauthpost.client.UserReactiveClient;
import com.example.oauthpost.domain.Post;
import com.example.oauthpost.domain.PostRepository;
import com.example.oauthpost.security.CanWritePosts;
import com.example.oauthpost.security.SecurityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/posts")
public class PostController {
	
	private final PostRepository postRepository;
	private final SecurityService securityService;
	private final UserReactiveClient userClient;

	public PostController(PostRepository postRepository, 
	                      SecurityService securityService,
						  UserReactiveClient userClient) {
		this.postRepository = postRepository;
		this.securityService = securityService;
		this.userClient = userClient;
	}

	@GetMapping
	public Page<PostSummaryResponse> findAll(Pageable pageable) {
		final Page<Post> postPage = postRepository.findAll(pageable);
		final var postResponses = postPage.getContent()
				.stream()
				.map(PostSummaryResponse::of)
				.collect(Collectors.toList());
		return new PageImpl<>(postResponses, pageable, postPage.getTotalElements());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@CanWritePosts
	public PostDetailedResponse create(@RequestBody @Valid PostRequest postRequest) {
		final Post post = new Post(securityService.getUserId(), postRequest.getTitle(), postRequest.getContent());
		postRepository.save(post);
		return userClient.findById(post.getEditorId())
				.map(userResponse -> PostDetailedResponse.of(post, EditorResponse.of(userResponse)))
				.blockOptional()
				.orElseGet(() -> PostDetailedResponse.of(post));
	}

	@GetMapping("/{id}")
	public PostDetailedResponse findById(@PathVariable Long id) {
		final Post post = postRepository.findById(id).orElseThrow(ResourceNotFoundException::new);

		return userClient.findById(post.getEditorId())
				.map(userResponse -> PostDetailedResponse.of(post, EditorResponse.of(userResponse)))
				.blockOptional()
				.orElseGet(() -> PostDetailedResponse.of(post));
	}
}
