package app.link;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.link.dto.CreateLinkRequest;
import app.link.dto.LinkResponse;

/** API de enlaces (contrato: docs/api/openapi.yaml). */
@RestController
@RequestMapping("/api/v1/links")
public class LinkController {

	private final LinkService linkService;

	public LinkController(LinkService linkService) {
		this.linkService = linkService;
	}

	@PostMapping
	public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
		Link link = linkService.create(request.url());
		String shortUrl = linkService.shortUrlOf(link);
		return ResponseEntity.created(URI.create(shortUrl)).body(LinkResponse.of(link, shortUrl));
	}
}
