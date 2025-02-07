/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.micrometer.core.instrument.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsContributor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultWebMvcTagsProvider}.
 *
 * @author Andy Wilkinson
 */
@SuppressWarnings("removal")
class DefaultWebMvcTagsProviderTests {

	@Test
	void whenTagsAreProvidedThenDefaultTagsArePresent() {
		Map<String, Tag> tags = asMap(new DefaultWebMvcTagsProvider().getTags(null, null, null, null));
		assertThat(tags).containsOnlyKeys("exception", "method", "outcome", "status", "uri");
	}

	@Test
	void givenSomeContributorsWhenTagsAreProvidedThenDefaultTagsAndContributedTagsArePresent() {
		Map<String, Tag> tags = asMap(
				new DefaultWebMvcTagsProvider(Arrays.asList(new TestWebMvcTagsContributor("alpha"),
						new TestWebMvcTagsContributor("bravo", "charlie"))).getTags(null, null, null, null));
		assertThat(tags).containsOnlyKeys("exception", "method", "outcome", "status", "uri", "alpha", "bravo",
				"charlie");
	}

	@Test
	void whenLongRequestTagsAreProvidedThenDefaultTagsArePresent() {
		Map<String, Tag> tags = asMap(new DefaultWebMvcTagsProvider().getLongRequestTags(null, null));
		assertThat(tags).containsOnlyKeys("method", "uri");
	}

	@Test
	void givenSomeContributorsWhenLongRequestTagsAreProvidedThenDefaultTagsAndContributedTagsArePresent() {
		Map<String, Tag> tags = asMap(
				new DefaultWebMvcTagsProvider(Arrays.asList(new TestWebMvcTagsContributor("alpha"),
						new TestWebMvcTagsContributor("bravo", "charlie"))).getLongRequestTags(null, null));
		assertThat(tags).containsOnlyKeys("method", "uri", "alpha", "bravo", "charlie");
	}

	@Test
	void trailingSlashIsIncludedByDefault() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/the/uri/");
		request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "{one}/{two}/");
		Map<String, Tag> tags = asMap(new DefaultWebMvcTagsProvider().getTags(request, null, null, null));
		assertThat(tags.get("uri").getValue()).isEqualTo("{one}/{two}/");
	}

	@Test
	void trailingSlashCanBeIgnored() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/the/uri/");
		request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "{one}/{two}/");
		Map<String, Tag> tags = asMap(new DefaultWebMvcTagsProvider(true).getTags(request, null, null, null));
		assertThat(tags.get("uri").getValue()).isEqualTo("{one}/{two}");
	}

	private Map<String, Tag> asMap(Iterable<Tag> tags) {
		return StreamSupport.stream(tags.spliterator(), false)
				.collect(Collectors.toMap(Tag::getKey, Function.identity()));
	}

	private static final class TestWebMvcTagsContributor implements WebMvcTagsContributor {

		private final List<String> tagNames;

		private TestWebMvcTagsContributor(String... tagNames) {
			this.tagNames = Arrays.asList(tagNames);
		}

		@Override
		public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler,
				Throwable exception) {
			return this.tagNames.stream().map((name) -> Tag.of(name, "value")).toList();
		}

		@Override
		public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
			return this.tagNames.stream().map((name) -> Tag.of(name, "value")).toList();
		}

	}

}
