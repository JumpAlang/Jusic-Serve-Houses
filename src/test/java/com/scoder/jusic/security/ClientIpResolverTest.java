package com.scoder.jusic.security;

import com.scoder.jusic.configuration.TrustedProxyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void forwardedClientIsUsedWithoutCheckingPeerWhitelist() {
        ClientIpResolver resolver = resolver(true, "127.0.0.1");
        MockHttpServletRequest request = requestFrom("203.0.113.9");
        request.addHeader("X-Forwarded-For", "198.51.100.21");
        request.addHeader("X-Real-IP", "198.51.100.22");

        assertEquals("198.51.100.21", resolver.resolve(request));
    }

    @Test
    void proxyChainResolvesLeftmostClient() {
        ClientIpResolver resolver = resolver(true, "127.0.0.1", "10.0.0.2", "10.0.0.3");
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.25, 10.0.0.2, 10.0.0.3");

        assertEquals("198.51.100.25", resolver.resolve(request));
    }

    @Test
    void leftmostValidAddressTakesPrecedenceOverIntermediateHops() {
        ClientIpResolver resolver = resolver(true, "127.0.0.1", "10.0.0.2");
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For", "192.0.2.99, 198.51.100.40, 10.0.0.2");

        assertEquals("192.0.2.99", resolver.resolve(request));
    }

    @Test
    void disablingForwardHeadersUsesNetworkPeerEvenWhenItIsConfiguredAsTrusted() {
        ClientIpResolver resolver = resolver(false, "127.0.0.1");
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.25");

        assertEquals("127.0.0.1", resolver.resolve(request));
    }

    @Test
    void validRealIpIsFallbackWhenTrustedPeerSuppliesNoUsableForwardedChain() {
        ClientIpResolver resolver = resolver(true, "127.0.0.1");
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For", "attacker.example.invalid");
        request.addHeader("X-Real-IP", "2001:db8::8");

        assertEquals("2001:db8::8", resolver.resolve(request));
    }

    @Test
    void dockerGatewayNeedsNoConfiguredProxyWhitelist() {
        ClientIpResolver resolver = resolver(true);
        MockHttpServletRequest request = requestFrom("172.18.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.25");

        assertEquals("198.51.100.25", resolver.resolve(request));
    }

    @Test
    void missingOrInvalidForwardedHeadersFallBackToPeer() {
        ClientIpResolver resolver = resolver(true);
        MockHttpServletRequest request = requestFrom("172.18.0.1");
        assertEquals("172.18.0.1", resolver.resolve(request));
        request.addHeader("X-Forwarded-For", "unknown, invalid");
        request.addHeader("X-Real-IP", "invalid");
        assertEquals("172.18.0.1", resolver.resolve(request));
    }

    @Test
    void hexadecimalWordIsNotAcceptedAsAnIpAddress() {
        ClientIpResolver resolver = resolver(true, "127.0.0.1");
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For", "deadbeef");
        request.addHeader("X-Real-IP", "198.51.100.30");

        assertEquals("198.51.100.30", resolver.resolve(request));
    }

    @Test
    void malformedIpv4AndIpv6EntriesAreSkippedWithoutNameResolution() {
        ClientIpResolver resolver = resolver(true, "127.0.0.1");
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        request.addHeader("X-Forwarded-For",
                "1.2.3, 1.2.3.999, 192.0.2.01, 2001:::7");
        request.addHeader("X-Real-IP", "203.0.113.7");

        assertEquals("203.0.113.7", resolver.resolve(request));
    }

    @Test
    void ipv6CompressionAndEmbeddedIpv4AreCanonicalized() {
        ClientIpResolver resolver = resolver(true, "127.0.0.1");
        MockHttpServletRequest compressedRequest = requestFrom("127.0.0.1");
        compressedRequest.addHeader("X-Forwarded-For", "2001:0DB8:0:0:0:0:0:8");
        MockHttpServletRequest mappedRequest = requestFrom("127.0.0.1");
        mappedRequest.addHeader("X-Forwarded-For", "::FFFF:192.0.2.1");

        assertEquals("2001:db8::8", resolver.resolve(compressedRequest));
        assertEquals("::ffff:c000:201", resolver.resolve(mappedRequest));
    }

    @Test
    void compressedConfiguredProxyStillTrustsEquivalentExpandedPeer() {
        ClientIpResolver resolver = resolver(true, "::1");
        MockHttpServletRequest request = requestFrom("0:0:0:0:0:0:0:1");
        request.addHeader("X-Forwarded-For", "198.51.100.31");

        assertEquals("198.51.100.31", resolver.resolve(request));
    }

    private static ClientIpResolver resolver(boolean useForwardHeaders, String... trustedProxies) {
        TrustedProxyProperties properties = new TrustedProxyProperties();
        properties.setUseForwardHeaders(useForwardHeaders);
        properties.setTrustedProxies(trustedProxies.length == 0
                ? Collections.<String>emptyList()
                : Arrays.asList(trustedProxies));
        return new ClientIpResolver(properties);
    }

    private static MockHttpServletRequest requestFrom(String peerAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peerAddress);
        return request;
    }
}
