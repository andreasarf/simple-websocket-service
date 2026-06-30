package dev.andreasarf.websocket.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.test.TestRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import dev.andreasarf.websocket.config.AsyncTestConfig;
import dev.andreasarf.websocket.config.RabbitTestConfig;
import dev.andreasarf.websocket.payload.identity.AuthResponse;
import dev.andreasarf.websocket.payload.identity.UserDataResponse;
import dev.andreasarf.websocket.util.FileTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("wstest")
@EnableWireMock({
        @ConfigureWireMock(
                name = "identity-service",
                port = 58882)
})
@Import({RabbitTestConfig.class, AsyncTestConfig.class})
public abstract class BaseWebSocketTest {

    protected static final String WS_URI = "ws://localhost:58881/ws-connect";
    protected static final String TOKEN = "token_" + UUID.randomUUID();
    protected static final UUID CHANNEL_UUID = UUID.fromString("0698f4ff-211e-4849-97c6-7ac9e11c0c9e");
    protected static final UUID CHANNEL_UUID_2 = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    protected static final String IDENTITY_AUTH_PATH = "/api/v1/auth/authenticate";
    protected static final String IDENTITY_USER_DATA_PATH = "/api/v1/user-data/";

    protected BlockingQueue<Object> blockingQueue;
    protected WebSocketStompClient stompClient;
    protected StompSession stompSession;
    protected CompletableFuture<Void> receiptFuture;
    protected CompletableFuture<Object> errorFuture;

    @Autowired
    @Qualifier("testTemplate")
    protected TestRabbitTemplate testTemplate;
    @Autowired
    protected ObjectMapper objectMapper;
    @Qualifier("virtualTaskScheduler")
    @Autowired
    protected TaskScheduler taskScheduler;

    @MockitoSpyBean
    protected SimpMessagingTemplate simpMessagingTemplate;

    @Value("${app.identity.clientSecret}")
    protected String clientSecret;
    @Value("${app.notification.queue}")
    protected String systemQueueName;

    @InjectWireMock("identity-service")
    protected WireMockServer identityWireMockServer;

    @BeforeEach
    public void setup() {
        blockingQueue = new LinkedBlockingQueue<>();

        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))
        ));
        stompClient.setTaskScheduler(this.taskScheduler);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter(objectMapper));

        receiptFuture = new CompletableFuture<>();
        errorFuture = new CompletableFuture<>();
    }

    @AfterEach
    public void tearDown() {
        stompClient.stop();

        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    protected AuthResponse arrangeIdentityAuth() throws IOException {
        final var authResponseJson = FileTestUtils.readFileToString("test_files/auth_response.json");
        final var authResponse = objectMapper.readValue(authResponseJson, AuthResponse.class);
        identityWireMockServer.stubFor(post(urlPathEqualTo(IDENTITY_AUTH_PATH))
                .withHeader(AppHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
                .withHeader(AppHeaders.X_INTERNAL_SECRET, equalTo(clientSecret))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(authResponseJson)));
        return authResponse;
    }

    protected UserDataResponse arrangeIdentityUserData(AuthResponse authResponse) throws IOException {
        return arrangeIdentityUserData(authResponse, "test_files/user_data_response.json");
    }

    protected UserDataResponse arrangeIdentityUserData(AuthResponse authResponse, String userDataFile) throws IOException {
        final var userDataJson = FileTestUtils.readFileToString(userDataFile);
        final var userData = objectMapper.readValue(userDataJson, UserDataResponse.class);
        identityWireMockServer.stubFor(get(urlPathEqualTo(IDENTITY_USER_DATA_PATH + authResponse.getTenantId()))
                .withHeader(AppHeaders.X_INTERNAL_SECRET, equalTo(clientSecret))
                .withQueryParam("email", equalTo(authResponse.getEmail()))
                .withQueryParam("itemUuid", equalTo(authResponse.getItemUuid().toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userDataJson)));
        return userData;
    }
}
