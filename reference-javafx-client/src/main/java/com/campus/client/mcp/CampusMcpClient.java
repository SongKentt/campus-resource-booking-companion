package com.campus.client.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps the MCP {@link McpSyncClient} for the Campus server. It connects over the HTTP/SSE
 * transport and exposes simple methods for discovery (tools, resources, prompts) and invocation.
 *
 * <p>This is the class students study most closely: it shows the full MCP client lifecycle &mdash;
 * build a transport, build a client, {@code initialize()} (the JSON-RPC handshake), then list and
 * call capabilities.</p>
 */
public final class CampusMcpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(CampusMcpClient.class);

    private final String baseUrl;
    private McpSyncClient client;

    // Callback that runs when the server disconnects, which is registered in the MainView.
    private Runnable onDisconnect;

    public CampusMcpClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // When the server is disconnected, register a callback
    public void setOnDisconnect(Runnable callback) {
        this.onDisconnect = callback;
    }


    /** Opens the SSE stream and performs the MCP initialize handshake. */
    public McpSchema.InitializeResult connect() {
        // The SSE transport only needs the base URL; it discovers the message endpoint from the
        // server's first "endpoint" SSE event. Current SDK versions require the builder.
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(baseUrl)
                .jsonMapper(new JacksonMcpJsonMapper(JsonMapper.builder().build()))
                .sseEndpoint("/sse")
                .build();

        this.client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .capabilities(ClientCapabilities.builder().build())
                .build();

        McpSchema.InitializeResult init = client.initialize();
        log.info("Connected to '{}' (protocol {})", init.serverInfo().name(), init.protocolVersion());
        return init;
    }

    // This method is to list available tools offered by the mcp and a try catch block is added to trigger disconnection callback on failure
    public List<McpSchema.Tool> listTools() {
        try {
            return client.listTools().tools();
        } catch (Exception e) {
            if (onDisconnect != null){
                onDisconnect.run();
            }
            throw new RuntimeException("Failed to list tools", e);
        }
    }

    // this method is to list available resource offered by the mcp and a try catch block is added to trigger disconnection callback on failure
    public List<McpSchema.Resource> listResources() {
        try {
            return client.listResources().resources();
        } catch (Exception e) {
            if (onDisconnect != null){
                onDisconnect.run();
            }
            throw new RuntimeException("Failed to list resources", e);
        }
    }

    // this method is to list all prompts offered by the mcp and a try catch block is added to trigger disconnection callback on failure
    public List<McpSchema.Prompt> listPrompts() {
        try {
            return client.listPrompts().prompts();
        } catch (Exception e) {
            if (onDisconnect != null) {
                onDisconnect.run();
            }
            throw new RuntimeException("Failed to list prompts", e);
        }
    }

    /** Calls a tool and flattens its text content into one string. */
    // Try-catch block is added to trigger disconnection callback on failure
    public String callTool(String name, Map<String, Object> arguments) {
        log.info("callTool {} {}", name, arguments);
        try {
            CallToolResult result = client.callTool(new CallToolRequest(name, arguments));
            String text = result.content().stream()
                    .filter(c -> c instanceof TextContent)
                    .map(c -> ((TextContent) c).text())
                    .collect(Collectors.joining("\n"));
            return Boolean.TRUE.equals(result.isError()) ? "ERROR: " + text : text;
        } catch (Exception e) {
            if (onDisconnect != null){
                onDisconnect.run();
            }
            throw new RuntimeException("MCP callTool failed for " + name, e);
        }
    }

    /** Reads a resource and returns its concatenated text contents. */
    // Try-catch block is added to trigger disconnection callback on failure
    public String readResource(String uri) {
        try {
            ReadResourceResult result = client.readResource(new ReadResourceRequest(uri));
            return result.contents().stream()
                    .filter(c -> c instanceof TextResourceContents)
                    .map(c -> ((TextResourceContents) c).text())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            if (onDisconnect != null){
                onDisconnect.run();
            }
            throw new RuntimeException("MCP readResource failed for " + uri, e);
        }
    }

    /** Fetches a server-defined prompt template, returning its rendered text. */
    // Try-catch block is added to trigger disconnection callback on failure
    public String getPrompt(String name, Map<String, Object> arguments) {
        try {
            GetPromptResult result = client.getPrompt(new GetPromptRequest(name, arguments));
            return result.messages().stream()
                    .map(m -> m.content() instanceof TextContent tc ? tc.text() : "")
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            if (onDisconnect != null){
                onDisconnect.run();
            }
            throw new RuntimeException("MCP getPrompt failed for " + name, e);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.closeGracefully();
            client = null;
        }
    }
}
