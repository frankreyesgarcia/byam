package com.jcabi.http.response;

import com.jcabi.aspects.Immutable;
import com.jcabi.http.Request;
import com.jcabi.http.Response;
import com.jcabi.log.Logger;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import lombok.EqualsAndHashCode;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

/**
 * REST response.
 *
 * <p>This response decorator is able to make basic assertions on
 * HTTP response and manipulate with it afterwords, for example:
 *
 * <pre> String name = new JdkRequest("http://my.example.com")
 *   .fetch()
 *   .as(RestResponse.class)
 *   .assertStatus(200)
 *   .assertBody(Matchers.containsString("hello, world!"))
 *   .assertHeader("Content-Type", Matchers.hasItem("text/plain"))
 *   .jump(URI.create("/users"))
 *   .fetch();</pre>
 *
 * <p>Method {@link #jump(URI)} creates a new instance of class
 * {@link Request} with all cookies transferred from the current one.
 *
 * <p>The class is immutable and thread-safe.
 *
 * @since 0.8
 */
@Immutable
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("PMD.TooManyMethods")
public final class RestResponse extends AbstractResponse {

    /**
     * Public ctor.
     * @param resp Response
     */
    public RestResponse(final Response resp) {
        super(resp);
    }

    /**
     * Assert using custom matcher.
     * @param matcher The matcher to use
     * @return The same object
     */
    public RestResponse assertThat(final Matcher<Response> matcher) {
        MatcherAssert.assertThat(
            String.format("HTTP response is not valid: %s", this),
            this,
            matcher
        );
        return this;
    }

    /**
     * Verifies HTTP response status code against the provided absolute value,
     * and throws {@link AssertionError} in case of mismatch.
     * @param status Expected status code
     * @return The same object
     */
    public RestResponse assertStatus(final int status) {
        final String message = String.format(
            "HTTP response with status %d", status
        );
        MatcherAssert.assertThat(
            String.format(
                "HTTP response status is not equal to %d:%n%s",
                status, this
            ),
            this,
            new RestResponse.StatusMatch(message, status)
        );
        return this;
    }

    /**
     * Verifies HTTP response status code against the provided matcher,
     * and throws {@link AssertionError} in case of mismatch.
     * @param matcher Matcher to validate status code
     * @return This object
     */
    public RestResponse assertStatus(final Matcher<Integer> matcher) {
        MatcherAssert.assertThat(
            String.format(
                "HTTP response status is not the one expected:%n%s",
                this
            ),
            this.status(), matcher
        );
        return this;
    }

    /**
     * Verifies HTTP response body content against provided matcher,
     * and throws {@link AssertionError} in case of mismatch.
     * @param matcher The matcher to use
     * @return This object
     */
    public RestResponse assertBody(final Matcher<String> matcher) {
        MatcherAssert.assertThat(
            String.format(
                "HTTP response body content is not valid:%n%s",
                this
            ),
            this.body(), matcher
        );
        return this;
    }

    /**
     * Verifies HTTP response body content against provided matcher,
     * and throws {@link AssertionError} in case of mismatch.
     * @param matcher The matcher to use
     * @return This object
     */
    public RestResponse assertBinary(final Matcher<byte[]> matcher) {
        MatcherAssert.assertThat(
            String.format(
                "HTTP response binary content is not valid:%n%s",
                this
            ), this.binary(),
            matcher
        );
        return this;
    }

    /**
     * Verifies HTTP header against provided matcher, and throws
     * {@link AssertionError} in case of mismatch.
     *
     * <p>The iterator for the matcher will always be a real object an never
     * {@code NULL}, even if such a header is absent in the response. If the
     * header is absent the iterable will be empty.
     *
     * @param name Name of the header to match
     * @param matcher The matcher to use
     * @return This object
     */
    public RestResponse assertHeader(
        final String name,
        final Matcher<Iterable<String>> matcher
    ) {
        Iterable<String> values = this.headers().get(name);
        if (values == null) {
            values = Collections.emptyList();
        }
        MatcherAssert.assertThat(
            String.format(
                "HTTP header '%s' is not valid:%n%s",
                name, this
            ),
            values, matcher
        );
        return this;
    }

    /**
     * Verifies HTTP header against provided matcher, and throws
     * {@link AssertionError} in case of mismatch.
     * @param name Name of the header to match
     * @param value The value to expect in one of the headers
     * @return This object
     * @since 0.9
     */
    public RestResponse assertHeader(final String name, final String value) {
        return this.assertHeader(name, Matchers.hasItems(value));
    }

    /**
     * Jump to a new location.
     * @param uri Destination to jump to
     * @return New request
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Request jump(final URI uri) {
        Request req = this.back().uri()
            .set(this.back().uri().get().resolve(uri))
            .back();
        final Map<String, List<String>> headers = this.headers();
        if (headers.containsKey(HttpHeaders.SET_COOKIE)) {
            for (final String header : headers.get(HttpHeaders.SET_COOKIE)) {
                for (final HttpCookie cookie : HttpCookie.parse(header)) {
                    req = req.header(
                        HttpHeaders.COOKIE,
                        String.format(
                            "%s=%s", cookie.getName(), cookie.getValue()
                        )
                    );
                }
            }
        }
        return req;
    }

    /**
     * Follow LOCATION header.
     * @return New request
     */
    public Request follow() {
        this.assertHeader(
            HttpHeaders.LOCATION,
            Matchers.not(Matchers.emptyIterableOf(String.class))
        );
        return this.jump(
            URI.create(this.headers().get(HttpHeaders.LOCATION).get(0))
        );
    }

    /**
     * Get one cookie by name.
     * @param name Cookie name
     * @return Cookie found
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Cookie cookie(final String name) {
        final Map<String, List<String>> headers = this.headers();
        MatcherAssert.assertThat(
            "cookies should be set in HTTP header",
            headers.containsKey(HttpHeaders.SET_COOKIE)
        );
        final List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        final Iterator<String> iterator = cookies.iterator();
        Cookie cookie = null;
        while (iterator.hasNext()) {
            final String obj = iterator.next();
            for (final HttpCookie candidate : HttpCookie.parse(obj)) {
                if (candidate.getName().equals(name)) {
                    cookie = RestResponse.cookie(candidate);
                    break;
                }
            }
        }
        MatcherAssert.assertThat(
            Logger.format(
                "cookie '%s' not found in Set-Cookie header: '%s'",
                name,
                cookies
            ),
            cookie,
            Matchers.notNullValue()
        );
        assert cookie != null;
        return cookie;
    }

    /**
     * Convert HTTP cookie to a standard one.
     * @param cookie HTTP cookie
     * @return Regular one
     */
    private static Cookie cookie(final HttpCookie cookie) {
        return new Cookie(
            cookie.getName(),
            cookie.getValue(),
            cookie.getPath(),
            cookie.getDomain(),
            cookie.getVersion()
        );
    }

    /**
     * Status matcher.
     *
     * @since 1.2
     */
    private static final class StatusMatch extends CustomMatcher<Response> {

        /**
         * HTTP status to check.
         */
        private final transient int status;

        /**
         * Ctor.
         * @param msg Message to show
         * @param sts HTTP status to check
         */
        StatusMatch(final String msg, final int sts) {
            super(msg);
            this.status = sts;
        }

        public boolean matches(final Object resp) {
            return Response.class.cast(resp).status() == this.status;
        }
    }

    private static final class Matchers {
        static <T> Matcher<Iterable<T>> hasItems(final T... items) {
            return new CustomMatcher<Iterable<T>>(
                "iterable containing items " + Arrays.toString(items)
            ) {
                @Override
                public boolean matches(final Object item) {
                    if (!(item instanceof Iterable)) {
                        return false;
                    }
                    Iterable<?> iterable = (Iterable<?>) item;
                    for (T element : items) {
                        boolean found = false;
                        for (Object actual : iterable) {
                            if ((element == null && actual == null) ||
                                (element != null && element.equals(actual))) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }

        static <T> Matcher<Iterable<T>> emptyIterableOf(final Class<T> clazz) {
            return new CustomMatcher<Iterable<T>>("an empty iterable") {
                @Override
                public boolean matches(final Object item) {
                    if (!(item instanceof Iterable)) {
                        return false;
                    }
                    return !((Iterable<?>) item).iterator().hasNext();
                }
            };
        }

        static <T> Matcher<T> notNullValue() {
            return new CustomMatcher<T>("not null") {
                @Override
                public boolean matches(final Object item) {
                    return item != null;
                }
            };
        }

        static <T> Matcher<T> not(final Matcher<T> matcher) {
            return new CustomMatcher<T>("not " + matcher.toString()) {
                @Override
                public boolean matches(final Object item) {
                    return !matcher.matches(item);
                }
            };
        }
    }
}